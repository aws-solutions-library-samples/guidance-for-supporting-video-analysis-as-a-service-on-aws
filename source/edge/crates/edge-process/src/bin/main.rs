use edge_process::utils::{
    args::get_cli_args,
    config::{Config, ConfigImpl},
    logger_setup::init_tracing,
};

use device_traits::connections::PubSubClient;
use edge_process::connections::aws_iot::{new_iot_shadow_manager, setup_and_start_iot_event_loop};
use edge_process::log_sync::log_sync::setup_and_start_log_sync_loop;
use iot_connections::client::IotMqttClientManager;
use once_cell::sync::Lazy;
use serde_json::{json, Value};
use std::error::Error;
use std::process::ExitCode;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::sync::mpsc::channel;
use tokio::time::sleep;
use tokio::{select, try_join};
use edge_process::constants::{BUFFER_SIZE, IS_ENABLED, IS_STATUS_CHANGED, LOG_LEVEL, LOG_SYNC, PROVISION_SHADOW_NAME, SYNC_FREQUENCY};
use tracing::{debug, info};

#[tokio::main]
async fn main() -> Result<ExitCode, Box<dyn Error>> {
    let cli_args = get_cli_args();

    let config_path = cli_args.settings_path.expect("No config path entered!");

    // Get settings from file
    let configurations = ConfigImpl::new(&config_path.clone()).await?;

    let settings = configurations.get_settings();

    let dir = settings.get_dir().await.expect("Invalid Directory Entered");

    let local_log_file_path = dir.to_owned().join("logs");
    let dir_path = dir.to_owned().display().to_string();

    //File Writer collects tracing logs, Returns a guard which must exist for the lifetime of the program.
    let _log_file_guard = init_tracing(&settings).await;

    let log_sync = std::env::var(LOG_SYNC).unwrap_or("FALSE".to_string()).eq("TRUE");

    let pub_sub_client_manager =
        IotMqttClientManager::new_iot_connection_manager(configurations.get_config());
    let iot_client: Box<dyn PubSubClient + Send + Sync> =
        pub_sub_client_manager.new_pub_sub_client().await?;

    let mut iot_shadow_client_provision_for_log = new_iot_shadow_manager(
        settings.get_client_id(),
        Some(PROVISION_SHADOW_NAME.to_string()),
        dir.to_owned(),
    ).await;

    let (logger_config_tx, mut logger_config_rx) = channel::<String>(BUFFER_SIZE);
    let (log_tx, log_rx) = channel::<Value>(BUFFER_SIZE);

    static LOG_SYNC_STATUS: Lazy<Arc<Mutex<bool>>> = Lazy::new(|| Arc::new(Mutex::new(true)));
    static LOG_SYNC_LEVEL: Lazy<Arc<Mutex<String>>> =
        Lazy::new(|| Arc::new(Mutex::new("INFO".to_string())));
    static LOG_SYNC_FREQUENCY: Lazy<Arc<Mutex<u64>>> = Lazy::new(|| Arc::new(Mutex::new(1)));

    let _config_join_handle = tokio::spawn(async move {
        loop {
            select! {
                Some(logger_settings) = logger_config_rx.recv() => {
                    /* expected structure of logger_settings
                        {
                            "isEnabled": true,
                            "syncFrequency": 300,
                            "logLevel": "INFO"
                        }
                    */
                    if log_sync {
                        let logger_settings_value: Value = serde_json::from_str(logger_settings.as_str()).unwrap();
                        let logger_settings_object = logger_settings_value.as_object().unwrap();
                        info!("Logger settings: {:?}", logger_settings_value);

                        let mut data = json!({});
                        data[IS_STATUS_CHANGED] = json!(false);

                        if !logger_settings_object.is_empty() {
                            let mut global_sync_status = LOG_SYNC_STATUS.lock().unwrap();
                            let mut global_sync_frequency = LOG_SYNC_FREQUENCY.lock().unwrap();
                            let mut global_sync_level = LOG_SYNC_LEVEL.lock().unwrap();
                            if logger_settings_object.contains_key(IS_ENABLED) {
                                let value = logger_settings_object.get(IS_ENABLED);
                                *global_sync_status = value.expect("Can not get log sync status").as_bool().unwrap_or(true);
                                data[IS_STATUS_CHANGED] = json!(true);
                            }
                            if logger_settings_object.contains_key(SYNC_FREQUENCY) {
                                let value = logger_settings_object.get(SYNC_FREQUENCY);
                                *global_sync_frequency = value.expect("Can not get sync frequency").as_u64().unwrap_or(300);
                            }
                            if logger_settings_object.contains_key(LOG_LEVEL) {
                                let value = logger_settings_object.get(LOG_LEVEL);
                                *global_sync_level = value.expect("Can not get log level").as_str().unwrap_or("INFO").to_string();
                            }
                            let sync_status = *global_sync_status;
                            data[IS_ENABLED] = json!(sync_status);
                            let sync_frequency = *global_sync_frequency;
                            data[SYNC_FREQUENCY] = json!(sync_frequency);
                            let sync_level = (*global_sync_level).clone();
                            data[LOG_LEVEL] = json!(sync_level);
                        }
                        let _ = log_tx.send(data.clone()).await;

                        let report_logger_settings = json!({"loggerSettings": logger_settings_value});
                        while let Err(_e) = iot_shadow_client_provision_for_log
                            .update_reported_state(report_logger_settings.to_owned())
                            .await
                        {
                            debug!("Failed to update logger configuration in provision shadow");
                            sleep(Duration::from_millis(100)).await;
                        }
                    }
                }
            }
        }
    });

    let logger_config_tx_clone = logger_config_tx.clone();

    let _iot_loop_handle =
        setup_and_start_iot_event_loop(
            &configurations,
            logger_config_tx_clone,
            pub_sub_client_manager,
            iot_client
        ).await?;

    let _log_loop_handle =
        setup_and_start_log_sync_loop(dir_path, local_log_file_path, log_rx).await?;

    //This will keep process 2 alive until connections task stops.
    try_join!(_iot_loop_handle)?;

    Ok(ExitCode::SUCCESS)
}
