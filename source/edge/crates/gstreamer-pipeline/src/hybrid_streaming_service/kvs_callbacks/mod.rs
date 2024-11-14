// Holds static structs needed for callbacks to kvs plugin.
// GRCOV_STOP_COVERAGE : Not testable with unit tests.
pub(crate) mod fragment_ack;

use crate::hybrid_streaming_service::kvs_callbacks::fragment_ack::{
    KVSReceiver, RealtimeKVSFrameAckManager, KVS_FRAGMENT_REALTIME_ACK_MANAGER,
};
use glib::ObjectType;
use gstreamer::{ffi, Element};
use std::mem;
use std::sync::{Arc, Mutex};
use tracing::{debug, error, warn};

#[allow(unused)]
pub(crate) const FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_UNDEFINED: FragmentAckType = 0;
#[allow(unused)]
pub(crate) const FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_BUFFERING: FragmentAckType = 1;
#[allow(unused)]
pub(crate) const FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_RECEIVED: FragmentAckType = 2;
#[allow(unused)]
pub(crate) const FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_PERSISTED: FragmentAckType = 3;
#[allow(unused)]
pub(crate) const FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_ERROR: FragmentAckType = 4;
#[allow(unused)]
pub(crate) const FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_IDLE: FragmentAckType = 5;

#[doc = " Fragment ACK type enum"]
pub type FragmentAckType = ::std::os::raw::c_uint;

/// C struct that gets passed in to fragment ack callback.
#[repr(C)]
#[allow(non_snake_case)]
#[derive(Debug, Copy, Clone)]
pub struct __FragmentAck {
    pub version: u32,
    pub ackType: std::os::raw::c_uint,
    pub timestamp: u64,
    pub sequenceNumber: [std::os::raw::c_char; 129usize],
    pub result: std::os::raw::c_uint,
}

/// Fragment ack callback for Real time cloud ingestion.
/// Use of unsafe rust is required for this callback as it takes in C style structs.
/// Never access the glib pointer or the _sink variable.  Only access the fragment_ack pointer.
unsafe extern "C" fn fragment_ack_callback_realtime_c_abi(
    _sink: *mut ffi::GstElement,
    fragment_ack: *mut __FragmentAck,
    _: glib::Pointer,
) {
    if fragment_ack.is_null() {
        warn!("KVS real time client passed in a null value for Fragment Ack!");
        return;
    }
    // Extern C methods should be unsafe as C can pass in null pointers which can conflict with safe rust.
    // To take full advantage of Rust compiler core logic is held within an inline function to minimize the
    // unsafe sections of code to accessing C pointers only.
    // NEVER USE ANY PARAMETER PASSED IN OTHER THAN FRAGMENT_ACK!
    // Inject KVS REALTIME manager so callback sends messages on the correct channel.
    fragment_ack_callback(fragment_ack, KVS_FRAGMENT_REALTIME_ACK_MANAGER.clone());
}

#[inline]
fn fragment_ack_callback(
    fragment_ack: *mut __FragmentAck,
    fragment_manager: Arc<Mutex<RealtimeKVSFrameAckManager>>,
) {
    match unsafe { (*fragment_ack).ackType } {
        FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_BUFFERING
        | FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_RECEIVED => {
            // Do nothing, refer to: https://tiny.amazon.com/4pyvezkb/githawslamazblobmastdocsstru
        }
        FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_UNDEFINED
        | FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_ERROR
        | FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_IDLE => {
            let Ok(mut fragment_ack_manager_lock) = fragment_manager.lock() else {
                warn!(
                    "KVS Fragment was faulty, time-code = {}, ackType = {}",
                    unsafe { (*fragment_ack).timestamp },
                    unsafe { (*fragment_ack).ackType }
                );
                return;
            };
            let error_msg = format!(
                "error sending timeframe, streaming stopped. time-code = {}, ackType = {}",
                unsafe { (*fragment_ack).timestamp },
                unsafe { (*fragment_ack).ackType }
            );
            match fragment_ack_manager_lock
                .try_send_error(KVSReceiver::Disconnected(error_msg.to_string()))
            {
                Ok(_) => debug!("Published error to kvs_fragment_ack channel."),
                Err(e) => error!("Message failed to publish to callback channel : {:?}", e),
            };
        }
        // Generate timeline information
        FRAGMENT_ACK_TYPE_FRAGMENT_ACK_TYPE_PERSISTED => {
            let Ok(mut fragment_ack_manager_lock) = fragment_manager.lock() else {
                warn!("Error in acquiring lock on kvs_frame_ack_manager in KVS Frame ACK manager for \
                fragment ack callback");
                return;
            };
            let ack_fragment_timestamp = unsafe { (*fragment_ack).timestamp };
            match fragment_ack_manager_lock
                .try_send_persisted_fragment_time_code( KVSReceiver::FrameTimeCode(ack_fragment_timestamp))
            {
                Ok(_) => debug!("KVS Fragment realtime published persisted time-code to kvs_fragment_ack, time-code = {} channel.", unsafe { (*fragment_ack).timestamp }),
                Err(e) => error!("Message failed to publish to callback channel : {:?}", e),
            };
        }
        _ => {
            // Add other fragment ack handling if needed
        }
    };
}

/// Unsafe rust is required due to direct glib bindings (C library)
/// This method passes in a pointer to our callback function.
/// This will be called by the KVS sync plugin
pub(crate) fn setup_kvs_fragment_ack_callback_realtime(kvssink: &Element) {
    let f = Box::new(fragment_ack_callback_realtime_c_abi);
    unsafe {
        glib::signal::connect_raw(
            kvssink.as_ptr() as *mut _,
            b"fragment-ack\0".as_ptr() as *const _,
            Some(mem::transmute::<_, unsafe extern "C" fn()>(
                fragment_ack_callback_realtime_c_abi as *const (),
            )),
            Box::into_raw(f),
        )
    };
}
