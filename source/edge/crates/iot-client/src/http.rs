use async_trait::async_trait;
use mockall::automock;
use reqwest::{header::HeaderMap, Client, Error, Identity};

pub struct Request {
    client: Client,
}

#[automock]
#[async_trait]
pub trait RequestHttp: Send + Sync {
    async fn get_text(&self, url: String) -> Result<String, Error>;
}

impl Request {
    pub fn create(cert: &mut [u8], headers: HeaderMap) -> Result<Self, Error> {
        let id = Identity::from_pem(cert)?;
        Ok(Request {
            client: Client::builder()
                .identity(id)
                .default_headers(headers)
                .use_rustls_tls()
                .build()?,
        })
    }
}

#[async_trait]
impl RequestHttp for Request {
    async fn get_text(&self, url: String) -> Result<String, Error> {
        self.client.get(url).send().await?.text().await
    }
}
