// GRCOV_STOP_COVERAGE : No unit tests required for CLI interface as no value is added in testing clap dependency
use clap::Parser;

#[derive(Parser, Debug)]
#[clap(author, version, about)]
pub struct CLIArgs {
    #[arg(long = "settings-path", short = 'c')]
    ///Full path to config file, Program will return an error if not passed in.
    pub settings_path: Option<String>,
}

/// Get CLI arguments if run from commandline.
pub fn get_cli_args() -> CLIArgs {
    CLIArgs::parse()
}
