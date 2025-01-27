//! This module contains the dependency injection logic for device models
//! Currently this only supports the OnvifClient

use device_traits::DeviceStateModel;
use http_client::client::HttpClientImpl;
use onvif_client::client::onvif_client::OnvifClient;

/// Dependency injection for the DeviceStateModel trait.  This conditionally
/// Compiles as default and implements the Onvif feature.
// #[cfg(feature = "default")]
pub fn get_device_model(http_client: reqwest::Client) -> Box<dyn DeviceStateModel + Send + Sync> {
    Box::new(OnvifClient::new(HttpClientImpl::new(http_client)))
}
