//! Module contains onvif communication related to reading the state of the device.
//! This implements a Model for our client.  See link for more details.
//! https://iotatlas.net/en/patterns/mvc/

/// Error enum for OnvifClient
pub mod error;

/// onvif client
pub mod onvif_client;

/// digest authentication client
pub mod digest_auth;

/// enums related to onvif services and onvif service capabilities
pub mod onvif_service;

/// onvif constant
pub mod constant;
