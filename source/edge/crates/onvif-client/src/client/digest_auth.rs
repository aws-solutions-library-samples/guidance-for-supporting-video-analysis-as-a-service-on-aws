use crate::client::constant::VHT_DIGEST_URI;
use crate::client::error::OnvifClientError;
use crate::client::onvif_client::Credential;
use http_auth::{ChallengeParser, DigestClient, PasswordParams};
use mockall::automock;
use reqwest::header::HeaderValue;
use tracing::{info, instrument, warn};

/// This trait responds to a given challenge.
/// It has Debug as a supertrait to satisfy log tracing attribute #[instrument]
#[allow(clippy::needless_lifetimes)]
#[automock]
pub trait DigestClientTrait: std::fmt::Debug + Send + Sync {
    /// digest client calculates the digest based on the given credentials
    fn respond<'a>(&mut self, p: PasswordParams<'a>) -> Result<String, String>;
}

/// a struct to wrap DigestClient.
#[derive(Debug)]
pub struct DigestClientImpl {
    inner: DigestClient,
}

impl DigestClientImpl {
    /// Initialization of the DigestClient wrapper
    pub fn new(inner: DigestClient) -> Self {
        Self { inner }
    }
}

// This allow is necessary to cope with the incorrect needless_lifetimes warning.
// This github issue  https://github.com/rust-lang/rust-clippy/issues/2944 illustrate similar clippy issue.
#[allow(clippy::needless_lifetimes)]
impl DigestClientTrait for DigestClientImpl {
    fn respond<'a>(&mut self, p: PasswordParams<'a>) -> Result<String, String> {
        self.inner.respond(&p)
    }
}

/// the service struct that holds a reference to the digest client
/// and implements the digest authentication logic
#[derive(Debug)]
pub struct DigestAuth {
    digest_client: Box<dyn DigestClientTrait>,
    credential: Credential,
}

impl DigestAuth {
    /// Initialization of DigestAuth
    pub fn new(digest_client: Box<dyn DigestClientTrait>, credential: Credential) -> Self {
        Self { digest_client, credential }
    }

    /// returns digest authentication in HeaderValue format that can be added to the ONVIF http request
    #[instrument]
    pub(crate) fn create_digest_header(
        &mut self,
        device_service_uri: &str,
    ) -> Result<reqwest::header::HeaderValue, OnvifClientError> {
        info!("Generating digest authentication http header");

        let authorization = self.create_auth_string_from_digest_client(device_service_uri)?;

        // converts digest from String to datatype HeaderValue
        match HeaderValue::try_from(authorization) {
            Ok(auth_header) => Ok(auth_header),
            Err(_) => {
                warn!("Failed to create the header value from the given credential ");
                Err(OnvifClientError::DigestError)
            }
        }
    }

    /// the digest client responds to the challenge and returns autthentication in String format
    #[instrument]
    fn create_auth_string_from_digest_client(
        &mut self,
        device_service_uri: &str,
    ) -> Result<String, OnvifClientError> {
        let authorization = self
            .digest_client
            .respond(http_auth::PasswordParams {
                username: self.credential.username.as_str(),
                password: self.credential.password.as_str(),
                uri: VHT_DIGEST_URI,
                method: reqwest::Method::POST.as_str(),
                body: None,
            })
            .map_err(|_| {
                warn!("Digest client failed to respond to the challenge with supplied credential!");
                OnvifClientError::DigestError
            })?;

        Ok(authorization)
    }

    /// given digest parameters in HeaderValue format, returns digest client
    /// that will be used to respond to challenge.
    /// Digest parameters are: digest algo, realm, qop, nonce, opaque
    #[instrument]
    pub(crate) fn generate_digest_client(
        digest_params: Option<&HeaderValue>,
    ) -> Result<DigestClient, OnvifClientError> {
        let digest_params = match digest_params {
            None => {
                warn!("Digest parameters is none!");
                return Err(OnvifClientError::DigestError);
            }
            Some(params) => params,
        };

        let digest_params_str = match digest_params.to_str() {
            Ok(str) => str,
            Err(_) => {
                warn!("Failed to transform Digest HeaderValue into string slice!");
                return Err(OnvifClientError::DigestError);
            }
        };

        let challenge_ref = ChallengeParser::new(digest_params_str)
            .next()
            //If ChallengeParser returns None.
            .ok_or_else(|| {
                warn!("The ChallengeRef list is empty. No digest challenges were parsed. ");
                OnvifClientError::DigestError
            })?
            //If ChallengeParser returns an result that is an error.
            .map_err(|e| {
                warn!("Failed to parse the digest params into a challenge: {}", e);
                OnvifClientError::DigestError
            })?;

        let digest_auth_client =
            http_auth::DigestClient::try_from(&challenge_ref).map_err(|_| {
                warn!("Failed to build the digest client!");
                OnvifClientError::DigestError
            })?;

        Ok(digest_auth_client)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use http_auth::PasswordClient;

    /// when given digest challenge, correct digest client is generated
    #[test]
    fn verify_generate_digest_client() {
        // Arrange
        let digest_params = Some(HeaderValue::from_str(DIGEST_CHALLENGE).unwrap());
        let expected_digest_client =
            PasswordClient::builder().challenges(DIGEST_CHALLENGE).build().unwrap();

        let expected_digest_client = match expected_digest_client {
            PasswordClient::Digest(c) => c,
            _ => {
                panic!("Failed to create expected digest client for unit test. ")
            }
        };

        // Act
        let digest_client = DigestAuth::generate_digest_client(digest_params.as_ref()).unwrap();

        // Assert
        assert_eq!(digest_client, expected_digest_client);
    }

    /// when digest parameter doesn't exist, the digest client is not generated, return error
    #[test]
    fn generate_digest_client_no_digest_params_error() {
        //Arrange
        let digest_params = None;

        //Act
        let err = DigestAuth::generate_digest_client(digest_params.as_ref()).unwrap_err();

        // Assert
        assert_eq!(err.to_string(), OnvifClientError::DigestError.to_string());
    }

    /// when digest parameter is missing nonce, the digest client is not generated, return error
    #[test]
    fn generate_digest_client_digest_params_has_no_nonce_error() {
        // Arrange
        let digest_params = Some(HeaderValue::from_str(NO_NONCE_DIGEST_CHALLENGE).unwrap());

        // Act
        let err = DigestAuth::generate_digest_client(digest_params.as_ref()).unwrap_err();

        // Assert
        assert_eq!(err.to_string(), OnvifClientError::DigestError.to_string());
    }

    /// given an uri, return the correct authentication string responded from digest client
    #[test]
    fn verify_create_auth_string_from_digest_client() {
        // Arrange
        let mut mock_digest_client = MockDigestClientTrait::default();
        mock_digest_client
            .expect_respond()
            .return_once(|_| Ok(DIGEST_CLIENT_RESPOND_TO_CHALLENGE.to_string()));

        let mut digest_auth = DigestAuth::new(
            Box::new(mock_digest_client),
            Credential { username: "VideoAnalytics".to_string(), password: "12345".to_string() },
        );
        let expected_auth_string = format!(
            r#"Digest username="{}", realm="Silvan_http_digest", uri="{}", nonce="{}", algorithm=MD5, nc=00000001, cnonce="{}", qop=auth, response="928519bbe2f96a625e4282c7761b080f", opaque="5b6ea228""#,
            DIGEST_USERNAME, DEVICE_SERVICE_URI, TEST_CNONCE, TEST_CNONCE
        );

        // Act
        let auth_string =
            digest_auth.create_auth_string_from_digest_client(DEVICE_SERVICE_URI).unwrap();

        // Assert
        assert_eq!(auth_string, expected_auth_string);
    }

    /// when digest client fails to respond to the challenge, return error
    #[test]
    fn create_auth_string_from_digest_client_respond_fails_error() {
        // Arrange
        let mut mock_digest_client = MockDigestClientTrait::default();
        mock_digest_client
            .expect_respond()
            .return_once(|_| Err("nonce count exhausted".to_string()));

        let mut digest_auth = DigestAuth::new(
            Box::new(mock_digest_client),
            Credential { username: "VideoAnalytics".to_string(), password: "12345".to_string() },
        );

        // Act
        let err =
            digest_auth.create_auth_string_from_digest_client(DEVICE_SERVICE_URI).unwrap_err();

        // Assert
        assert_eq!(err.to_string(), OnvifClientError::DigestError.to_string());
    }

    const DIGEST_CHALLENGE: &str = r#"Digest algorithm=MD5, realm="Silvan_http_digest", qop="auth", nonce="62d82aa9ca59e3a04cd1", opaque="5b6ea228""#;

    // TODO: add the ability to generate random cnonce if it makes the test more robust
    const TEST_CNONCE: &str = r#"62d82aa9ca59e3a04cd1"#;

    const NO_NONCE_DIGEST_CHALLENGE: &str =
        r#"Digest algorithm=MD5, realm="Silvan_http_digest", qop="auth", opaque="5b6ea228""#;

    const DEVICE_SERVICE_URI: &str = r#"http://192.168.37.12/onvif/device_service"#;

    const DIGEST_USERNAME: &str = "admin";

    const DIGEST_CLIENT_RESPOND_TO_CHALLENGE: &str = r#"Digest username="admin", realm="Silvan_http_digest", uri="http://192.168.37.12/onvif/device_service", nonce="62d82aa9ca59e3a04cd1", algorithm=MD5, nc=00000001, cnonce="62d82aa9ca59e3a04cd1", qop=auth, response="928519bbe2f96a625e4282c7761b080f", opaque="5b6ea228""#;
}
