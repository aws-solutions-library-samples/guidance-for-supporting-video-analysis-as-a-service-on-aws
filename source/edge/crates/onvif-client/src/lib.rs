//! This crate is used to define Onvif communication with the device.
//! Onvif is used to determine the capabilities and state of the device as well as
//! control device operations (Process 1).
#![warn(missing_docs, missing_debug_implementations, rust_2018_idioms)]

/// module that handles calling Onvif APIs on the device
pub mod client;

/// This module holds Rust structs that are converted from wsdl files
pub mod wsdl_rs;

/// This module deals with data conversion between soap messages and Rust structs
pub mod soap;

/// This module holds Xsd structs converted from xsd files
pub mod xsd_rs;

/// This module holds config related function
pub mod config;
