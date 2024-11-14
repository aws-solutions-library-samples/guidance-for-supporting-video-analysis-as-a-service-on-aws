use crate::xsd_rs::soap_envelop::Fault;
use serialization_error::SerializationError;
use xmltree::{Element, Namespace, XMLNode};
use yaserde::{YaDeserialize, YaSerialize};

/// Errors that the serialization and deserialization may throw
pub mod serialization_error;

/// Rust struct -> XML string -> SOAP message
pub fn serialize<T: YaSerialize>(onvif_struct: &T) -> Result<String, SerializationError> {
    let soap_message = soap(rust2xml(onvif_struct)?.as_str())?;
    Ok(soap_message)
}

/// XML string -> SOAP message
fn soap(onvif_xml: &str) -> Result<String, SerializationError> {
    let soap_body = wrap_xml_with_soap_body(onvif_xml)?;
    let mut soap = wrap_body_with_soap_envelop(soap_body);

    // covert from xmltree Element to String
    element_to_string(&mut soap)
}

/// Rust struct -> XML string
fn rust2xml<T: YaSerialize>(onvif_struct: &T) -> Result<String, SerializationError> {
    // Rust struct to XML using yaserde
    let onvif_xml =
        yaserde::ser::to_string(onvif_struct).map_err(|_| SerializationError::YaserDeError)?;

    Ok(onvif_xml)
}

/// wrap onvif xml with <s:Body></s:Body>
fn wrap_xml_with_soap_body(onvif_xml: &str) -> Result<Element, SerializationError> {
    let parsed_xml = Element::parse(onvif_xml.as_bytes())?;

    let mut body = Element::new("Body");
    body.prefix = Some("s".to_string());
    body.children.push(XMLNode::Element(parsed_xml));

    Ok(body)
}

/// wrap onvif xml with <s:Envelope></s:Envelope>
fn wrap_body_with_soap_envelop(soap_body: Element) -> Element {
    let mut envelope = Element::new("Envelope");
    envelope.prefix = Some("s".to_string());
    envelope.children.push(XMLNode::Element(soap_body));

    let mut name_spaces = Namespace::empty();
    name_spaces.put("s", "http://www.w3.org/2003/05/soap-envelope");
    envelope.namespaces = Some(name_spaces);
    envelope
}

/// SOAP message -> XML string -> Rust struct or
/// SOAP fault message -> OnvifClientError
pub fn deserialize<T: YaDeserialize>(soap: &str) -> Result<T, SerializationError> {
    let rust = xml2rust(unsoap(soap)?.as_str())?;

    Ok(rust)
}

/// SOAP message -> XML string
fn unsoap(soap: &str) -> Result<String, SerializationError> {
    // Element::parse strips the soap envelop + soap body and pass the namespaces down to the child of the body
    let mut xml_soap_envelope = Element::parse(soap.as_bytes())?;
    let xml_soap_body: &mut Element =
        xml_soap_envelope.get_mut_child("Body").ok_or(SerializationError::SoapBodyNotFound)?;

    // handle SOAP fault scenario
    if let Some(fault) = xml_soap_body.get_mut_child("Fault") {
        let _fault = deserialize_fault(fault)?;

        // for now return this error to encapsulate all soap fault.
        return Err(SerializationError::SoapFault);
    }

    // normal ONVIF SOAP body
    let xml_soap_body_element = xml_soap_body
        .children
        .iter_mut()
        .find_map(|node| match node {
            XMLNode::Element(xml_element) => Some(xml_element),
            _ => None,
        })
        .ok_or(SerializationError::SoapBodyIsEmpty)?;

    let onvif_xml = element_to_string(xml_soap_body_element)?;

    Ok(onvif_xml)
}

/// XML string -> Rust struct
fn xml2rust<T: YaDeserialize>(onvif_xml: &str) -> Result<T, SerializationError> {
    // XML to Rust struct using yaserde
    let onvif_struct: T =
        yaserde::de::from_str(onvif_xml).map_err(|_| SerializationError::YaserDeError)?;

    Ok(onvif_struct)
}

/// convert data type xmltree::Element to String
fn element_to_string(input: &mut Element) -> Result<String, SerializationError> {
    let mut output = vec![];
    let _ = input.write(&mut output);
    let string = String::from_utf8(output).map_err(|_| SerializationError::Others)?;

    Ok(string)
}

/// deserialize onvif SOAP fault and return corresponding OnvifClientError
fn deserialize_fault(fault: &mut Element) -> Result<Fault, SerializationError> {
    xml2rust(element_to_string(fault)?.as_str())
}

#[cfg(test)]
mod test {
    use super::*;
    use crate::wsdl_rs::devicemgmt::{GetDeviceInformation, GetDeviceInformationResponse};
    use crate::xsd_rs::soap_envelop::Fault;

    #[test]
    fn verify_serialize() {
        // Arrange
        let get_device_info_onvif_struct: GetDeviceInformation = GetDeviceInformation {};

        // Act
        let mut onvif_soap_message = serialize(&get_device_info_onvif_struct).unwrap();
        onvif_soap_message.retain(|c| !c.is_whitespace());

        // Assert
        assert_eq!(onvif_soap_message, expected_get_device_info_soap_string())
    }

    #[test]
    fn verify_deserialize() {
        // Arrange
        let expected_get_device_info_resp_struct = GetDeviceInformationResponse {
            manufacturer: "foobar".to_string(),
            model: "foo_bar".to_string(),
            firmware_version: "0.0.1".to_string(),
            serial_number: "987654".to_string(),
            hardware_id: "1".to_string(),
        };

        // Act
        let onvif_struct: GetDeviceInformationResponse =
            deserialize(GET_DEVICE_INFO_RESPONSE_SOAP_STRING).unwrap();

        // Assert
        assert_eq!(onvif_struct, expected_get_device_info_resp_struct)
    }

    #[test]
    fn verify_deserialize_fault() {
        // Arrange and Act
        let mut xml_soap_envelope = Element::parse(SOAP_FAULT_NOT_AUTHORIZED.as_bytes()).unwrap();
        let xml_soap_body: &mut Element = xml_soap_envelope.get_mut_child("Body").unwrap();
        let fault = xml_soap_body.get_mut_child("Fault").unwrap();

        let fault: Fault = deserialize_fault(fault).unwrap();

        // Assert
        assert_eq!(fault.code.subcode.unwrap().value, "ter:NotAuthorized".to_string());
    }

    #[test]
    fn verify_unsoap_when_soap_fault_return_error() {
        // Arrange and Act
        let err = unsoap(SOAP_FAULT_NOT_AUTHORIZED).unwrap_err();

        // Assert
        assert_eq!(err.to_string(), SerializationError::SoapFault.to_string());
    }

    fn expected_get_device_info_soap_string() -> String {
        let expected_get_device_info_soap_message = r#"
        <?xml version="1.0" encoding="UTF-8"?>
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
            <s:Body>
                <tds:GetDeviceInformation xmlns:tds="http://www.onvif.org/ver10/device/wsdl" />
            </s:Body>
        </s:Envelope>"#;

        remove_whitespace_from_str(expected_get_device_info_soap_message)
    }

    fn remove_whitespace_from_str(str: &str) -> String {
        let mut str_string_type = str.to_string();
        str_string_type.retain(|c| !c.is_whitespace());

        str_string_type
    }

    // soap message copied from onvif response of GetDeviceInformation
    const GET_DEVICE_INFO_RESPONSE_SOAP_STRING: &str = r#"<?xml version="1.0" encoding="utf-8"?>
    <SOAP-ENV:Envelope
        xmlns:SOAP-ENV="http://www.w3.org/2003/05/soap-envelope"
        xmlns:SOAP-ENC="http://www.w3.org/2003/05/soap-encoding"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:chan="http://schemas.microsoft.com/ws/2005/02/duplex"
        xmlns:wsa5="http://www.w3.org/2005/08/addressing"
        xmlns:c14n="http://www.w3.org/2001/10/xml-exc-c14n#"
        xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
        xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
        xmlns:wsc="http://schemas.xmlsoap.org/ws/2005/02/sc"
        xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
        xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
        xmlns:xmime="http://tempuri.org/xmime.xsd"
        xmlns:xop="http://www.w3.org/2004/08/xop/include"
        xmlns:tt="http://www.onvif.org/ver10/schema"
        xmlns:wsrfbf="http://docs.oasis-open.org/wsrf/bf-2"
        xmlns:wstop="http://docs.oasis-open.org/wsn/t-1"
        xmlns:wsrfr="http://docs.oasis-open.org/wsrf/r-2"
        xmlns:tan="http://www.onvif.org/ver20/analytics/wsdl"
        xmlns:tds="http://www.onvif.org/ver10/device/wsdl"
        xmlns:tev="http://www.onvif.org/ver10/events/wsdl"
        xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
        xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl"
        xmlns:tmd="http://www.onvif.org/ver10/deviceIO/wsdl"
        xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl"
        xmlns:trt="http://www.onvif.org/ver10/media/wsdl"
        xmlns:ter="http://www.onvif.org/ver10/error"
        xmlns:tns1="http://www.onvif.org/ver10/topics">
        <SOAP-ENV:Body>
        <tds:GetDeviceInformationResponse>
            <tds:Manufacturer>foobar</tds:Manufacturer>
            <tds:Model>foo_bar</tds:Model>
            <tds:FirmwareVersion>0.0.1</tds:FirmwareVersion>
            <tds:SerialNumber>987654</tds:SerialNumber>
            <tds:HardwareId>1</tds:HardwareId>
        </tds:GetDeviceInformationResponse>
        </SOAP-ENV:Body>
    </SOAP-ENV:Envelope>"#;

    const SOAP_FAULT_NOT_AUTHORIZED: &str = r#"<?xml version="1.0" encoding="UTF-8"?>
    <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://www.w3.org/2003/05/soap-envelope" xmlns:SOAP-ENC="http://www.w3.org/2003/05/soap-encoding" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:chan="http://schemas.microsoft.com/ws/2005/02/duplex" xmlns:wsa5="http://www.w3.org/2005/08/addressing" xmlns:c14n="http://www.w3.org/2001/10/xml-exc-c14n#" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" xmlns:saml1="urn:oasis:names:tc:SAML:1.0:assertion" xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" xmlns:xenc="http://www.w3.org/2001/04/xmlenc#" xmlns:wsc="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:xmime="http://tempuri.org/xmime.xsd" xmlns:xop="http://www.w3.org/2004/08/xop/include" xmlns:ns2="http://www.onvif.org/ver20/analytics/humanface" xmlns:ns3="http://www.onvif.org/ver20/analytics/humanbody" xmlns:wsrfbf="http://docs.oasis-open.org/wsrf/bf-2" xmlns:wstop="http://docs.oasis-open.org/wsn/t-1" xmlns:tt="http://www.onvif.org/ver10/schema" xmlns:wsrfr="http://docs.oasis-open.org/wsrf/r-2" xmlns:ns1="http://www.onvif.org/ver20/media/wsdl" xmlns:tan="http://www.onvif.org/ver20/analytics/wsdl" xmlns:tds="http://www.onvif.org/ver10/device/wsdl" xmlns:tev="http://www.onvif.org/ver10/events/wsdl" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2" xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl" xmlns:tmd="http://www.onvif.org/ver10/deviceIO/wsdl" xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl" xmlns:trt="http://www.onvif.org/ver10/media/wsdl" xmlns:ter="http://www.onvif.org/ver10/error" xmlns:tns1="http://www.onvif.org/ver10/topics">
      <SOAP-ENV:Body>
        <SOAP-ENV:Fault>
          <SOAP-ENV:Code>
            <SOAP-ENV:Value>SOAP-ENV:Sender</SOAP-ENV:Value>
            <SOAP-ENV:Subcode>
              <SOAP-ENV:Value>ter:NotAuthorized</SOAP-ENV:Value>
            </SOAP-ENV:Subcode>
          </SOAP-ENV:Code>
          <SOAP-ENV:Reason>
            <SOAP-ENV:Text xml:lang="en">Sender not Authorized</SOAP-ENV:Text>
          </SOAP-ENV:Reason>
        </SOAP-ENV:Fault>
      </SOAP-ENV:Body>
    </SOAP-ENV:Envelope>"#;
}
