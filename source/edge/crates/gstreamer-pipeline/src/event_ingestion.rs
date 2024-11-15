use crate::hybrid_streaming_service::HybridStreamingService;
use device_traits::channel_utils::error::ChannelUtilError;
use device_traits::channel_utils::ServiceCommunicationManager;
use std::sync::mpsc::Receiver;
use streaming_traits::{StreamUriConfiguration, StreamingServiceConfigurations};

/// Create streaming service.
pub fn create_streaming_service(
    stream_uri_configuration: StreamUriConfiguration,
    motion_based_streaming_rx: Receiver<String>,
) -> HybridStreamingService {
    let configs = get_pipeline_config(stream_uri_configuration).unwrap();
    HybridStreamingService::new(configs, motion_based_streaming_rx)
        .expect("Failed to create streaming service.")
}

// Get iot info from the global messaging service and
// the stream_uri_configuration to generate PipelineConfigurations.
fn get_pipeline_config(
    stream_uri_configuration: StreamUriConfiguration,
) -> Result<StreamingServiceConfigurations, ChannelUtilError> {
    // Confirm that other components can get the generated config object which is deserialize from the input config.
    let mut get_configs = ServiceCommunicationManager::default();
    let configs = get_configs.get_configurations()?;

    Ok(StreamingServiceConfigurations {
        rtsp_uri: stream_uri_configuration.rtsp_uri,
        username: stream_uri_configuration.username,
        password: stream_uri_configuration.password,
        ca_cert_path: configs.ca_path.to_owned(),
        iot_cert_path: configs.cert_path.to_owned(),
        iot_endpoint: configs.credential_endpoint.to_owned(),
        private_key_path: configs.key_path.to_owned(),
        role_alias: configs.role_aliases.to_owned(),
        stream_name: configs.client_id.to_owned(),
        aws_region: configs.aws_region.to_owned(),
        region: configs.aws_region.to_owned(),
    })
}
