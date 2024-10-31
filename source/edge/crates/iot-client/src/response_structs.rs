use serde::{Deserialize, Serialize};
use serde_json::{from_str, Error};

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
pub struct IotCredsResult {
    pub credentials: Option<AwsCredentials>,
    pub message: Option<String>,
}

#[derive(Clone, Debug, Deserialize, PartialEq, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AwsCredentials {
    pub access_key_id: String,
    pub secret_access_key: String,
    pub session_token: String,
    pub expiration: String,
}

pub fn parse_iot_creds_result(text: &str) -> Result<IotCredsResult, Error> {
    from_str(text)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_valid_credentials() {
        let parsed = parse_iot_creds_result(MOCK_CORRECT_IOT_RESPONSE).unwrap();
        assert_eq!(
            parsed,
            IotCredsResult {
                credentials: Some(AwsCredentials {
                    access_key_id: "hello".to_string(),
                    secret_access_key: "secret".to_string(),
                    session_token: "token".to_string(),
                    expiration: "2024-03-26T19:33:10Z".to_string(),
                }),
                message: None,
            }
        )
    }

    #[test]
    fn parse_invalid_credentials() {
        let parsed = parse_iot_creds_result(MOCK_ERROR_IOT_RESPONSE).unwrap();
        assert_eq!(
            parsed,
            IotCredsResult {
                credentials: None,
                message: Some("Role alias does not exist".to_string()),
            }
        )
    }

    const MOCK_CORRECT_IOT_RESPONSE: &str = r#"{"credentials":{"accessKeyId":"hello","secretAccessKey":"secret","sessionToken":"token","expiration":"2024-03-26T19:33:10Z"}}"#;
    const MOCK_ERROR_IOT_RESPONSE: &str = r#"{"message":"Role alias does not exist"}"#;
}
