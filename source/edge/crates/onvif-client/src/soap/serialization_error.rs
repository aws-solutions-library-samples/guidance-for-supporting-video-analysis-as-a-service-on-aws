use thiserror::Error;

/// Error types for serialization and deserialization between xml string and Rust struct
#[derive(Debug, Error)]
pub enum SerializationError {
    /// Errors from create YaserDe
    #[error("Failed to serialize/deserialize between onvif xml and Rust struct")]
    YaserDeError,

    /// Fail to parse string to data type xmltree::Element
    #[error("Failed to parse string to a xmltree::Element")]
    ParseError(#[from] xmltree::ParseError),

    /// Xml string does not have the SOAP body as a child element
    #[error("Soap body doesn't exist")]
    SoapBodyNotFound,

    /// Xml string has the SOAP body, but the body is empty
    #[error("Soap body is empty")]
    SoapBodyIsEmpty,

    /// Onvif server returns soap error message
    #[error("Onvif server returns soap error")]
    SoapFault,

    /// Other internal error
    #[error("Other internal error")]
    Others,
}
