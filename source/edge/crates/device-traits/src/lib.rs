/// Utils to organize tokio channels in the system.
pub mod channel_utils;

/// This mod will hold traits related to communications with AWS Cloud services
pub mod connections;
pub mod merge;

use async_trait::async_trait;
use mockall::automock;
use serde_json::Value;
use std::error::Error;
use std::fmt::Debug;

/// This is the DeviceStateModel trait.  It is used by the View (the device client)
/// to inject dependencies that gather dependencies for a specific set of devices.
#[automock]
#[async_trait]
pub trait DeviceStateModel: Debug {
    /// bootstrap DeviceStateModel
    async fn bootstrap(
        &mut self,
        config_path: &str,
        ip_address: String,
    ) -> Result<(), Box<dyn Error>>;

    ///Get device information in json String format
    async fn get_device_information(&mut self) -> Result<Value, Box<dyn Error>>;
}
