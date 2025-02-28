/// onvif device service
pub const DEVICE_SERVICE: &str = "device_service";

/// namespace for onvif device service
pub const DEVICE_SERVICE_NAMESPACE: &str = "http://www.onvif.org/ver10/device/wsdl";

/// onvif media service ver1
pub const MEDIA_SERVICE_VER10: &str = "media_service_ver10";

/// namespace for onvif media service ver1
pub const MEDIA_SERVICE_VER10_NAMESPACE: &str = "http://www.onvif.org/ver10/media/wsdl";

/// onvif media service ver2
pub const MEDIA_SERVICE_VER20: &str = "media_service_ver20";

/// namespace for onvif media service ver2
pub const MEDIA_SERVICE_VER20_NAMESPACE: &str = "http://www.onvif.org/ver20/media/wsdl";

/// onvif events service
pub const EVENTS_SERVICE: &str = "events_service";

/// namespace for onvif events service
pub const EVENTS_SERVICE_NAMESPACE: &str = "http://www.onvif.org/ver10/events/wsdl";

/// onvif imaging service ver2
pub const IMAGING_SERVICE_VER20: &str = "imaging_service_ver20";

/// namespace for onvif imaging service ver2
pub const IMAGING_SERVICE_VER20_NAMESPACE: &str = "http://www.onvif.org/ver20/imaging/wsdl";

/// onvif device IO service
pub const DEVICE_IO_SERVICE: &str = "deviceIO_service";

/// namespace for onvif device IO service
pub const DEVICE_IO_SERVICE_NAMESPACE: &str = "http://www.onvif.org/ver10/deviceIO/wsdl";

/// onvif analytics service ver2
pub const ANALYTICS_SERVICE_VER20: &str = "analytics_service";

/// namespace for onvif analytics service
pub const ANALYTICS_SERVICE_VER20_NAMESPACE: &str = "http://www.onvif.org/ver20/analytics/wsdl";

/// onvif appmgmt service
pub const APPMGMT_SERVICE: &str = "appmgmt_service";

/// namespace for onvif appmgmt service
pub const APPMGMT_SERVICE_NAMESPACE: &str = "http://www.onvif.org/ver10/appmgmt/wsdl";

/// Video Analytics' default onvif username
pub const VIDEO_ANALYTICS: &str = "VideoAnalytics";

/// onvif config path
pub const CONFIG_PATH: &str = "tests/";

/// digest uri.
/// Note, certain devices read uri in this format. This may not be true for all ONVIF devices.
pub const DIGEST_URI: &str = "/onvif/device_service";

/// Video encoder configuration payload fields
/// name field
pub const NAME_FIELD: &str = "name";

/// codec field
pub const CODEC_FIELD: &str = "codec";

/// bitRateType field
pub const BIT_RATE_TYPE_FIELD: &str = "bitRateType";

/// frameRate field
pub const FRAME_RATE_FIELD: &str = "frameRate";

/// resolution field
pub const RESOLUTION_FIELD: &str = "resolution";

/// bitRate field
pub const BIT_RATE_FIELD: &str = "bitRate";

/// gopRange field
pub const GOP_RANGE_FIELD: &str = "gopRange";
