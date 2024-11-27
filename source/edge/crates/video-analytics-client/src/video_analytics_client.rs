use crate::constants::{GRACE_PERIOD, X_AMZ_DATE, X_AMZ_SECURITY_TOKEN};
use aws_config::default_provider::credentials::DefaultCredentialsChain;
use aws_credential_types::Credentials;
use aws_sigv4::http_request::{
    sign, SignableBody, SignableRequest, SignatureLocation, SigningParams, SigningSettings,
};
use aws_sigv4::sign::v4;
use aws_smithy_types::body::SdkBody;
use aws_smithy_types::byte_stream::ByteStream;
use aws_types::region::{Region, SigningRegion};
use http::Uri;
use iot_client::client::IotCredentialProvider;
use iot_client::error::IoTClientError::ClientError;
use reqwest::header::{HeaderMap, HeaderValue};
use reqwest::Client;
use std::error::Error;
use std::time::{Duration, SystemTime};
use tracing::{debug, error};

#[derive(Debug, Clone)]
pub struct VideoAnalyticsClient {
    expiration: Option<SystemTime>,
    api_gw_endpoint: Option<String>,
    credentials: Option<Credentials>,
    iot_credential_provider: Option<IotCredentialProvider>,
}

impl VideoAnalyticsClient {
    pub async fn from_conf() -> Self {
        let iot_credential_provider = IotCredentialProvider::from_config();

        let credentials = match iot_credential_provider.retrieve_creds_from_iot().await {
            Ok(creds) => creds,
            Err(e) => {
                error!("Unable to fetch credentials {:?}", e);
                return VideoAnalyticsClient {
                    expiration: None,
                    api_gw_endpoint: None,
                    credentials: None,
                    iot_credential_provider: None,
                };
            }
        };

        let expiration = credentials.expiry().expect("Unable to get expiration time from sts");

        // TODO: dynamically retrieve this endpoint
        let api_gw_endpoint = std::env::var("API_GW_ENDPOINT").unwrap();

        VideoAnalyticsClient {
            expiration: Some(expiration),
            api_gw_endpoint: Some(api_gw_endpoint),
            credentials: Some(credentials),
            iot_credential_provider: Some(iot_credential_provider),
        }
    }

    pub async fn import_media_object(
        &mut self,
        device_id: String,
        media_object: Vec<u8>,
    ) -> Result<(), Box<dyn Error>> {
        self.check_session_token_expiration().await;
        let credentials = self.credentials.as_ref().expect("Failed to retrieve IoT credentials");
        let iot_credential_provider = self
            .iot_credential_provider
            .as_ref()
            .expect("Failed to retrieve IoT credential provider");

        let mut signing_settings = SigningSettings::default();
        signing_settings.signature_location = SignatureLocation::Headers;
        let identity = credentials.clone().into();
        let signing_params: SigningParams = v4::SigningParams::builder()
            .identity(&identity)
            .region(iot_credential_provider.region.as_str())
            .name("execute-api")
            .time(SystemTime::now())
            .settings(signing_settings)
            .build()
            .unwrap()
            .into();

        let uri_string =
            format!("{}/import-media-object/{}", self.api_gw_endpoint.as_ref().unwrap(), device_id);

        let signable_request = SignableRequest::new(
            "POST",
            uri_string.clone(),
            vec![("content-type", "application/octet-stream")].into_iter(),
            SignableBody::Bytes(media_object.as_slice()),
        )
        .unwrap();

        let mut request = http::Request::builder()
            .uri(uri_string.clone())
            .method("POST")
            .body(media_object.clone())
            .unwrap();

        let (signing_instructions, _signature) =
            sign(signable_request, &signing_params).unwrap().into_parts();
        signing_instructions.apply_to_request_http1x(&mut request);

        let client = Client::new();

        // Convert http::Request to reqwest::Request
        let mut headers = HeaderMap::new();
        headers.insert(
            reqwest::header::AUTHORIZATION,
            HeaderValue::from_str(
                request.headers().get(http::header::AUTHORIZATION).unwrap().to_str().unwrap(),
            )
            .unwrap(),
        );
        headers.insert(
            reqwest::header::CONTENT_TYPE,
            HeaderValue::from_static("application/octet-stream"),
        );
        headers.insert(
            reqwest::header::HeaderName::from_static(X_AMZ_DATE),
            HeaderValue::from_str(
                request
                    .headers()
                    .get(http::header::HeaderName::from_static(X_AMZ_DATE))
                    .unwrap()
                    .to_str()
                    .unwrap(),
            )
            .unwrap(),
        );
        headers.insert(
            reqwest::header::HeaderName::from_static(X_AMZ_SECURITY_TOKEN),
            HeaderValue::from_str(
                request
                    .headers()
                    .get(http::header::HeaderName::from_static(X_AMZ_SECURITY_TOKEN))
                    .unwrap()
                    .to_str()
                    .unwrap(),
            )
            .unwrap(),
        );

        let resp = client
            .post(uri_string.clone())
            .headers(headers)
            .body(media_object.clone())
            .send()
            .await?;

        match resp.status() {
            reqwest::StatusCode::OK => return Ok(()),
            _ => {
                error!("Failed to import media object");
                return Err(Box::new(ClientError(resp.text().await?)));
            }
        }

        Ok(())
    }

    pub async fn check_session_token_expiration(&mut self) {
        // If the previous credentials expire within the grace period, we need to refresh the credentials
        if self.is_expiration_within_grace_period() {
            let iot_credential_provider = IotCredentialProvider::from_config();

            let credential_provider = match iot_credential_provider.retrieve_creds_from_iot().await
            {
                Ok(credential_provider) => credential_provider,
                Err(e) => {
                    error!("Unable to fetch credentials {:?}", e);
                    return;
                }
            };

            let expiration =
                credential_provider.expiry().expect("Unable to get expiration time from sts");

            self.expiration = Some(expiration);
        }
    }

    pub fn is_expiration_within_grace_period(&self) -> bool {
        // None means that the original Video Analytics client initialization failed
        if self.expiration.is_none() {
            return true;
        }
        let current_time = SystemTime::now();
        if let Ok(duration_since_past) = self.expiration.unwrap().duration_since(current_time) {
            return duration_since_past <= Duration::from_secs(GRACE_PERIOD);
        }
        false
    }
}
