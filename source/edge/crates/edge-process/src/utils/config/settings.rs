use serde::Deserialize;
use std::io::ErrorKind;
use std::path::PathBuf;
use tokio::fs;
use tokio::io::Error;

#[derive(Debug, Clone, Deserialize, Default)]
/// Holds settings required for edge-process
pub struct Settings {
    /// Edge Process must have read + write permissions for this directory
    /// This directory will store the generated files (logs+state)
    dir_path: PathBuf,
    /// Path to provided configurations
    config_path: PathBuf,
    /// Name of client
    client_id: String,
}

impl Settings {
    /// Get endpoint for logging.
    pub async fn get_dir(&self) -> std::io::Result<PathBuf> {
        let md = fs::metadata(self.dir_path.clone()).await?;
        let permissions = md.permissions();
        // This checks that it exists and is a directory.
        if !self.dir_path.is_dir() {
            return Err(Error::new(
                ErrorKind::Other,
                format!("{} : is not a directory", self.dir_path.to_str().unwrap_or_default()),
            ));
        }
        // Edge process needs to be able to write to this directory
        if permissions.readonly() {
            return Err(Error::new(
                ErrorKind::Other,
                format!("Cannot write to : {}", self.dir_path.to_str().unwrap_or_default()),
            ));
        }

        Ok(self.dir_path.clone())
    }

    /// Get a copy of the path to the config file
    pub fn get_config(&self) -> &PathBuf {
        &self.config_path
    }
    /// Get client_id
    pub fn get_client_id(&self) -> &String {
        &self.client_id
    }
    /// Create new settings object, useful for tests.
    pub fn new(dir_path: PathBuf, config_path: PathBuf, client_id: String) -> Settings {
        Settings { dir_path, config_path, client_id }
    }
}

#[cfg(test)]
mod tests {
    use super::Settings;

    const CONFIG_PATH: &str = "/fake/path";
    const DIRECTORY: &str = "/fake/path/dir";
    const CLIENT_ID: &str = "name";

    #[test]
    fn get_config_test() {
        let settings = setup_settings();
        assert_eq!(settings.get_config().as_os_str().to_str().unwrap().to_string(), CONFIG_PATH);
    }

    #[test]
    fn settings_get_client_id() {
        let settings = setup_settings();
        assert_eq!(settings.get_client_id(), CLIENT_ID);
    }

    fn setup_settings() -> Settings {
        Settings::new(DIRECTORY.into(), CONFIG_PATH.into(), CLIENT_ID.to_owned())
    }
}
