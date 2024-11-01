use super::error::IoTClientError;
use super::error::IoTClientError::CredentialsError;
use super::http::{Request, RequestHttp};
use super::response_structs::{parse_iot_creds_result, IotCredsResult};
use aws_credential_types::Credentials;
use chrono::{DateTime, Utc};
use device_traits::channel_utils::ServiceCommunicationManager;
use mockall::automock;
use reqwest::header::{HeaderMap, InvalidHeaderValue};
use std::time::{SystemTime, UNIX_EPOCH};
use std::{error::Error, fs, io::Read};

const SIGV4_SIGNER: &str = "AWSS4";

#[derive(Debug, Default, Clone)]
pub struct IotCredentialProvider {
    pub endpoint: String,
    pub thing_name: String,
    pub role_name: String,
    pub cert_filepath: String,
    pub key_filepath: String,
    pub region: String,
}

#[automock]
impl IotCredentialProvider {
    /// create IotCredentialProvider for kvs access
    pub fn from_config() -> IotCredentialProvider {
        let mut get_configs = ServiceCommunicationManager::default();
        let config = get_configs.get_configurations().expect("Failed to retrieve configurations");
        IotCredentialProvider {
            endpoint: config.credential_endpoint.to_owned(),
            thing_name: config.client_id.to_owned(),
            role_name: config.role_aliases.to_owned(),
            cert_filepath: config.cert_path.to_owned(),
            key_filepath: config.key_path.to_owned(),
            region: config.aws_region.to_owned(),
        }
    }

    /// create IotCredentialProvider based on the role_name (aka role alias) passed in
    pub fn from_s3_role_alias(role_name: String) -> IotCredentialProvider {
        let mut get_configs = ServiceCommunicationManager::default();
        let config = get_configs.get_configurations().expect("Failed to retrieve configurations");
        IotCredentialProvider {
            endpoint: config.credential_endpoint.to_owned(),
            thing_name: config.client_id.to_owned(),
            role_name: role_name.to_owned(),
            cert_filepath: config.cert_path.to_owned(),
            key_filepath: config.key_path.to_owned(),
            region: config.aws_region.to_owned(),
        }
    }

    pub async fn retrieve_creds_from_iot(&self) -> Result<Credentials, IoTClientError> {
        let client = self.create_client().map_err(|e| CredentialsError(e.to_string()))?;

        self.retrieve_creds_from_iot_with_client(&client).await
    }

    async fn retrieve_creds_from_iot_with_client(
        &self,
        client: &dyn RequestHttp,
    ) -> Result<Credentials, IoTClientError> {
        let response = self
            .role_creds_from_client(client)
            .await
            .map_err(|e| CredentialsError(e.to_string()))?;

        match response.credentials {
            Some(x) => {
                let expiration = self
                    .convert_date_time_to_system_time(x.expiration)
                    .map_err(|e| CredentialsError(e.to_string()))?;
                Ok(Credentials::new(
                    x.access_key_id,
                    x.secret_access_key,
                    Some(x.session_token),
                    Some(expiration),
                    SIGV4_SIGNER,
                ))
            }
            None => Err(CredentialsError(
                response.message.unwrap_or("Failed to get iot creds or error message!".to_string()),
            )),
        }
    }

    // Convert DateTime<Utc> to a timestamp
    fn convert_date_time_to_system_time(
        &self,
        date_str: String,
    ) -> Result<SystemTime, Box<dyn Error>> {
        let datetime = date_str.parse::<DateTime<Utc>>()?;
        let timestamp = datetime.timestamp();
        let nsecs = datetime.timestamp_subsec_nanos();
        let system_time = UNIX_EPOCH + std::time::Duration::new(timestamp as u64, nsecs);
        Ok(system_time)
    }

    fn read_cert_file_to_buf(&self, cert_buf: &mut Vec<u8>) -> std::io::Result<usize> {
        fs::File::open(&self.cert_filepath)?.read_to_end(cert_buf)
    }

    fn read_key_file_to_buf(&self, cert_buf: &mut Vec<u8>) -> std::io::Result<usize> {
        fs::File::open(&self.key_filepath)?.read_to_end(cert_buf)
    }

    fn client_cert(&self) -> Result<Vec<u8>, Box<dyn Error>> {
        let mut cert_buf = Vec::new();
        self.read_cert_file_to_buf(&mut cert_buf)?;
        self.read_key_file_to_buf(&mut cert_buf)?;
        Ok(cert_buf)
    }

    fn client_header(&self) -> Result<HeaderMap, InvalidHeaderValue> {
        let mut headers = HeaderMap::new();
        headers.insert("x-amzn-iot-thingname", self.thing_name.to_owned().parse()?);
        Ok(headers)
    }

    fn create_client(&self) -> Result<Request, Box<dyn Error>> {
        let headers = self.client_header()?;
        let mut cert = self.client_cert()?;

        Ok(Request::create(&mut cert, headers)?)
    }

    async fn role_creds_from_client(
        &self,
        client: &dyn RequestHttp,
    ) -> Result<IotCredsResult, Box<dyn Error>> {
        let response = client
            .get_text(format!(
                "https://{}/role-aliases/{}/credentials",
                self.endpoint, self.role_name,
            ))
            .await?;
        Ok(parse_iot_creds_result(response.as_str())?)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::constants::SIGV4_SIGNER;
    use crate::iot::http::MockRequestHttp;

    const MOCK_CORRECT_IOT_RESPONSE: &str = r#"{"credentials":{"accessKeyId":"a","secretAccessKey":"b","sessionToken":"c","expiration":"2024-03-26T19:33:10Z"}}"#;
    const MOCK_ERROR_IOT_RESPONSE: &str = r#"{"message":"??????"}"#;

    #[tokio::test]
    async fn retrieve_creds_success() {
        let mut mock_request = MockRequestHttp::new();
        mock_request
            .expect_get_text()
            .return_once(move |_| Ok(MOCK_CORRECT_IOT_RESPONSE.to_string()));

        let iot = IotCredentialProvider {
            endpoint: "".to_string(),
            thing_name: "".to_string(),
            role_name: "".to_string(),
            cert_filepath: "README.md".to_string(),
            key_filepath: "README.md".to_string(),
            region: "".to_string(),
        };
        let creds = iot.retrieve_creds_from_iot_with_client(&mock_request).await.unwrap();
        let converted_time =
            iot.convert_date_time_to_system_time("2024-03-26T19:33:10Z".to_string()).unwrap();
        assert_eq!(
            creds,
            Credentials::new("a", "b", Some("c".to_string()), Some(converted_time), SIGV4_SIGNER)
        );
    }

    #[tokio::test]
    async fn retrieve_creds_error() {
        let mut mock_request = MockRequestHttp::new();
        mock_request
            .expect_get_text()
            .return_once(move |_| Ok(MOCK_ERROR_IOT_RESPONSE.to_string()));

        let iot = IotCredentialProvider {
            endpoint: "".to_string(),
            thing_name: "".to_string(),
            role_name: "".to_string(),
            cert_filepath: "README.md".to_string(),
            key_filepath: "README.md".to_string(),
            region: "".to_string(),
        };
        let creds_error = iot.retrieve_creds_from_iot_with_client(&mock_request).await;
        assert!(creds_error.is_err());
    }

    #[test]
    fn cert_file_doesnt_exist_errors() {
        let iot = IotCredentialProvider {
            endpoint: "".to_string(),
            thing_name: "".to_string(),
            role_name: "".to_string(),
            cert_filepath: "this_file_doesnt_exist.cert".to_string(),
            key_filepath: "".to_string(),
            region: "".to_string(),
        };
        let mut cert_buf = Vec::new();
        let result = iot.read_cert_file_to_buf(&mut cert_buf).unwrap_err();
        assert_eq!(result.kind(), std::io::ErrorKind::NotFound);
    }

    #[test]
    fn key_file_doesnt_exist_errors() {
        let iot = IotCredentialProvider {
            endpoint: "".to_string(),
            thing_name: "".to_string(),
            role_name: "".to_string(),
            cert_filepath: "".to_string(),
            key_filepath: "this_file_doesnt_exist.key".to_string(),
            region: "".to_string(),
        };
        let mut cert_buf = Vec::new();
        let result = iot.read_key_file_to_buf(&mut cert_buf).unwrap_err();
        assert_eq!(result.kind(), std::io::ErrorKind::NotFound);
    }
}
