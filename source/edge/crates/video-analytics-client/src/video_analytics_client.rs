use crate::constants::{GRACE_PERIOD, VL_API_GW_NAME, X_AMZ_DATE, X_AMZ_SECURITY_TOKEN};
use aws_credential_types::Credentials;
use aws_sdk_apigateway::types::RestApi;
use aws_sdk_apigateway::Client as ApiGatewayClient;
use aws_sdk_apigateway::Config;
use aws_sigv4::http_request::{
    sign, SignableBody, SignableRequest, SignatureLocation, SigningParams, SigningSettings,
};
use aws_sigv4::sign::v4;
use aws_types::region::Region;
use iot_client::client::IotCredentialProvider;
use iot_client::error::IoTClientError::ClientError;
use reqwest::header::{HeaderMap, HeaderValue};
use reqwest::Client;
use serde_json::json;
use std::error::Error;
use std::time::{Duration, SystemTime};
use tracing::{debug, error, warn};

#[derive(Debug, Clone)]
pub struct VideoAnalyticsClient {
    http_client: Option<Client>,
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
                    http_client: None,
                    expiration: None,
                    api_gw_endpoint: None,
                    credentials: None,
                    iot_credential_provider: None,
                };
            }
        };

        let expiration = credentials.expiry().expect("Unable to get expiration time from sts");

        // Use api gw client to infer api gw endpoint
        let config = Config::builder()
            .region(Region::new(iot_credential_provider.region.to_owned()))
            .credentials_provider(credentials.clone())
            .build();

        let api_gw_client = ApiGatewayClient::from_conf(config);
        let mut vl_api_id = String::from("");
        match api_gw_client.get_rest_apis().send().await {
            Ok(rest_apis_resp) => {
                let filtered_apis: Vec<&RestApi> = rest_apis_resp
                    .items()
                    .iter()
                    .filter(|api| api.name().unwrap_or("") == VL_API_GW_NAME)
                    .collect();
                if !filtered_apis.is_empty() {
                    vl_api_id = filtered_apis[0].id().unwrap_or("").to_owned();
                }
            }
            Err(e) => {
                warn!("Unable to fetch api gw endpoint {:?}", e);
            }
        };

        // If API_GW_ENDPOINT env var is set, use that value
        let api_gw_endpoint = std::env::var("API_GW_ENDPOINT").unwrap_or(format!(
            "https://{}.execute-api.{}.amazonaws.com/prod",
            vl_api_id,
            iot_credential_provider.region.to_owned()
        ));
        debug!("Using API GW endpoint: {}", api_gw_endpoint);

        VideoAnalyticsClient {
            http_client: Some(Client::new()),
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

        if self.http_client.is_none() {
            return Err(Box::new(ClientError(
                "Unable to connect to Video Analytics client".to_string(),
            )));
        }

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

        let uri_string = format!("{}/import-media-object", self.api_gw_endpoint.as_ref().unwrap());
        let body = json!({
            "deviceId": device_id,
            "mediaObject": media_object
        });
        let body_string = body.to_string();

        let signable_request = SignableRequest::new(
            "POST",
            uri_string.clone(),
            vec![("content-type", "application/json")].into_iter(),
            SignableBody::Bytes(body_string.as_bytes()),
        )
        .unwrap();

        let mut request = http::Request::builder()
            .uri(uri_string.clone())
            .method("POST")
            .body(body_string.clone())
            .unwrap();

        let (signing_instructions, _signature) =
            sign(signable_request, &signing_params).unwrap().into_parts();
        signing_instructions.apply_to_request_http1x(&mut request);

        // Convert http::Request to reqwest::Request
        let mut headers = HeaderMap::new();
        headers.insert(
            reqwest::header::AUTHORIZATION,
            HeaderValue::from_str(
                request.headers().get(http::header::AUTHORIZATION).unwrap().to_str().unwrap(),
            )
            .unwrap(),
        );
        headers.insert(reqwest::header::CONTENT_TYPE, HeaderValue::from_static("application/json"));
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

        let client = self.http_client.to_owned().unwrap();

        let resp = client
            .post(uri_string.clone())
            .headers(headers)
            .body(body_string.clone())
            .send()
            .await?;

        match resp.status() {
            reqwest::StatusCode::OK => return Ok(()),
            _ => {
                error!("Failed to import media object");
                return Err(Box::new(ClientError(resp.text().await?)));
            }
        }
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
