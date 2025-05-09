// Suppress linter warnings for autogenerated files.
#![allow(missing_docs, non_snake_case)]

/// This file holds Rust structs of the ONVIF media1.0 APIs
/// The structs are autogenerated from the CLI from xsd-parser-rs-0.x package
use yaserde_derive::{YaDeserialize, YaSerialize};

/// onvif API GetProfiles for Media Service
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "trt", namespace = "trt: http://www.onvif.org/ver10/media/wsdl")]
pub struct GetProfiles {}

/// response of onvif API GetProfiles for media service
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "trt", namespace = "trt: http://www.onvif.org/ver10/media/wsdl")]
pub struct GetProfilesResponse {
    /// lists all profiles that exist in the media service
    #[yaserde(prefix = "trt", rename = "Profiles")]
    pub profiles: Vec<Profile>,
}

/// Struct that defines holds Onvif Profile info
/// A media profile consists of a set of media configurations. Media profiles are
/// used by a client
/// to configure properties of a media stream from an NVT.
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct Profile {
    /// Optional configuration of the Video input.
    #[yaserde(prefix = "tt", rename = "VideoSourceConfiguration")]
    pub video_source_configuration: Option<VideoSourceConfiguration>,

    /// Reference to the physical input
    #[yaserde(attribute, rename = "token")]
    pub token: String,
}

/// Struct that holds Video Configuration of Profiles
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct VideoSourceConfiguration {
    /// Reference to the physical input.
    #[yaserde(prefix = "tt", rename = "SourceToken")]
    pub source_token: String,

    /// Rectangle specifying the Video capturing area. The capturing area shall
    /// not be larger than the whole Video source area.
    #[yaserde(prefix = "tt", rename = "Bounds")]
    pub bounds: IntRectangle,

    /// Optional Extension for Video Source Configuration
    #[yaserde(prefix = "tt", rename = "Extension")]
    pub extension: Option<VideoSourceConfigurationExtension>,

    /// Readonly parameter signalling Source configuration's view mode, for
    /// devices supporting different view modes as defined in tt:viewModes.
    #[yaserde(attribute, rename = "ViewMode")]
    pub view_mode: Option<String>,

    /// User readable name. Length up to 64 characters.
    #[yaserde(prefix = "tt", rename = "Name")]
    pub name: String,

    /// Number of internal references currently using this configuration.
    #[yaserde(prefix = "tt", rename = "UseCount")]
    pub use_count: i32,
}

/// Rectangle defined by lower left corner position and size. Units are pixel.
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct IntRectangle {
    /// Number specifying the Video capturing area
    #[yaserde(attribute, rename = "x")]
    pub x: i32,

    /// Number specifying the Video capturing area
    #[yaserde(attribute, rename = "y")]
    pub y: i32,

    /// Width specifying the Video capturing area
    #[yaserde(attribute, rename = "width")]
    pub width: i32,

    ///Height specifying the Video capturing area
    #[yaserde(attribute, rename = "height")]
    pub height: i32,
}

/// User readable name. Length up to 64 characters.
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct Name {
    /// User readable name for specifying the VideoSourceConfiguration
    #[yaserde(attribute, rename = "name")]
    pub name: String,
}

/// Unique identifier for a physical or logical resource.
/// Tokens should be assigned such that they are unique within a device. Tokens
/// must be at least unique within its class.
/// Length up to 64 characters.
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct ReferenceToken {
    /// Unique identifier of the profile.
    #[yaserde(attribute, rename = "token")]
    pub reference_token: String,
}

/// Struct that holds VideoSourceConfigurationExtension of VideoSourceConfiguration
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct VideoSourceConfigurationExtension {
    /// Optional element to configure rotation of captured image.
    /// What resolutions a device supports shall be unaffected by the Rotate
    /// parameters.
    #[yaserde(prefix = "tt", rename = "Rotate")]
    pub rotate: Option<Rotate>,

    /// Extension for VideoSourceConfigurationExtension
    #[yaserde(prefix = "tt", rename = "Extension")]
    pub extension: Option<VideoSourceConfigurationExtension2>,
}

/// Struct that holds VideoSourceConfigurationExtension2 of VideoSourceConfigurationExtension
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct VideoSourceConfigurationExtension2 {
    /// Optional element describing the geometric lens distortion. Multiple
    /// instances for future variable lens support.
    #[yaserde(prefix = "tt", rename = "LensDescription")]
    pub lens_description: Vec<LensDescription>,

    /// Optional element describing the scene orientation in the camera’s field
    /// of view.
    #[yaserde(prefix = "tt", rename = "SceneOrientation")]
    pub scene_orientation: SceneOrientation,
}

///Struct that holds SceneOrientationOption of VideoSourceConfigurationOptionsExtension2
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct SceneOrientation {
    /// Parameter to assign the way the camera determines the scene orientation.
    #[yaserde(prefix = "tt", rename = "Mode")]
    pub mode: SceneOrientationMode,

    /// Assigned or determined scene orientation based on the Mode. When
    /// assigning the Mode to AUTO, this field
    /// is optional and will be ignored by the device. When assigning the Mode to
    /// MANUAL, this field is required
    /// and the device will return an InvalidArgs fault if missing.
    #[yaserde(prefix = "tt", rename = "Orientation")]
    pub orientation: Option<String>,
}

///Struct that holds SceneOrientationMode of VideoSourceConfigurationOptionsExtension2
#[derive(PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub enum SceneOrientationMode {
    /// Parameter Manual to assign the way the camera determines the scene orientation.
    #[yaserde(rename = "MANUAL")]
    Manual,

    /// Parameter Auto to assign the way the camera determines the scene orientation.
    #[yaserde(rename = "AUTO")]
    Auto,

    /// Parameter Auto to assign the way the camera determines the scene orientation.
    #[yaserde(rename = "UNKNOWN")]
    __Unknown__(String),
}

impl Default for SceneOrientationMode {
    fn default() -> SceneOrientationMode {
        Self::__Unknown__("No valid variants".into())
    }
}

/// Struct that holds Rotate of VideoSourceConfigurationExtension
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct Rotate {
    /// Parameter to enable/disable Rotation feature.
    #[yaserde(prefix = "tt", rename = "Mode")]
    pub mode: RotateMode,

    /// Optional parameter to configure how much degree of clockwise rotation of
    /// image for On mode. Omitting this parameter for On mode means 180 degree
    /// rotation.
    #[yaserde(prefix = "tt", rename = "Degree")]
    pub degree: Option<i32>,

    /// Extension for Rotate
    #[yaserde(prefix = "tt", rename = "Extension")]
    pub extension: Option<RotateExtension>,
}

/// Struct that holds RotateExtention of Rotate
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct RotateExtension {}

/// Struct that holds RotateMode of Rotate
#[derive(PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub enum RotateMode {
    /// Enable the Rotate feature. Degree of rotation is specified Degree
    /// parameter.
    #[yaserde(rename = "OFF")]
    Off,
    /// Disable the Rotate feature.
    #[yaserde(rename = "ON")]
    On,
    /// Rotate feature is automatically activated by the device.
    #[yaserde(rename = "AUTO")]
    Auto,

    /// Rotate feature is automatically activated by the device.
    #[yaserde(rename = "UNKNOWN")]
    __Unknown__(String),
}

impl Default for RotateMode {
    fn default() -> RotateMode {
        Self::__Unknown__("No valid variants".into())
    }
}

/// Struct that holds LensProjection of LensDescription
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct LensProjection {
    /// Angle of incidence.
    #[yaserde(prefix = "tt", rename = "Angle")]
    pub angle: f64,

    /// Mapping radius as a consequence of the emergent angle.
    #[yaserde(prefix = "tt", rename = "Radius")]
    pub radius: f64,

    /// Optional ray absorption at the given angle due to vignetting. A value of
    /// one means no absorption.
    #[yaserde(prefix = "tt", rename = "Transmittance")]
    pub transmittance: Option<f64>,
}

/// Struct that holds LensOffset of LensDescription
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct LensOffset {
    /// Optional horizontal offset of the lens center in normalized coordinates.
    #[yaserde(attribute, rename = "x")]
    pub x: Option<f64>,

    /// Optional vertical offset of the lens center in normalized coordinates.
    #[yaserde(attribute, rename = "y")]
    pub y: Option<f64>,
}

/// Struct that holds LensDescription of VideoSourceConfigurationExtension2
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct LensDescription {
    /// Offset of the lens center to the imager center in normalized coordinates.
    #[yaserde(prefix = "tt", rename = "Offset")]
    pub offset: LensOffset,

    /// Radial description of the projection characteristics. The resulting curve
    /// is defined by the B-Spline interpolation
    /// over the given elements. The element for Radius zero shall not be
    /// provided. The projection points shall be ordered with ascending Radius.
    /// Items outside the last projection Radius shall be assumed to be invisible
    /// (black).
    #[yaserde(prefix = "tt", rename = "Projection")]
    pub projection: Vec<LensProjection>,

    /// Compensation of the x coordinate needed for the ONVIF normalized
    /// coordinate system.
    #[yaserde(prefix = "tt", rename = "XFactor")]
    pub x_factor: f64,

    /// Optional focal length of the optical system.
    #[yaserde(attribute, rename = "FocalLength")]
    pub focal_length: Option<f64>,
}

/// onvif API GetStreamUri for media service
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "trt", namespace = "trt: http://www.onvif.org/ver10/media/wsdl")]
pub struct GetStreamUri {
    /// Stream Setup that should be used with the uri
    #[yaserde(prefix = "trt", rename = "StreamSetup")]
    pub stream_setup: StreamSetup,

    /// The ProfileToken element indicates the media profile to use and will
    /// define the configuration of the content of the stream.
    #[yaserde(prefix = "trt", rename = "ProfileToken")]
    pub profile_token: String,
}

///Struct that holds StreamSetup of GetStreamUri
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct StreamSetup {
    /// Defines if a multicast or unicast stream is requested
    #[yaserde(prefix = "tt", rename = "Stream")]
    pub stream: StreamType,

    /// contains information related to network
    #[yaserde(prefix = "tt", rename = "Transport")]
    pub transport: Transport,
}

/// Struct that holds StreamType of StreamSetup
#[derive(PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub enum StreamType {
    /// Defines the StreamType RTP is Unicast
    #[yaserde(rename = "RTP-Unicast")]
    RtpUnicast,

    /// Defines the StreamType RTP is Multicast
    #[yaserde(rename = "RTP-Multicast")]
    RtpMulticast,

    /// Defines the StreamType
    #[yaserde(rename = "UNKNOWN")]
    __Unknown__(String),
}

impl Default for StreamType {
    fn default() -> StreamType {
        Self::__Unknown__("No valid variants".into())
    }
}

/// Struct that holds Transport of StreamSetup
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct Transport {
    /// Defines the network protocol for streaming, either UDP=RTP/UDP,
    /// RTSP=RTP/RTSP/TCP or HTTP=RTP/RTSP/HTTP/TCP
    #[yaserde(prefix = "tt", rename = "Protocol")]
    pub protocol: TransportProtocol,

    /// Optional element to describe further tunnel options. This element is
    /// normally not needed
    #[yaserde(prefix = "tt", rename = "Tunnel")]
    pub tunnel: Vec<Transport>,
}

/// Struct that holds TransportProtocol of Transport
#[derive(PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub enum TransportProtocol {
    /// Defines the network protocol for streaming is UDP
    #[yaserde(rename = "UDP")]
    Udp,

    /// Defines the network protocol for streaming is TCP
    #[yaserde(rename = "TCP")]
    Tcp,

    /// Defines the network protocol for streaming is RTSP
    #[yaserde(rename = "RTSP")]
    Rtsp,

    /// Defines the network protocol for streaming is HTTP
    #[yaserde(rename = "HTTP")]
    Http,

    /// Defines the network protocol for streaming
    #[yaserde(rename = "UNKNOWN")]
    __Unknown__(String),
}

impl Default for TransportProtocol {
    fn default() -> TransportProtocol {
        Self::__Unknown__("No valid variants".into())
    }
}

/// Onvif API GetStreamUri Response for Media service
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "trt", namespace = "trt: http://www.onvif.org/ver10/media/wsdl")]
pub struct GetStreamUriResponse {
    /// lists all media that exist in the media service
    #[yaserde(prefix = "trt", rename = "MediaUri")]
    pub media_uri: MediaUri,
}

/// Struct that holds MediaUri
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct MediaUri {
    /// Stable Uri to be used for requesting the media stream
    #[yaserde(prefix = "tt", rename = "Uri")]
    pub uri: String,

    /// Indicates if the Uri is only valid until the connection is established.
    /// The value shall be set to "false".
    #[yaserde(prefix = "tt", rename = "InvalidAfterConnect")]
    pub invalid_after_connect: bool,

    /// Indicates if the Uri is invalid after a reboot of the device. The value
    /// shall be set to "false".
    #[yaserde(prefix = "tt", rename = "InvalidAfterReboot")]
    pub invalid_after_reboot: bool,
}

/// onvif API GetSnapshotUri for media service
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "trt", namespace = "trt: http://www.onvif.org/ver10/media/wsdl")]
pub struct GetSnapshotUri {
    /// The ProfileToken element indicates the media profile to use and will
    /// define the configuration of the content of the stream.
    #[yaserde(prefix = "trt", rename = "ProfileToken")]
    pub profile_token: String,
}

/// Onvif API GetStreamUri Response for Media service
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "trt", namespace = "trt: http://www.onvif.org/ver10/media/wsdl")]
pub struct GetSnapshotUriResponse {
    /// lists all media that exist in the media service
    #[yaserde(prefix = "trt", rename = "MediaUri")]
    pub media_uri: MediaUri,
}
