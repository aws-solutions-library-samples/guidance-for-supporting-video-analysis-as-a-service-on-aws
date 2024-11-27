#![allow(clippy::needless_borrows_for_generic_args)]
#![allow(clippy::borrow_deref_ref)]

use glib::prelude::*;
use gstreamer as gst;
use gstreamer_app as gst_app;
use std::collections::VecDeque;
use std::env;
use std::error::Error;
use std::string::String;
// This is a special case of import with prelude. Otherwise, it is recommended to have specific
// imports.
use gstreamer::prelude::*;

use async_trait::async_trait;
// std::sync::{Arc, Mutex} is used here as we are working with C library. If dealing with Rust
// objects, use tokio:sync::{Arc,Mutex} instead. For more details, follow
// https://tokio.rs/tokio/tutorial/shared-state
use std::sync::{Arc, Condvar, Mutex};
use tracing::{error, info, instrument, warn};

use crate::constants::{APP_SINK, APP_SINK_BUFFER_SIZE, RTSP_SRC};
use crate::constants::{CAPSFILTER, METADATA_STREAM_START_TAG};
use streaming_traits::error::VideoStreamingError::GstreamerError;
use streaming_traits::{StreamUriConfiguration, StreamingPipeline};

/// Struct for initializing Pipeline Elements for Metadata Streaming Pipeline.
#[derive(Debug)]
pub struct PipelineElements {
    rtsp_src: gst::Element,
    capsfilter: gst::Element,
    appsink: gst::Element,
}

/// Struct for Gtsreamer Pipeline to ingest events in in-memory buffer.
#[derive(Debug)]
pub struct Pipeline {
    pipeline: gst::Pipeline,
    shared_buffer: Arc<(Mutex<VecDeque<String>>, Condvar)>,
    accumulator_buffer: Arc<Mutex<Vec<u8>>>,
    pipeline_state: gst::State,
}

impl Pipeline {
    /// Constructor: Initialize pipeline object
    #[instrument]
    pub async fn new(
        shared_buffer: Arc<(Mutex<VecDeque<String>>, Condvar)>,
    ) -> anyhow::Result<Self> {
        // Initialize GStreamer
        gst::init()?;
        let pipeline_state = gst::State::Null;
        let pipeline = gst::Pipeline::new(Some("MetadataStreamingPipeline"));
        let accumulator_buffer = Arc::new(Mutex::new(Vec::<u8>::new()));
        Ok(Pipeline { pipeline, shared_buffer, accumulator_buffer, pipeline_state })
    }

    /// Starts pipeline and starts consuming metadata stream from RTSP server.
    pub async fn start(&mut self) {
        self.pipeline
            .set_state(gst::State::Playing)
            .expect("Failed to start metadata streaming pipeline");
        self.pipeline_state = gst::State::Playing;
    }

    /// Stop pipeline to consume metadata stream from RTSP server.
    pub async fn stop(&mut self) {
        self.pipeline
            .set_state(gst::State::Null)
            .expect("Failed while stopping metadata streaming pipeline");
        self.pipeline_state = gst::State::Null;
    }

    /// Start the pipeline unless it is already running.
    pub async fn ensure_start(&mut self) {
        if self.pipeline_state != gst::State::Playing {
            self.start().await;
        }
    }
    /// Stop the pipeline unless it is already stopped.
    pub async fn ensure_stop(&mut self) {
        if self.pipeline_state != gst::State::Null {
            self.stop().await;
        }
    }

    /// Create elements and add it to bin for pipeline
    pub async fn create_pipeline_elements(
        &self,
        stream_uri_config: StreamUriConfiguration,
    ) -> Result<PipelineElements, Box<dyn Error>> {
        // Create the pipeline elements
        let rtsp_src = gst::ElementFactory::make_with_name(RTSP_SRC, Some(RTSP_SRC))?;
        let capsfilter = gst::ElementFactory::make_with_name(CAPSFILTER, Some(CAPSFILTER))?;
        let appsink = gst::ElementFactory::make_with_name(APP_SINK, None)?;

        // If we are tunneling IP, the remote endpoint is getting reached
        // We need to set up a correct proxy for reqwest to use
        if let Ok(rtsp_endpoint) = env::var("RTSP_ENDPOINT") {
            rtsp_src.set_property("location", rtsp_endpoint);
        } else {
            rtsp_src.set_property("location", &stream_uri_config.rtsp_uri);
        }
        rtsp_src.set_property("user-id", &stream_uri_config.username);
        rtsp_src.set_property("user-pw", &stream_uri_config.password);
        appsink.set_property("emit-signals", &true);
        appsink.set_property("max-buffers", APP_SINK_BUFFER_SIZE);

        // Add the elements to the pipeline
        self.pipeline.add_many(&[&rtsp_src, &capsfilter, &appsink])?;
        Ok(PipelineElements { rtsp_src, capsfilter, appsink })
    }

    /// Link elements in pipeline in order: rtspsrc -> capsfilter -> appsink and
    /// register callback to appsink responsible for flushing appsink buffer to shared buffer.
    #[allow(deprecated)]
    pub async fn link_pipeline_elements(
        &self,
        pipeline_elements: PipelineElements,
    ) -> Result<(), Box<dyn Error>> {
        // This is needed because 'rtspsrc' does not have any static pads
        let rtsp_src_clone = pipeline_elements.rtsp_src.clone();
        let capsfilter_clone = pipeline_elements.capsfilter.clone();
        rtsp_src_clone.connect_pad_added(move |_src, src_pad| {
            let sink_pad = match capsfilter_clone.static_pad("sink") {
                Some(pad) => pad,
                None => {
                    error!("Failed to get depay sink pad");
                    return;
                }
            };
            // Link rtsp_src src paid with capsfilter sink pad
            if let Err(err) = src_pad.link(&sink_pad) {
                error!("Failed to link pads: {}", err);
            } else {
                info!("Pads linked: {} -> {}", src_pad.name(), sink_pad.name());
            }
        });
        pipeline_elements.capsfilter.link_filtered(
            &pipeline_elements.appsink,
            &gst::Caps::new_simple(
                "application/x-rtp",
                &[("encoding-name", &"VND.ONVIF.METADATA"), ("media", &"application")],
            ),
        )?;

        // Register Callback for appsink to write to in-memory buffer
        let shared_buffer = self.shared_buffer.clone();
        let accumulator_buffer = self.accumulator_buffer.clone();
        let appsink_for_video: gstreamer_app::AppSink = pipeline_elements
            .appsink
            .dynamic_cast::<gstreamer_app::AppSink>()
            .expect("Sink element is expected to be an appsink!");

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
                let Ok(data) = buffer.map_readable() else {
                    warn!("Error in fetching data from appsink buffer");
                    return Ok(gst::FlowSuccess::Ok);
                };

                let mut accumulator = accumulator_buffer.lock().unwrap();
                if buffer.flags().contains(gst::BufferFlags::DISCONT) {
                    accumulator.clear();
                }

                // Buffer event in adapter till end of RTP packet is reached.
                accumulator.append(&mut data[12..].to_vec());

                // Calculate RTP packet end marker.
                let rtp_header = &data.to_vec()[0..12];
                let marker = (rtp_header[1] & 0x80) != 0;
                if !marker {
                    return Ok(gst::FlowSuccess::Ok);
                }

                // We have found the last chunk for this document, empty the adapter
                let rtp_data = &*accumulator.as_slice();
                let utf8_data = match std::str::from_utf8(rtp_data) {
                    Ok(s) => s,
                    Err(_err) => {
                        warn!("XML formatting error");
                        return Ok(gst::FlowSuccess::Ok);
                    }
                };
                let valid_event = Self::is_valid_event(utf8_data);
                // Incomplete, wait for the next document
                if !valid_event {
                    return Ok(gst::FlowSuccess::Ok);
                }
                if let Ok(mut shared_buffer_mut) = shared_buffer.0.lock() {
                    shared_buffer_mut.push_back(utf8_data.to_string());
                    shared_buffer.1.notify_all();
                }
                accumulator.clear();
                Ok(gst::FlowSuccess::Ok)
            })
            .build();
        appsink_for_video.set_callbacks(callback);
        Ok(())
    }

    /// Setup event listeners
    pub async fn add_event_listener(&self) -> Result<(), Box<dyn Error>> {
        // Create the bus to monitor for messages from the pipeline
        let bus = self.pipeline.bus().ok_or(GstreamerError(
            "Unable to get bus for metadata streaming pipeline".to_string(),
        ))?;
        bus.add_watch(move |_, msg| {
            if let gst::MessageView::Error(err) = msg.view() {
                error!("Error received from element {:?}: {}", err.src(), err.error());
            }
            glib::Continue(true)
        })?;
        Ok(())
    }

    fn is_valid_event(utf8_data: &str) -> bool {
        let mut forward = false;
        for token in xmlparser::Tokenizer::from(utf8_data) {
            match token {
                Ok(token) => match token {
                    xmlparser::Token::Comment { .. } => {
                        continue;
                    }
                    xmlparser::Token::Declaration { .. } => {
                        continue;
                    }
                    xmlparser::Token::ElementStart { local, .. } => {
                        if local.as_str() == METADATA_STREAM_START_TAG {
                            forward = true;
                        }
                        break;
                    }
                    _ => {
                        forward = false;
                        break;
                    }
                },
                Err(_err) => return false,
            }
        }
        forward
    }
}

#[async_trait]
impl StreamingPipeline for Pipeline {
    /// Create metadata streaming pipeline: RTSPSRC -> CAPSFILTER -> APPSINK -> BUFFER
    #[instrument]
    async fn create_pipeline(
        &self,
        stream_uri_config: StreamUriConfiguration,
    ) -> Result<(), Box<dyn Error>> {
        let pipeline_elements = self.create_pipeline_elements(stream_uri_config).await?;
        self.link_pipeline_elements(pipeline_elements).await?;
        self.add_event_listener().await?;
        Ok(())
    }

    async fn ensure_start_pipeline(&mut self) {
        self.ensure_start().await
    }

    async fn ensure_stop_pipeline(&mut self) {
        self.ensure_stop().await
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::Duration;

    #[tokio::test]
    async fn test_video_streaming_pipeline() {
        let shared_buffer = Arc::new((Mutex::new(VecDeque::new()), Condvar::new()));
        let mut pipeline = Pipeline::new(shared_buffer.clone()).await.unwrap();
        let stream_uri_config = StreamUriConfiguration {
            rtsp_uri: "rtsp://username:password@10.132.51.53.554/h264".to_string(),
            username: "username".to_string(),
            password: "password".to_string(),
        };

        // create pipeline elements
        let pipeline_elements = pipeline.create_pipeline_elements(stream_uri_config).await.unwrap();
        assert_eq!(pipeline_elements.rtsp_src.name(), RTSP_SRC);
        assert_eq!(pipeline_elements.capsfilter.name(), CAPSFILTER);

        // link pipeline elements
        pipeline.link_pipeline_elements(pipeline_elements).await.unwrap();
        // start pipeline
        pipeline.start().await;
        tokio::time::sleep(Duration::from_secs(2)).await;
        pipeline.stop().await;

        // check if pipeline is stopped
        let state = pipeline.pipeline.current_state();
        assert_eq!(state, gst::State::Null);

        // check if shared buffer is cleared
        let buffer = shared_buffer.0.lock().unwrap();
        assert!(buffer.is_empty());
    }
}
