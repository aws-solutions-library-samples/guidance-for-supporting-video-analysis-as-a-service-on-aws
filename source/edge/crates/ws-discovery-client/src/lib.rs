//! This crate is used to define Onvif communication with the device.
//! Onvif is used to determine the capabilities and state of the device as well as
//! control device operations (Process 1).
#![warn(missing_docs, missing_debug_implementations, rust_2018_idioms)]

/// module that handles calling Onvif APIs on the device
pub mod client;

/// This module holds Rust structs that are converted from wsdl files
pub mod wsdl_rs;
