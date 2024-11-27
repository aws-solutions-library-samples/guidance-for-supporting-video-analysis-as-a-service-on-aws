//! Store top-level service for all of device streaming
// GRCOV_STOP_COVERAGE

use crate::constants::FRAME_BUFFER_SIZE;
use crate::hybrid_streaming_service::real_time_cloud_ingestion::RealtimeIngestionPipeline;
use crate::hybrid_streaming_service::rtsp_pipeline::RTSPVideoPipeline;
use std::sync::mpsc::{sync_channel};
use streaming_traits::error::VideoStreamingError;
use streaming_traits::StreamingServiceConfigurations;
use crate::hybrid_streaming_service::forwarding_service::ForwardingService;

pub(crate) mod frame;
pub(crate) mod kvs_callbacks;
mod message_service;
mod real_time_cloud_ingestion;
mod rtsp_pipeline;
mod forwarding_service;
mod fragment;

/// StreamingService for hybrid streaming model.
#[derive(Debug)]
pub struct HybridStreamingService {
    rtsp_video_pipeline: RTSPVideoPipeline,
    realtime_ingestion_pipeline: RealtimeIngestionPipeline,
    _forwarding_service: ForwardingService,
}

impl HybridStreamingService {
    /// Create a new streaming service, created in stopped state
    pub fn new(
        streaming_configs: StreamingServiceConfigurations
    ) -> Result<Self, VideoStreamingError> {
        // Define channels for communication between components in the streaming service.
        let (rtsp_tx, rtsp_rx) = sync_channel(FRAME_BUFFER_SIZE);
        let (realtime_tx, realtime_rx) = sync_channel(FRAME_BUFFER_SIZE);
        // Create components of this service
        // Note: Gstreamer will cleanup pipelines if Rust objects go out of scope.
        let rtsp_video_pipeline =
            RTSPVideoPipeline::new(rtsp_tx.clone(), streaming_configs.clone())?;
        let realtime_ingestion_pipeline =
            RealtimeIngestionPipeline::new(streaming_configs.clone(), realtime_rx)?;
        let _forwarding_service = ForwardingService::new(
            rtsp_rx,
            realtime_tx,
        );

        Ok(HybridStreamingService { rtsp_video_pipeline, realtime_ingestion_pipeline, _forwarding_service })
    }

    /// Ensure the service is running.
    pub async fn ensure_start(&mut self) {
        self.realtime_ingestion_pipeline.ensure_start();
        self.rtsp_video_pipeline.ensure_start();
    }
    /// Ensure the streaming service is stopped.
    pub async fn ensure_stop(&mut self) {
        self.rtsp_video_pipeline.ensure_stop();
        self.realtime_ingestion_pipeline.ensure_stop();
    }
}
