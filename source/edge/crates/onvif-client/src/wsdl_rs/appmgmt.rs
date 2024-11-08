// Suppress linter warnings for autogenerated files.
#![allow(missing_docs, non_snake_case)]
use yaserde_derive::{YaDeserialize, YaSerialize};

#[derive(PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "ans", namespace = "ans: http://www.onvif.org/ver10/appmgmt/wsdl")]
pub enum AppState {
    Active,
    Inactive,
    Installing,
    Uninstalling,
    Removed,
    InstallationFailed,
    __Unknown__(String),
}

impl Default for AppState {
    fn default() -> AppState {
        Self::__Unknown__("No valid variants".into())
    }
}

#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "ans", namespace = "ans: http://www.onvif.org/ver10/appmgmt/wsdl")]
pub struct GetAppsInfo {
    // Optional ID to only retrieve information for a single application.
    #[yaserde(prefix = "ans", rename = "AppID")]
    pub app_id: Option<String>,
}

#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "ans", namespace = "ans: http://www.onvif.org/ver10/appmgmt/wsdl")]
pub struct GetAppsInfoResponse {
    #[yaserde(prefix = "ans", rename = "Info")]
    pub info: Vec<AppInfo>,
}

#[derive(Default, PartialEq, Debug, YaSerialize, YaDeserialize)]
#[yaserde(prefix = "ans", namespace = "ans: http://www.onvif.org/ver10/appmgmt/wsdl")]
pub struct AppInfo {
    // Unique app identifier of the application instance.
    #[yaserde(prefix = "ans", rename = "AppID")]
    pub app_id: String,

    // User readable application name
    #[yaserde(prefix = "ans", rename = "Name")]
    pub name: String,

    // Version of the installed application. The details of the format are
    // outside of the scope of this specificaton.
    #[yaserde(prefix = "ans", rename = "Version")]
    pub version: String,

    // VHT doesn't return this value. Commenting out so we don't need to introduce the struct LicenseInfo
    // Licenses associated with the application.
    // #[yaserde(prefix = "ans", rename = "Licenses")]
    // pub licenses: Vec<LicenseInfo>,

    // List of privileges granted to the application.
    #[yaserde(prefix = "ans", rename = "Privileges")]
    pub privileges: Vec<String>,

    // TODO: revisit DateTime implementation if installation_date is needed.
    // Currently process 1 returns 1970-01-01T00:00:00Z for the field.
    // Date and time when the application has been installed.
    // #[yaserde(prefix = "ans", rename = "InstallationDate")]
    // pub installation_date: xs::DateTime,

    // TODO: revisit DateTime implementation if installation_date is needed.
    // Currently process 1 returns 1970-01-01T00:00:00Z for the field.
    // Time of last update to this app, i.e. the time when this particular
    // version was installed.
    // #[yaserde(prefix = "ans", rename = "LastUpdate")]
    // pub last_update: xs::DateTime,

    // InstallationFailed state shall not be used here.
    #[yaserde(prefix = "ans", rename = "State")]
    pub state: AppState,

    // Supplemental information why the application is in the current state. In
    // error cases this field contains the error reason.
    #[yaserde(prefix = "ans", rename = "Status")]
    pub status: String,

    // If set the application will start automatically after booting of the
    // device.
    #[yaserde(prefix = "ans", rename = "Autostart")]
    pub autostart: bool,

    // Link to supplementary information about the application or its vendor.
    #[yaserde(prefix = "ans", rename = "Website")]
    pub website: String,

    // Link to a list of open source licenses used by the application.
    #[yaserde(prefix = "ans", rename = "OpenSource")]
    pub open_source: Option<String>,

    // Optional Uri for backup and restore of the application configuration.
    #[yaserde(prefix = "ans", rename = "Configuration")]
    pub configuration: Option<String>,

    // Optional reference to the interface definition of the application.
    #[yaserde(prefix = "ans", rename = "InterfaceDescription")]
    pub interface_description: Vec<String>,
}
