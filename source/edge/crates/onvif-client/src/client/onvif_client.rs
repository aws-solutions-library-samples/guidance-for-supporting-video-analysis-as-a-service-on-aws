use std::borrow::Borrow;
use std::collections::HashMap;
use std::error::Error;
use std::fmt::Debug;
use std::str::FromStr;

use async_trait::async_trait;
use device_traits::{merge::Merge, DeviceStateModel};
use http_client::client::HttpClient;
use reqwest::{Request, Response, StatusCode};
use serde_json::{json, Value};
use tracing::{debug, error, info, instrument, warn};

use crate::client::digest_auth::{DigestAuth, DigestClientImpl};
use crate::client::error::OnvifClientError;
use crate::client::onvif_service::OnvifServiceName;
use crate::config::{get_onvif_config_from_file, OnvifClientConfig};
use crate::soap;
use crate::wsdl_rs::appmgmt::{GetAppsInfo, GetAppsInfoResponse};
use crate::wsdl_rs::devicemgmt::{
    GetDeviceInformation, GetDeviceInformationResponse, GetServices, GetServicesResponse,
};

/// struct to hold credential from GetUsers onvif call
/// suppressing the warning for derive Debug, since credential shouldn't be printed at all
#[derive(Debug, PartialEq, Clone)]
pub struct Credential {
    /// username to onvif server
    pub username: String,

    /// password to onvif server
    pub password: String,
}

/// struct for Onvif communication, implements the DeviceStateModel Trait.
#[derive(Debug)]
pub struct OnvifClient<T> {
    http_client: T,
    digest_params: Option<reqwest::header::HeaderValue>,
    service_paths: HashMap<OnvifServiceName, String>,
    credential: Option<Credential>,
}

impl<T> OnvifClient<T>
where
    T: HttpClient + std::fmt::Debug,
{
    /// Get new OnvifClient
    #[instrument]
    pub fn new(http_client: T) -> Self {
        info!("New OnvifClient Created.");
        Self { http_client, digest_params: None, service_paths: HashMap::new(), credential: None }
    }

    #[instrument]
    fn get_service_uri(&self, service: OnvifServiceName) -> Result<String, OnvifClientError> {
        let service_uri = self.service_paths.get(&service).ok_or({
            warn!("service uri for {:?} does not exist in process 2", service);
            OnvifClientError::OnvifGetServicesError
        })?;

        Ok(service_uri.to_string())
    }

    /// creates onvif request based on the service uri and the request body
    #[instrument]
    fn create_onvif_request(
        &self,
        service_uri: String,
        request_body: String,
    ) -> Result<Request, OnvifClientError> {
        match self.http_client.create_http_request_with_body(service_uri, request_body) {
            Ok(req) => Ok(req),
            Err(err) => Err(OnvifClientError::HttpClientError(err)),
        }
    }

    /// given a http request, add the  digest calculated by the digest client to the http header
    #[instrument]
    fn sign_onvif_request_with_digest(
        &self,
        http_request: &Request,
        mut digest_auth: DigestAuth,
    ) -> Result<Request, OnvifClientError> {
        let http_request_clone = http_request.try_clone().ok_or(OnvifClientError::DigestError)?;

        let digest_header = digest_auth.create_digest_header(
            self.get_service_uri(OnvifServiceName::DeviceService)?.as_str(),
        )?;

        let request_with_digest =
            self.http_client.add_header_to_http_request(digest_header, http_request_clone)?;

        Ok(request_with_digest)
    }

    /// given a http request, add the  digest calculated by the digest client to the http header
    #[instrument]
    async fn sign_onvif_request_with_digest_and_send(
        &mut self,
        onvif_http_request_no_digest: &Request,
    ) -> Result<String, OnvifClientError> {
        // Generates digest client. The Err case of this call is handled with match instead of bubbling up,
        // because it is a possibility at this stage for digest_params to not exist or be expired. Therefore, even though
        // the result could be Err, the code should continue
        let digest_client = DigestAuth::generate_digest_client(self.digest_params.as_ref());

        let mut onvif_response = match digest_client {
            Ok(client) => {
                let digest_auth = DigestAuth::new(
                    Box::new(DigestClientImpl::new(client)),
                    // Using clone for now. Will revisit after bug fixing is done to implement this in lifetime.
                    // Same with L178
                    self.credential.clone().expect("Onvif credential doesn't exist!"),
                );

                let onvif_http_request_with_digest =
                    self.sign_onvif_request_with_digest(onvif_http_request_no_digest, digest_auth)?;

                // first attempt of sending the onvif request
                self.http_client.send_http_request(onvif_http_request_with_digest).await?
            }

            Err(_) => {
                let onvif_request =
                    onvif_http_request_no_digest.try_clone().ok_or(OnvifClientError::Others)?;
                self.http_client.send_http_request(onvif_request).await?
            }
        };

        let mut resp_status = onvif_response.status();

        if resp_status == StatusCode::INTERNAL_SERVER_ERROR {
            panic!("Onvif server error. Error code: {}", resp_status);
        };

        // when digest_params doesn't exist or has expired
        if resp_status == StatusCode::UNAUTHORIZED {
            self.set_digest_params(onvif_response.borrow());

            // Generates digest client. Since the cnonce in digest_params could expire. As a result, everytime there's a new digest params,
            // the digest client ought to be regenerated with the latest digest_params.
            // Contrary to the same call above, at this stage, digest_params is already set in the previous line, if this call returns Err,
            // something is wrong, thus, bubble up the Err
            let digest_client = DigestAuth::generate_digest_client(self.digest_params.as_ref())?;

            let digest_auth = DigestAuth::new(
                Box::new(DigestClientImpl::new(digest_client)),
                self.credential.clone().expect("Onvif credential doesn't exist!"),
            );

            let onvif_request_with_digest =
                self.sign_onvif_request_with_digest(onvif_http_request_no_digest, digest_auth)?;

            // second attempt of sending request (with the latest digest_params)
            onvif_response = self.http_client.send_http_request(onvif_request_with_digest).await?;
            resp_status = onvif_response.status();
        };

        let resp_text = onvif_response.text().await?;

        // special error handling for GetStorageConfigurations
        if resp_status == StatusCode::BAD_REQUEST {
            if resp_text.contains("The requested storage configuration does not exist") {
                return Err(OnvifClientError::OnvifNoCardError);
            }
            if resp_text.contains("The requested storage is on error") {
                return Err(OnvifClientError::OnvifCardNotMountedError);
            }
            if resp_text.contains("The requested storage is not formatted") {
                return Err(OnvifClientError::OnvifCardNotFormattedError);
            }
            if resp_text.contains("The requested storage is on readonly mode") {
                return Err(OnvifClientError::OnvifCardError);
            }

            // If bad request response doesn't contain these error messages, treat as unexpected error
        }

        let onvif_response_str = if resp_status == StatusCode::OK {
            resp_text
        } else {
            // TODO: Temporary change the behavior to not panic for DI2, so the flow
            // can proceed even when setUser fails.
            // Revert back to panic behavior once setUser issue is solved
            // panic!("unexpected http status : {} from onvif server.", resp_status);
            warn!("unexpected http status : {} returned from onvif server.", resp_status);
            debug!("onvif error message: {}", resp_text);
            return Err(OnvifClientError::UnexpectedHttpStatusError);
        };

        Ok(onvif_response_str)
    }

    /// Retrieve digest parameters of the 401 response body from the ONVIF server.
    /// Depending on how nonce is implemented on the server, digest parameters could expire.
    /// Call this function for every onvif call for now
    #[instrument]
    fn set_digest_params(&mut self, response: &Response) -> () {
        info!("Getting digest parameters from ONVIF server.");
        self.digest_params = response.headers().get(reqwest::header::WWW_AUTHENTICATE).cloned();
    }

    /// Send request to Onvif server to get all available Onvif services
    #[instrument]
    pub async fn get_services(&mut self, ip_address: String) -> Result<(), OnvifClientError> {
        // each time get_services is called, the previous records of service_paths should be removed
        // so later on whne inserting services, we can be sure whether there are duplicated services or not
        self.service_paths.clear();

        let get_services_onvif_struct: GetServices = GetServices { include_capability: false };
        let get_services_no_digest_body = soap::serialize(&get_services_onvif_struct)?;

        // TODO: remove after IP discovery is implemented

        // TODO: remove after IP discovery is implemented
        let device_service_uri = format!("http://{}/onvif/device_service", ip_address);
        // create and send onvif request
        let get_services_onvif_request_no_digest =
            self.create_onvif_request(device_service_uri, get_services_no_digest_body)?;
        let resp = self.http_client.send_http_request(get_services_onvif_request_no_digest).await?;

        let device_services_resp: GetServicesResponse =
            soap::deserialize(resp.text().await?.as_str())?;

        // save the [service_name, service_path] mapping into OnvifClient struct
        for service in &device_services_resp.service {
            info!("Successfully retrieved ONVIF service: {}", service.namespace.as_str());

            let namespace_str = service.namespace.as_str();
            // try to map the service namespace with a known service
            match OnvifServiceName::from_str(namespace_str) {
                // if service is known (and not mapped yet), adds to the known service paths
                Ok(service_name) => {
                    let service_path = service.x_addr.to_string();
                    if self.service_paths.contains_key(&service_name) {
                        warn!(
                            "The service path for ONVIF service: {:?} already exist",
                            service_name
                        );
                        return Err(OnvifClientError::OnvifGetServicesError);
                    }
                    self.service_paths.insert(service_name, service_path);
                }
                Err(_err) => {
                    warn!("Service {} is unknown.", namespace_str);
                }
            }
        }

        Ok(())
    }

    /// Send request to Onvif server to get information related to the state of th device.
    #[instrument]
    async fn get_device_information(&mut self) -> Result<Value, Box<dyn Error>> {
        let get_device_info_onvif_struct: GetDeviceInformation = GetDeviceInformation {};
        let no_digest_body_from_struct = soap::serialize(&get_device_info_onvif_struct)?;

        // Create onvif request with given uri and body
        let onvif_request_no_digest = self.create_onvif_request(
            self.get_service_uri(OnvifServiceName::DeviceService)?,
            no_digest_body_from_struct,
        )?;

        let onvif_response_str =
            self.sign_onvif_request_with_digest_and_send(onvif_request_no_digest.borrow()).await?;
        let device_info_resp: GetDeviceInformationResponse =
            soap::deserialize(onvif_response_str.as_str())?;

        info!("Successfully retrieved device information: {:?}", device_info_resp);

        let device_info_json = serde_json::to_value(&device_info_resp)?;
        Ok(device_info_json)
    }

    async fn get_apps_info(
        &mut self,
        application_id: Option<String>,
    ) -> Result<GetAppsInfoResponse, OnvifClientError> {
        let get_apps_info_struct: GetAppsInfo = GetAppsInfo { app_id: application_id };
        let get_apps_info_http_body = soap::serialize(&get_apps_info_struct)?;

        let get_apps_info_no_digest = self.create_onvif_request(
            self.get_service_uri(OnvifServiceName::DeviceService)?,
            get_apps_info_http_body,
        )?;

        let resp_str =
            self.sign_onvif_request_with_digest_and_send(get_apps_info_no_digest.borrow()).await?;

        let get_apps_info_resp: GetAppsInfoResponse = soap::deserialize(resp_str.as_str())?;
        debug!("Rust struct Response from get_apps_info: {:?}", get_apps_info_resp);
        Ok(get_apps_info_resp)
    }
}

// GRCOV_STOP_COVERAGE
#[async_trait]
impl<T> DeviceStateModel for OnvifClient<T>
where
    T: HttpClient + std::fmt::Debug + Send + Sync,
{
    async fn bootstrap(
        &mut self,
        config_path: &str,
        ip_address: String,
    ) -> Result<(), Box<dyn Error>> {
        info!("Bootstrapping the device! ");
        // prior to interacting with onvif server, get temp onvif password from config
        let onvif_config: OnvifClientConfig = get_onvif_config_from_file(config_path).await?;
        self.credential = Some(Credential {
            username: onvif_config.onvif_account_name.to_owned(),
            password: onvif_config.onvif_password.to_owned(),
        });

        self.get_services(ip_address).await?;

        Ok(())
    }

    // since this function is making pass through calls to onvif client functions and merging the result, no unit test
    #[instrument]
    async fn get_device_information(&mut self) -> Result<Value, Box<dyn Error>> {
        let mut device_info = self.get_device_information().await?;

        let app_info_resp: GetAppsInfoResponse = self.get_apps_info(None).await?;
        // Find the AppInfo with app_id "ID_3"
        if let Some(app_info) = app_info_resp.info.iter().find(|app| app.app_id == "ID_3") {
            let version = &app_info.version;
            device_info.merge(&json!({ "ai_model_version": version }));
            info!("Added ai model version to device information with version as {}", version);
        } else {
            error!("App with ID_3 not found");
        }

        // Find the AppInfo with app_id "ID_2"
        if let Some(app_info) = app_info_resp.info.iter().find(|app| app.app_id == "ID_2") {
            let version = &app_info.version;
            device_info.merge(&json!({ "ai_sdk_version": version }));
            info!("Added ai sdk version to device information with version as {}", version);
        } else {
            error!("App with ID_2 not found");
        }

        info!("Updated device info {:?}", device_info);

        Ok(device_info)
    }
}

// GRCOV_BEGIN_COVERAGE
#[cfg(test)]
mod tests {
    use http::header::WWW_AUTHENTICATE;
    use http::HeaderValue;
    use http::Response as http_Response;
    use http::StatusCode;
    use http_auth::{PasswordClient, PasswordParams};
    use http_client::client::MockHttpClient;
    use mockall::predicate;
    use reqwest::{Client, Response};

    use crate::client::constant::VIDEO_ANALYTICS;
    use crate::client::digest_auth::MockDigestClientTrait;
    use crate::client::onvif_client::OnvifClient;
    use crate::wsdl_rs::appmgmt::AppInfo;
    use crate::wsdl_rs::devicemgmt::Service;
    use crate::xsd_rs::onvif_xsd::OnvifVersion;

    use super::*;

    /// Test function returns correct uri for the onvif service
    #[test]
    fn verify_get_service_uri() {
        // Arrange
        let mut onvif_client = OnvifClient::new(MockHttpClient::default());
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        // Act and Assert
        assert_eq!(
            onvif_client.get_service_uri(OnvifServiceName::DeviceService).unwrap(),
            "http://192.168.37.12/onvif/device_service"
        )
    }

    /// Test function returns the correct http request for onvif
    #[test]
    fn verify_create_onvif_request() {
        // Arrange
        let expected_request = reqwest::Client::new()
            .post(DEVICE_SERVICE_URI.to_string())
            .body(GET_DEVICE_INFORMATION_REQUEST_BODY.to_string())
            .build()
            .unwrap();

        let expected_request_clone = expected_request.try_clone().unwrap();
        let mut mock_http_client = MockHttpClient::default();
        mock_http_client
            .expect_create_http_request_with_body()
            .with(
                predicate::eq(DEVICE_SERVICE_URI.to_string()),
                predicate::eq(GET_DEVICE_INFORMATION_REQUEST_BODY.to_string()),
            )
            .return_once(|_uri, _body| Ok(expected_request_clone));

        let onvif_client = OnvifClient::new(mock_http_client);

        // Act
        let onvif_request = onvif_client
            .create_onvif_request(
                DEVICE_SERVICE_URI.to_string(),
                GET_DEVICE_INFORMATION_REQUEST_BODY.to_string(),
            )
            .unwrap();

        // Assert
        assert_eq!(
            onvif_request.body().unwrap().as_bytes(),
            expected_request.body().unwrap().as_bytes()
        );
    }

    /// Test a http_request is signed with the digest that's calculated from the digest client
    #[test]
    fn verify_sign_onvif_request_with_digest() {
        // Arrange - mock http_client
        let mut mock_http_client = MockHttpClient::default();
        mock_http_client
            .expect_add_header_to_http_request()
            // was not able to use .with() to compare the inputs, due to reqwest::Request not implementing the Eq trait,
            // hence, not comparable.
            // TODO: Investigate how to handle comparison of object when by default the struct doesn't implement Eq trait
            // .return_once(|_, _| Ok(expected_request_with_digest_for_closure));
            .return_once(|_, _| Ok(get_device_info_request_with_valid_digest_header()));
        let mut onvif_client = OnvifClient::new(mock_http_client);
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        // Arrange - mock digest client
        let mut mock_digest_client = MockDigestClientTrait::default();

        mock_digest_client
            .expect_respond()
            .return_once(|_| Ok(generate_valid_digest_auth_header()));

        // Act
        let request_with_digest = onvif_client
            .sign_onvif_request_with_digest(
                get_device_info_request_no_digest().borrow(),
                DigestAuth::new(
                    Box::new(mock_digest_client),
                    Credential {
                        username: "VideoAnalytics".to_string(),
                        password: "12345".to_string(),
                    },
                ),
            )
            .unwrap();

        // Assert
        assert_eq!(
            request_with_digest.headers(),
            get_device_info_request_with_valid_digest_header().headers()
        )
    }

    /// test onvif API GetDeviceInformation
    #[tokio::test]
    async fn verify_get_device_information_valid_digest_params_returns_device_info() {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_device_info_request_no_digest()));

        let mut http_response_ok: http_Response<String> = http_Response::default();
        *http_response_ok.status_mut() = StatusCode::OK;
        http_response_ok.body_mut().push_str(GET_DEVICE_INFORMATION_RESPONSE_BODY);
        http_response_ok
            .headers_mut()
            .insert(WWW_AUTHENTICATE, HeaderValue::from_static(DIGEST_CHALLENGE));
        let response_ok = Response::from(http_response_ok);

        mock_http_client.expect_send_http_request().return_once(|_| Ok(response_ok));

        mock_http_client
            .expect_add_header_to_http_request()
            .return_once(|_, _| Ok(get_device_info_request_with_valid_digest_header()));

        let mut onvif_client = OnvifClient::new(mock_http_client);
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        onvif_client.set_digest_params(&digest_in_http_response_header());
        onvif_client.credential = Some(generate_onvif_credential());

        // Act
        let device_info = onvif_client.get_device_information().await.unwrap();
        let device_info_struct: GetDeviceInformationResponse =
            serde_json::from_value(device_info).unwrap();

        // Assert
        assert_eq!(device_info_struct, get_device_information_response());
    }

    /// when process 2 has valid digest parameter, onvif server returns 200 and the onvif response in http body
    #[tokio::test]
    async fn verify_sign_onvif_request_with_digest_and_send_valid_digest_params_returns_onvif_response(
    ) {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_device_info_request_no_digest()));

        let mut http_response_ok: http_Response<String> = http_Response::default();
        *http_response_ok.status_mut() = StatusCode::OK;
        http_response_ok.body_mut().push_str(GET_DEVICE_INFORMATION_RESPONSE_BODY);
        http_response_ok
            .headers_mut()
            .insert(WWW_AUTHENTICATE, HeaderValue::from_static(DIGEST_CHALLENGE));
        let response_ok = Response::from(http_response_ok);

        mock_http_client.expect_send_http_request().return_once(|_| Ok(response_ok));

        mock_http_client
            .expect_add_header_to_http_request()
            .return_once(|_, _| Ok(get_device_info_request_with_valid_digest_header()));

        let mut onvif_client = OnvifClient::new(mock_http_client);
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        onvif_client.set_digest_params(&digest_in_http_response_header());
        onvif_client.credential = Some(generate_onvif_credential());

        // Act
        let device_info = onvif_client.get_device_information().await.unwrap();
        let device_info_struct: GetDeviceInformationResponse =
            serde_json::from_value(device_info).unwrap();

        // Assert
        assert_eq!(device_info_struct, get_device_information_response());
    }

    /// when the onvif request is sent without digest parameters (because the parameters doesn't exist yet on process 2),
    /// onvif server returns 401 along with digest information. Process 2 uses that info
    /// to craft a http request with digest parameters and make the request again
    #[tokio::test]
    async fn verify_sign_onvif_request_with_digest_and_send_no_digest_params_returns_onvif_response(
    ) {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_device_info_request_no_digest()));

        // mock the return of first attempt at sending onvif request (with no digest)
        mock_http_client
            .expect_send_http_request()
            .times(1)
            .returning(|_| Ok(generate_unauthorized_401_reqwest_response()));

        mock_http_client
            .expect_add_header_to_http_request()
            .return_once(|_, _| Ok(get_device_info_request_with_valid_digest_header()));

        // mock the return of second attempt at sending onvif request (with digest)
        // note, all expectations set on a given method are evaluated in FIFO order
        mock_http_client
            .expect_send_http_request()
            .times(1)
            .returning(|_| Ok(generate_200_reqwest_response()));

        let mut onvif_client = OnvifClient::new(mock_http_client);
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        onvif_client.credential = Some(generate_onvif_credential());

        // Act
        let device_info = onvif_client.get_device_information().await.unwrap();
        print!("{:?}", device_info);
        let device_info_struct: GetDeviceInformationResponse =
            serde_json::from_value(device_info).unwrap();

        // Assert
        assert_eq!(device_info_struct, get_device_information_response());
    }

    /// when the existing digest parameters on process 2 have expired, onvif server returns 401 along with digest information
    /// process 2 uses that info to update its digest parameters and make the call again
    #[tokio::test]
    async fn verify_sign_onvif_request_with_digest_and_send_expire_digest_params_return_onvif_response(
    ) {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_device_info_request_no_digest()));

        mock_http_client
            .expect_send_http_request()
            .times(1)
            .returning(|_| Ok(generate_unauthorized_401_reqwest_response()));

        mock_http_client.expect_send_http_request().times(1).returning(|_| {
            let mut http_response_ok: http_Response<String> = http_Response::default();
            *http_response_ok.status_mut() = StatusCode::OK;
            http_response_ok.body_mut().push_str(GET_DEVICE_INFORMATION_RESPONSE_BODY);
            http_response_ok.headers_mut().insert(
                reqwest::header::WWW_AUTHENTICATE,
                HeaderValue::from_static(DIGEST_CHALLENGE),
            );
            let response_ok = Response::from(http_response_ok);
            Ok(response_ok)
        });

        mock_http_client
            .expect_add_header_to_http_request()
            .times(1)
            .returning(|_, _| Ok(get_device_info_request_with_expired_digest_header()));

        mock_http_client
            .expect_add_header_to_http_request()
            .times(1)
            .returning(|_, _| Ok(get_device_info_request_with_valid_digest_header()));

        let mut onvif_client = OnvifClient::new(mock_http_client);
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        let mut http_response_with_digest_header: http_Response<String> = http_Response::default();
        http_response_with_digest_header
            .headers_mut()
            .insert(WWW_AUTHENTICATE, HeaderValue::from_static(EXPIRED_DIGEST_CHALLENGE));
        let response_with_digest_header = Response::from(http_response_with_digest_header);

        onvif_client.set_digest_params(response_with_digest_header.borrow());

        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        onvif_client.credential = Some(generate_onvif_credential());
        // Act
        let device_info = onvif_client.get_device_information().await.unwrap();
        let device_info_struct: GetDeviceInformationResponse =
            serde_json::from_value(device_info).unwrap();

        // Assert
        assert_eq!(device_info_struct, get_device_information_response());
    }

    /// when onvif server returns 500, nothing process 2 can do, process 2 should panic
    #[tokio::test]
    #[should_panic]
    async fn sign_onvif_request_with_digest_and_send_server_error_panic() {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_device_info_request_no_digest()));

        let mut http_response_server_error: http_Response<String> = http_Response::default();
        *http_response_server_error.status_mut() = StatusCode::INTERNAL_SERVER_ERROR;
        let response_error = Response::from(http_response_server_error);

        mock_http_client.expect_send_http_request().return_once(|_| Ok(response_error));

        mock_http_client
            .expect_add_header_to_http_request()
            .return_once(|_, _| Ok(get_device_info_request_with_valid_digest_header()));

        let mut onvif_client = OnvifClient::new(mock_http_client);

        onvif_client.set_digest_params(&digest_in_http_response_header());

        // Act
        let _device_info = onvif_client.get_device_information().await.unwrap();

        // Assert - expect to panic. using #[should_panic]
    }

    /// when onvif server returns http status other than 200, 401, or 500
    /// it is unexpected, process 2 should panic
    #[tokio::test]
    #[should_panic]
    async fn sign_onvif_request_with_digest_and_send_unexpected_http_status_panic() {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_device_info_request_no_digest()));

        let mut http_response_server_error: http_Response<String> = http_Response::default();
        *http_response_server_error.status_mut() = StatusCode::HTTP_VERSION_NOT_SUPPORTED;
        let response_ok = Response::from(http_response_server_error);

        mock_http_client.expect_send_http_request().return_once(|_| Ok(response_ok));

        mock_http_client
            .expect_add_header_to_http_request()
            .return_once(|_, _| Ok(get_device_info_request_with_valid_digest_header()));

        let mut onvif_client = OnvifClient::new(mock_http_client);

        onvif_client.set_digest_params(&digest_in_http_response_header());

        // Act
        let _device_info = onvif_client.get_device_information().await.unwrap();

        // Assert - expect to panic. using #[should_panic]
    }

    /// when given a 401 response, the digest params is set
    #[tokio::test]
    async fn verify_set_digest_param() {
        // Arrange
        let digest_params_header = r#"Digest algorithm=MD5, realm="Silvan_http_digest", qop="auth", nonce="62d82aa9ca59e3a04cd1", opaque="5b6ea228""#;

        let mut http_resp: http_Response<String> = http_Response::default();
        http_resp.headers_mut().insert(
            reqwest::header::WWW_AUTHENTICATE,
            HeaderValue::from_static(digest_params_header),
        );
        *http_resp.status_mut() = StatusCode::UNAUTHORIZED;
        let reqwest_unauthorized_resp = Response::from(http_resp);

        let mock_http_client = MockHttpClient::default();
        let mut onvif_client = OnvifClient::new(mock_http_client);

        // Act
        onvif_client.set_digest_params(reqwest_unauthorized_resp.borrow());

        // Assert
        assert_eq!(onvif_client.digest_params.unwrap(), digest_params_header);
    }

    /// test function inserts correct device services uri for each services
    #[tokio::test]
    async fn verify_get_services() {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_services_request_no_digest()));

        let mut http_get_services_response: http_Response<String> = http_Response::default();
        http_get_services_response.body_mut().push_str(get_services_response_body().as_str());
        let get_services_response = Response::from(http_get_services_response);

        mock_http_client.expect_send_http_request().return_once(|_| Ok(get_services_response));

        let mut onvif_client = OnvifClient::new(mock_http_client);

        // Act
        onvif_client.get_services(DEVICE_IP.to_string()).await.unwrap();

        // Assert
        let service_paths = onvif_client.service_paths;
        assert_eq!(service_paths.len(), 2);
        assert_eq!(
            service_paths.get(&OnvifServiceName::DeviceService).unwrap(),
            DEVICE_SERVICE_URI
        );
        assert_eq!(
            service_paths.get(&OnvifServiceName::MediaServiceVer10).unwrap(),
            MEDIA_SERVICE_URI
        );
    }

    /// when trying to insert a service_name that already existed on process 2, return error
    #[tokio::test]
    async fn get_services_duplicate_service_error() {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_services_request_no_digest()));

        let mut http_get_services_response_dup: http_Response<String> = http_Response::default();
        http_get_services_response_dup
            .body_mut()
            .push_str(get_services_response_body_duplicated_services().as_str());
        let get_services_response_dup = Response::from(http_get_services_response_dup);

        mock_http_client.expect_send_http_request().return_once(|_| Ok(get_services_response_dup));

        let mut onvif_client = OnvifClient::new(mock_http_client);

        // Act
        let err = onvif_client.get_services(DEVICE_IP.to_string()).await.unwrap_err();

        // Assert
        assert_eq!(err.to_string(), OnvifClientError::OnvifGetServicesError.to_string());
    }

    #[tokio::test]
    async fn verify_bootstrap() {
        let mut mock_http_client = MockHttpClient::default();
        // Arrange - set up for get_services
        mock_http_client
            .expect_create_http_request_with_body()
            .times(1)
            .returning(|_uri, _body| Ok(get_services_request_no_digest()));

        mock_http_client.expect_send_http_request().times(1).returning(|_| {
            let mut http_get_services_response: http_Response<String> = http_Response::default();
            http_get_services_response.body_mut().push_str(get_services_response_body().as_str());
            let get_services_response = Response::from(http_get_services_response);
            Ok(get_services_response)
        });

        let mut onvif_client = OnvifClient::new(mock_http_client);
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        onvif_client.set_digest_params(&digest_in_http_response_header());

        // Act
        device_traits::DeviceStateModel::bootstrap(
            &mut onvif_client,
            TEST_CONFIG_PATH,
            String::from("127.0.0.1"),
        )
        .await
        .unwrap();

        // Assert
        assert_eq!(
            onvif_client.credential.unwrap(),
            Credential {
                username: DIGEST_USERNAME.to_string(),
                password: DIGEST_PASSWORD.to_string(),
            }
        );
    }

    /// test onvif API GetAppsInfo
    #[tokio::test]
    async fn verify_get_apps_info_valid_digest_params_returns_apps_info() {
        // Arrange
        let mut mock_http_client = MockHttpClient::default();

        mock_http_client
            .expect_create_http_request_with_body()
            .return_once(|_uri, _body| Ok(get_apps_info_request_no_digest()));

        let mut http_response_ok: http_Response<String> = http_Response::default();
        *http_response_ok.status_mut() = StatusCode::OK;
        http_response_ok.body_mut().push_str(GET_APPS_INFO_RESPONSE_BODY);
        http_response_ok
            .headers_mut()
            .insert(WWW_AUTHENTICATE, HeaderValue::from_static(DIGEST_CHALLENGE));
        let response_ok = Response::from(http_response_ok);

        mock_http_client.expect_send_http_request().return_once(|_| Ok(response_ok));

        mock_http_client
            .expect_add_header_to_http_request()
            .return_once(|_, _| Ok(get_apps_info_request_with_valid_digest_header()));

        let mut onvif_client = OnvifClient::new(mock_http_client);
        onvif_client
            .service_paths
            .insert(OnvifServiceName::DeviceService, DEVICE_SERVICE_URI.to_string());

        onvif_client.set_digest_params(&digest_in_http_response_header());
        onvif_client.credential = Some(generate_onvif_credential());

        // Act
        let apps_info_struct = onvif_client.get_apps_info(None).await.unwrap();

        // Assert
        assert_eq!(apps_info_struct, get_apps_info_response());
    }

    const DIGEST_USERNAME: &str = VIDEO_ANALYTICS;

    const DIGEST_PASSWORD: &str = "videoanalytics12345";

    // TODO: add the ability to generate random cnonce if it makes the test more robust
    const CNONCE: &str = r#"62d82aa9ca59e3a04cd1"#;

    // EXPIRED_CNONCE and CNONCE are just random strings in the test. We do not check the expiration of these values in anyway.
    // Technically we don't need to create separate const for it. Separate const are created to make test more readable.
    const EXPIRED_CNONCE: &str = r#"382d2aB9ca59e3a54cd1"#;

    const DEVICE_IP: &str = r#"192.168.37.12"#;

    const DEVICE_SERVICE_NAMESPACE: &str = r#"http://www.onvif.org/ver10/device/wsdl"#;

    const MEDIA_SERVICE_NAMESPACE: &str = r#"http://www.onvif.org/ver10/media/wsdl"#;

    const DEVICE_SERVICE_URI: &str = r#"http://192.168.37.12/onvif/device_service"#;

    const MEDIA_SERVICE_URI: &str = r#"http://10.132.51.53/onvif/media_service"#;

    const DIGEST_CHALLENGE: &str = r#"Digest algorithm=MD5, realm="Silvan_http_digest", qop="auth", nonce="62d82aa9ca59e3a04cd1", opaque="5b6ea228""#;

    // EXPIRED_DIGEST_CHALLENGE uses a different nonce as DIGEST_CHALLENGE to simulate the expired digest
    const EXPIRED_DIGEST_CHALLENGE: &str = r#"Digest algorithm=MD5, realm="Silvan_http_digest", qop="auth", nonce="382d2aB9ca59e3a54cd1", opaque="5b6ea228""#;

    const CREDENTIAL: PasswordParams<'_> = http_auth::PasswordParams {
        username: DIGEST_USERNAME,
        password: DIGEST_PASSWORD,
        uri: DEVICE_SERVICE_URI,
        method: "POST",
        body: Some(&[]),
    };

    const TEST_CONFIG_PATH: &str = "tests/onvif_config.yaml";

    fn build_request_no_digest(service_uri: &str, request_body: &'static str) -> Request {
        Client::new().post(service_uri).body(request_body).build().unwrap()
    }

    fn build_request_with_valid_digest_header(
        service_uri: &str,
        request_body: &'static str,
    ) -> Request {
        Client::new()
            .post(service_uri)
            .body(request_body)
            .header(reqwest::header::CONTENT_TYPE, "application/xml")
            .header(reqwest::header::WWW_AUTHENTICATE, generate_valid_digest_auth_header())
            .build()
            .unwrap()
    }

    fn generate_valid_digest_auth_header() -> String {
        let pwd_client = PasswordClient::builder().challenges(DIGEST_CHALLENGE).build().unwrap();

        let mut digest_auth_pwd_client = match pwd_client {
            PasswordClient::Digest(d) => d,
            _ => {
                panic!("Failed to create digest client for unit test. ")
            }
        };

        digest_auth_pwd_client.respond_with_testing_cnonce(&CREDENTIAL, CNONCE).unwrap()
    }

    fn digest_in_http_response_header() -> Response {
        let mut http_response_with_digest_header: http_Response<String> = http_Response::default();
        http_response_with_digest_header
            .headers_mut()
            .insert(WWW_AUTHENTICATE, HeaderValue::from_static(DIGEST_CHALLENGE));

        Response::from(http_response_with_digest_header)
    }

    fn generate_expired_digest_auth_header() -> String {
        let pwd_client = PasswordClient::builder().challenges(DIGEST_CHALLENGE).build().unwrap();

        let mut digest_auth_pwd_client = match pwd_client {
            PasswordClient::Digest(d) => d,
            _ => {
                panic!("Failed to create digest client for unit test. ")
            }
        };

        digest_auth_pwd_client.respond_with_testing_cnonce(&CREDENTIAL, EXPIRED_CNONCE).unwrap()
    }

    fn get_device_info_request_no_digest() -> Request {
        build_request_no_digest(DEVICE_SERVICE_URI, GET_DEVICE_INFORMATION_REQUEST_BODY)
    }

    fn get_device_info_request_with_valid_digest_header() -> Request {
        build_request_with_valid_digest_header(
            DEVICE_SERVICE_URI,
            GET_DEVICE_INFORMATION_REQUEST_BODY,
        )
    }

    fn get_device_info_request_with_expired_digest_header() -> Request {
        Client::new()
            .post(DEVICE_SERVICE_URI)
            .body(GET_DEVICE_INFORMATION_REQUEST_BODY)
            .header(reqwest::header::CONTENT_TYPE, "application/xml")
            .header(reqwest::header::WWW_AUTHENTICATE, generate_expired_digest_auth_header())
            .build()
            .unwrap()
    }

    fn generate_unauthorized_401_reqwest_response() -> Response {
        let mut http_response_unauthorized: http_Response<String> = http_Response::default();
        *http_response_unauthorized.status_mut() = StatusCode::UNAUTHORIZED;
        http_response_unauthorized
            .headers_mut()
            .insert(WWW_AUTHENTICATE, HeaderValue::from_static(DIGEST_CHALLENGE));

        Response::from(http_response_unauthorized)
    }

    fn generate_200_reqwest_response() -> Response {
        let mut http_response_ok: http_Response<String> = http_Response::default();
        *http_response_ok.status_mut() = StatusCode::OK;
        http_response_ok.body_mut().push_str(GET_DEVICE_INFORMATION_RESPONSE_BODY);
        http_response_ok
            .headers_mut()
            .insert(reqwest::header::WWW_AUTHENTICATE, HeaderValue::from_static(DIGEST_CHALLENGE));

        Response::from(http_response_ok)
    }

    fn generate_onvif_credential() -> Credential {
        Credential { username: VIDEO_ANALYTICS.to_string(), password: "mockPassword".to_string() }
    }

    fn get_device_information_response() -> GetDeviceInformationResponse {
        GetDeviceInformationResponse {
            manufacturer: "CAPS".to_string(),
            model: "SKD-DM2A1-W".to_string(),
            firmware_version: "2.00 20220819".to_string(),
            serial_number: "00:26:e6:40:8f:0c".to_string(),
            hardware_id: "1.0".to_string(),
        }
    }

    const GET_DEVICE_INFORMATION_REQUEST_BODY: &str = r#"
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
                <s:Header>
                </s:Header>
                <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                    <GetDeviceInformation xmlns="http://www.onvif.org/ver10/device/wsdl">
                    </GetDeviceInformation>
                </s:Body>
            </s:Envelope>"#;

    const GET_DEVICE_INFORMATION_RESPONSE_BODY: &str = r#"<?xml version="1.0" encoding="UTF-8"?>
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://www.w3.org/2003/05/soap-envelope"
            xmlns:SOAP-ENC="http://www.w3.org/2003/05/soap-encoding"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:chan="http://schemas.microsoft.com/ws/2005/02/duplex"
            xmlns:wsa5="http://www.w3.org/2005/08/addressing"
            xmlns:c14n="http://www.w3.org/2001/10/xml-exc-c14n#"
            xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
            xmlns:xenc="http://www.w3.org/2001/04/xmlenc#"
            xmlns:wsc="http://schemas.xmlsoap.org/ws/2005/02/sc"
            xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
            xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
            xmlns:xmime="http://tempuri.org/xmime.xsd" xmlns:xop="http://www.w3.org/2004/08/xop/include"
            xmlns:tt="http://www.onvif.org/ver10/schema"
            xmlns:wsrfbf="http://docs.oasis-open.org/wsrf/bf-2"
            xmlns:wstop="http://docs.oasis-open.org/wsn/t-1"
            xmlns:wsrfr="http://docs.oasis-open.org/wsrf/r-2"
            xmlns:tan="http://www.onvif.org/ver20/analytics/wsdl"
            xmlns:tds="http://www.onvif.org/ver10/device/wsdl"
            xmlns:tev="http://www.onvif.org/ver10/events/wsdl"
            xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
            xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl"
            xmlns:tmd="http://www.onvif.org/ver10/deviceIO/wsdl"
            xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl"
            xmlns:trt="http://www.onvif.org/ver10/media/wsdl"
            xmlns:ter="http://www.onvif.org/ver10/error"
            xmlns:tns1="http://www.onvif.org/ver10/topics">
            <SOAP-ENV:Body>
                <tds:GetDeviceInformationResponse>
                    <tds:Manufacturer>CAPS</tds:Manufacturer>
                    <tds:Model>SKD-DM2A1-W</tds:Model>
                    <tds:FirmwareVersion>2.00 20220819</tds:FirmwareVersion>
                    <tds:SerialNumber>00:26:e6:40:8f:0c</tds:SerialNumber>
                    <tds:HardwareId>1.0</tds:HardwareId>
                </tds:GetDeviceInformationResponse>
             </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>"#;

    fn get_services_response_body() -> String {
        let device_service = Service {
            namespace: DEVICE_SERVICE_NAMESPACE.to_string(),
            x_addr: DEVICE_SERVICE_URI.to_string(),
            capabilities: None,
            version: OnvifVersion { major: 19, minor: 12 },
        };

        let media_service = Service {
            namespace: MEDIA_SERVICE_NAMESPACE.to_string(),
            x_addr: MEDIA_SERVICE_URI.to_string(),
            capabilities: None,
            version: OnvifVersion { major: 19, minor: 12 },
        };

        let get_service_resp: GetServicesResponse =
            GetServicesResponse { service: vec![device_service, media_service] };

        soap::serialize(&get_service_resp).unwrap()
    }

    fn get_services_response_body_duplicated_services() -> String {
        let device_service = Service {
            namespace: DEVICE_SERVICE_NAMESPACE.to_string(),
            x_addr: DEVICE_SERVICE_URI.to_string(),
            capabilities: None,
            version: OnvifVersion { major: 19, minor: 12 },
        };

        let media_service = Service {
            namespace: DEVICE_SERVICE_NAMESPACE.to_string(),
            x_addr: MEDIA_SERVICE_URI.to_string(),
            capabilities: None,
            version: OnvifVersion { major: 19, minor: 12 },
        };

        let get_service_resp: GetServicesResponse =
            GetServicesResponse { service: vec![device_service, media_service] };

        soap::serialize(&get_service_resp).unwrap()
    }

    fn get_services_request_no_digest() -> Request {
        build_request_no_digest(DEVICE_SERVICE_URI, GET_SERVICES_REQUEST_BODY)
    }

    const GET_SERVICES_REQUEST_BODY: &str = r#"
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
            <s:Header>
            </s:Header>
            <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                <GetServices xmlns="http://www.onvif.org/ver10/device/wsdl">
                    <IncludeCapability>false</IncludeCapability>
                </GetServices>
            </s:Body>
        </s:Envelope>"#;

    const GET_USERS_RESPONSE_BODY: &str = r#"<?xml version="1.0" encoding="UTF-8"?>
        <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://www.w3.org/2003/05/soap-envelope" xmlns:SOAP-ENC="http://www.w3.org/2003/05/soap-encoding" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:chan="http://schemas.microsoft.com/ws/2005/02/duplex" xmlns:wsa5="http://www.w3.org/2005/08/addressing" xmlns:c14n="http://www.w3.org/2001/10/xml-exc-c14n#" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" xmlns:saml1="urn:oasis:names:tc:SAML:1.0:assertion" xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" xmlns:xenc="http://www.w3.org/2001/04/xmlenc#" xmlns:wsc="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:xmime="http://tempuri.org/xmime.xsd" xmlns:xop="http://www.w3.org/2004/08/xop/include" xmlns:ns2="http://www.onvif.org/ver20/analytics/humanface" xmlns:ns3="http://www.onvif.org/ver20/analytics/humanbody" xmlns:wsrfbf="http://docs.oasis-open.org/wsrf/bf-2" xmlns:wstop="http://docs.oasis-open.org/wsn/t-1" xmlns:tt="http://www.onvif.org/ver10/schema" xmlns:wsrfr="http://docs.oasis-open.org/wsrf/r-2" xmlns:ns1="http://www.onvif.org/ver20/media/wsdl" xmlns:tan="http://www.onvif.org/ver20/analytics/wsdl" xmlns:tds="http://www.onvif.org/ver10/device/wsdl" xmlns:tev="http://www.onvif.org/ver10/events/wsdl" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2" xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl" xmlns:tmd="http://www.onvif.org/ver10/deviceIO/wsdl" xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl" xmlns:trt="http://www.onvif.org/ver10/media/wsdl" xmlns:ter="http://www.onvif.org/ver10/error" xmlns:tns1="http://www.onvif.org/ver10/topics">
            <SOAP-ENV:Header/>
            <SOAP-ENV:Body>
            <tds:GetUsersResponse>
                <tds:User>
                <tt:Username>VideoAnalytics</tt:Username>
                <tt:Password>12345</tt:Password>
                <tt:UserLevel>Administrator</tt:UserLevel>
                </tds:User>
            </tds:GetUsersResponse>
            </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>"#;

    const GET_USERS_REQUEST_BODY: &str = r#"
        <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
            <s:Header>
            </s:Header>
            <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                <GetUsers xmlns="http://www.onvif.org/ver10/device/wsdl"/>
            </s:Body>
        </s:Envelope>"#;

    fn get_users_request_no_digest() -> Request {
        build_request_no_digest(DEVICE_SERVICE_URI, GET_USERS_REQUEST_BODY)
    }

    fn get_users_request_with_valid_digest_header() -> Request {
        build_request_with_valid_digest_header(DEVICE_SERVICE_URI, GET_USERS_REQUEST_BODY)
    }

    const GET_APPS_INFO_REQUEST_BODY: &str = r#"
    <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:ns4="http://www.onvif.org/ver10/appmgmt/wsdl">
        <s:Header>
        </s:Header>
        <s:Body xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
            <ns4:GetAppsInfo />
        </s:Body>
    </s:Envelope>  
    "#;

    fn get_apps_info_request_no_digest() -> Request {
        build_request_no_digest(DEVICE_SERVICE_URI, GET_APPS_INFO_REQUEST_BODY)
    }

    fn get_apps_info_request_with_valid_digest_header() -> Request {
        build_request_with_valid_digest_header(DEVICE_SERVICE_URI, GET_APPS_INFO_REQUEST_BODY)
    }

    const GET_APPS_INFO_RESPONSE_BODY: &str = r#"
    <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://www.w3.org/2003/05/soap-envelope" xmlns:SOAP-ENC="http://www.w3.org/2003/05/soap-encoding" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:chan="http://schemas.microsoft.com/ws/2005/02/duplex" xmlns:wsa5="http://www.w3.org/2005/08/addressing" xmlns:c14n="http://www.w3.org/2001/10/xml-exc-c14n#" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" xmlns:saml1="urn:oasis:names:tc:SAML:1.0:assertion" xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" xmlns:xenc="http://www.w3.org/2001/04/xmlenc#" xmlns:wsc="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512" xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" xmlns:xmime="http://tempuri.org/xmime.xsd" xmlns:xop="http://www.w3.org/2004/08/xop/include" xmlns:ns2="http://www.onvif.org/ver20/analytics/humanface" xmlns:ns3="http://www.onvif.org/ver20/analytics/humanbody" xmlns:wsrfbf="http://docs.oasis-open.org/wsrf/bf-2" xmlns:wstop="http://docs.oasis-open.org/wsn/t-1" xmlns:tt="http://www.onvif.org/ver10/schema" xmlns:wsrfr="http://docs.oasis-open.org/wsrf/r-2" xmlns:ns1="http://www.onvif.org/ver20/media/wsdl" xmlns:tan="http://www.onvif.org/ver20/analytics/wsdl" xmlns:tds="http://www.onvif.org/ver10/device/wsdl" xmlns:tev="http://www.onvif.org/ver10/events/wsdl" xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2" xmlns:timg="http://www.onvif.org/ver20/imaging/wsdl" xmlns:tmd="http://www.onvif.org/ver10/deviceIO/wsdl" xmlns:tptz="http://www.onvif.org/ver20/ptz/wsdl" xmlns:trt="http://www.onvif.org/ver10/media/wsdl" xmlns:ns4="http://www.onvif.org/ver10/appmgmt/wsdl" xmlns:ter="http://www.onvif.org/ver10/error" xmlns:tns1="http://www.onvif.org/ver10/topics">
    <SOAP-ENV:Header/>
    <SOAP-ENV:Body>
        <ns4:GetAppsInfoResponse>
        <Info>
            <AppID>ID_1</AppID>
            <Name>Process1</Name>
            <Version>1.40</Version>
            <InstallationDate>1970-01-01T00:00:00Z</InstallationDate>
            <LastUpdate>1970-01-01T00:00:00Z</LastUpdate>
            <State>Active</State>
            <Status/>
            <Autostart>true</Autostart>
            <Website/>
        </Info>
        <Info>
            <AppID>ID_2</AppID>
            <Name>AiLibrary</Name>
            <Version>v03.00.11</Version>
            <InstallationDate>1970-01-01T00:00:00Z</InstallationDate>
            <LastUpdate>1970-01-01T00:00:00Z</LastUpdate>
            <State>Active</State>
            <Status/>
            <Autostart>true</Autostart>
            <Website/>
        </Info>
        <Info>
            <AppID>ID_3</AppID>
            <Name>AiModel</Name>
            <Version>m01.51451030</Version>
            <InstallationDate>1970-01-01T00:00:00Z</InstallationDate>
            <LastUpdate>1970-01-01T00:00:00Z</LastUpdate>
            <State>Active</State>
            <Status/>
            <Autostart>true</Autostart>
            <Website/>
        </Info>
        <Info>
        <AppID>ID_4</AppID>
        <Name>Process2</Name>
        <Version>0.1</Version>
        <InstallationDate>1970-01-01T00:00:00Z</InstallationDate>
        <LastUpdate>1970-01-01T00:00:00Z</LastUpdate>
        <State>Active</State>
        <Status/>
        <Autostart>true</Autostart>
        <Website/>
    </Info>
        </ns4:GetAppsInfoResponse>
    </SOAP-ENV:Body>
    </SOAP-ENV:Envelope>
    "#;

    fn get_apps_info_response() -> GetAppsInfoResponse {
        let process1_info = AppInfo {
            app_id: String::from("ID_1"),
            name: String::from("Process1"),
            version: String::from("1.40"),
            privileges: vec![],
            state: crate::wsdl_rs::appmgmt::AppState::Active,
            status: String::from(""),
            autostart: true,
            website: String::from(""),
            open_source: None,
            configuration: None,
            interface_description: vec![],
        };

        let ai_lib = AppInfo {
            app_id: String::from("ID_2"),
            name: String::from("AiLibrary"),
            version: String::from("v03.00.11"),
            privileges: vec![],
            state: crate::wsdl_rs::appmgmt::AppState::Active,
            status: String::from(""),
            autostart: true,
            website: String::from(""),
            open_source: None,
            configuration: None,
            interface_description: vec![],
        };

        let ai_model = AppInfo {
            app_id: String::from("ID_3"),
            name: String::from("AiModel"),
            version: String::from("m01.51451030"),
            privileges: vec![],
            state: crate::wsdl_rs::appmgmt::AppState::Active,
            status: String::from(""),
            autostart: true,
            website: String::from(""),
            open_source: None,
            configuration: None,
            interface_description: vec![],
        };

        let process2_info = AppInfo {
            app_id: String::from("ID_4"),
            name: String::from("Process2"),
            version: String::from("0.1"),
            privileges: vec![],
            state: crate::wsdl_rs::appmgmt::AppState::Active,
            status: String::from(""),
            autostart: true,
            website: String::from(""),
            open_source: None,
            configuration: None,
            interface_description: vec![],
        };

        GetAppsInfoResponse { info: vec![process1_info, ai_lib, ai_model, process2_info] }
    }
}
