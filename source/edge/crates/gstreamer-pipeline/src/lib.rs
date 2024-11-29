//! This crate contains logic for building gstreamer pipeline for video streaming.
//!
//! Pipeline currently consumes video stream from RTSP source and enqueues video frames
//! to in-memory buffer.
//!
//! This crate also takes care of starting and stopping video streaming pipeline.
#![warn(missing_docs, missing_debug_implementations, rust_2018_idioms)]

mod constants;
/// Logic to build AI ingestion pipeline.
pub mod event_ingestion;
mod event_processor;
pub mod hybrid_streaming_service;
/// Logic to build metadata pipeline.
pub mod metadata_streaming_pipeline;
mod util;
