use config::builder::AsyncState;
use serde_derive::Deserialize;
use std::error::Error;

/// used for bootstrapping onvif client
#[derive(Debug, Clone, Deserialize)]
pub struct MediaConfig {
    /// Onvif account name.
    pub onvif_account_name: String,
    /// Onvif password.
    pub onvif_password: String,
    /// CA Cert Path.
    pub ca_cert_path: String,
    /// directory path.
    pub dir_path: String,
    /// AWS IOT Credential Endpoint.
    pub aws_iot_credential_endpoint: String,
    /// AWS Region.
    pub aws_region: String,
    /// KVS Role Alias.
    pub kvs_role_alias: String,
    /// Device Id.
    pub client_id: String,
}

/// get onvif config
pub async fn get_media_config(config_path: &str) -> Result<MediaConfig, Box<dyn Error>> {
    let cfg = config::ConfigBuilder::<AsyncState>::default()
        .add_source(config::File::with_name(config_path))
        .build()
        .await?;

    let config_path_to_stream = cfg.try_deserialize().expect("Could not generate config path.");
    Ok(config_path_to_stream)
}

#[cfg(test)]
mod tests {
    use crate::config::get_media_config;
    const PATH_TO_CONFIG_STREAMING: &str = "tests/test_config/test_streaming_config.yaml";
    const ACCOUNT_NAME: &str = "Foo";
    const ONVIF_PASSWORD: &str = "Bar";
    const CA_CERT_PATH: &str = "./certificates/AmazonRootCA1.crt";
    const DIR_PATH: &str = "./certificates";
    const AWS_IOT_CREDENTIALS_END_POINT: &str = "abc.foobar.us-west-2.amazonaws.com";
    const AWS_REGION: &str = "us-west-2";
    const KVS_ROLE_ALIAS: &str = "FooBarRoleAlias";
    const DEVICE_ID: &str = "TestClientId";

    #[tokio::test]
    async fn verify_get_video_processing_config() {
        let config_path = std::env::current_dir()
            .unwrap()
            .join(PATH_TO_CONFIG_STREAMING)
            .to_str()
            .unwrap()
            .to_owned();
        let config = get_media_config(&config_path).await.unwrap();
        assert_eq!(config.onvif_account_name, ACCOUNT_NAME);
        assert_eq!(config.onvif_password, ONVIF_PASSWORD);
        assert_eq!(config.ca_cert_path, CA_CERT_PATH);
        assert_eq!(config.dir_path, DIR_PATH);
        assert_eq!(config.aws_iot_credential_endpoint, AWS_IOT_CREDENTIALS_END_POINT);
        assert_eq!(config.aws_region, AWS_REGION);
        assert_eq!(config.kvs_role_alias, KVS_ROLE_ALIAS);
        assert_eq!(config.client_id, DEVICE_ID);
    }
}
