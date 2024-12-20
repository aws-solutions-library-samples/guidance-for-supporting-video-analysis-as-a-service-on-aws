use serde_derive::Deserialize;

/// Global configuration struct which can be accessed anywhere in edge process
#[derive(Debug, Clone, Deserialize, Default)]
#[cfg(not(feature = "sd-card-catchup"))]
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
    #[serde(alias = "kvs_role_alias")]
    pub role_aliases: String,
    /// Region
    pub aws_region: String,
    /// Primary directory to be used.
    #[serde(alias = "dir_path")]
    pub dir: String,
}

/// Alternate struct with fields for SD card catchup
#[derive(Debug, Clone, Deserialize, Default)]
#[cfg(feature = "sd-card-catchup")]
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
    #[serde(alias = "kvs_role_alias")]
    pub role_aliases: String,
    /// Region
    pub aws_region: String,
    /// Primary directory to be used.
    #[serde(alias = "dir_path")]
    pub dir: String,
    /// local storage path for sd card
    pub local_storage_path: String,
    /// optional db_path. Otherwise will just store SQLite DB in local_storage_path
    pub db_path: Option<String>,
    /// option local storage disk usage for SD card
    pub local_storage_disk_usage: Option<u64>,
}
