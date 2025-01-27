use async_trait::async_trait;
use mockall::automock;
use std::error::Error;

/// This trait is responsible to get stream URI for video from device.
#[automock]
#[async_trait]
pub trait SnapshotConsumer {
    /// set up services uri
    async fn set_up_services_uri(&mut self, ip_address: String) -> Result<(), Box<dyn Error>>;
    /// get snapshot uri
    async fn get_snapshot_uri(&mut self) -> Result<String, Box<dyn Error>>;
    /// Set new credentials for onvif.
    async fn bootstrap(&mut self, username: String, password: String)
        -> Result<(), Box<dyn Error>>;
}

#[automock]
#[async_trait]
pub trait SnapshotHandler {
    async fn try_send_request_presigned_url_to_iot(
        &mut self,
        snapshot_uri: String,
    ) -> Result<(), Box<dyn Error>>;

    async fn fetch_data_from_snapshot_uri(
        &mut self,
        snapshot_uri: String,
    ) -> Result<Vec<u8>, Box<dyn Error>>;

    async fn upload_snapshot_to_presigned_url(
        &mut self,
        presigned_url: String,
    ) -> Result<(), Box<dyn Error>>;
}
