// GRCOV_STOP_COVERAGE
use serde::{Deserialize, Serialize};
use webrtc::track::track_local::track_local_static_sample::TrackLocalStaticSample;

use std::fmt::Debug;
use std::str;
use std::sync::Arc;

use webrtc::peer_connection::RTCPeerConnection;

use anyhow::Result;
use async_trait::async_trait;
use aws_sdk_kinesisvideosignaling::types::IceServer;
use mockall::automock;
use webrtc::api::interceptor_registry::register_default_interceptors;
use webrtc::api::media_engine::MediaEngine;
use webrtc::api::APIBuilder;
use webrtc::ice_transport::ice_server::RTCIceServer;
use webrtc::interceptor::registry::Registry;
use webrtc::peer_connection::configuration::RTCConfiguration;
use webrtc::track::track_local::TrackLocal;
use webrtc::Error;

use crate::constants::{KVS_STUN_TURN_SERVER_PREFIX, KVS_STUN_TURN_SERVER_SUFFIX};
use webrtc::ice_transport::ice_credential_type::RTCIceCredentialType;

// GRCOV_STOP_COVERAGE
// We don't have setup to mock RTSP server or signaling channel

/// Set of access credentials and associated URLs of TURN servers
#[derive(Default, Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct TurnServerInfo {
    #[serde(rename = "Password")]
    /// Server password
    pub password: String,
    /// Server username
    #[serde(rename = "Username")]
    pub username: String,
    //#[serde(rename = "TTL")]
    /// Cred TTL
    // pub ttl: i64,
    #[serde(rename = "Uris")]
    /// Server URL list
    pub uris: Vec<String>,
}

/// The top-level WebRTC client built on top of the webrtc-rs crate
#[derive(Debug, Clone)]
pub struct WebrtcSession {
    ice_server_configs: Vec<IceServer>,
    video_track: Arc<TrackLocalStaticSample>,
    pub peer_connection: Option<Arc<RTCPeerConnection>>,
}

#[automock]
#[async_trait]
pub trait PeerConnection {
    async fn create_peer_connection_and_stream(&mut self, region: String) -> Result<(), Error>;
}

#[async_trait]
impl PeerConnection for WebrtcSession {
    async fn create_peer_connection_and_stream(&mut self, region: String) -> Result<(), Error> {
        let stun_server = RTCIceServer {
            urls: vec![format!(
                "{}{}{}",
                KVS_STUN_TURN_SERVER_PREFIX, region, KVS_STUN_TURN_SERVER_SUFFIX
            )
            .to_owned()],
            ..Default::default()
        };

        let turn_servers: Vec<RTCIceServer> = self
            .ice_server_configs
            .clone()
            .iter()
            .map(|server| RTCIceServer {
                urls: server.uris.clone().expect("IceServer config missing uris"),
                username: server.username.clone().expect("IceServer config missing username"),
                credential: server.password.clone().expect("IceServer config missing credential"),
                credential_type: RTCIceCredentialType::Password,
            })
            .collect();

        let mut ice_servers = turn_servers.clone();
        ice_servers.push(stun_server);

        let config = RTCConfiguration { ice_servers: ice_servers, ..Default::default() };

        // Create a MediaEngine object to configure the supported codec
        let mut media = MediaEngine::default();
        let _ = media.register_default_codecs();

        let mut registry = Registry::new();

        // Use the default set of Interceptors
        registry = register_default_interceptors(registry, &mut media)?;

        // Create the API object with the MediaEngine
        let api =
            APIBuilder::new().with_media_engine(media).with_interceptor_registry(registry).build();

        // Create a new RTCPeerConnection
        let peer_connection = Arc::new(api.new_peer_connection(config).await?);

        let rtp_sender = peer_connection
            .add_track(Arc::clone(&self.video_track) as Arc<dyn TrackLocal + Send + Sync>)
            .await?;

        // Read incoming RTCP packets.
        // Before these packets are returned they are processed by interceptors.
        // For things like NACK this needs to be called.
        tokio::spawn(async move {
            let mut rtcp_buf = vec![0u8; 1500];
            while let Ok((_, _)) = rtp_sender.read(&mut rtcp_buf).await {}
            Result::<()>::Ok(())
        });
        self.peer_connection = Some(peer_connection);
        Ok(())
    }
}

impl WebrtcSession {
    /// Create a new WebRTC Client for a peer-to-peer connection. This client will automatically open
    /// a WebSocket connection with KVS and begin signaling.
    pub fn new(
        ice_server_configs: Vec<IceServer>,
        video_track: Arc<TrackLocalStaticSample>,
    ) -> Self {
        WebrtcSession { ice_server_configs, video_track, peer_connection: None }
    }
}

#[cfg(test)]
mod tests {
    use crate::webrtc_client::WebrtcSession;
    use std::sync::Arc;
    use webrtc::api::media_engine::MIME_TYPE_H264;
    use webrtc::rtp_transceiver::rtp_codec::RTCRtpCodecCapability;
    use webrtc::track::track_local::track_local_static_sample::TrackLocalStaticSample;
    #[tokio::test]
    async fn verify_new_webrtc_client_creation() {
        let video_track = Arc::new(TrackLocalStaticSample::new(
            RTCRtpCodecCapability { mime_type: MIME_TYPE_H264.to_owned(), ..Default::default() },
            "video".to_string(),
            "video".to_string(),
        ));
        let webrtc_session = WebrtcSession::new(Vec::new(), video_track);
        assert_eq!(webrtc_session.ice_server_configs.len(), 0);
        assert!(webrtc_session.peer_connection.is_none());
    }
}
