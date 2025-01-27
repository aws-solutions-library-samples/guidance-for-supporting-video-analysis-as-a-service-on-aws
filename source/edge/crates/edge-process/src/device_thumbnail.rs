//! This module contains the dependency injection logic for thumbnail models
//! Currently this only supports the OnvifClient

use device_traits::channel_utils::ServiceCommunicationManager;
use http_client::client::HttpClientImpl;
use onvif_client::client::onvif_client::OnvifClient;
use snapshot_client::snapshot_handler::Snapshot;
use snapshot_traits::{SnapshotConsumer, SnapshotHandler};

/// Dependency injection for the SnapshotConsumer trait.
pub fn get_device_snapshot_config_instance(
    http_client: reqwest::Client,
) -> Box<dyn SnapshotConsumer + Send + Sync> {
    Box::new(OnvifClient::new(HttpClientImpl::new(http_client)))
}

/// Dependency injection for Snapshots
pub fn get_device_snapshot_instance(
    http_client: reqwest::Client,
    portal_user: String,
    portal_pass: String,
) -> Box<dyn SnapshotHandler + Send + Sync> {
    Box::new(Snapshot::new(
        Box::<ServiceCommunicationManager>::default(),
        HttpClientImpl::new(http_client),
        portal_user,
        portal_pass,
    ))
}
