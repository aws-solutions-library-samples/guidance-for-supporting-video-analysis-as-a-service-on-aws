use std::str::FromStr;

/// IoT Job statuses reportable by device
/// https://docs.aws.amazon.com/iot/latest/developerguide/iot-jobs-lifecycle.html
#[derive(Debug, Copy, Clone, Eq, PartialEq)]
pub enum CommandStatus {
    /// Command is in progress
    InProgress,
    /// Command has failed
    Failed,
    /// Command has completed and succeeded.
    Succeeded,
    /// Command is rejected (ex. not recognized)
    Rejected,
}

impl CommandStatus {
    /// converting enum to str
    pub fn as_str(&self) -> &'static str {
        match self {
            CommandStatus::InProgress => "IN_PROGRESS",
            CommandStatus::Failed => "FAILED",
            CommandStatus::Succeeded => "SUCCEEDED",
            CommandStatus::Rejected => "REJECTED",
        }
    }
}

/// Supported commands
#[derive(Debug, Copy, Clone, Eq, PartialEq)]
pub enum Command {
    /// Reboot
    Reboot,
    /// Unknown command type
    Unknown,
}

impl Command {
    /// converting enum to str
    pub fn as_str(&self) -> &'static str {
        match self {
            Command::Reboot => "REBOOT",
            Command::Unknown => "UNKNOWN",
        }
    }
}

impl FromStr for Command {
    type Err = ();

    fn from_str(input: &str) -> Result<Command, Self::Err> {
        match input {
            "REBOOT" => Ok(Command::Reboot),
            _ => Err(()),
        }
    }
}

#[cfg(test)]
mod test {
    use super::*;

    const IN_PROGRESS: &str = "IN_PROGRESS";
    const FAILED: &str = "FAILED";
    const SUCCEEDED: &str = "SUCCEEDED";
    const REJECTED: &str = "REJECTED";

    const REBOOT: &str = "REBOOT";
    const UNKNOWN: &str = "UNKNOWN";

    #[test]
    fn check_command_status_as_str() {
        assert_eq!(CommandStatus::InProgress.as_str(), IN_PROGRESS);

        assert_eq!(CommandStatus::Failed.as_str(), FAILED);

        assert_eq!(CommandStatus::Succeeded.as_str(), SUCCEEDED);

        assert_eq!(CommandStatus::Rejected.as_str(), REJECTED);
    }

    #[test]
    fn check_command_as_str() {
        assert_eq!(Command::Reboot.as_str(), REBOOT);
        assert_eq!(Command::Unknown.as_str(), UNKNOWN);
    }

    #[test]
    fn check_command_from_str() {
        assert_eq!(Command::Reboot, Command::from_str(REBOOT).unwrap());
        assert!(Command::from_str("random").is_err());
    }
}
