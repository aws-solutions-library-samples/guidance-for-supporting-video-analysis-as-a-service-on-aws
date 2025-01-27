// GRCOV_STOP_COVERAGE
//! This file contains the pipeline setup for KVS offline ingestion.  
#![allow(clippy::needless_borrows_for_generic_args)]

use crate::constants::{
    DEFAULT_BUFFER_DURATION, DEFAULT_FRAME_RATE, DEFAULT_KVS_PLUGIN_BUFFER_SIZE,
    DEFAULT_KVS_PLUGIN_EXPECTED_BANDWIDTH, KVS_OFFLINE, KVS_REALTIME_SINK, KVS_SINK,
};
use crate::hybrid_streaming_service::frame::Frame;
use crate::hybrid_streaming_service::kvs_callbacks::setup_kvs_fragment_ack_callback_offline;
use crate::hybrid_streaming_service::message_service::GST_CAPS_FOR_KVS;
use glib::{Cast, ObjectExt};
use gstreamer::prelude::{ElementExt, GObjectExtManualGst, GstBinExtManual};
use gstreamer::{Element, Format};
use gstreamer_app::{gst, AppSrc, AppSrcCallbacks, AppStreamType};
use std::sync::mpsc::Receiver;
use std::sync::Arc;
use streaming_traits::error::VideoStreamingError;
use streaming_traits::error::VideoStreamingError::GstreamerError;
use streaming_traits::StreamingServiceConfigurations;
use tracing::{error, info};

#[derive(Debug)]
pub(crate) struct OfflineIngestionPipeline {
    pipeline: gst::Pipeline,
    pipeline_state: gst::State,
}

impl OfflineIngestionPipeline {
    pub(crate) fn new(
        client_configs: StreamingServiceConfigurations,
        receiver_client: Receiver<Arc<Frame>>,
    ) -> Result<Self, VideoStreamingError> {
        // Initialize GStreamer, ok if called multiple times
        gst::init().map_err(|e| GstreamerError(e.to_string()))?;
        let pipeline = gst::Pipeline::new(Some("OfflineIngestionPipeline"));

        let kvs_plugin = Self::create_kvs_sink_for_offline(&client_configs);

        let appsrc = Self::create_app_src(receiver_client);

        let queue = gst::ElementFactory::make_with_name("queue", Some("queue"))
            .map_err(|e| GstreamerError(e.to_string()))?;
        let caps = gst::Caps::builder("video/x-h264")
            .field("stream-format", "avc")
            .field("alignment", "au")
            .field("aud", &true)
            .build();

        // let h264parse = gst::ElementFactory::make_with_name(H264_PARSE, Some(H264_PARSE))?;
        //        h264parse.set_property("config-interval", &-1i32);

        let filter = gst::ElementFactory::make_with_name("capsfilter", Some("capsfilter"))
            .map_err(|e| GstreamerError(e.to_string()))?;
        filter.set_property("caps", &caps);

        // Add plugins to pipeline and link them
        pipeline
            .add_many(&[appsrc.upcast_ref(), &queue, &filter, &kvs_plugin])
            .map_err(|e| GstreamerError(e.to_string()))?;
        Element::link_many(&[appsrc.upcast_ref(), &queue, &filter, &kvs_plugin])
            .map_err(|e| GstreamerError(e.to_string()))?;

        let pipeline_state = gst::State::Null;
        Ok(OfflineIngestionPipeline { pipeline, pipeline_state })
    }
    pub(crate) fn start(&mut self) {
        self.pipeline.set_state(gst::State::Playing).expect("Failed to start pipeline");
        self.pipeline_state = gst::State::Playing;
        info!("Pipeline Started.");
    }
    /// Stop pipeline to consume video stream from RTSP server.
    pub(crate) fn stop(&mut self) {
        self.pipeline.set_state(gst::State::Null).expect("Failed while stopping pipeline");
        self.pipeline_state = gst::State::Null;
        info!("Pipeline Stopped.");
    }
    /// Start the pipeline unless it is already running.
    pub(crate) fn ensure_start(&mut self) {
        if self.pipeline_state != gst::State::Playing {
            self.start();
        }
    }
    /// Stop the pipeline unless it is already stopped.
    pub(crate) fn ensure_stop(&mut self) {
        if self.pipeline_state != gst::State::Null {
            self.stop();
        }
    }
    fn create_kvs_sink_for_offline(configs: &StreamingServiceConfigurations) -> gst::Element {
        let kvs_sink_offline =
            gst::ElementFactory::make_with_name(KVS_SINK, Some(KVS_REALTIME_SINK))
                .expect("Failed to create kvs sink.");
        // Set kvs properties https://docs.aws.amazon.com/kinesisvideostreams/latest/dg/examples-gstreamer-plugin-parameters.html
        kvs_sink_offline.set_property("stream-name", &configs.stream_name);
        kvs_sink_offline.set_property("aws-region", &configs.aws_region);
        kvs_sink_offline.set_property_from_str("streaming-type", KVS_OFFLINE);
        kvs_sink_offline.set_property("framerate", DEFAULT_FRAME_RATE);
        // KVS interprets the pts in the buffer as an absolute time-code
        #[cfg(not(target_arch = "x86_64"))]
        kvs_sink_offline.set_property("use-original-pts", true);
        #[cfg(not(target_arch = "x86_64"))]
        setup_kvs_fragment_ack_callback_offline(&kvs_sink_offline);
        kvs_sink_offline.set_property("buffer-duration", DEFAULT_BUFFER_DURATION);
        kvs_sink_offline.set_property("track-name", "OFFLINE_TRACK");
        kvs_sink_offline.set_property("fragment-acks", true);
        kvs_sink_offline.set_property("frame-timecodes", true);
        kvs_sink_offline.set_property("avg-bandwidth-bps", DEFAULT_KVS_PLUGIN_EXPECTED_BANDWIDTH);
        kvs_sink_offline.set_property("storage-size", DEFAULT_KVS_PLUGIN_BUFFER_SIZE);
        // Format iot-properties, https://docs.aws.amazon.com/kinesisvideostreams/latest/dg/examples-gstreamer-plugin-parameters.html
        let iot_properties = format!(
            "iot-certificate,endpoint={},ca-path={},cert-path={},key-path={},role-aliases={}",
            &configs.iot_endpoint,
            &configs.ca_cert_path,
            &configs.iot_cert_path,
            &configs.private_key_path,
            &configs.role_alias
        );
        // Set iot-properties
        kvs_sink_offline.set_property_from_str("iot-certificate", &iot_properties);
        info!("Finished setting up kvs offline plugin.");
        kvs_sink_offline
    }
    fn create_app_src(receiver_client: Receiver<Arc<Frame>>) -> AppSrc {
        // Example https://gitlab.freedesktop.org/gstreamer/gstreamer-rs/-/blob/main/examples/src/bin/appsrc.rs?ref_type=heads
        // Setup so this can be linked to gstreamer plugin

        let callback = Self::create_appsrc_callback_for_cloud_ingestion(receiver_client);

        AppSrc::builder()
            .stream_type(AppStreamType::Stream)
            .block(true)
            .max_bytes(0_u64)
            .min_percent(0_u32)
            .format(Format::Time)
            .is_live(false)
            .callbacks(callback)
            .build()
    }
    fn create_appsrc_callback_for_cloud_ingestion(
        receiver_client: Receiver<Arc<Frame>>,
    ) -> AppSrcCallbacks {
        let mut set_caps = true;
        // Set callback for pull mode, this will try and pull data from buffer.
        let callback = AppSrcCallbacks::builder()
            .need_data(Box::new(move |appsrc: &AppSrc, _| {
                // Non-blocking call to the buffer.
                let frame = match receiver_client.recv() {
                    Ok(frame) => frame,
                    Err(e) => {
                        error!("Realtime receiver disconnected unexpectedly! {:?}", e);
                        let _ = appsrc.end_of_stream();
                        return;
                    }
                };
                // Set the caps for the AppSrc, Caps must be set by this point.
                if set_caps {
                    let lock = GST_CAPS_FOR_KVS.read().expect("Caps Poisoned.");
                    appsrc.set_caps(lock.as_ref());
                    set_caps = false;
                }

                let buffer = match frame.get_buffer_from_frame() {
                    Ok(buffer) => buffer,
                    Err(e) => {
                        error!("Buffer was not recovered from a frame : {:?}", e);
                        return;
                    }
                };
                if let Err(e) = appsrc.push_buffer(buffer) {
                    error!("Failed to push buffer to offline pipeline! {:?}", e);
                };
            }))
            .build();
        callback
    }
}
