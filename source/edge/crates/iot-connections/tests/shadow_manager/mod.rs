/// Test to ensure that shadow manager is correctly saving information to and from the filesystem.

#[cfg(test)]
mod tests {
    use device_traits::channel_utils::traits::MockIoTServiceSender;
    use device_traits::connections::ShadowManager;
    use iot_connections::shadow_manager::IotShadowManager;
    use mqtt_client::builder::MQTTMessageBuilder;
    use serde_json::{json, Value};
    use std::path::{Path, PathBuf};
    use tokio::fs::{remove_file, write};

    const THING_NAME: &str = "ThingName";
    const SHADOW_NAME: &str = "ShadowName";
    const DIR: &str = "tests/files-for-integration-tests";

    /// Test that shadow updates to the desired state update correctly and write to the FS
    #[tokio::test]
    async fn update_local_storage() {
        let dir = std::env::current_dir().unwrap().join(DIR);
        let shadow_file = dir.join(format!("{}_{}.json", THING_NAME, SHADOW_NAME));

        let mut named_shadow =
            get_iot_shadow_manager(THING_NAME, Some(SHADOW_NAME.to_owned()), dir.to_owned(), None)
                .await;
        // Test multiple updates
        shadow_update_and_check(
            json!({"connected":true}),
            json!({"connected":true}),
            &shadow_file,
            &mut named_shadow,
        )
        .await;
        shadow_update_and_check(
            json!({"status":"enabled"}),
            json!({"status":"enabled","connected":true}),
            &shadow_file,
            &mut named_shadow,
        )
        .await;
        shadow_update_and_check(
            json!({"connected":false}),
            json!({"status":"enabled","connected":false}),
            &shadow_file,
            &mut named_shadow,
        )
        .await;
        cleanup(&shadow_file).await;
    }

    ///Test restore from storage
    #[tokio::test]
    async fn restore_shadow_manager_from_storage() {
        let dir = std::env::current_dir().unwrap().join(DIR);
        let shadow_file = dir.join(format!("{}_{}.json", THING_NAME, "default"));

        // Create shadow file.
        create_shadow_file(&shadow_file, json!({"status":"enabled"})).await;

        let mut classic_shadow =
            get_iot_shadow_manager(THING_NAME, None, dir.to_owned(), None).await;

        shadow_update_and_check(
            json!({"connected":false}),
            json!({"status":"enabled","connected":false}),
            &shadow_file,
            &mut classic_shadow,
        )
        .await;
        cleanup(&shadow_file).await;
    }

    /// Helper function to create IoTShadowManagers for tests
    async fn get_iot_shadow_manager(
        thing_name: &str,
        shadow_name: Option<String>,
        dir_path: PathBuf,
        mock_sender: Option<Box<MockIoTServiceSender>>,
    ) -> Box<dyn ShadowManager> {
        let builder = MQTTMessageBuilder::new_pub_sub_message_builder();
        let mut manager = IotShadowManager::new_shadow_manager(
            thing_name,
            shadow_name,
            mock_sender.unwrap_or(Box::default()),
            builder,
            dir_path,
        );
        let _result = manager.enable_storage().await;
        manager
    }

    // Update shadow and check output
    async fn shadow_update_and_check(
        input: Value,
        expected: Value,
        shadow_file: &Path,
        shadow: &mut Box<dyn ShadowManager>,
    ) {
        let result = shadow.update_desired_state(input).await;
        assert!(result.is_ok());
        check_correct_shadow(shadow_file, expected).await;
    }
    // Check the associate file the shadow stores its state in, confirm correct values.
    async fn check_correct_shadow(path: &Path, expected_value: Value) {
        let shadow_doc_value = get_shadow_storage_value(path).await;
        assert_eq!(expected_value, shadow_doc_value);
    }
    /// We will watch the recording of the state in the filesystem.
    async fn get_shadow_storage_value(path: &Path) -> serde_json::Value {
        let file_contents = tokio::fs::read_to_string(path).await.unwrap();
        serde_json::from_str(&file_contents).unwrap()
    }
    // Create a shadow file
    async fn create_shadow_file(path: &Path, contents: Value) {
        write(path, contents.to_string().as_str()).await.expect("Could not write to file.");
    }
    //cleanup shadow stored file.
    async fn cleanup(path: &Path) {
        remove_file(path).await.expect("Could not cleanup file.");
    }
}
