// GRCOV_STOP_COVERAGE
use async_trait::async_trait;
use base64::{engine::general_purpose, Engine as _};
use device_traits::channel_utils::error::ChannelUtilError;
use device_traits::channel_utils::traits::IoTServiceSender;
use http_client::client::HttpClient;
use serde_json::json;
use sha256::digest;
use snapshot_traits::SnapshotHandler;
use std::error::Error;
use tracing::{debug, error, info, warn};

/// Struct to request snapshots
pub struct Snapshot<T> {
    pub iot_message_sender: Box<dyn IoTServiceSender + Send + Sync>,
    pub http_client: T,
    pub portal_user: String,
    pub portal_pass: String,
    pub data: Option<Vec<u8>>,
}

impl<T> Snapshot<T>
where
    T: HttpClient + std::fmt::Debug,
{
    pub fn new(
        iot_message_sender: Box<dyn IoTServiceSender + Send + Sync>,
        http_client: T,
        portal_user: String,
        portal_pass: String,
    ) -> Self {
        Self { iot_message_sender, http_client, portal_user, portal_pass, data: None }
    }

    // Format payload for timeline generation
    fn format_payload(checksum: String, size_of_bytes: usize) -> String {
        json!({"checksum":checksum,"contentLength":size_of_bytes}).to_string()
    }
}

#[async_trait]
impl<T> SnapshotHandler for Snapshot<T>
where
    T: HttpClient + std::fmt::Debug + Send + Sync,
{
    async fn try_send_request_presigned_url_to_iot(
        &mut self,
        snapshot_uri: String,
    ) -> Result<(), Box<dyn Error>> {
        // Client Id is obtained from global memory, only activates a mutex the first time
        let client_id = self.iot_message_sender.get_client_id()?;
        // Format topic and payload.
        let topic = format!("videoanalytics/{}/snapshot", client_id);

        if let Ok(image_data) = self.fetch_data_from_snapshot_uri(snapshot_uri.to_owned()).await {
            self.data = Some(image_data.clone());
            let sha_encoded = digest(image_data.clone());
            let sha_encoded_as_bytes = hex::decode(sha_encoded).unwrap();
            let checksum = general_purpose::STANDARD.encode(sha_encoded_as_bytes);
            let payload = Self::format_payload(checksum, image_data.len());
            // Try send message.  Will fail if the buffer is full.
            match self.iot_message_sender.try_build_and_send_iot_message(topic.as_str(), payload) {
                Ok(_) => {
                    debug!("Sent request for presigned url to iot");
                }
                Err(ChannelUtilError::BufferFullError) => {
                    error!(
                        "IoT service channel is full, unable to send request to fetch presigned url"
                    );
                }
                Err(e) => {
                    error!("{:?}", e);
                }
            };
        } else {
            warn!("Failed to fetch data from snapshot uri: {:?}", snapshot_uri.to_owned());
        }

        Ok(())
    }

    async fn fetch_data_from_snapshot_uri(
        &mut self,
        snapshot_uri: String,
    ) -> Result<Vec<u8>, Box<dyn Error>> {
        let resp = self
            .http_client
            .send_http_get_request_with_basic_auth(
                snapshot_uri,
                self.portal_user.clone(),
                self.portal_pass.clone(),
            )
            .await?;
        let bytes = resp.bytes().await?;
        Ok(bytes.to_vec())
    }

    async fn upload_snapshot_to_presigned_url(
        &mut self,
        presigned_url: String,
    ) -> Result<(), Box<dyn Error>> {
        let http_resp = self
            .http_client
            .send_http_request_put_for_presigned_url(
                presigned_url,
                self.data.clone().expect("No thumbnail data stored"),
            )
            .await?;
        info!("response text {:?}", http_resp.text().await);
        Ok(())
    }
}
