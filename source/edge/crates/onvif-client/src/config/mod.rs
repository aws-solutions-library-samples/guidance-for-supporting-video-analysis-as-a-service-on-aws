use config::builder::AsyncState;
use serde_derive::Deserialize;
use std::error::Error;

/// used for bootstrapping onvif client
#[derive(Debug, Clone, Deserialize)]
pub struct OnvifClientConfig {
    pub(crate) onvif_account_name: String,
    pub(crate) onvif_password: String,
}

/// get onvif config
pub async fn get_onvif_config_from_file(
    config_path: &str,
) -> Result<OnvifClientConfig, Box<dyn Error>> {
    let cfg = config::ConfigBuilder::<AsyncState>::default()
        .add_source(config::File::with_name(config_path))
        .build()
        .await?;

    let onvif_config = cfg.try_deserialize().expect("Could not generate onvif client config.");

    Ok(onvif_config)
}

#[cfg(test)]
mod tests {
    use crate::config::get_onvif_config_from_file;
    const PATH_TO_CONFIG: &str = "tests/onvif_config.yaml";
    const VIDEO_LOGISTICS: &str = "VideoAnalytics";
    const ONVIF_PASSWORD: &str = "videoanalytics12345";

    #[tokio::test]
    async fn verify_get_onvif_config_from_file() {
        let config_path =
            std::env::current_dir().unwrap().join(PATH_TO_CONFIG).to_str().unwrap().to_owned();

        let onvif_config = get_onvif_config_from_file(config_path.as_str()).await.unwrap();

        assert_eq!(onvif_config.onvif_account_name, VIDEO_LOGISTICS);
        assert_eq!(onvif_config.onvif_password, ONVIF_PASSWORD);
    }
}
