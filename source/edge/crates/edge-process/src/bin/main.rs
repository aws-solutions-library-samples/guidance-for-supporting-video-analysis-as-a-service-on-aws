use edge_process::utils::{
    args::get_cli_args,
    config::{Config, ConfigImpl},
    logger_setup::init_tracing,
};

use device_traits::connections::PubSubClient;
use edge_process::connections::aws_iot::setup_and_start_iot_event_loop;
use iot_connections::client::IotMqttClientManager;
use std::error::Error;
use std::process::ExitCode;
use tokio::try_join;
use tracing::debug;

#[tokio::main]
async fn main() -> Result<ExitCode, Box<dyn Error>> {
    let cli_args = get_cli_args();

    let config_path = cli_args.settings_path.expect("No config path entered!");

    // Get settings from file
    let configurations = ConfigImpl::new(&config_path.clone()).await?;

    let settings = configurations.get_settings();

    //File Writer collects tracing logs, Returns a guard which must exist for the lifetime of the program.
    let _log_file_guard = init_tracing(&settings).await;

    let pub_sub_client_manager =
        IotMqttClientManager::new_iot_connection_manager(configurations.get_config());
    let iot_client: Box<dyn PubSubClient + Send + Sync> =
        pub_sub_client_manager.new_pub_sub_client().await.unwrap();

    let _iot_loop_handle =
        setup_and_start_iot_event_loop(&configurations, pub_sub_client_manager, iot_client).await?;

    //This will keep process 2 alive until connections task stops.
    try_join!(_iot_loop_handle)?;

    Ok(ExitCode::SUCCESS)
}
