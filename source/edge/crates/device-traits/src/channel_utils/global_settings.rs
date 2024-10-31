use serde_derive::Deserialize;

/// Global configuration struct which can be accessed anywhere in edge process
#[derive(Debug, Clone, Deserialize, Default)]
pub struct Configurations {
    /// This must match the IoT Thing name
    pub client_id: String,
    /// CA Cert path
    #[serde(alias = "ca_cert_path")]
    pub ca_path: String,
    /// IoT Cert path
    #[serde(alias = "pem_cert_path")]
    pub cert_path: String,
    /// IoT Credential Endpoint
    #[serde(alias = "aws_iot_credential_endpoint")]
    pub credential_endpoint: String,
    /// IoT Private Key Path
    #[serde(alias = "key_path")]
    pub key_path: String,
    /// Role alias used for communicating with cloud
    #[serde(alias = "role_alias")]
    pub role_aliases: String,
    /// Region
    pub aws_region: String,
    /// Primary directory to be used.
    #[serde(alias = "dir_path")]
    pub dir: String,
}
