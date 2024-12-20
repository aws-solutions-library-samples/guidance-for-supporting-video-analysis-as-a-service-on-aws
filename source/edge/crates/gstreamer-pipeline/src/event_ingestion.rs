use crate::event_processor::get_event_processor_client;
use crate::hybrid_streaming_service::HybridStreamingService;
use crate::metadata_streaming_pipeline::Pipeline as EventPipeline;
use device_traits::channel_utils::error::ChannelUtilError;
use device_traits::channel_utils::traits::IoTServiceSender;
use device_traits::channel_utils::ServiceCommunicationManager;
use device_traits::state::{State, StateManager};
use std::collections::VecDeque;
use std::error::Error;
use std::sync::mpsc::{Receiver, SyncSender, TrySendError};
use std::sync::{Arc, Condvar, Mutex};
use std::thread::JoinHandle as ThreadJoinHandle;
use std::time::Duration;
use streaming_traits::{StreamUriConfiguration, StreamingPipeline, StreamingServiceConfigurations};
use tokio::task::JoinHandle as TokioJoinHandle;
use tokio::time::sleep as tokio_sleep;
use tracing::{debug, error};
use video_analytics_client::video_analytics_client::VideoAnalyticsClient;

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
#[cfg(not(feature = "sd-card-catchup"))]
fn get_pipeline_config(
    stream_uri_configuration: StreamUriConfiguration,
) -> Result<StreamingServiceConfigurations, ChannelUtilError> {
    // Confirm that other components can get the generated config object which is deserialized from the input config.
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

#[cfg(feature = "sd-card-catchup")]
fn get_pipeline_config(
    stream_uri_configuration: StreamUriConfiguration,
) -> Result<StreamingServiceConfigurations, ChannelUtilError> {
    // Confirm that other components can get the generated config object which is deserialized from the input config.
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
        local_storage_path: configs.local_storage_path.to_owned(),
        db_path: configs.db_path.to_owned(),
        local_storage_disk_usage: configs.local_storage_disk_usage,
    })
}

/// Ensures the pipeline is started/stopped according to device state
pub async fn ensure_streaming_pipeline_in_correct_state(
    streaming_pipeline: &mut (dyn StreamingPipeline + Send + Sync),
    state: State,
) {
    match state {
        // Device has been enabled
        State::CreateOrEnableSteamingResources => {
            streaming_pipeline.ensure_start_pipeline().await;
        }
        // Device has been disabled
        State::DisableStreamingResources => {
            streaming_pipeline.ensure_stop_pipeline().await;
        }
    }
}

/// Setup AI Event Ingestion pipeline for edge process.  Will send messages for publish to Video Analytics Cloud.
pub async fn initiate_event_ingestion(
    stream_uri_config: StreamUriConfiguration,
    motion_based_streaming_tx: SyncSender<String>,
) -> Result<(ThreadJoinHandle<()>, TokioJoinHandle<()>), Box<dyn Error>> {
    let shared_buffer_for_ai_events = Arc::new((Mutex::new(VecDeque::new()), Condvar::new()));
    let buffer = shared_buffer_for_ai_events.clone();
    let mut event_pipeline = EventPipeline::new(buffer).await?;

    // Run gstreamer pipeline to consume AI events from RTSP server - metadata stream.
    let stream_uri_config_clone = stream_uri_config.clone();
    let tokio_join_handle = tokio::spawn(async move {
        event_pipeline
            .create_pipeline(stream_uri_config_clone)
            .await
            .expect("Metadata Streaming Pipeline creation failed!");
        loop {
            ensure_streaming_pipeline_in_correct_state(
                &mut event_pipeline,
                StateManager::get_state(),
            )
            .await;
            tokio_sleep(Duration::from_millis(250)).await;
        }
    });
    // Need to have this thread inside of the tokio runtime since we have a tokio async function
    let rt = tokio::runtime::Runtime::new().unwrap();
    let video_analytics_client = VideoAnalyticsClient::from_conf().await;
    let mut iot_message_service = Box::<ServiceCommunicationManager>::default();
    let thread_join_handle = std::thread::spawn(move || {
        let event_processor_client = get_event_processor_client();
        let rt = rt;
        let mut video_analytics_client = video_analytics_client;
        loop {
            // Lock buffer to setup Condvar.  Poisoned mutexes cannot be recovered.
            let buffer =
                shared_buffer_for_ai_events.0.lock().expect("AI Events buffer mutex poisoned!");

            // Condvar has timeout to prevent threads sleeping indefinitely
            let mut buffer = match shared_buffer_for_ai_events
                .1
                .wait_timeout(buffer, Duration::from_millis(5000))
            {
                // If times out continue
                Ok(guard_and_timout) if guard_and_timout.1.timed_out() => {
                    continue;
                }
                // If Condvar is tripped return mutex guard
                Ok(guard_and_timout) => guard_and_timout.0,
                // Guard is poisoned, program should panic.
                Err(_e) => panic!("Video router buffer mutex poisoned!"),
            };

            // Send messages for events in the buffer.
            while let Some(data) = buffer.pop_front() {
                let data_clone = data.clone();
                match event_processor_client.get_motion_based_event(data_clone) {
                    Ok(Some(event)) => {
                        match motion_based_streaming_tx.try_send(event.to_string()) {
                            Ok(_) => {
                                debug!("Motion based event was sent");
                                continue;
                            }
                            // If internet connection slow or disconnected this buffer can fill up
                            // This is normal behavior in the real world.
                            Err(TrySendError::Full(_)) => {
                                debug!("Motion based event channel full.");
                            }
                            // If channel breaks it means the Stream service is being shutdown
                            // or an unrecoverable error has occurred.
                            Err(TrySendError::Disconnected(_)) => {
                                error!("Motion based channel disconnected. Stopping Thread");
                            }
                        }
                    }
                    Ok(None) => {}
                    Err(err) => {
                        error!("Received invalid event! : {:?}", err);
                        continue;
                    }
                }
                let processed_event = match event_processor_client.post_process_event(data) {
                    Ok(event) if !event.is_empty() => event,
                    Ok(_event) => {
                        debug!("Received an event which did not map to a payload!");
                        continue;
                    }
                    Err(err) => {
                        error!("Received invalid event! : {:?}", err);
                        continue;
                    }
                };

                // blocking on async to execute async code inside of a sync thread
                rt.block_on(async {
                    // Try and send message
                    match try_send_ai_event_to_cloud(
                        &mut video_analytics_client,
                        iot_message_service.get_client_id().expect("Unable to fetch client id"),
                        processed_event.clone(),
                    )
                    .await
                    {
                        Ok(_) => {
                            debug!("Sent ai event to cloud.");
                        }
                        Err(e) => {
                            error!("Failed to send ai event {:?}", e);
                        }
                    }
                });
            }
        }
    });
    // Return join handles
    Ok((thread_join_handle, tokio_join_handle))
}

async fn try_send_ai_event_to_cloud(
    video_analytics_client: &mut VideoAnalyticsClient,
    client_id: String,
    processed_event: String,
) -> Result<(), Box<dyn Error>> {
    return video_analytics_client
        .import_media_object(client_id, processed_event.into_bytes())
        .await;
}

#[cfg(test)]
///Unit tests for utils methods
mod tests {
    use super::*;
    use streaming_traits::MockStreamingPipeline;

    #[tokio::test]
    async fn test_ensure_streaming_pipeline_in_correct_state_enable_or_create() {
        let mut mock_streaming_pipeline = MockStreamingPipeline::new();
        let _ = mock_streaming_pipeline.expect_ensure_start_pipeline().once().returning(|| ());
        ensure_streaming_pipeline_in_correct_state(
            &mut mock_streaming_pipeline,
            State::CreateOrEnableSteamingResources,
        )
        .await;
    }

    #[tokio::test]
    async fn test_ensure_streaming_pipeline_in_correct_state_disable() {
        let mut mock_streaming_pipeline = MockStreamingPipeline::new();
        let _ = mock_streaming_pipeline.expect_ensure_stop_pipeline().once().returning(|| ());
        ensure_streaming_pipeline_in_correct_state(
            &mut mock_streaming_pipeline,
            State::DisableStreamingResources,
        )
        .await;
    }
}
