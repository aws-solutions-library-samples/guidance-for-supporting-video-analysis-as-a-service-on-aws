// GRCOV_STOP_COVERAGE
#![allow(clippy::needless_borrows_for_generic_args)]
use glib::prelude::*;
use gstreamer as gst;
use gstreamer_app as gst_app;
use std::env;
use std::error::Error;
use std::sync::Arc;
// This is a special case of import with prelude. Otherwise, it is recommended to have specific
// imports.
use crate::constants::H264_PARSE;
use crate::constants::RTP_DEPAY;
use crate::constants::{APP_SINK, APP_SINK_BUFFER_SIZE, RTSP_SRC};
use crate::hybrid_streaming_service::frame::Frame;
use crate::hybrid_streaming_service::message_service::GST_CAPS_FOR_KVS;
use gstreamer::prelude::*;
use gstreamer::Sample;
use gstreamer_rtsp::RTSPLowerTrans;
use std::sync::mpsc::{SyncSender, TrySendError};
use streaming_traits::error::{VideoStreamingError, VideoStreamingError::GstreamerError};
use streaming_traits::StreamingServiceConfigurations;
use tracing::{debug, error, info, warn};

/// Struct for initializing Pipeline Elements for VideoStreamingPipeline.
#[derive(Debug)]
pub(crate) struct PipelineElements {
    rtsp_src: gst::Element,
    rtph264depay: gst::Element,
    h264parse: gst::Element,
    appsink: gst::Element,
}

/// Struct for Gstreamer Pipeline to ingest video in in-memory buffer.
#[derive(Debug)]
pub(crate) struct RTSPVideoPipeline {
    pipeline: gst::Pipeline,
    channel_to_forwarding_service: SyncSender<Arc<Frame>>,
    pipeline_state: gst::State,
}

impl RTSPVideoPipeline {
    /// Constructor: Initialize pipeline object
    pub(crate) fn new(
        channel_to_forwarding_service: SyncSender<Arc<Frame>>,
        config: StreamingServiceConfigurations,
    ) -> Result<Self, VideoStreamingError> {
        // Initialize GStreamer
        gst::init().map_err(|e| GstreamerError(e.to_string()))?;
        let pipeline = gst::Pipeline::new(Some("VideoStreamingPipeline"));

        let pipeline_state = gst::State::Null;
        let pipeline =
            RTSPVideoPipeline { pipeline, channel_to_forwarding_service, pipeline_state };
        pipeline.create_pipeline(config).map_err(|e| GstreamerError(e.to_string()))?;
        Ok(pipeline)
    }

    /// Starts pipeline and starts consuming video stream from RTSP server.
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

    /// Create elements and add it to bin for pipeline
    pub(crate) fn create_pipeline_elements(
        &self,
        stream_uri_config: StreamingServiceConfigurations,
    ) -> Result<PipelineElements, Box<dyn Error>> {
        // Create the pipeline elements
        let rtsp_src = gst::ElementFactory::make_with_name(RTSP_SRC, Some(RTSP_SRC))?;
        let rtph264depay = gst::ElementFactory::make_with_name(RTP_DEPAY, Some(RTP_DEPAY))?;
        let h264parse = gst::ElementFactory::make_with_name(H264_PARSE, Some(H264_PARSE))?;
        let appsink = gst::ElementFactory::make_with_name(APP_SINK, None)?;

        // If we are tunneling IP, the remote endpoint is getting reached
        // We need to set up a correct proxy for reqwest to use
        if let Ok(rtsp_endpoint) = env::var("RTSP_ENDPOINT") {
            rtsp_src.set_property("location", rtsp_endpoint);
        } else {
            rtsp_src.set_property("location", &stream_uri_config.rtsp_uri);
        }

        // Set the properties of the pipeline elements
        rtsp_src.set_property("user-id", &stream_uri_config.username);
        rtsp_src.set_property("user-pw", &stream_uri_config.password);
        rtsp_src.set_property("protocols", RTSPLowerTrans::TCP);
        h264parse.set_property("config-interval", &-1i32);
        appsink.set_property("emit-signals", &true);
        appsink.set_property("max-buffers", APP_SINK_BUFFER_SIZE);

        // Add the elements to the pipeline
        self.pipeline.add_many(&[&rtsp_src, &rtph264depay, &h264parse, &appsink])?;
        Ok(PipelineElements { rtsp_src, rtph264depay, h264parse, appsink })
    }

    /// Link elements in pipeline in order: rtspsrc -> rtph264depay -> h264parse -> appsink and
    /// register callback to appsink responsible for flushing appsink buffer to shared buffer.
    pub(crate) fn link_pipeline_elements(
        &self,
        pipeline_elements: PipelineElements,
    ) -> Result<(), Box<dyn Error>> {
        // This is needed because 'rtspsrc' does not have any static pads
        let rtsp_src_clone = pipeline_elements.rtsp_src.clone();
        let rtph264depay_clone = pipeline_elements.rtph264depay.clone();
        rtsp_src_clone.connect_pad_added(move |_src, src_pad| {
            let depay_pad = match rtph264depay_clone.static_pad("sink") {
                Some(pad) => pad,
                None => {
                    error!("Failed to get depay sink pad");
                    return;
                }
            };
            // Link rtsp_src src paid with rtph264depay sink pad
            if let Err(err) = src_pad.link(&depay_pad) {
                error!("Failed to link pads: {:?}", err);
            } else {
                info!("Pads linked: {} -> {}", src_pad.name(), depay_pad.name());
            }
        });
        #[allow(deprecated)]
        pipeline_elements.rtph264depay.link_filtered(
            &pipeline_elements.h264parse,
            &gst::Caps::new_simple(
                "video/x-h264",
                &[("stream-format", &"avc"), ("alignment", &"au"), ("aud", &true)],
            ),
        )?;
        pipeline_elements.h264parse.link(&pipeline_elements.appsink)?;

        // Register Callback for appsink to write to in-memory buffer

        let appsink_for_video: gstreamer_app::AppSink = pipeline_elements
            .appsink
            .dynamic_cast::<gstreamer_app::AppSink>()
            .expect("Sink element is expected to be an appsink!");

        let send_to_forwarding_service = self.channel_to_forwarding_service.clone();

        let mut caps_publish = true;
        let callback = gst_app::AppSinkCallbacks::builder()
            .new_sample(move |appsink| {
                let Ok(sample) = appsink.pull_sample() else {
                    warn!("Error in fetching sample from appsink");
                    return Ok(gst::FlowSuccess::Ok);
                };
                let Some(buffer) = sample.buffer() else {
                    warn!("Error in fetching buffer from appsink");
                    return Ok(gst::FlowSuccess::Ok);
                };
                // Publish Caps to other pipelines. The information is needed for KVS plugin.
                Self::ensure_publish_caps(&mut caps_publish, &sample);

                let Some(base_time) = appsink.base_time() else {
                    warn!("Unable to get base-time gstreamer.");
                    return Ok(gst::FlowSuccess::Ok);
                };

                // Create frame from  buffer
                let Some(frame) = Frame::new_from_gst_buffer(buffer, base_time) else {
                    warn!("Unable to make frame from gstreamer buffer.");
                    return Ok(gst::FlowSuccess::Ok);
                };

                // Try send. If buffer if full log error.
                // If channel is disconnected shutdown pipeline.
                match send_to_forwarding_service.try_send(Arc::new(frame)) {
                    Ok(_) => debug!("Frame forwarded."),
                    Err(TrySendError::Full(_e)) => {
                        error!("Failed to forward frame: buffer full");
                    }
                    Err(TrySendError::Disconnected(_e)) => {
                        error!("Failed to forward frame: disconnected terminating the steam.");
                        return Err(gst::FlowError::Eos);
                    }
                };

                Ok(gst::FlowSuccess::Ok)
            })
            .build();
        appsink_for_video.set_callbacks(callback);
        Ok(())
    }

    fn ensure_publish_caps(caps_publish: &mut bool, sample: &Sample) {
        if *caps_publish {
            let mut guard = GST_CAPS_FOR_KVS.write().unwrap();
            *guard = Some(sample.caps().expect("Failed to get Caps from RTSP Pipeline").copy());
            *caps_publish = false;
        }
    }

    /// Setup event listeners
    pub(crate) fn add_event_listener(&self) -> Result<(), Box<dyn Error>> {
        // Create the bus to monitor for messages from the pipeline
        let bus = self
            .pipeline
            .bus()
            .ok_or(GstreamerError("Unable to get bus for pipeline".to_string()))?;
        bus.add_watch(move |_, msg| {
            match msg.view() {
                gst::MessageView::Error(err) => {
                    error!("Error received from element {:?}", err);
                }
                gst::MessageView::Warning(warn) => {
                    warn!("Warning received from element {:?}", warn);
                }
                gst::MessageView::Eos(eos) => {
                    error!("Pipeline terminated unexpectedly : {:?}", eos);
                }
                view => {
                    debug!("Message received off gstreamer bus : {:?}", view)
                }
            }
            Continue(true)
        })?;
        Ok(())
    }

    fn create_pipeline(
        &self,
        stream_uri_config: StreamingServiceConfigurations,
    ) -> Result<(), Box<dyn Error>> {
        let pipeline_elements = self.create_pipeline_elements(stream_uri_config)?;
        self.link_pipeline_elements(pipeline_elements)?;
        self.add_event_listener()?;
        Ok(())
    }
}
