use config::builder::AsyncState;
use config::{Config as RustConfig, ConfigError};
use mockall::automock;

/// Settings structure for main process, pulled from a config file
pub mod settings;

/// Trait used for mocking config operations, by injecting config objects into tests.
#[automock]
pub trait Config {
    /// Get clone of config used for generating structs unique to injected dependencies
    fn get_config(&self) -> RustConfig;
    /// Get settings, will panic if they cannot be generated
    fn get_settings(&self) -> settings::Settings;
}

#[derive(Clone, Debug, Default)]
///Wrapper for config dependency, used for mocking.
pub struct ConfigImpl {
    config: RustConfig,
}

impl Config for ConfigImpl {
    /// Get clone of config used for generating structs unique to injected dependencies
    fn get_config(&self) -> RustConfig {
        self.config.clone()
    }
    /// Get settings, will panic if they cannot be generated
    fn get_settings(&self) -> settings::Settings {
        self.config.clone().try_deserialize().expect("Could not generate Settings.")
    }
}

impl ConfigImpl {
    /// Constructor, take in a file path as a string and generate config object.
    /// Ensures this is performed correctly for the tokio runtime.
    pub async fn new(path_to_config: &str) -> Result<ConfigImpl, ConfigError> {
        // This call safely pulls information from the filesystem.  The config object can be serialized as needed
        // into easy to use rust structs.  The config file can be .toml, .json, or .yaml format.
        let provision_config = config::ConfigBuilder::<AsyncState>::default()
            .add_source(config::File::with_name(path_to_config))
            .build()
            .await?;
        Ok(ConfigImpl { config: provision_config })
    }
}
