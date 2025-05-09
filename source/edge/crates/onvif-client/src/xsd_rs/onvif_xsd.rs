// GRCOV_STOP_COVERAGE
// Suppress linter warnings for autogenerated files.
#![allow(missing_docs, non_snake_case)]

use serde_derive::{Deserialize, Serialize};
use yaserde_derive::{YaDeserialize, YaSerialize};

/// onvif version
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct OnvifVersion {
    /// Major version number.
    #[yaserde(prefix = "tt", rename = "Major")]
    pub major: i32,

    /// Two digit minor version number.
    /// If major version number is less than "16", X.0.1 maps to "01" and X.2.1
    /// maps to "21" where X stands for Major version number.
    /// Otherwise, minor number is month of release, such as "06" for June.
    #[yaserde(prefix = "tt", rename = "Minor")]
    pub minor: i32,
}

/// List of String wrapped in struct
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize, Serialize, Deserialize)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct StringAttrList {
    /// vector of String
    #[yaserde(prefix = "tt", rename = "Items")]
    pub items: Vec<String>,
}

#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize, Serialize, Deserialize, Clone)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
#[serde(rename_all = "camelCase")]
pub struct VideoEncoder2Configuration {
    /// Video Media Subtype for the video format.
    /// Enabling default for serde so that deserialization does not fail if field is missing
    #[yaserde(prefix = "tt", rename = "Encoding")]
    #[serde(rename = "codec", default)]
    pub encoding: String,

    /// Configured video resolution
    /// Enabling default for serde so that deserialization does not fail if field is missing
    #[yaserde(prefix = "tt", rename = "Resolution")]
    #[serde(with = "resolution_as_str", default)]
    pub resolution: VideoResolution2,

    /// Optional element to configure rate control related parameters.
    #[yaserde(prefix = "tt", rename = "RateControl")]
    #[serde(skip_serializing_if = "Option::is_none", flatten)]
    pub rate_control: Option<VideoRateControl2>,

    /// Defines the multicast settings that could be used for video streaming.
    #[yaserde(prefix = "tt", rename = "Multicast")]
    #[serde(skip)]
    pub multicast: Option<MulticastConfiguration>,

    /// Relative value for the video quantizers and the quality of the video. A
    /// high value within supported quality range means higher quality
    #[yaserde(prefix = "tt", rename = "Quality")]
    #[serde(skip)]
    pub quality: f64,

    /// Group of Video frames length. Determines typically the interval in which
    /// the I-Frames will be coded. An entry of 1 indicates I-Frames are
    /// continuously generated. An entry of 2 indicates that every 2nd image is
    /// an I-Frame, and 3 only every 3rd frame, etc. The frames in between are
    /// coded as P or B Frames.
    #[yaserde(attribute, rename = "GovLength")]
    #[serde(skip_serializing_if = "Option::is_none", rename = "gopRange")]
    pub gov_length: Option<i32>,

    /// The encoder profile as defined in tt:VideoEncodingProfiles.
    #[yaserde(attribute, rename = "Profile")]
    #[serde(skip)]
    pub profile: Option<String>,

    /// A value of true indicates that frame rate is a fixed value rather than an
    /// upper limit,
    /// and that the video encoder shall prioritize frame rate over all other
    /// adaptable
    /// configuration values such as bitrate. Default is false.
    #[yaserde(attribute, rename = "GuaranteedFrameRate")]
    #[serde(skip)]
    pub guaranteed_frame_rate: Option<bool>,

    /// User readable name. Length up to 64 characters.
    /// Enabling default for serde so that deserialization does not fail if field is missing
    #[yaserde(prefix = "tt", rename = "Name")]
    #[serde(default)]
    pub name: String,

    /// Number of internal references currently using this configuration.
    #[yaserde(prefix = "tt", rename = "UseCount")]
    #[serde(skip)]
    pub use_count: i32,

    /// Token that uniquely references this configuration. Length up to 64
    /// characters.
    #[yaserde(attribute, rename = "token")]
    #[serde(skip)]
    pub token: String,
}

#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize, Serialize, Deserialize, Clone)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct VideoResolution2 {
    /// Number of the columns of the Video image.
    #[yaserde(prefix = "tt", rename = "Width")]
    pub width: i32,

    /// Number of the lines of the Video image.
    #[yaserde(prefix = "tt", rename = "Height")]
    pub height: i32,
}

mod resolution_as_str {
    use super::VideoResolution2;
    use serde::de::{Error, Visitor};
    use serde::{self, Deserializer, Serializer};

    pub fn serialize<S>(resolution: &VideoResolution2, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_str(format!("{}x{}", resolution.width, resolution.height).as_str())
    }

    struct VideoResolution2Visitor;
    impl<'de> Visitor<'de> for VideoResolution2Visitor {
        type Value = VideoResolution2;

        fn expecting(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            formatter.write_str("a string of format {width}x{}")
        }

        fn visit_str<E>(self, value: &str) -> Result<Self::Value, E>
        where
            E: Error,
        {
            let str_split: Vec<&str> = value.split('x').collect();
            let resolution = VideoResolution2 {
                width: str_split[0].parse::<i32>().unwrap(),
                height: str_split[1].parse::<i32>().unwrap(),
            };
            Ok(resolution)
        }
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<VideoResolution2, D::Error>
    where
        D: Deserializer<'de>,
    {
        deserializer.deserialize_str(VideoResolution2Visitor)
    }
}

#[derive(
    Default, PartialEq, Debug, YaSerialize, YaDeserialize, Serialize, Deserialize, Clone, Copy,
)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct VideoRateControl2 {
    /// Desired frame rate in fps. The actual rate may be lower due to e.g.
    /// performance limitations.
    /// Enabling default for serde so that deserialization does not fail if field is missing
    #[yaserde(prefix = "tt", rename = "FrameRateLimit")]
    #[serde(rename = "frameRate", default)]
    pub frame_rate_limit: f64,

    /// the maximum output bitrate in kbps
    /// Enabling default for serde so that deserialization does not fail if field is missing
    #[yaserde(prefix = "tt", rename = "BitrateLimit")]
    #[serde(rename = "bitRate", default)]
    pub bitrate_limit: i32,

    /// Enforce constant bitrate.
    #[yaserde(attribute, rename = "ConstantBitRate")]
    #[serde(rename = "bitRateType", with = "constant_bit_rate_as_str", default)]
    pub constant_bit_rate: Option<bool>,
}

mod constant_bit_rate_as_str {
    use serde::de::{Error, Visitor};
    use serde::{self, Deserializer, Serializer};

    pub fn serialize<S>(constant_bit_rate: &Option<bool>, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        if let Some(cbr) = constant_bit_rate {
            match cbr {
                true => {
                    return serializer.serialize_str("CBR");
                }
                false => {
                    return serializer.serialize_str("VBR");
                }
            }
        } else {
            return serializer.serialize_none();
        }
    }

    struct ConstantBitRateVisitor;
    impl<'de> Visitor<'de> for ConstantBitRateVisitor {
        type Value = Option<bool>;

        fn expecting(&self, formatter: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
            formatter.write_str("CBR or VBR")
        }

        fn visit_str<E>(self, value: &str) -> Result<Self::Value, E>
        where
            E: Error,
        {
            Ok(Some(value == "CBR"))
        }
    }

    pub fn deserialize<'de, D>(deserializer: D) -> Result<Option<bool>, D::Error>
    where
        D: Deserializer<'de>,
    {
        deserializer.deserialize_str(ConstantBitRateVisitor)
    }
}

#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize, Serialize, Deserialize, Clone)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct MulticastConfiguration {
    /// The multicast address (if this address is set to 0 no multicast streaming
    /// is enaled)
    #[yaserde(prefix = "tt", rename = "Address")]
    pub address: Ipaddress,

    /// The RTP mutlicast destination port. A device may support RTCP. In this
    /// case the port value shall be even to allow the corresponding RTCP stream
    /// to be mapped to the next higher (odd) destination port number as defined
    /// in the RTSP specification.
    #[yaserde(prefix = "tt", rename = "Port")]
    pub port: i32,

    /// In case of IPv6 the TTL value is assumed as the hop limit. Note that for
    /// IPV6 and administratively scoped IPv4 multicast the primary use for hop
    /// limit / TTL is to prevent packets from (endlessly) circulating and not
    /// limiting scope. In these cases the address contains the scope.
    #[yaserde(prefix = "tt", rename = "TTL")]
    pub ttl: i32,

    /// Read only property signalling that streaming is persistant. Use the
    /// methods StartMulticastStreaming and StopMulticastStreaming to switch its
    /// state.
    #[yaserde(prefix = "tt", rename = "AutoStart")]
    pub auto_start: bool,
}

#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize, Serialize, Deserialize, Clone)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub struct Ipaddress {
    /// Indicates if the address is an IPv4 or IPv6 address.
    #[yaserde(prefix = "tt", rename = "Type")]
    pub _type: Iptype,

    /// IPv4 address.
    #[yaserde(prefix = "tt", rename = "IPv4Address")]
    pub i_pv_4_address: Option<String>,

    /// IPv6 address
    #[yaserde(prefix = "tt", rename = "IPv6Address")]
    pub i_pv_6_address: Option<String>,
}

#[derive(PartialEq, Debug, YaSerialize, YaDeserialize, Serialize, Deserialize, Clone)]
#[yaserde(prefix = "tt", namespace = "tt: http://www.onvif.org/ver10/schema")]
pub enum Iptype {
    #[yaserde(rename = "IPv4")]
    Ipv4,
    #[yaserde(rename = "IPv6")]
    Ipv6,
    __Unknown__(String),
}

impl Default for Iptype {
    fn default() -> Iptype {
        Self::__Unknown__("No valid variants".into())
    }
}
