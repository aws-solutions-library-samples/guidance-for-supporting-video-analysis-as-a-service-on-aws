// GRCOV_STOP_COVERAGE
// We don't have setup to mock signaling channel

use std::time::{Duration, SystemTime};

use super::error::KVSClientError;
use super::error::KVSClientError::ClientError;
use aws_credential_types::Credentials;
use aws_sdk_kinesisvideo::operation::get_signaling_channel_endpoint::GetSignalingChannelEndpointOutput;
use aws_sdk_kinesisvideo::{
    types::{ChannelProtocol, ChannelRole, SingleMasterChannelEndpointConfiguration},
    Client, Config,
};
use aws_sdk_kinesisvideosignaling::types::IceServer;
use aws_sdk_kinesisvideosignaling::Client as SignalingClient;
use aws_sigv4::http_request::{
    sign, SignableBody, SignableRequest, SignatureLocation, SigningParams, SigningSettings,
};
use aws_sigv4::sign::v4;
use aws_smithy_types::body::SdkBody;
use aws_smithy_types::retry::RetryConfig;
use aws_types::region::Region;
use device_traits::channel_utils::ServiceCommunicationManager;
use http::Uri;
use iot_client::client::IotCredentialProvider;
use tracing::error;

const NUMBER_OF_RETRIES_FOR_FATHOM: u32 = 3;

#[derive(Debug, Default, Clone)]
pub struct KinesisVideoStreamClient {
    pub presigned_url: String,
    pub ice_server_configs: Vec<IceServer>,
}

impl KinesisVideoStreamClient {
    pub async fn from_conf() -> Result<KinesisVideoStreamClient, KVSClientError> {
        let mut get_configs = ServiceCommunicationManager::default();
        let communication_config =
            get_configs.get_configurations().expect("Failed to retrieve configurations");
        let iot_credential_provider = IotCredentialProvider::from_config();

        let credentials_provider =
            Self::retrieve_creds_from_iot(iot_credential_provider.to_owned()).await?;

        let config = Config::builder()
            .region(Region::new(iot_credential_provider.region.to_owned()))
            .credentials_provider(credentials_provider.clone())
            .retry_config(RetryConfig::standard().with_max_attempts(NUMBER_OF_RETRIES_FOR_FATHOM))
            .build();

        let client = Client::from_conf(config.clone());

        let channel_arn = Self::get_signaling_channel_arn(
            client.to_owned(),
            communication_config.client_id.to_owned(),
        )
        .await?;

        let signaling_channel_endpoint =
            Self::get_signaling_channel_endpoint(client.to_owned(), channel_arn.clone()).await?;

        let presigned_url = Self::sign_master_signaling_channel(
            credentials_provider.clone(),
            signaling_channel_endpoint.clone(),
            iot_credential_provider.region.clone(),
            channel_arn.clone(),
        )
        .await?;

        let ice_server_configs = Self::get_ice_server_configs(
            iot_credential_provider.region.clone(),
            credentials_provider.clone(),
            signaling_channel_endpoint,
            channel_arn.clone(),
        )
        .await?;

        return Ok(KinesisVideoStreamClient { presigned_url, ice_server_configs });
    }

    async fn retrieve_creds_from_iot(
        iot_credential_provider: IotCredentialProvider,
    ) -> Result<Credentials, KVSClientError> {
        match iot_credential_provider.retrieve_creds_from_iot().await {
            Ok(credentials_provider) => return Ok(credentials_provider),
            Err(e) => {
                error!("Unable to fetch credentials {:?}", e);
                return Err(ClientError("Unable to fetch credentials".to_string()));
            }
        };
    }

    fn get_signaling_channel_name(thing_name: String) -> String {
        format!("{}-LiveStreamSignalingChannel", thing_name)
    }

    async fn get_signaling_channel_arn(
        client: Client,
        thing_name: String,
    ) -> Result<String, KVSClientError> {
        let channel_name = Self::get_signaling_channel_name(thing_name);

        let resp = match client
            .describe_signaling_channel()
            .set_channel_name(Option::from(channel_name))
            .send()
            .await
        {
            Ok(res) => res,
            Err(err) => {
                error!("Cannot call DescribeSignalingChannel {:?}", err);
                return Err(ClientError("Cannot call DescribeSignalingChannel".to_string()));
            }
        };

        let Some(cinfo) = resp.channel_info() else {
            error!("No description found");
            return Err(ClientError("No description found".to_string()));
        };

        let Some(channel_arn) = cinfo.channel_arn() else {
            error!("No channel ARN found");
            return Err(ClientError("No channel ARN found".to_string()));
        };

        return Ok(channel_arn.to_string());
    }

    async fn get_signaling_channel_endpoint(
        client: Client,
        channel_arn: String,
    ) -> Result<GetSignalingChannelEndpointOutput, KVSClientError> {
        let config = SingleMasterChannelEndpointConfiguration::builder()
            .set_protocols(Some(vec![ChannelProtocol::Wss, ChannelProtocol::Https]))
            .set_role(Some(ChannelRole::Master))
            .build();

        client
            .get_signaling_channel_endpoint()
            .set_channel_arn(Some(channel_arn))
            .set_single_master_channel_endpoint_configuration(Some(config))
            .send()
            .await
            .map_err(|e| ClientError(e.to_string()))
    }

    async fn sign_master_signaling_channel(
        credentials_provider: Credentials,
        signaling_channel_endpoint: GetSignalingChannelEndpointOutput,
        region: String,
        channel_arn: String,
    ) -> Result<String, KVSClientError> {
        let endpoint_wss_uri =
            match signaling_channel_endpoint.resource_endpoint_list().iter().find_map(|endpoint| {
                if endpoint.protocol == Some(ChannelProtocol::Wss) {
                    Some(endpoint.resource_endpoint().unwrap().to_owned())
                } else {
                    None
                }
            }) {
                Some(endpoint_uri_str) => Uri::from_maybe_shared(endpoint_uri_str).unwrap(),
                None => {
                    error!("No WSS endpoint found");
                    return Err(ClientError("No WSS endpoint found".to_string()));
                }
            };

        let mut signing_settings = SigningSettings::default();
        signing_settings.signature_location = SignatureLocation::QueryParams;
        signing_settings.expires_in = Some(Duration::from_secs(5 * 60));
        let identity = credentials_provider.clone().into();
        let signing_params: SigningParams = v4::SigningParams::builder()
            .identity(&identity)
            .region(&region)
            .name("kinesisvideo")
            .time(SystemTime::now())
            .settings(signing_settings)
            .build()
            .unwrap()
            .into();

        let transcribe_uri = Uri::builder()
            .scheme("wss")
            .authority(endpoint_wss_uri.authority().unwrap().to_owned())
            .path_and_query(format!(
                "/?X-Amz-ChannelARN={}",
                aws_smithy_http::query::fmt_string(channel_arn)
            ))
            .build()
            .map_err(|err| {})
            .unwrap();

        let signable_request = SignableRequest::new(
            "GET",
            transcribe_uri.to_string(),
            std::iter::empty(),
            SignableBody::Bytes(&[]),
        )
        .unwrap();

        let mut request =
            http::Request::builder().uri(transcribe_uri).body(SdkBody::empty()).unwrap();

        let (signing_instructions, _signature) =
            sign(signable_request, &signing_params).unwrap().into_parts();
        signing_instructions.apply_to_request_http1x(&mut request);

        return Ok(request.uri().to_string());
    }

    async fn get_ice_server_configs(
        region: String,
        credentials_provider: Credentials,
        signaling_channel_endpoint: GetSignalingChannelEndpointOutput,
        channel_arn: String,
    ) -> Result<Vec<IceServer>, KVSClientError> {
        let endpoint_https_uri =
            match signaling_channel_endpoint.resource_endpoint_list().iter().find_map(|endpoint| {
                if endpoint.protocol == Some(ChannelProtocol::Https) {
                    Some(endpoint.resource_endpoint().unwrap().to_owned())
                } else {
                    None
                }
            }) {
                Some(endpoint_uri_str) => endpoint_uri_str,
                None => {
                    error!("No HTTPS endpoint found");
                    return Err(ClientError("No HTTPS endpoint found".to_string()));
                }
            };

        let signaling_config = aws_sdk_kinesisvideosignaling::Config::builder()
            .region(Region::new(region))
            .credentials_provider(credentials_provider.clone())
            .retry_config(RetryConfig::standard().with_max_attempts(NUMBER_OF_RETRIES_FOR_FATHOM))
            .endpoint_url(endpoint_https_uri)
            .build();
        let signaling_client = SignalingClient::from_conf(signaling_config);

        let resp = match signaling_client
            .get_ice_server_config()
            .set_channel_arn(Some(channel_arn))
            .send()
            .await
        {
            Ok(res) => res,
            Err(e) => {
                error!("Unable to fetch ice_server_configs {:?}", e);
                return Err(ClientError("Unable to fetch ice_server_configs".to_string()));
            }
        };

        let ice_server_list = match resp.ice_server_list {
            None => return Err(ClientError("Unable to fetch ice server configs".to_string())),
            Some(ice_server) => ice_server,
        };

        return Ok(ice_server_list);
    }
}
