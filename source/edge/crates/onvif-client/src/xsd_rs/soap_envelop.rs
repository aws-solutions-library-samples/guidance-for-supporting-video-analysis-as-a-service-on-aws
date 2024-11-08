//use http://www.w3.org/2001/xml.xsd  http://www.w3.org/XML/1998/namespace;

use yaserde_derive::{YaDeserialize, YaSerialize};

// Video analytics only cares about the the subcode within Faultcode. But for completeness, including all fields of Fault.
// For more ONVIF SOAP error details, refer to 5.11.2 SOAP errors of this doc
// https://www.onvif.org/specs/core/ONVIF-Core-Specification-v1912.pdf

/// SOAP fault
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tns", namespace = "tns: http://www.w3.org/2003/05/soap-envelope")]
pub struct Fault {
    /// fault code
    #[yaserde(prefix = "tns", rename = "Code")]
    pub code: Faultcode,

    /// fault reason
    #[yaserde(prefix = "tns", rename = "Reason")]
    pub reason: Faultreason,

    /// The node contains the URI of the SOAP node that generated the fault.
    #[yaserde(prefix = "tns", rename = "Node")]
    pub node: Option<String>,

    /// The role  contains a URI that identifies the role in which the node was operating at the point the fault occurred.
    #[yaserde(prefix = "tns", rename = "Role")]
    pub role: Option<String>,

    /// application-specific error information related to the SOAP fault codes describing the fault
    #[yaserde(prefix = "tns", rename = "Detail")]
    pub detail: Option<Detail>,
}

/// fault reason
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tns", namespace = "tns: http://www.w3.org/2003/05/soap-envelope")]
pub struct Faultreason {
    /// Vec that holds the entire fault reason
    #[yaserde(prefix = "tns", rename = "Text")]
    pub text: Vec<Reasontext>,
}

/// string that holds fault reason wrapped in struct
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tns", namespace = "tns: http://www.w3.org/2003/05/soap-envelope")]
pub struct Reasontext {
    /// string that holds fault reason
    #[yaserde(attribute, prefix = "xml", rename = "lang")]
    // swap out xml::Lang and use String instead
    pub lang: String,
}

/// fault code
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tns", namespace = "tns: http://www.w3.org/2003/05/soap-envelope")]
pub struct Faultcode {
    /// string that holds FaultCode
    #[yaserde(prefix = "tns", rename = "Value")]
    // swap out FaultcodeEnum data type and use String to avoid UtilsTupleIo and UtilsDefaultSerde macro
    pub value: String,

    /// application specific subcode
    #[yaserde(prefix = "tns", rename = "Subcode")]
    pub subcode: Option<Subcode>,
}

// Using String to replace FaultcodeEnum
/*
#[derive(Default, PartialEq, Debug, UtilsTupleIo, UtilsDefaultSerde)]
pub struct FaultcodeEnum (pub String);
 */

/// subcode
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tns", namespace = "tns: http://www.w3.org/2003/05/soap-envelope")]
pub struct Subcode {
    /// string that holds subcod value
    #[yaserde(prefix = "tns", rename = "Value")]
    pub value: String,
    // TODO: if we decide to use this field, investigate why this field causes serialization issue.
    // this field is causing problem in test, since in onvif documentation it look like it'll be
    // empty field, comment out for now.
    /*
    #[yaserde(prefix = "tns", rename = "Subcode")]
    pub subcode: Vec<Subcode>,

     */
}

/// dummy struct
#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "tns", namespace = "tns: http://www.w3.org/2003/05/soap-envelope")]
pub struct Detail {}
