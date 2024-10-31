# Description

This directory holds the source code for the edge process that will talk to the cloud code included in this repository.
The source code is written in Rust and uses Rust toolset for build and compilation.

**This package is a Cargo Workspace**

[Cargo Workspace Explained](https://doc.rust-lang.org/cargo/reference/workspaces.html). Crates should inherit external rust dependencies from the top Cargo.toml file.

### Running from Cargo

The executable allows for CLI parameters to be entered. The required parameter is a path to config file. The supported file format for the config file is `Yaml`. A sample `Yaml` file has been included with the source code.

To execute, enter the following command

```
cargo run -- -c "\<path to config file\>"
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

### Logging

- [Async Rust Logging Documentation](https://crates.io/crates/tracing)

This crate uses the tracing crate for logging.  See the link for additional details.

#### Setting the Logger

The logger is set through specific environment variables.

The default behavior of the logger will be to log to the filesystem.  However, if the PRINT_LOGS_TO_TERMINAL environment variable is set to any non-null value the logs will print to the terminal.

The default log level is error but by setting the LOG_LEVEL environment variable the log level can be set with the following values:
1. DEBUG
2. INFO
3. WARN
4. ERROR (default, variable does not exist)
5. OFF

See https://docs.rs/tracing/latest/tracing/struct.Level.html for more details.

### Formatter

This project uses the `Rustfmt` tool to format all rust files.  IDEs can be configured to format on save or you can format all files in the project by running the `cargo fmt` command.

### Code Structure

This crate contains all rust crates related to building the edge process client.

- `edge-process` is the top level crate. This contains the `main.rs` file which is the entry point for the client.
- `device-traits` is a supporting crate to enable local/remote communication.
- `iot-connections` contains helper functions and utilities for publishing and parsing MQTT messages exchanged between cloud and edge.
- `mqtt-client` is a wrapper over `rumqttc` which allows the edge process to communicate with AWS IoT.
- `iot-client` is a wrapper over `aws-iot-sdk` which allows the edge processs to communicate with AWS IoT.

Code Organization: TBD.