// GRCOV_STOP_COVERAGE
use crossbeam_channel::{unbounded, Receiver, Sender, TrySendError};
use glib::once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};

/// The Fragment ack manager is held in static memory so callbacks can access channels to notify realtime + offline client
/// that fragments have persisted in KVS cloud.  Used for realtime callbacks.
pub(super) static KVS_FRAGMENT_REALTIME_ACK_MANAGER: Lazy<Arc<Mutex<RealtimeKVSFrameAckManager>>> =
    Lazy::new(|| {
        let (sender, receiver) = unbounded::<KVSReceiver>();
        let manager = RealtimeKVSFrameAckManager { sender, receiver };
        Arc::new(Mutex::new(manager))
    });

// creating a enum to support multiple data type on receiver end for segregating if camera recording status.
#[derive(Debug)]
pub enum KVSReceiver {
    FrameTimeCode(u64),
    Disconnected(String),
}

/// Struct to hold frame data in memory till frame acknowledgement is received from KVS.
pub(crate) struct RealtimeKVSFrameAckManager {
    sender: Sender<KVSReceiver>,
    receiver: Receiver<KVSReceiver>,
}

impl RealtimeKVSFrameAckManager {
    pub(crate) fn try_send_persisted_fragment_time_code(
        &mut self,
        persisted_fragment_time_code: KVSReceiver,
    ) -> Result<(), TrySendError<KVSReceiver>> {
        self.sender.try_send(persisted_fragment_time_code)
    }

    pub(crate) fn try_send_error(
        &mut self,
        error_msg: KVSReceiver,
    ) -> Result<(), TrySendError<KVSReceiver>> {
        self.sender.try_send(error_msg)
    }
}
