use crate::constants::MAX_FRAGMENTS;
use crate::hybrid_streaming_service::fragment::FragmentManager;
use crate::hybrid_streaming_service::frame::Frame;
use crate::hybrid_streaming_service::kvs_callbacks::fragment_ack::{
    get_kvs_fragment_rx_channel_for_realtime, KVSReceiver,
};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::mpsc::{Receiver, RecvTimeoutError, SyncSender};
use std::sync::Arc;
use std::thread::JoinHandle;
use std::time::Duration;
use tracing::{debug, error, info, warn};

#[derive(Debug)]
pub(crate) struct ForwardingService {
    // Main event loop, handle held in struct to be checked by watchdog thread.
    _event_loop: JoinHandle<()>,
    // Loop handles callbacks from KVS client.
    _callback_loop: JoinHandle<()>,
    // Thread safe object to organize incoming frames as video clips + forward + store
    _fragment_manager: FragmentManager,
    // Used to cancel threads when struct is dropped.
    cancellation_token: Arc<AtomicBool>,
}

impl ForwardingService {
    pub fn new(
        rtsp_buffer: Receiver<Arc<Frame>>,
        realtime_tx: SyncSender<Arc<Frame>>,
    ) -> ForwardingService {
        let cancellation_token = Arc::new(AtomicBool::new(false));
        let fragment_ack_rx = get_kvs_fragment_rx_channel_for_realtime();
        // Thread safe struct, clones are shallow copies.
        let fragment_manager = FragmentManager::new(realtime_tx, MAX_FRAGMENTS);
        let _event_loop = Self::setup_event_loop(
            rtsp_buffer,
            fragment_manager.clone(),
            cancellation_token.clone(),
        );
        let _callback_loop = Self::setup_callback_loop(cancellation_token.clone(), fragment_ack_rx);

        ForwardingService {
            _event_loop,
            _callback_loop,
            cancellation_token,
            _fragment_manager: fragment_manager,
        }
    }

    fn setup_event_loop(
        rtsp_buffer: Receiver<Arc<Frame>>,
        mut fragment_manager: FragmentManager,
        cancellation_token: Arc<AtomicBool>,
    ) -> JoinHandle<()> {
        std::thread::spawn(move || {
            while !cancellation_token.load(Ordering::Relaxed) {
                // Try and pull frame from RTSP buffer.  Need timeout or memory leak can occur
                // load into fragment_manager
                let frame = match rtsp_buffer.recv_timeout(Duration::from_secs(1)) {
                    Ok(frame) => frame,
                    Err(RecvTimeoutError::Timeout) => {
                        info!("RTSP Stream did not send frame within timeout.");
                        continue;
                    }
                    Err(RecvTimeoutError::Disconnected) => {
                        warn!("RTSP channel disconnected. Stopping Thread");
                        break;
                    }
                };

                // Push frame to fragment manager.
                fragment_manager.add_frame(frame);
            }
        })
    }

    /// Loop checks for realtime client fragment ack callbacks.
    fn setup_callback_loop(
        cancellation_token: Arc<AtomicBool>,
        fragment_ack_rx: crossbeam_channel::Receiver<KVSReceiver>,
    ) -> JoinHandle<()> {
        std::thread::spawn(move || {
            while !cancellation_token.load(Ordering::Relaxed) {
                let ack_time_code: u64;

                // Timeout so loop will check token periodically and shutdown thread when service is dropped.
                match fragment_ack_rx.recv_timeout(Duration::from_secs(1)) {
                    Ok(payload) => {
                        if let KVSReceiver::FrameTimeCode { .. } = payload {
                            ack_time_code = payload.into_u64();
                            debug!("Received time code to persist {:?}", ack_time_code);
                        }
                    }
                    Err(crossbeam_channel::RecvTimeoutError::Timeout) => {
                        debug!("Realtime fragment ack loop timeout.");
                        continue;
                    }
                    Err(crossbeam_channel::RecvTimeoutError::Disconnected) => {
                        error!("Realtime Fragment Ack Channel closed unexpectedly.");
                        break;
                    }
                };

                debug!("Forwarding Service Callback received.");
            }
        })
    }
}
