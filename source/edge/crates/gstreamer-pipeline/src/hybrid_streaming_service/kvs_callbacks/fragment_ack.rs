// GRCOV_STOP_COVERAGE
use crossbeam_channel::{unbounded, Receiver, Sender, TrySendError};
use glib::once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};

#[cfg(feature = "sd-card-catchup")]
pub(crate) type OfflineTimeStampMs = u64;

/// The Fragment ack manager is held in static memory so callbacks can access channels to notify realtime + offline client
/// that fragments have persisted in KVS cloud.  Used for realtime callbacks.
pub(super) static KVS_FRAGMENT_REALTIME_ACK_MANAGER: Lazy<Arc<Mutex<RealtimeKVSFrameAckManager>>> =
    Lazy::new(|| {
        let (sender, receiver) = unbounded::<KVSReceiver>();
        let manager = RealtimeKVSFrameAckManager { sender, receiver };
        Arc::new(Mutex::new(manager))
    });

/// Blocking but gets the global kvs fragment ack receiver channel.
pub(crate) fn get_kvs_fragment_rx_channel_for_realtime() -> Receiver<KVSReceiver> {
    let mut fragment_ack_lock =
        KVS_FRAGMENT_REALTIME_ACK_MANAGER.lock().expect("KVS Fragment Ack Mutex is poisoned.");
    fragment_ack_lock.get_callback_receiver()
}

/// The Fragment ack manager is held in static memory so callbacks can access channels to notify realtime + offline client
/// that fragments have persisted in KVS cloud.  Used for offline callbacks.
#[cfg(feature = "sd-card-catchup")]
pub(super) static KVS_FRAGMENT_OFFLINE_ACK_MANAGER: Lazy<Arc<Mutex<OfflineKVSFrameAckManager>>> =
    Lazy::new(|| {
        let (sender, receiver) = unbounded::<OfflineTimeStampMs>();
        let manager = OfflineKVSFrameAckManager { sender, receiver };
        Arc::new(Mutex::new(manager))
    });
/// Blocking but gets the global kvs fragment ack receiver channel.
#[cfg(feature = "sd-card-catchup")]
pub(crate) fn get_kvs_fragment_rx_channel_for_offline() -> Receiver<OfflineTimeStampMs> {
    let mut fragment_ack_lock =
        KVS_FRAGMENT_OFFLINE_ACK_MANAGER.lock().expect("KVS Fragment Ack Mutex is poisoned.");
    fragment_ack_lock.get_callback_receiver()
}

// creating a enum to support multiple data type on receiver end for segregating if camera recording status.
#[derive(Debug)]
pub enum KVSReceiver {
    FrameTimeCode(u64),
    Disconnected(String),
}

impl KVSReceiver {
    pub fn into_u64(self) -> u64 {
        match self {
            KVSReceiver::FrameTimeCode(code) => code,
            _ => panic!("Expected FrameTimeCode variant"),
        }
    }
}

/// Struct to hold frame data in memory till frame acknowledgement is received from KVS.
#[cfg(feature = "sd-card-catchup")]
pub(crate) struct OfflineKVSFrameAckManager {
    sender: Sender<u64>,
    receiver: Receiver<u64>,
}

#[cfg(feature = "sd-card-catchup")]
impl OfflineKVSFrameAckManager {
    pub(crate) fn get_callback_receiver(&mut self) -> Receiver<u64> {
        self.receiver.clone()
    }

    pub(crate) fn try_send_persisted_fragment_time_code(
        &mut self,
        persisted_fragment_time_code: u64,
    ) -> Result<(), TrySendError<u64>> {
        self.sender.try_send(persisted_fragment_time_code)
    }
}

/// Struct to hold frame data in memory till frame acknowledgement is received from KVS.
pub(crate) struct RealtimeKVSFrameAckManager {
    sender: Sender<KVSReceiver>,
    receiver: Receiver<KVSReceiver>,
}

impl RealtimeKVSFrameAckManager {
    pub(crate) fn get_callback_receiver(&mut self) -> Receiver<KVSReceiver> {
        self.receiver.clone()
    }
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
