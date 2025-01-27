//! Store top-level service for all of device streaming
// GRCOV_STOP_COVERAGE

use crate::constants::FRAME_BUFFER_SIZE;
#[cfg(feature = "sd-card-catchup")]
use crate::data_storage::video_storage::FileMetadataStorage;
#[cfg(feature = "sd-card-catchup")]
use crate::hybrid_streaming_service::catchup_video_service::CatchupVideoService;
use crate::hybrid_streaming_service::forwarding_service::ForwardingService;
#[cfg(feature = "sd-card-catchup")]
use crate::hybrid_streaming_service::offline_cloud_ingestion::OfflineIngestionPipeline;
use crate::hybrid_streaming_service::real_time_cloud_ingestion::RealtimeIngestionPipeline;
use crate::hybrid_streaming_service::rtsp_pipeline::RTSPVideoPipeline;
use std::sync::mpsc::{sync_channel, Receiver};
#[cfg(feature = "sd-card-catchup")]
use std::sync::{Arc, Mutex};
use streaming_traits::error::VideoStreamingError;
use streaming_traits::StreamingServiceConfigurations;

#[cfg(feature = "sd-card-catchup")]
mod catchup_video_service;
#[cfg(feature = "sd-card-catchup")]
mod device_timeline;
mod forwarding_service;
pub(crate) mod fragment;
pub(crate) mod frame;
pub(crate) mod kvs_callbacks;
mod message_service;
#[cfg(feature = "sd-card-catchup")]
mod offline_cloud_ingestion;
mod real_time_cloud_ingestion;
mod rtsp_pipeline;

/// StreamingService for hybrid streaming model.
#[derive(Debug)]
#[cfg(not(feature = "sd-card-catchup"))]
pub struct HybridStreamingService {
    rtsp_video_pipeline: RTSPVideoPipeline,
    realtime_ingestion_pipeline: RealtimeIngestionPipeline,
    _forwarding_service: ForwardingService,
}

#[cfg(not(feature = "sd-card-catchup"))]
impl HybridStreamingService {
    /// Create a new streaming service, created in stopped state
    pub fn new(
        streaming_configs: StreamingServiceConfigurations,
        motion_based_streaming_rx: Receiver<String>,
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
        let _forwarding_service =
            ForwardingService::new(rtsp_rx, realtime_tx, motion_based_streaming_rx);

        Ok(HybridStreamingService {
            rtsp_video_pipeline,
            realtime_ingestion_pipeline,
            _forwarding_service,
        })
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

/// StreamingService for hybrid streaming model.
#[derive(Debug)]
#[cfg(feature = "sd-card-catchup")]
pub struct HybridStreamingService {
    rtsp_video_pipeline: RTSPVideoPipeline,
    realtime_ingestion_pipeline: RealtimeIngestionPipeline,
    _forwarding_service: ForwardingService,
    offline_ingestion_pipeline: OfflineIngestionPipeline,
    _catchup_video_service: CatchupVideoService,
    #[allow(unused)]
    database_client: Arc<Mutex<FileMetadataStorage>>,
}

#[cfg(feature = "sd-card-catchup")]
impl HybridStreamingService {
    /// Create a new streaming service, created in stopped state
    #[cfg(feature = "sd-card-catchup")]
    pub fn new(
        streaming_configs: StreamingServiceConfigurations,
        motion_based_streaming_rx: Receiver<String>,
    ) -> Result<Self, VideoStreamingError> {
        // Define channels for communication between components in the streaming service.
        let (rtsp_tx, rtsp_rx) = sync_channel(FRAME_BUFFER_SIZE);
        let (realtime_tx, realtime_rx) = sync_channel(FRAME_BUFFER_SIZE);
        let (offline_tx, offline_rx) = sync_channel(FRAME_BUFFER_SIZE);
        let database_client = Self::create_database_client(
            &streaming_configs.local_storage_path,
            streaming_configs.local_storage_disk_usage,
            streaming_configs.db_path.to_owned(),
        );
        // Create components of this service
        // Note: Gstreamer will cleanup pipelines if Rust objects go out of scope.
        let rtsp_video_pipeline =
            RTSPVideoPipeline::new(rtsp_tx.clone(), streaming_configs.clone())?;
        let realtime_ingestion_pipeline =
            RealtimeIngestionPipeline::new(streaming_configs.clone(), realtime_rx)?;
        let offline_ingestion_pipeline =
            OfflineIngestionPipeline::new(streaming_configs, offline_rx)?;
        let _catchup_video_service = CatchupVideoService::new(offline_tx, database_client.clone());
        let _forwarding_service = ForwardingService::new(
            rtsp_rx,
            realtime_tx,
            motion_based_streaming_rx,
            database_client.clone(),
        );

        Ok(HybridStreamingService {
            rtsp_video_pipeline,
            realtime_ingestion_pipeline,
            _forwarding_service,
            offline_ingestion_pipeline,
            _catchup_video_service,
            database_client,
        })
    }

    /// Ensure the service is running.
    pub async fn ensure_start(&mut self) {
        self.realtime_ingestion_pipeline.ensure_start();
        self.rtsp_video_pipeline.ensure_start();
        self.offline_ingestion_pipeline.ensure_start();
    }
    /// Ensure the streaming service is stopped.
    pub async fn ensure_stop(&mut self) {
        self.rtsp_video_pipeline.ensure_stop();
        self.realtime_ingestion_pipeline.ensure_stop();
        self.offline_ingestion_pipeline.ensure_stop();
    }

    fn create_database_client(
        local_storage_path: &str,
        local_storage_disk_usage: u64,
        db_path: Option<String>,
    ) -> Arc<Mutex<FileMetadataStorage>> {
        let database_client = FileMetadataStorage::new_connection(
            local_storage_path,
            local_storage_disk_usage,
            db_path,
        )
        .expect("Error creating database connection.");
        Arc::new(Mutex::new(database_client))
    }
}
