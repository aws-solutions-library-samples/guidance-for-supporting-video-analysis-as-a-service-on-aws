use crate::client::constant::{
    ANALYTICS_SERVICE_VER20, ANALYTICS_SERVICE_VER20_NAMESPACE, APPMGMT_SERVICE,
    APPMGMT_SERVICE_NAMESPACE, DEVICE_IO_SERVICE, DEVICE_IO_SERVICE_NAMESPACE, DEVICE_SERVICE,
    DEVICE_SERVICE_NAMESPACE, EVENTS_SERVICE, EVENTS_SERVICE_NAMESPACE, IMAGING_SERVICE_VER20,
    IMAGING_SERVICE_VER20_NAMESPACE, MEDIA_SERVICE_VER10, MEDIA_SERVICE_VER10_NAMESPACE,
    MEDIA_SERVICE_VER20, MEDIA_SERVICE_VER20_NAMESPACE,
};
use crate::client::error::OnvifClientError;
use std::str::FromStr;

/// This enum is a centralized place to access all onvif service name
#[derive(Debug, PartialEq, Eq, Hash)]
pub enum OnvifServiceName {
    /// onvif device service
    /// https://www.onvif.org/ver10/device/wsdl/devicemgmt.wsdl
    DeviceService,

    /// onvif media service ver1
    /// https://www.onvif.org/ver10/media/wsdl/media.wsdl
    MediaServiceVer10,

    /// onvif media service ver2
    /// https://www.onvif.org/ver20/media/wsdl/media.wsdl
    MediaServiceVer20,

    /// onvif events service
    /// https://www.onvif.org/ver10/events/wsdl/event.wsdl
    EventsService,

    /// onvif imaging service
    /// https://www.onvif.org/ver20/imaging/wsdl/imaging.wsdl
    ImagingService,

    /// onvif deviceIO service
    /// https://www.onvif.org/ver10/deviceio.wsdl
    DeviceIoService,

    /// onvif analytics service
    /// https://www.onvif.org/ver20/analytics/wsdl/analytics.wsdl
    AnalyticsService,

    /// onvif app mgmt service
    /// https://www.onvif.org/ver10/appmgmt/wsdl/appmgmt.wsdl
    AppMgmtService,
}

impl FromStr for OnvifServiceName {
    type Err = OnvifClientError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            DEVICE_SERVICE => Ok(Self::DeviceService),
            DEVICE_SERVICE_NAMESPACE => Ok(Self::DeviceService),
            MEDIA_SERVICE_VER10 => Ok(Self::MediaServiceVer10),
            MEDIA_SERVICE_VER10_NAMESPACE => Ok(Self::MediaServiceVer10),
            MEDIA_SERVICE_VER20 => Ok(Self::MediaServiceVer20),
            MEDIA_SERVICE_VER20_NAMESPACE => Ok(Self::MediaServiceVer20),
            EVENTS_SERVICE => Ok(Self::EventsService),
            EVENTS_SERVICE_NAMESPACE => Ok(Self::EventsService),
            IMAGING_SERVICE_VER20 => Ok(Self::ImagingService),
            IMAGING_SERVICE_VER20_NAMESPACE => Ok(Self::ImagingService),
            DEVICE_IO_SERVICE => Ok(Self::DeviceIoService),
            DEVICE_IO_SERVICE_NAMESPACE => Ok(Self::DeviceIoService),
            ANALYTICS_SERVICE_VER20 => Ok(Self::AnalyticsService),
            ANALYTICS_SERVICE_VER20_NAMESPACE => Ok(Self::AnalyticsService),
            APPMGMT_SERVICE => Ok(Self::AppMgmtService),
            APPMGMT_SERVICE_NAMESPACE => Ok(Self::AppMgmtService),
            _ => Err(OnvifClientError::OnvifServiceDoesntExist),
        }
    }
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::client::constant::DEVICE_SERVICE;

    #[test]
    fn verify_create_onvif_service_name_device() {
        let service = OnvifServiceName::from_str(DEVICE_SERVICE).unwrap();

        assert_eq!(service, OnvifServiceName::DeviceService);
    }

    #[test]
    fn verify_create_onvif_service_name_media() {
        let service = OnvifServiceName::from_str(MEDIA_SERVICE_VER10_NAMESPACE).unwrap();

        assert_eq!(service, OnvifServiceName::MediaServiceVer10);
    }

    #[test]
    fn create_onvif_service_name_service_does_not_exist_error() {
        let err = OnvifServiceName::from_str("a_nonexistent_service").unwrap_err();

        assert_eq!(err.to_string(), OnvifClientError::OnvifServiceDoesntExist.to_string());
    }
}
