// GRCOV_STOP_COVERAGE
use super::error::ClientError;
use async_trait::async_trait;
use base64::{engine::general_purpose, Engine as _};
use http::HeaderValue;
use mockall::automock;
use reqwest::{Body, Request, Response};
use sha256::digest;
use std::error::Error;
use tracing::{error, warn};

/// This trait captures http activities
#[automock]
#[async_trait]
pub trait HttpClient {
    /// create http request given service uri and http body
    fn create_http_request_with_body(
        &self,
        service_uri: String,
        request_body: String,
    ) -> Result<Request, ClientError>;

    /// add header to a given http request
    fn add_header_to_http_request(
        &self,
        header: reqwest::header::HeaderValue,
        http_request: reqwest::Request,
    ) -> Result<Request, ClientError>;

    /// send the http request
    async fn send_http_request(&self, http_request: Request) -> Result<Response, ClientError>;

    async fn send_http_get_request_with_basic_auth(
        &self,
        service_uri: String,
        username: String,
        password: String,
    ) -> Result<Response, Box<dyn Error>>;

    async fn send_http_request_put_for_presigned_url(
        &self,
        service_uri: String,
        data: Vec<u8>,
    ) -> Result<Response, Box<dyn Error>>;

    /// send firmware file to process 1 by uploading the file through http POST request
    async fn send_software_update_file(
        &self,
        upload_uri: String,
        firmware_body: Body,
    ) -> Result<(), ClientError>;
}

/// a struct to wrap reqwest. Wrapping reqwest client allows implementing the defined trait - HttpClient
/// on the wrapper - HttpClientImpl, instead of implementing it on the actual reqwest client.
#[derive(Debug)]
pub struct HttpClientImpl {
    inner: reqwest::Client,
}

impl HttpClientImpl {
    /// Initialization of the reqwest wrapper
    pub fn new(inner: reqwest::Client) -> Self {
        Self { inner }
    }
}

/// return reqwest response and let the caller convert to text
#[async_trait]
impl HttpClient for HttpClientImpl {
    fn create_http_request_with_body(
        &self,
        service_uri: String,
        request_body: String,
    ) -> Result<Request, ClientError> {
        Ok(self.inner.post(service_uri).body(request_body).build()?)
    }

    fn add_header_to_http_request(
        &self,
        header: reqwest::header::HeaderValue,
        mut http_request: reqwest::Request,
    ) -> Result<Request, ClientError> {
        http_request
            .headers_mut()
            .insert(reqwest::header::CONTENT_TYPE, HeaderValue::from_static("application/xml"));
        http_request.headers_mut().insert(reqwest::header::AUTHORIZATION, header);

        Ok(http_request)
    }

    async fn send_http_request(
        &self,
        http_request: reqwest::Request,
    ) -> Result<Response, ClientError> {
        Ok(self.inner.execute(http_request).await?)
    }

    async fn send_http_get_request_with_basic_auth(
        &self,
        service_uri: String,
        username: String,
        password: String,
    ) -> Result<Response, Box<dyn Error>> {
        match self.inner.get(service_uri.to_owned()).basic_auth(username, Some(password)).build() {
            Ok(req) => return Ok(self.inner.execute(req).await?),
            Err(e) => {
                warn!(
                    "Failed to build http GET request for service_uri {:?}",
                    service_uri.to_owned()
                );
                return Err(Box::new(e));
            }
        };
    }

    async fn send_http_request_put_for_presigned_url(
        &self,
        service_uri: String,
        data: Vec<u8>,
    ) -> Result<Response, Box<dyn Error>> {
        let sha_encoded = digest(data.clone());
        let sha_encoded_as_bytes = hex::decode(sha_encoded).unwrap();
        let checksum = general_purpose::STANDARD.encode(sha_encoded_as_bytes);
        let req = self
            .inner
            .put(service_uri)
            .header(reqwest::header::CONTENT_TYPE, HeaderValue::from_static("image/jpeg"))
            .header("x-amz-checksum-sha256", checksum)
            .body(data)
            .build()
            .expect("Unable to build http put request");
        let res = self.inner.execute(req).await;
        Ok(res.expect("Failed to send http put request"))
    }

    async fn send_software_update_file(
        &self,
        upload_uri: String,
        firmware_body: Body,
    ) -> Result<(), ClientError> {
        match self
            .inner
            .post(upload_uri)
            .header(
                reqwest::header::CONTENT_TYPE,
                HeaderValue::from_static("application/x-www-form-urlencoded"),
            )
            .body(firmware_body)
            .build()
        {
            Ok(req) => {
                self.send_http_request(req).await?;
                Ok(())
            }
            Err(e) => {
                error!("Failed to build the http request for uploading firmware file. {:?}", e);
                Ok(())
            }
        }
    }
}
