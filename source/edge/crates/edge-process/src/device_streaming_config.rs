//! This module contains the dependency injection logic for Streaming models
//! Currently this only supports the OnvifClient
use http_client::client::HttpClientImpl;
use onvif_client::client::onvif_client::OnvifClient;
use streaming_traits::VideoStreamConsumer;

/// Dependency injection for the VideoStreamingConsumer trait.
pub fn get_device_streaming_config_instance(
    http_client: reqwest::Client,
) -> Box<dyn VideoStreamConsumer> {
    Box::new(OnvifClient::new(HttpClientImpl::new(http_client)))
}
