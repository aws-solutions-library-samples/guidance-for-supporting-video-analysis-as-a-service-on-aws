# Description

This directory holds the source code for the edge process that will talk to the cloud code included in this repository.
The source code is written in Rust and uses Rust toolset for build and compilation.

**This package is a Cargo Workspace**

[Cargo Workspace Explained](https://doc.rust-lang.org/cargo/reference/workspaces.html). Crates should inherit external rust dependencies from the top Cargo.toml file.

### Testing locally

#### Prerequisites
This assumes the cameras are connected to a SSM host (AWS Systems Manager). If you have direct access to the camera via its IP Address, you can skip this section and jump directly to the "Steps" section below to setup port forwarding to enable communication with the camera from local machine.

1. Install the SSM plugin https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html
2. Configure SSH to use session manager https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-getting-started-enable-ssh-connections.html#ssh-connections-enable
3. Assume credentials for specific role that gives access to the SSM host

#### Steps
1. Forward port 80 on camera to port 8023 on local machine. Assuming the ONVIF Server is running on port 80 on the remote host.
2. Set environment variable `LOCALHOST_ENDPOINT` to `127.0.0.1:8023` by running `export LOCALHOST_ENDPOINT=127.0.0.1:8023`.
3. Forward port 554 on camera to port 8024 on local machine. Assuming the RTSP Server is running on port 554 on the remote host.
4. Set environment variable `RTSP_ENDPOINT` to `127.0.0.1:8024` by running `export RTSP_ENDPOINT=rtsp://127.0.0.1:8024/Ch1`.

### Running from Cargo

The executable allows for CLI parameters to be entered. The required parameter is a path to config file. The supported file format for the config file is `Yaml`. A sample `Yaml` file has been included with the source code.

To execute, enter the following command

```
cargo run -- -c <path to config file>
```
Eg:
```
cargo run -- -c config.yaml
```
if the `config.yaml` is at the same level as the `edge-process` crate. or
```
cargo run -p edge-process -- -c configurations/config.yaml
```
if `config.yaml` is placed in a `configurations` directory and the command is being executed from the root level directory.

#### Running with Optional Features

Currently, the optional features available are:
- config (video encoder configurations)
  - If a configuration is sent from cloud when the config feature is not enabled, edge process will be no-op.
- command (reboot command)
  - If a command is sent from the cloud when the command feature is not enabled on edge, edge process will not receive the message.
- ip-discovery (private IP self-discovery)
- sd-card-catchup (store fragments locally if failed to send to KVS)
  - If this feature is enabled, extra fields must be added to config.yaml. Ex:
  ```
  # Where fragments are stored on device
  local_storage_path: "/media/fragments"
  # Local storage max for disk usage on SD card, in MB. 
  # Optional, default will be 1000 (1 GB). This acts as a high watermark. If crossed data will be deleted until below this.
  local_storage_disk_usage: 2000
  # Optional: if not defined, will use local_storage_path
  db_path: "/media/db"
  ```
  - If the `ROUTE_VIDEO_SD` environment variable is set to `TRUE`, then edge process will not send any data to KVS. This will simulate internet disconnection. All data will be stored on the SD card. This allows for the easy testing of the catchup feature.
  - If the `CATCHUP_BUFFER_SIZE` is set to a positive integer, this will allow modifying the buffer size of the number of fragments that can be held in memory. Use this to tune between memory efficiency + performance. If not set it will default to the value set by the constant.
  - This feature can be expanded on to support multiple types of data catchup (i.e. AI events).

To execute, enter the following command

```
cargo run --features <feature> -- -c <path to config file>
```
Eg:
```
cargo run --features config -- -c config.yaml
```

Cargo documentation for [command-line feature options](https://doc.rust-lang.org/cargo/reference/features.html#command-line-feature-options)

### Cross compile for armv7
The Cargo.toml in edge root directory is configured to cross compile for armv7. To cross compile for other architecture, follow similar pattern 

1. Install cross rs by running `cargo install cross --git https://github.com/cross-rs/cross`
2. Run `cross build --target armv7-unknown-linux-gnueabihf --release`
3. If successfully compiled, the binary can be found at `guidance-for-video-analytics-infrastructure-on-aws/source/edge/target/armv7-unknown-linux-gnueabihf/release/edge-process`

### Sending AI Events to Cloud

By default, edge process will use the API GW with the name "VideoAnalyticsVideoLogisticsAPIGateway" (should match VIDEO_LOGISTICS_API_NAME in common-constructs). To override this, set API_GW_ENDPOINT environment variable to your desired API GW endpoint `export API_GW_ENDPOINT=<endpoint>`.

### Motion-based Streaming

By default, edge process will only stream between a motion-start and a motion-stop event received from the ONVIF event metadata stream (1 GOP before start and 3 GOP after stop). To stream continuously, set `MOTION_BASED_STREAMING` environment variable to `FALSE` (`export MOTION_BASED_STREAMING=FALSE`).

### Use mp4 video as RTSP stream 
1. Follow instructions in [implementation guide]() to set up infrastructure to publish RTSP stream from mp4 videos.  

2. Set environment variable `SIM_RTSP_STREAM` to `rtsp://127.0.0.1:8554/simStream0` by running `export SIM_RTSP_STREAM=rtsp://127.0.0.1:8554/simStream0`.

3. set `MOTION_BASED_STREAMING` environment variable to `FALSE` by running`export MOTION_BASED_STREAMING=FALSE`.

4. Build or run edge process with 
```
cargo build --features simulated-rtsp-stream -- -c <path to config file>
```

or

```
cargo run --features simulated-rtsp-stream -- -c <path to config file>
```

### Logging

- [Async Rust Logging Documentation](https://crates.io/crates/tracing)

This crate uses the tracing crate for logging.  See the link for additional details.

#### Setting the Logger

The logger is set through specific environment variables.

The default behavior of the logger will be to log to the filesystem.  However, if the `PRINT_LOGS_TO_TERMINAL` environment variable is set to any non-null value the logs will print to the terminal.

The default log level is error but by setting the `LOG_LEVEL` environment variable the log level can be set with the following values:
1. `TRACE`
2. `DEBUG`
3. `INFO`
4. `WARN`
5. `ERROR` (default, variable does not exist)
6. `OFF`

See https://docs.rs/tracing/latest/tracing/struct.Level.html for more details.

Set `LOG_SYNC` environment variable to `TRUE` if you want to enable the log sync feature(`export LOG_SYNC=TRUE`). The default value is `FALSE`. Also make sure `PRINT_LOGS_TO_TERMINAL` environment variable is unset. Log sync only happens for logs written to local log files - log sync will not sync logs printed to terminal. All logs from the edge process will be published to AWS CloudWatch if the value is set to `TRUE`.



### Formatter

This project uses the `Rustfmt` tool to format all rust files.  IDEs can be configured to format on save or you can format all files in the project by running the `cargo fmt` command.

### Code Structure

This crate contains all rust crates related to building the edge process client.

- `edge-process` is the top level crate. This contains the `main.rs` file which is the entry point for the client.
- `device-traits`, `snapshot-traits`, and `streaming-traits` are a supporting crates to enable local/remote communication.
- `iot-connections` contains helper functions and utilities for publishing and parsing MQTT messages exchanged between cloud and edge.
- `mqtt-client` is a wrapper over `rumqttc` which allows the edge process to communicate with AWS IoT.
- `iot-client` is a wrapper over `aws-iot-sdk` which allows the edge process to communicate with AWS IoT.
- `http-client` is a wrapper over `reqwest`.
- `onvif-client` is a wrapper over `http-client` which allows the edge process to communicate with the ONVIF service on devices. wsdl_rs and xsd_rs files were generated using [xsd-parser-rs](https://github.com/lumeohq/xsd-parser-rs).
- `snapshot-client` is a wrapper over `http-client` for retrieving snapshots using the URI returned from the ONVIF service.
- `ws-discovery-client` is a wrapper over `tokio::net` which allows the edge process to discover a device's public IP address (WS-discovery spec is required for all ONVIF devices).
- `kvs-client` is a wrapper over `aws-sdk-kinesisvideo`, `aws-sdk-kinesisvideosignaling`, and `aws-sigv4` which allows the edge process to communicate with AWS KVS.
- `event-processor` is a wrapper over `quickxml_to_serde` which allows the edge process to process events received from Onvif metadata stream
- `video-analytics-client` is a wrapper over `aws-sdk-apigateway`, `aws-sigv4`, and `reqwest` which allows the edge process to send AI events to the video analytics guidance cloud (API GW deployed from VideoAnalyticsVideoLogisticsCDK).
- `webrtc-client` enables the edge process for peer to peer livestream. 


Code Organization: TBD.

### Credentials Manager

Currently, the ONVIF client reads a username and password from a local plain text config file. For better security practice, we recommend using a credentials manager like [AWS Secrets Manager](https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/rustv1/examples/secretsmanager). Implementation will include:

1. Initializing the Secrets Manager client
2. Using IoT Credentials Manager to get SigV4 credentials
3. Making API calls to Secrets Manager to get the ONVIF credentials