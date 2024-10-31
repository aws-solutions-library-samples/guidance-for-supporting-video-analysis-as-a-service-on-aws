//! This module sets up the global subscriber to collect async log entries within the
//! device client. This function should only be executed once per execution of the program
//! as only one subscriber should be set as the global logger.

//The recommended logging tool for tokio runtimes.
// https://github.com/tokio-rs/tracing
// https://docs.rs/tracing-subscriber/latest/tracing_subscriber/fmt/index.html#formatters
// Example of custom functionality https://burgers.io/custom-logging-in-rust-using-tracing

use crate::utils::config::settings::Settings;
use std::{env, io, path::PathBuf};
use tracing::debug;
use tracing_appender::{non_blocking::WorkerGuard, rolling::hourly};
use tracing_subscriber::{prelude::*, registry::LookupSpan, EnvFilter, Layer};

const LOG_FILE_PREFIX: &str = "logs";
/// Set log level for logger.  Valid options are TRACE, DEBUG, INFO, WARN, ERROR, OFF
/// https://docs.rs/tracing/latest/tracing/struct.Level.html
pub const LOG_LEVEL: &str = "LOG_LEVEL";
/// If set print to terminal rather than log to file-system.
pub const PRINT_LOGS_TO_TERMINAL: &str = "PRINT_LOGS_TO_TERMINAL";

/// This function sets up the global logger using the tracing crate.  The logger is a fundamental
/// operation to the device client.  As such if this fails we will call a panic! and exit the application.
pub async fn init_tracing(settings: &Settings) -> Option<WorkerGuard> {
    // TODO: Determine whether logger should incorporate tokio-console or OpenTelemetry (Dev Tools)
    let log_path = settings
        .get_dir()
        .await
        .expect("Unable to find valid logging directory.")
        .join(LOG_FILE_PREFIX);

    // If LOG_LEVEL is not set the default behavior of this filter will be to log ERROR.
    // Valid LOG_LEVEL values are TRACE ,DEBUG, INFO, WARN, ERROR, OFF
    // https://docs.rs/tracing/latest/tracing/struct.Level.html
    let log_level = EnvFilter::from_env(LOG_LEVEL);

    // If PRINT_LOGS_TO_TERMINAL is set print logs to terminal, else log them to the file-system.
    let (log_config, _guard) = match env::var_os(PRINT_LOGS_TO_TERMINAL) {
        None => LogConfig::File(log_path).layer(),
        Some(_) => LogConfig::Stdout.layer(),
    };

    let logger_subscriber = tracing_subscriber::registry().with(log_config).with(log_level);

    // This will fail if run more than once, will panic if run more (which we should never do).
    tracing::subscriber::set_global_default(logger_subscriber)
        .expect("Global logger was not established!");

    debug!("Setup global subscriber for Async Logs.");

    //Return guard or output to the log-files will stop.
    _guard
}

#[allow(dead_code)]
enum LogConfig {
    File(PathBuf),
    Stdout,
    Stderr,
}

impl LogConfig {
    pub fn layer<S>(self) -> (Box<dyn Layer<S> + Send + Sync + 'static>, Option<WorkerGuard>)
    where
        S: tracing_core::Subscriber,
        for<'a> S: LookupSpan<'a>,
    {
        // Shared configuration regardless of where logs are output to.
        let log_fmt = tracing_subscriber::fmt::layer()
            //Display which thread the event was logged
            .with_thread_ids(true)
            // Display source code file paths
            .with_file(true)
            // Display source code line numbers
            .with_line_number(true)
            // Don't display the event's target (module path)
            .with_target(false);

        // Configure the writer based on the desired log target:
        match self {
            LogConfig::File(path_to_logs) => {
                let file_appender = hourly(path_to_logs, LOG_FILE_PREFIX);
                let (file_writer, _guard) = tracing_appender::non_blocking(file_appender);
                (Box::new(log_fmt.json().with_writer(file_writer)), Some(_guard))
            }
            LogConfig::Stdout => (Box::new(log_fmt.pretty().with_writer(io::stdout)), None),
            LogConfig::Stderr => (Box::new(log_fmt.pretty().with_writer(io::stderr)), None),
        }
    }
}
