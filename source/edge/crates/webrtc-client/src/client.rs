use bytes::Bytes;
// GRCOV_STOP_COVERAGE
use serde::{Deserialize, Serialize};
use serde_json::json;
use tokio::sync::mpsc::Sender;
use tracing::info;
use webrtc::media::Sample;
use webrtc::peer_connection::peer_connection_state::RTCPeerConnectionState;
use webrtc::track::track_local::track_local_static_sample::TrackLocalStaticSample;

use std::fmt::Debug;
use std::str;
use std::sync::Arc;

use tokio::time::Duration;
use webrtc::peer_connection::RTCPeerConnection;

use anyhow::{anyhow, Result};
use async_trait::async_trait;
use futures_util::StreamExt;
use mockall::automock;
use retina::client::SetupOptions;
use retina::codec::{CodecItem, ParametersRef};
use tokio::sync::Mutex;
use webrtc::api::interceptor_registry::register_default_interceptors;
use webrtc::api::media_engine::{MediaEngine, MIME_TYPE_H264};
use webrtc::api::APIBuilder;
use webrtc::ice_transport::ice_server::RTCIceServer;
use webrtc::interceptor::registry::Registry;
use webrtc::peer_connection::configuration::RTCConfiguration;
use webrtc::rtp_transceiver::rtp_codec::RTCRtpCodecCapability;
use webrtc::track::track_local::TrackLocal;
use webrtc::Error;

use crate::constants::{
    CLIENT_ID, KVS_STUN_TURN_SERVER_PREFIX, KVS_STUN_TURN_SERVER_SUFFIX, STATE,
};
use tokio::time::sleep;
use url::Url;

use crate::signaling::WebrtcSignalingClient;
use base64::{engine::general_purpose, Engine as _};

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
/// https://github.com/webrtc-rs/webrtc
#[derive(Debug)]
pub struct WebrtcClient {
    /// The WebRTC PeerConnection
    pub peer_connection: Option<Arc<RTCPeerConnection>>,
    /// The KVS clientId of the viewer. Same as sessionId at the moment
    pub client_id: String,
    pub signaling_client: Option<WebrtcSignalingClient>,
    pub connect_as_master_url: String,
}

#[automock]
#[async_trait]
pub trait PeerConnection {
    async fn create_signaling_client(
        &self,
        connect_as_master_url: String,
        peer_connection: Arc<RTCPeerConnection>,
        client_id: String,
    ) -> Result<WebrtcSignalingClient>;
    async fn create_peer_connection(
        &self,
        client_id: String,
        streaming_signal_tx: Sender<String>,
        region: String,
    ) -> Result<Arc<RTCPeerConnection>, Error>;
    async fn connect_peer(
        &mut self,
        streaming_signal_tx: Sender<String>,
        region: String,
    ) -> Result<()>;
}

#[async_trait]
impl PeerConnection for WebrtcClient {
    async fn create_signaling_client(
        &self,
        connect_as_master_url: String,
        peer_connection: Arc<RTCPeerConnection>,
        client_id: String,
    ) -> Result<WebrtcSignalingClient> {
        let pending_candidates = Arc::new(Mutex::new(vec![]));
        info!("signaling client {:?}", connect_as_master_url);
        WebrtcSignalingClient::new(
            connect_as_master_url,
            peer_connection,
            pending_candidates.clone(),
            client_id.clone(),
        )
        .await
    }

    /// Creates and returns an Arc pointer to an RTCPeerConnection
    async fn create_peer_connection(
        &self,
        client_id: String,
        streaming_signal_tx: Sender<String>,
        region: String,
    ) -> Result<Arc<RTCPeerConnection>, Error> {
        let config = RTCConfiguration {
            ice_servers: vec![RTCIceServer {
                urls: vec![format!(
                    "{}{}{}",
                    KVS_STUN_TURN_SERVER_PREFIX, region, KVS_STUN_TURN_SERVER_SUFFIX
                )
                .to_owned()],
                ..Default::default()
            }],
            ..Default::default()
        };

        // Create a MediaEngine object to configure the supported codec
        let mut media = MediaEngine::default();
        media.register_default_codecs()?;

        let mut registry = Registry::new();

        // Use the default set of Interceptors
        registry = register_default_interceptors(registry, &mut media)?;

        // Create the API object with the MediaEngine
        let api =
            APIBuilder::new().with_media_engine(media).with_interceptor_registry(registry).build();

        // Create a new RTCPeerConnection
        let peer_connection = Arc::new(api.new_peer_connection(config).await?);

        // Callback exposed from webrtc-rs crate for peerconnection state changes.
        peer_connection.on_peer_connection_state_change(Box::new(
            move |peer_connection_state: RTCPeerConnectionState| {
                info!("Peer Connection State has changed: {peer_connection_state}");

                let state = match peer_connection_state {
                    RTCPeerConnectionState::New => None,
                    RTCPeerConnectionState::Connecting => None,
                    RTCPeerConnectionState::Connected => Some("Connected"),
                    RTCPeerConnectionState::Disconnected => Some("Disconnected"),
                    RTCPeerConnectionState::Closed => Some("Disconnected"),
                    RTCPeerConnectionState::Unspecified => None,
                    RTCPeerConnectionState::Failed => Some("Failed"),
                };

                if state.is_some() {
                    let shadow_tx = streaming_signal_tx.clone();
                    let client_id_clone = client_id.clone();
                    tokio::spawn(async move {
                        info!("send updated state {:?}", state);
                        let _res = shadow_tx
                            .send(
                                json!({
                                    STATE: format!("{}", state.expect("Unreachable state")),
                                    CLIENT_ID: format!("{}", client_id_clone),
                                })
                                .to_string(),
                            )
                            .await;
                    });
                }
                Box::pin(async {})
            },
        ));
        Ok(peer_connection)
    }

    async fn connect_peer(
        &mut self,
        streaming_signal_tx: Sender<String>,
        region: String,
    ) -> Result<()> {
        let client_id_clone = self.client_id.clone();
        let connect_as_master_url = self.connect_as_master_url.clone();
        let peer_connection = self
            .create_peer_connection(client_id_clone.clone(), streaming_signal_tx, region)
            .await?;
        let signaling_client = self
            .create_signaling_client(
                connect_as_master_url,
                peer_connection.clone(),
                client_id_clone,
            )
            .await?;
        self.peer_connection = Some(peer_connection);
        self.signaling_client = Some(signaling_client);
        Ok(())
    }
}

#[automock]
#[async_trait]
pub trait PeerStreaming {
    // Parse AVCC extradata
    fn parse_avcc_extra_data(extra_data: &[u8]) -> (usize, usize, usize, usize);
    async fn stream_from_rtsp(
        &self,
        rtsp_url: String,
        username: String,
        password: String,
    ) -> Result<()>;
}

#[async_trait]
impl PeerStreaming for WebrtcClient {
    // Parse AVCC extradata
    /*
     The codec private data follows the following format:
        bits
        8   version ( always 0x01 )
        8   avc profile ( sps[0][1] )
        8   avc compatibility ( sps[0][2] )
        8   avc level ( sps[0][3] )
        6   reserved ( all bits on )
        2   NALULengthSizeMinusOne
        3   reserved ( all bits on )
        5   number of SPS NALUs (usually 1)

        repeated once per SPS:
        16         SPS NALU data size
        variable   SPS NALU data

        8   number of PPS NALUs (usually 1)

        repeated once per PPS:
        16       PPS NALU data size
        variable PPS NALU data
    */
    fn parse_avcc_extra_data(extra_data: &[u8]) -> (usize, usize, usize, usize) {
        // number of bytes for avcc extradata header (fixed value)
        let extra_data_header_size: u8 = 6;
        let bits_to_shift: u8 = 8;
        let reserved_size: u16 = 2;

        // Assuming 1 SPS and PPS
        let sps_size: u16 = ((extra_data[usize::from(extra_data_header_size)] as u16)
            << bits_to_shift)
            | (extra_data[usize::from(extra_data_header_size) + 1] as u16);

        let pps_offset_for_size: u16 =
            (extra_data_header_size as u16) + reserved_size + sps_size + 1;

        let pps_size: u16 = ((extra_data[usize::from(pps_offset_for_size)] as u16)
            << bits_to_shift)
            + (extra_data[usize::from(pps_offset_for_size) + 1] as u16);

        (
            usize::from(bits_to_shift),
            usize::from(sps_size),
            usize::from(pps_offset_for_size) + usize::from(reserved_size),
            usize::from(pps_size),
        )
    }

    /// Streams video from rtsp stream
    async fn stream_from_rtsp(
        &self,
        rtsp_url: String,
        username: String,
        password: String,
    ) -> Result<()> {
        let url = Url::parse(&*rtsp_url).expect("RTSP url is not valid");
        // convert the password into MD5 and encoded in base64
        let pw_md5_hash = md5::compute(password.as_str());
        let pw_md5_base64 = general_purpose::STANDARD.encode(*pw_md5_hash);

        let mut tracks = Vec::new();
        let upstream_session_group = Arc::new(retina::client::SessionGroup::default());
        let creds = retina::client::Credentials { username, password: pw_md5_base64 };
        let mut upstream_session = retina::client::Session::describe(
            url,
            retina::client::SessionOptions::default()
                .creds(Some(creds))
                .session_group(upstream_session_group.clone())
                .user_agent("VideoAnalytics".to_owned())
                .teardown(retina::client::TeardownPolicy::Auto),
        )
        .await?;

        for (i, stream) in upstream_session.streams().iter().enumerate() {
            if stream.media() != "video" && stream.encoding_name() != "h264" {
                // Currently we only support H.264 video.
                continue;
            }

            let client_id = self.client_id.to_owned();
            let video_track = Arc::new(TrackLocalStaticSample::new(
                RTCRtpCodecCapability {
                    mime_type: MIME_TYPE_H264.to_owned(),
                    ..Default::default()
                },
                format!("{i}-{client_id}-video"),
                client_id,
            ));

            // Add this newly created track to the PeerConnection
            let rtp_sender = self
                .peer_connection
                .as_ref()
                .expect("No peer connection exists")
                .add_track(Arc::clone(&video_track) as Arc<dyn TrackLocal + Send + Sync>)
                .await?;

            // Read incoming RTCP packets.
            // Before these packets are returned they are processed by interceptors.
            // For things like NACK this needs to be called.
            tokio::spawn(async move {
                let mut rtcp_buf = vec![0u8; 1500];
                while let Ok((_, _)) = rtp_sender.read(&mut rtcp_buf).await {}
                Result::<()>::Ok(())
            });

            if tracks.len() <= i {
                tracks.resize(i + 1, None);
            }

            tracks[i] = Some(video_track);
        }

        for i in tracks.iter().enumerate().filter_map(|(i, track)| track.as_ref().map(|_| i)) {
            upstream_session
                .setup(
                    i,
                    SetupOptions::default()
                        .transport(retina::client::Transport::Tcp(Default::default())),
                )
                .await?;
        }
        let mut upstream_session = upstream_session
            .play(retina::client::PlayOptions::default().ignore_zero_seq(true))
            .await?
            .demuxed()?;

        let peer_connection_clone =
            self.peer_connection.clone().expect("No peer connection exists");

        tokio::spawn(async move {
            let mut state: RTCPeerConnectionState;
            loop {
                sleep(Duration::from_millis(100)).await;
                info!("connection_state {:?}", peer_connection_clone.connection_state());
                state = peer_connection_clone.connection_state();
                if state != RTCPeerConnectionState::New
                    && state != RTCPeerConnectionState::Connecting
                    && state != RTCPeerConnectionState::Unspecified
                {
                    break;
                }
            }

            loop {
                state = peer_connection_clone.connection_state();
                if state != RTCPeerConnectionState::Connected {
                    break;
                }
                let item = upstream_session.next().await;
                match item {
                    Some(Ok(CodecItem::VideoFrame(frame))) => {
                        if let Some(t) = tracks.get(frame.stream_id()).and_then(Option::as_ref) {
                            let mut nalus: Vec<Vec<u8>> = Vec::with_capacity(3);

                            // Is key frame
                            if frame.is_random_access_point() {
                                let params = upstream_session.streams()[0].parameters();
                                match params {
                                    Some(ParametersRef::Video(params)) => {
                                        let extra_data: &[u8] = params.extra_data();
                                        let (sps_from, sps_size, pps_from, pps_size) =
                                            Self::parse_avcc_extra_data(extra_data);
                                        // SPS data
                                        nalus.push(
                                            extra_data[sps_from..(sps_from + sps_size)].to_vec(),
                                        );
                                        // PPS data
                                        nalus.push(
                                            extra_data[pps_from..(pps_from + pps_size)].to_vec(),
                                        );
                                        info!("Key frame found");
                                    }
                                    None => {
                                        info!("Discarding video frame received before parameters");
                                        return Ok(());
                                    }
                                    _ => unreachable!(),
                                }
                            }
                            // This also may be 3 bytes ... more info can be retrieved through avcc encoding format
                            let reserved_size_for_length_info = 4;
                            let mut frame_data = Vec::with_capacity(
                                frame.data().len() - reserved_size_for_length_info,
                            );
                            frame_data.extend_from_slice(
                                &frame.data()[reserved_size_for_length_info..frame.data().len()],
                            );
                            nalus.push(frame_data);

                            for nalu in nalus {
                                t.write_sample(&Sample {
                                    data: Bytes::from(nalu),
                                    // From my testing, this doesn't actually do anything. The FPS is included in the
                                    // SPS and the player uses the value there for calculations. Omitting the value entirely
                                    // causes browser-side playback to freeze.
                                    duration: Duration::from_millis(1),
                                    ..Default::default()
                                })
                                .await?;
                            }
                        }
                    }
                    Some(Ok(_)) => {
                        info!("No data frame");
                    }
                    Some(Err(e)) => {
                        return Err(anyhow!(e).context("Device failure"));
                    }
                    None => {
                        info!("Device no data from packet");
                        return Ok(());
                    }
                }
            }
            Result::<()>::Ok(())
        });
        Ok(())
    }
}

impl WebrtcClient {
    /// Create a new WebRTC Client for a peer-to-peer connection. This client will automatically open
    /// a WebSocket connection with KVS and begin signaling.
    pub fn new(client_id: String, connect_as_master_url: String) -> Result<Self> {
        info!("New WebrtcClient Client Created.");
        Ok(WebrtcClient {
            peer_connection: None,
            client_id,
            signaling_client: None,
            connect_as_master_url,
        })
    }
}

#[cfg(test)]
mod tests {
    use crate::client::{PeerStreaming, WebrtcClient};

    #[tokio::test]
    async fn verify_new_webrtc_client_creation() {
        let webrtc_client = WebrtcClient::new(String::from("client"), String::from("url"))
            .expect("Error creating client");
        assert_eq!(webrtc_client.client_id, String::from("client"));
        assert_eq!(webrtc_client.connect_as_master_url, String::from("url"));
        assert!(webrtc_client.peer_connection.is_none());
        assert!(webrtc_client.signaling_client.is_none());
    }

    #[tokio::test]
    async fn verify_parse_avcc_data() {
        let input = "014d0033ffe10016674d00338a8a503c0113f2cd1000003e80000753004001000468ee3c80";
        let extra_data = hex::decode(input).expect("error decoding");
        let (sps_from, sps_size, pps_from, pps_size) =
            WebrtcClient::parse_avcc_extra_data(&*extra_data);
        assert_eq!(sps_from, 8);
        assert_eq!(sps_size, 22);
        assert_eq!(pps_from, 33);
        assert_eq!(pps_size, 4);
    }

    #[tokio::test]
    async fn verify_stream_from_rtsp_fail_invalid_url() {
        let webrtc_client = WebrtcClient::new(String::from("client"), String::from("url"))
            .expect("Error creating client");

        let result = webrtc_client
            .stream_from_rtsp(
                String::from("rtsp://fail"),
                String::from("username"),
                String::from("Ew9!gnDA$7vz"),
            )
            .await;
        assert!(result.is_err());
    }
}
