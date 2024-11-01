use crate::{
    constants::{IS_ENABLED, IS_STATUS_CHANGED, LOG_LEVEL, SYNC_FREQUENCY},
    data_storage::log_metadata_storage::LogMetadataStorage,
    utils::logger_setup::{
        construct_current_log_name, get_next_log_name, is_log_eligible, match_log_level,
    },
};
use device_traits::channel_utils::traits::IoTServiceSender;
use device_traits::channel_utils::ServiceCommunicationManager;
use serde_json::{json, Map, Value};
use std::fs::File;
use std::io::{BufRead, BufReader, Seek, SeekFrom};
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::sync::mpsc::Receiver;
use tokio::task::JoinHandle;
use tokio::time::sleep;
use tracing::{debug, error, info, instrument, warn};

// GRCOV_STOP_COVERAGE

#[instrument]
pub async fn setup_and_start_log_sync_loop(
    dir_path: String,
    local_log_file_path: PathBuf,
    mut log_rx: Receiver<Value>,
) -> anyhow::Result<JoinHandle<()>> {
    let mut iot_sender = Box::<ServiceCommunicationManager>::default();
    let topic = match iot_sender.get_client_id() {
        Ok(client_id) => {
            format!("$aws/rules/DeviceTelemetryCloudWatchLogsRule/things/{}/logs", client_id)
        }
        Err(e) => {
            panic!("Failed to get client id : {:?}", e);
        }
    };
    let data_base_client = create_database_client(dir_path);
    let log_read_join_handle = tokio::spawn(async move {
        let mut log_setting_data = Value::String("".to_string());
        let mut is_status_changed = false;
        while let Some(initialized_log_setting_data) = log_rx.recv().await {
            log_setting_data = initialized_log_setting_data.clone();
            is_status_changed =
                initialized_log_setting_data[IS_STATUS_CHANGED].as_bool().unwrap_or(false);
            loop {
                match log_rx.try_recv() {
                    Ok(updated_log_setting_data) => {
                        log_setting_data = updated_log_setting_data.clone();
                        is_status_changed =
                            updated_log_setting_data[IS_STATUS_CHANGED].as_bool().unwrap_or(false);
                    }
                    Err(_) => {
                        info!("No data received from log channel, assume logger configuration is not updated.")
                    }
                }
                let log_sync_status = log_setting_data[IS_ENABLED].as_bool().unwrap_or(false);
                // Exit the thread if isEnabled is updated to false in IoT shadow
                if !log_sync_status {
                    break;
                }

                // Get value of log sync settings
                let log_sync_frequency = log_setting_data[SYNC_FREQUENCY].as_u64().unwrap_or(300);
                let log_sync_level =
                    log_setting_data[LOG_LEVEL].as_str().unwrap_or("INFO").to_string();
                let input_log_sync_level = match_log_level(log_sync_level);

                let current_log_file = construct_current_log_name();
                let mut position = 0;
                let mut log_file_name = current_log_file.clone();
                let res = get_log_db(data_base_client.clone()).unwrap();
                // If log sync is just enabled, do not catch up local logs
                if is_status_changed {
                    if log_file_name == res.0 {
                        position = get_end_position(
                            &local_log_file_path.join(&log_file_name).display().to_string(),
                        );
                    }
                    store_log_db(data_base_client.clone(), &log_file_name, position);
                    is_status_changed = false;
                } else {
                    log_file_name = res.0;
                    position = res.1;
                }
                // vec_data is to store the local log message and will be published to cloud side
                let mut vec_data: Vec<Value> = Vec::new();
                // Create the inner loop to keep reading next local log file
                'inner: loop {
                    let path = &local_log_file_path.join(&log_file_name).display().to_string();
                    debug!("Opening log file: {}", path);
                    let mut end_position = 0;
                    let _file = match File::open(path) {
                        Ok(mut file) => {
                            if log_file_name == current_log_file {
                                end_position = file
                                    .seek(SeekFrom::End(0))
                                    .expect("Can not get the end position.");
                            }
                            let mut reader = BufReader::new(file);
                            debug!("Start position is: {}", position);
                            let _ = reader.seek(SeekFrom::Start(position));

                            let mut lines = reader.lines();
                            // While loop to keep reading log data in the current log file
                            while let Some(Ok(current_line)) = lines.next() {
                                if current_line == "" || !current_line.starts_with("{") {
                                    continue;
                                }
                                // Parse the log data and retrieve the required fields
                                let invalid_value = Value::String("INVALID".to_string());
                                let mut invalid_map = Map::new();
                                invalid_map.insert("Key".to_string(), invalid_value.to_owned());
                                let parsed_current_line: Map<String, Value> =
                                    serde_json::from_str(&current_line).unwrap_or(invalid_map);
                                let timestamp =
                                    parsed_current_line.get("timestamp").unwrap_or(&invalid_value);
                                let level =
                                    parsed_current_line.get("level").unwrap_or(&invalid_value);

                                // Check if the priority of current log level equals to or higher than the required log level
                                let is_eligible =
                                    is_log_eligible(level.to_string(), input_log_sync_level);
                                if is_eligible {
                                    let mut parsed_current_line_clone = parsed_current_line.clone();
                                    parsed_current_line_clone.remove("timestamp");
                                    // Construct log data with required format
                                    let msg_json: Value = json!({
                                        "timestamp": timestamp,
                                        "message": parsed_current_line_clone
                                    });
                                    let vec_size = serde_json::to_string(&vec_data).unwrap();
                                    let msg_size = serde_json::to_string(&msg_json).unwrap();
                                    // Check if the current MQTT message size exceeds the payload limit
                                    if (vec_size.len() + msg_size.len()) >= 128 * 1000 {
                                        let payload =
                                            serde_json::to_string::<Vec<Value>>(&vec_data.clone())
                                                .expect("Issue formatting Json.");
                                        if let Err(e) = iot_sender
                                            .try_build_and_send_iot_message(topic.as_str(), payload)
                                        {
                                            error!("Failed to publish edge log: {:?}", e);
                                        }
                                        vec_data.clear();
                                    }
                                    vec_data.push(msg_json.clone());
                                }
                            }
                            debug!("Last line reached.");
                            if log_file_name == current_log_file {
                                if !vec_data.is_empty() {
                                    // If the current log file is latest, publish the current log data
                                    let payload =
                                        serde_json::to_string::<Vec<Value>>(&vec_data.clone())
                                            .expect("Issue formatting Json.");
                                    if let Err(e) = iot_sender
                                        .try_build_and_send_iot_message(topic.as_str(), payload)
                                    {
                                        error!("Failed to publish edge log: {:?}", e);
                                    }
                                    vec_data.clear();
                                }
                                store_log_db(
                                    data_base_client.clone(),
                                    &current_log_file,
                                    end_position,
                                );
                                break 'inner;
                            }
                        }
                        Err(_) => {
                            warn!("Can not open local log file: {}", path);
                            store_log_db(data_base_client.clone(), &log_file_name, 0);
                            break 'inner;
                        }
                    };
                    log_file_name = get_next_log_name(&log_file_name);
                    position = 0;
                }
                if !vec_data.is_empty() {
                    let payload = serde_json::to_string::<Vec<Value>>(&vec_data.clone())
                        .expect("Issue formatting Json.");
                    if let Err(e) =
                        iot_sender.try_build_and_send_iot_message(topic.as_str(), payload)
                    {
                        error!("Failed to publish edge log: {:?}", e);
                    }
                    vec_data.clear();
                }

                info!("Log reading thread will be sleeping for {}", log_sync_frequency);
                sleep(Duration::from_secs(log_sync_frequency)).await;
            }
            debug!("End of the loop");
        }
    });

    Ok(log_read_join_handle)
}

fn get_end_position(file_name: &String) -> u64 {
    match File::open(file_name) {
        Ok(mut file) => match file.seek(SeekFrom::End(0)) {
            Ok(position) => position,
            Err(_) => {
                warn!("Can not get end position of file: {}", file_name);
                0
            }
        },
        Err(_) => {
            warn!("Can not open local log file: {}", file_name);
            0
        }
    }
}

fn store_log_db(
    database_client: Arc<Mutex<LogMetadataStorage>>,
    name: &String,
    position: u64,
) -> () {
    let mut database_lock = database_client.lock().expect("Database client is poisoned!");
    match database_lock.save_log_metadata(name, position) {
        Ok(_) => {
            debug!("log stored in database.")
        }
        Err(e) => {
            error!("Error storing log in database : {:?}", e);
        }
    }
    ()
}

fn create_database_client(db_path: String) -> Arc<Mutex<LogMetadataStorage>> {
    let data_base_client =
        LogMetadataStorage::new_connection(db_path).expect("Error creating database connection.");
    Arc::new(Mutex::new(data_base_client))
}

fn get_log_db(
    database_client: Arc<Mutex<LogMetadataStorage>>,
) -> Result<(String, u64), (String, u64)> {
    let mut database_lock = database_client.lock().expect("Database client is poisoned!");
    match database_lock.get_log_metadata() {
        Ok((file_name, position)) => {
            debug!("get log from database.");
            Ok((file_name, position))
        }
        Err(e) => {
            error!("Failed getting log in database, return the default value. {:?}", e);
            Ok((construct_current_log_name(), 0))
        }
    }
}