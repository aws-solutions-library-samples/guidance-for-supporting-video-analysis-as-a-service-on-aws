//! This module contains helper structs for device command
use std::error::Error;

use crate::client::topics::TopicHelper;
use crate::constants::{JOB_EXECUTION_FIELD, JOB_ID_FIELD};
use device_traits::command::{Command, CommandStatus};
use device_traits::connections::PubSubClient;
use device_traits::connections::PubSubMessage;
use mqtt_client::builder::MQTTMessageBuilder;
use serde_json::{json, Value};
use tracing::{debug, info, instrument, warn};

///Helper class to aid in device commands.
#[derive(Debug)]
pub struct CommandHelper<'a> {
    factory_mqtt_client: &'a mut Box<dyn PubSubClient + Send + Sync>,
    topic_helper: TopicHelper,
}

impl CommandHelper<'_> {
    /// Constructor for Command Helper
    pub fn new(
        factory_mqtt_client: &mut Box<dyn PubSubClient + Send + Sync>,
        client_id: String,
    ) -> CommandHelper {
        let topic_helper = TopicHelper::new(client_id.clone(), None);
        CommandHelper { factory_mqtt_client, topic_helper }
    }

    /// Helper function to update job status
    /// https://docs.aws.amazon.com/iot/latest/developerguide/jobs-mqtt-api.html
    #[instrument]
    pub(crate) async fn update_command_status(
        &mut self,
        status: CommandStatus,
        job_id: String,
    ) -> anyhow::Result<()> {
        let jobs_update_accepted_topic =
            self.topic_helper.get_jobs_update_accepted(Some(job_id.clone()));
        let jobs_update_rejected_topic =
            self.topic_helper.get_jobs_update_rejected(Some(job_id.clone()));

        'outer: for i in 1..10 {
            let topic = self.topic_helper.get_jobs_update_topic(job_id.clone());
            self.factory_mqtt_client
                .send(Self::create_message(json!({"status": status.as_str()}), topic))
                .await?;

            'inner: loop {
                let iot_message = self.factory_mqtt_client.recv().await?;
                if iot_message.get_topic().eq(&jobs_update_rejected_topic) {
                    warn!("Failed to update command status, change rejected on attempt : {}", i);
                    break 'inner;
                }
                if iot_message.get_topic().eq(&jobs_update_accepted_topic) {
                    info!("Successfully updated command status.");
                    break 'outer;
                }
                debug!("Received an unexpected message when updating command status.");
            }
        }
        Ok(())
    }

    /// Helper function to start next command, called when received a message from notify topic
    /// https://docs.aws.amazon.com/iot/latest/developerguide/jobs-mqtt-api.html
    #[instrument]
    pub(crate) async fn start_next_command(&mut self) -> anyhow::Result<Option<Command>> {
        let jobs_start_next_accepted_topic = self.topic_helper.get_jobs_start_next_accepted();
        let jobs_start_next_rejected_topic = self.topic_helper.get_jobs_start_next_rejected();

        for i in 1..10 {
            let topic = self.topic_helper.get_jobs_start_next_topic();
            self.factory_mqtt_client.send(Self::create_message(json!({}), topic)).await?;

            'inner: loop {
                let iot_message = self.factory_mqtt_client.recv().await?;
                if iot_message.get_topic().eq(&jobs_start_next_rejected_topic) {
                    warn!("Failed to start next command, change rejected on attempt : {}", i);
                    break 'inner;
                }
                if iot_message.get_topic().eq(&jobs_start_next_accepted_topic) {
                    info!("Successfully started next command.");
                    // If received message from start-next/accepted topic, parse command
                    let payload =
                        serde_json::from_str(iot_message.get_payload()).unwrap_or(Value::default());
                    return Ok(Self::parse_new_job(payload));
                }
                debug!("Received an unexpected message when starting next command.");
            }
        }
        Ok(None)
    }

    /// Get the next pending job execution for a thing
    /// https://docs.aws.amazon.com/iot/latest/developerguide/jobs-mqtt-api.html
    /// According to the linked doc, you can set the jobId to $next to return
    /// the next pending job execution for a thing with a status of IN_PROGRESS or QUEUED.
    /// In this case, any job executions with status IN_PROGRESS are returned first.
    /// Job executions are returned in the order in which they were created.
    #[instrument]
    pub(crate) async fn get_next_pending_job_execution(&mut self) -> anyhow::Result<Option<Value>> {
        let jobs_get_accepted_topic = self.topic_helper.get_next_job_get_accepted();
        let jobs_get_rejected_topic = self.topic_helper.get_next_job_get_rejected();

        for i in 1..10 {
            let topic = self.topic_helper.get_next_job_topic();
            // Tested in test client, request payload can be empty
            self.factory_mqtt_client.send(Self::create_message(json!({}), topic)).await?;

            'inner: loop {
                let iot_message = self.factory_mqtt_client.recv().await?;
                if iot_message.get_topic().eq(&jobs_get_rejected_topic) {
                    warn!("Failed to get next pending job execution, rejected on attempt : {}", i);
                    break 'inner;
                }
                if iot_message.get_topic().eq(&jobs_get_accepted_topic) {
                    info!("Successfully got next pending job execution.");
                    let job_execution_payload =
                        serde_json::from_str(iot_message.get_payload()).unwrap_or(Value::default());
                    return Ok(Some(job_execution_payload));
                }
                debug!("Received an unexpected message when getting next job exeuction.");
            }
        }
        Ok(None)
    }

    // GRCOV_BEGIN_COVERAGE

    /// Helper function to build message
    fn create_message(message: Value, topic: String) -> Box<dyn PubSubMessage + Send + Sync> {
        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        message_builder.set_topic(topic.as_str());
        let payload = serde_json::to_string::<Value>(&message)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        message_builder.build_box_message()
    }

    /// Helper function to check if there is a new queued job from $aws/things/{thingName}/jobs/notify
    /// This function will return false if there are in-progress jobs (do not want to start a new job if there is 1 in progress already)
    /// https://docs.aws.amazon.com/iot/latest/developerguide/jobs-comm-notifications.html
    /*
    {
        "timestamp": 1517016948,
        "jobs": {
            "QUEUED": [ {
                "jobId": "job1",
                "queuedAt": 1517016947,
                "lastUpdatedAt": 1517016947,
                "executionNumber": 1,
                "versionNumber": 1
            } ]
        }
    }
    */
    pub fn received_jobs_notify_message(
        message: &(dyn PubSubMessage + Send + Sync),
        expected_topic: String,
    ) -> Option<String> {
        if !message.get_topic().contains(expected_topic.as_str()) {
            return None;
        }
        // let v: Value = serde_json::from_str(message.get_payload()).unwrap_or(Value::default());
        let job_message: Value =
            serde_json::from_str(message.get_payload()).unwrap_or(Value::default());

        // If the valid jason message shows there's a job "IN_PROGRESS", don't proceed to process any other jobs.
        if job_message.get("jobs").unwrap_or(&Value::default()).get("IN_PROGRESS").is_some() {
            // Do not want to start command if another command is already in progress
            info!("A command is already in progress");
            return None;
        }

        // When there's only "QUEUED" jobs, return the first job in the queue to process. Otherwise, return None if there's no job in the queue.
        return job_message
            .get("jobs")
            .unwrap_or(&Value::default())
            .get("QUEUED")
            .and_then(|queued_jobs| queued_jobs.as_array())
            .and_then(|queued_jobs_array| queued_jobs_array.first())
            .and_then(|job| job.get(JOB_ID_FIELD))
            .and_then(|job_id| job_id.as_str())
            .map(|job_id_str| job_id_str.to_string());
    }

    // GRCOV_STOP_COVERAGE: we don't have mocks for mqtt client that's needed in update_command_status to unit test this
    /// Helper function to update the job that's in progress when device boots up.
    /// Command that trigger device to restart: REBOOT.
    pub async fn update_in_progress_job_status(
        &mut self,
        next_pending_job_exec: Option<Value>,
    ) -> Result<(), Box<dyn Error>> {
        // update job status for REBOOT
        match next_pending_job_exec
            .clone()
            .filter(Self::is_reboot)
            .unwrap_or(json!({}))
            .get(JOB_EXECUTION_FIELD)
            .and_then(|value| value.get(JOB_ID_FIELD))
            .and_then(|value| value.as_str())
        {
            None => {
                info!(
                    "Either the command is not reboot or job id for the next pending job execution was not retrieved successfully."
                );
                return Ok(());
            }
            Some(job_id) => {
                self.update_command_status(CommandStatus::Succeeded, job_id.to_owned()).await?;
                return Ok(());
            }
        }
    }
    // GRCOV_BEGIN_COVERAGE

    /// Helper function to parse new job from $aws/things/{thingName}/jobs/start-next/accepted
    /// https://docs.aws.amazon.com/iot/latest/developerguide/jobs-mqtt-api.html
    /// Any job executions with status IN_PROGRESS are returned first.
    /// Job executions are returned in the order in which they were queued.
    /// If the next pending job execution is QUEUED, its state changes to IN_PROGRESS and the job execution's status details are set as specified.
    /// If the next pending job execution is already IN_PROGRESS, its status details aren't changed.
    /// If no job executions are pending, the response doesn't include the execution field.
    /*

    // execution for AWS-command
    {
        "execution" : {
            "jobId" : "022",
            "thingName" : "MyThing",
            "jobDocument" : "< contents of job document >",
            "status" : "IN_PROGRESS",
            "queuedAt" : 1489096123309,
            "lastUpdatedAt" : 1489096123309,
            "versionNumber" : 1,
            "executionNumber" : 1234567890
        },
        "clientToken" : "client-1",
        "timestamp" : 1489088524284,
    }

    // jobDocument for AWS-command
    {
        "version": "1.0",
        "steps": [
            {
            "action": {
                "name": "Run-Command",
                "type": "runCommand",
                "input": {
                "command": <REBOOT, FACTORY_RESET, or SD_CARD_FORMAT>
                },
                "runAsUser": "${aws:iot:parameter:runAsUser}"
            }
            }
        ]
    }
    */
    pub fn parse_new_job(payload: Value) -> Option<Command> {
        let binding = Value::default();
        let job_doc_str = payload
            .get("execution")
            .unwrap_or(&binding)
            .get("jobDocument")
            .unwrap_or(&binding)
            .to_string();

        if job_doc_str.contains(Command::Reboot.as_str()) {
            return Some(Command::Reboot);
        }

        None
    }

    // Determine if there's a in_progress reboot command
    fn is_reboot(job_execution: &Value) -> bool {
        info!("Checking if an existing reboot command is in progress.");
        let job_execution_as_str = job_execution.to_string();
        if job_execution_as_str.contains(Command::Reboot.as_str()) {
            info!("Confirmed a reboot command is in progress. ");
            return true;
        }

        false
    }
}

// Ignore borrowed box as it is required by mockall test tool.
#[cfg(test)]
#[allow(clippy::borrowed_box)]
mod tests {
    use super::*;
    use serde_json::Value;
    use std::fs;

    const CLIENT_ID: &str = "ThingName";
    const JOB_ID: &str = "12345";
    const PATH_TO_REBOOT_JOB_EXECUTION_FILE: &str =
        "tests/files-for-integration-tests/reboot_job_execution_in_progress_test_file";
    #[test]
    fn received_jobs_notify_message_test() {
        let iot_jobs_topic_helper = get_topic_helper();
        let topic = iot_jobs_topic_helper.get_jobs_notify_topic();
        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        message_builder.set_topic(topic.as_str());

        let payload = json!({
            "timestamp": 1517016948,
            "jobs": {
                "QUEUED": [ {
                    JOB_ID_FIELD: JOB_ID,
                    "queuedAt": 1517016947,
                    "lastUpdatedAt": 1517016947,
                    "executionNumber": 1,
                    "versionNumber": 1
                } ]
            }
        });
        let payload = serde_json::to_string::<Value>(&payload)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        let message = message_builder.build_box_message();

        let resp = CommandHelper::received_jobs_notify_message(message.as_ref(), topic);

        assert_eq!(resp.unwrap(), JOB_ID);
    }

    #[test]
    fn received_jobs_notify_message_job_in_progress_test() {
        let iot_jobs_topic_helper = get_topic_helper();
        let topic = iot_jobs_topic_helper.get_jobs_notify_topic();
        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        message_builder.set_topic(topic.as_str());

        let payload = json!({
            "timestamp" : 10011,
            "jobs" : {
                "IN_PROGRESS" : [ {
                    JOB_ID_FIELD : "other-job",
                    "queuedAt" : 10003,
                    "lastUpdatedAt" : 10009,
                    "executionNumber" : 1,
                    "versionNumber" : 1
                } ],
                "QUEUED" : [ {
                    JOB_ID_FIELD : JOB_ID,
                    "queuedAt" : 10011,
                    "lastUpdatedAt" : 10011,
                    "executionNumber" : 1,
                    "versionNumber" : 0
                } ]
            }
        });
        let payload = serde_json::to_string::<Value>(&payload)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        let message = message_builder.build_box_message();

        let resp = CommandHelper::received_jobs_notify_message(message.as_ref(), topic);

        assert!(resp.is_none());
    }

    #[test]
    fn received_jobs_notify_message_wrong_topic_test() {
        let iot_jobs_topic_helper = get_topic_helper();
        let topic = iot_jobs_topic_helper.get_jobs_notify_topic();
        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        message_builder.set_topic("random");

        let payload = json!({});
        let payload = serde_json::to_string::<Value>(&payload)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        let message = message_builder.build_box_message();

        let resp = CommandHelper::received_jobs_notify_message(message.as_ref(), topic);

        assert!(resp.is_none());
    }

    #[test]
    fn received_jobs_notify_message_no_jobs_queued_test() {
        let iot_jobs_topic_helper = get_topic_helper();
        let topic = iot_jobs_topic_helper.get_jobs_notify_topic();
        let mut message_builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        message_builder.set_topic(topic.as_str());

        let payload = json!({
            "timestamp": 1517016948,
            "jobs": {
                "QUEUED" : [ ]
            }
        });
        let payload = serde_json::to_string::<Value>(&payload)
            .expect("Issue formatting Json.  Invalid code change.");
        message_builder.set_payload(payload);
        let message = message_builder.build_box_message();

        let resp = CommandHelper::received_jobs_notify_message(message.as_ref(), topic);

        assert!(resp.is_none());
    }

    #[test]
    fn create_message_test() {
        let payload = json!({"test":"value"});
        let topic = "topic".to_string();

        let pub_sub_message = CommandHelper::create_message(payload.to_owned(), topic.to_owned());

        assert_eq!(pub_sub_message.get_payload().to_owned(), payload.to_string());
        assert_eq!(pub_sub_message.get_topic(), topic);
    }

    #[test]
    fn parse_new_job_reboot_test() {
        let payload = json!({
            "execution" : {
                JOB_ID_FIELD : JOB_ID,
                "thingName" : CLIENT_ID,
                "jobDocument" : {
                    "version": "1.0",
                    "steps": [
                        {
                            "action": {
                                "name": "Run-Command",
                                "type": "runCommand",
                                "input": {
                                    "command": Command::Reboot.as_str()
                                }
                            }
                        }
                    ]
                }
            }
        });

        let command = CommandHelper::parse_new_job(payload);

        assert_eq!(command.unwrap(), Command::Reboot);
    }

    #[test]
    fn parse_new_job_invalid_command_test() {
        let payload = json!({
            "execution" : {
                JOB_ID_FIELD : JOB_ID,
                "thingName" : CLIENT_ID,
                "jobDocument" : "{
                    \"version\": \"1.0\",
                    \"steps\": [
                        {
                            \"action\": {
                                \"name\": \"Run-Command\",
                                \"type\": \"runCommand\",
                                \"input\": {
                                    \"command\": \"INVALID\"
                                }
                            }
                        }
                    ]
                }",
            }
        });

        let command = CommandHelper::parse_new_job(payload);

        assert!(command.is_none());
    }

    fn get_topic_helper() -> TopicHelper {
        TopicHelper::new(CLIENT_ID.to_string(), None)
    }

    #[test]
    fn is_reboot_test() {
        let job_execution_file = std::env::current_dir()
            .unwrap()
            .join(PATH_TO_REBOOT_JOB_EXECUTION_FILE)
            .to_str()
            .unwrap()
            .to_owned();
        let job_execution_str =
            fs::read_to_string(job_execution_file).expect("Unable to read test file");
        let job_execution: Value = serde_json::from_str(job_execution_str.as_str())
            .expect("Unable to convert string to Json");

        assert!(CommandHelper::is_reboot(&job_execution));
    }
}
