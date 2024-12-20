use crate::constants::MAX_FRAGMENTS;
#[cfg(feature = "sd-card-catchup")]
use crate::data_storage::video_storage::FileMetadataStorage;
use crate::hybrid_streaming_service::fragment::FragmentManager;
use crate::hybrid_streaming_service::frame::Frame;
use crate::hybrid_streaming_service::kvs_callbacks::fragment_ack::{
    get_kvs_fragment_rx_channel_for_realtime, KVSReceiver,
};
use event_processor::constants::{DATA_KEY, SIMPLE_ITEM_KEY, VALUE_KEY};
use serde_json::Value;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::mpsc::{Receiver, RecvTimeoutError, SyncSender};
use std::sync::Arc;
#[cfg(feature = "sd-card-catchup")]
use std::sync::Mutex;
use std::thread::JoinHandle;
use std::time::Duration;
use tracing::{debug, error, info, warn};

#[derive(Debug)]
pub(crate) struct ForwardingService {
    // Main event loop, handle held in struct to be checked by watchdog thread.
    _event_loop: JoinHandle<()>,
    // Event loop that handles incoming motion detection events.
    _motion_detection_event_loop: JoinHandle<()>,
    // Loop handles callbacks from KVS client.
    _callback_loop: JoinHandle<()>,
    // Thread safe object to organize incoming frames as video clips + forward + store
    _fragment_manager: FragmentManager,
    // Used to cancel threads when struct is dropped.
    cancellation_token: Arc<AtomicBool>,
}

impl ForwardingService {
    #[cfg(not(feature = "sd-card-catchup"))]
    pub fn new(
        rtsp_buffer: Receiver<Arc<Frame>>,
        realtime_tx: SyncSender<Arc<Frame>>,
        motion_based_streaming_rx: Receiver<String>,
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
        let _motion_detection_event_loop = Self::setup_motion_detection_event_loop(
            fragment_manager.clone(),
            cancellation_token.clone(),
            motion_based_streaming_rx,
        );
        let _callback_loop = Self::setup_callback_loop(
            cancellation_token.clone(),
            fragment_manager.clone(),
            fragment_ack_rx,
        );

        ForwardingService {
            _event_loop,
            _motion_detection_event_loop,
            _callback_loop,
            cancellation_token,
            _fragment_manager: fragment_manager,
        }
    }

    #[cfg(feature = "sd-card-catchup")]
    pub fn new(
        rtsp_buffer: Receiver<Arc<Frame>>,
        realtime_tx: SyncSender<Arc<Frame>>,
        motion_based_streaming_rx: Receiver<String>,
        database_client: Arc<Mutex<FileMetadataStorage>>,
    ) -> ForwardingService {
        let cancellation_token = Arc::new(AtomicBool::new(false));
        let fragment_ack_rx = get_kvs_fragment_rx_channel_for_realtime();
        // Thread safe struct, clones are shallow copies.
        let fragment_manager = FragmentManager::new(realtime_tx, MAX_FRAGMENTS, database_client);
        let _event_loop = Self::setup_event_loop(
            rtsp_buffer,
            fragment_manager.clone(),
            cancellation_token.clone(),
        );
        let _motion_detection_event_loop = Self::setup_motion_detection_event_loop(
            fragment_manager.clone(),
            cancellation_token.clone(),
            motion_based_streaming_rx,
        );
        let _callback_loop = Self::setup_callback_loop(
            cancellation_token.clone(),
            fragment_manager.clone(),
            fragment_ack_rx,
        );

        ForwardingService {
            _event_loop,
            _motion_detection_event_loop,
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

    fn setup_motion_detection_event_loop(
        mut fragment_manager: FragmentManager,
        cancellation_token: Arc<AtomicBool>,
        motion_based_streaming_rx: Receiver<String>,
    ) -> JoinHandle<()> {
        std::thread::spawn(move || {
            while !cancellation_token.load(Ordering::Relaxed) {
                match motion_based_streaming_rx.recv_timeout(Duration::from_secs(1)) {
                    Ok(motion_event) => {
                        debug!("Motion based event was received {:?}", motion_event);
                        if let Ok(event_value) =
                            serde_json::from_str::<Value>(motion_event.as_str())
                        {
                            if let Some(motion_bool) =
                                event_value[DATA_KEY][SIMPLE_ITEM_KEY][VALUE_KEY].as_bool()
                            {
                                fragment_manager.handle_motion_detection(motion_bool);
                            }
                        }
                    }
                    Err(RecvTimeoutError::Timeout) => {
                        info!("Motion detection channel did not send event within timeout.");
                        continue;
                    }
                    Err(RecvTimeoutError::Disconnected) => {
                        warn!("Motion detection channel disconnected. Stopping Thread");
                        break;
                    }
                }
            }
        })
    }

    /// Loop checks for realtime client fragment ack callbacks.
    fn setup_callback_loop(
        cancellation_token: Arc<AtomicBool>,
        mut fragment_manager: FragmentManager,
        fragment_ack_rx: crossbeam_channel::Receiver<KVSReceiver>,
    ) -> JoinHandle<()> {
        std::thread::spawn(move || {
            while !cancellation_token.load(Ordering::Relaxed) {
                let mut streaming_status: bool = false;
                let mut ack_time_code: u64 = 0;

                // Timeout so loop will check token periodically and shutdown thread when service is dropped.
                match fragment_ack_rx.recv_timeout(Duration::from_secs(1)) {
                    Ok(payload) => {
                        if let KVSReceiver::FrameTimeCode { .. } = payload {
                            streaming_status = true;
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

                if streaming_status {
                    let Some(_fragment) = fragment_manager.remove_fragment(ack_time_code) else {
                        info!("Fragment Ack for fragment that does not exist in manager.");
                        continue;
                    };
                }
            }
        })
    }
}

/// Threads must check this token in their loops.
impl Drop for ForwardingService {
    fn drop(&mut self) {
        self.cancellation_token.store(true, Ordering::Relaxed);
    }
}
