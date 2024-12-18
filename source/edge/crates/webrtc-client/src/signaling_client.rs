use futures_util::stream::{SplitSink, SplitStream};
use futures_util::{SinkExt, StreamExt, TryStreamExt};
use std::collections::HashMap;
use tokio::task::JoinHandle;
use tokio_tungstenite::tungstenite::Message;
use tokio_tungstenite::{connect_async, MaybeTlsStream, WebSocketStream};
use tracing::{error, info, instrument};

use base64::{engine::general_purpose, Engine as _};
use std::{env, str};
use tokio::sync::mpsc::{Receiver, Sender};
use webrtc::ice_transport::ice_candidate::{RTCIceCandidate, RTCIceCandidateInit};

use anyhow::Result;
use bytes::Bytes;
use retina::client::SetupOptions;
use retina::codec::{CodecItem, ParametersRef};
use std::sync::Arc;
use std::time::Duration;
use tokio::net::TcpStream;
use tokio::sync::mpsc::channel;
use tokio::sync::Mutex;
use tracing::debug;
use webrtc::ice_transport::ice_connection_state::RTCIceConnectionState;
use webrtc::peer_connection::sdp::session_description::RTCSessionDescription;
use webrtc::Error;

use crate::constants::MAXIMUM_NUMBER_OF_CONCURRENT_VIEWERS;
use aws_sdk_kinesisvideosignaling::types::IceServer;
use serde::Serialize;
use url::Url;
use webrtc::api::media_engine::MIME_TYPE_H264;
use webrtc::media::Sample;
use webrtc::peer_connection::peer_connection_state::RTCPeerConnectionState;
use webrtc::rtp_transceiver::rtp_codec::RTCRtpCodecCapability;
use webrtc::track::track_local::track_local_static_sample::TrackLocalStaticSample;

use crate::error::WebrtcClientError;
use crate::webrtc_client::{PeerConnection, WebrtcSession};

use kvs_client::client::KinesisVideoStreamClient;

// GRCOV_STOP_COVERAGE

/// Represents an action that can be taken via KVS WebSocket APIS
/// https://docs.aws.amazon.com/kinesisvideostreams-webrtc-dg/latest/devguide/kvswebrtc-websocket-apis.html
#[derive(Debug)]
pub enum KVSSignalingAction {
    /// Send SDP Offer
    SdpOffer,
    /// Send SDP Answer
    SdpAnswer,
    /// Send Ice Candidate
    IceCandidate,
}

impl KVSSignalingAction {
    /// Returns the string used to indicate the WebSocket API path.
    fn as_str(&self) -> &'static str {
        match self {
            KVSSignalingAction::SdpAnswer => "SDP_ANSWER",
            KVSSignalingAction::SdpOffer => "SDP_OFFER",
            KVSSignalingAction::IceCandidate => "ICE_CANDIDATE",
        }
    }
}

#[derive(Serialize)]
struct KVSSignalingMessage<'a> {
    action: &'a str,
    #[serde(rename = "messagePayload")]
    message_payload: String,
    #[serde(rename = "recipientClientId")]
    recipient_client_id: &'a str,
}

/// Handler class for incoming KVS WebSocket messages.
#[derive(Debug)]
pub struct WebrtcPeerConnectionsHandler {
    ice_server_configs: Vec<IceServer>,
    video_track: Arc<TrackLocalStaticSample>,
    tx: Sender<String>,
    region: String,
    pub peer_connections: Arc<Mutex<HashMap<String, WebrtcSession>>>,
}

impl WebrtcPeerConnectionsHandler {
    /// Returns a new message handler.
    pub async fn new(
        ice_server_configs: Vec<IceServer>,
        tx: Sender<String>,
        rtsp_url: String,
        region: String,
        onvif_username: String,
        onvif_password: String,
    ) -> Result<WebrtcPeerConnectionsHandler, WebrtcClientError> {
        let video_track = Self::create_initial_track(
            rtsp_url.clone(),
            onvif_username.clone(),
            onvif_password.clone(),
        )
        .await?;

        Ok(WebrtcPeerConnectionsHandler {
            ice_server_configs,
            video_track,
            tx,
            region,
            peer_connections: Arc::new(Mutex::new(HashMap::new())),
        })
    }

    async fn create_initial_track(
        rtsp_url: String,
        onvif_username: String,
        onvif_password: String,
    ) -> Result<Arc<TrackLocalStaticSample>, WebrtcClientError> {
        let mut rtsp_endpoint = rtsp_url;

        if let Ok(rtsp_overwrite) = env::var("RTSP_ENDPOINT") {
            rtsp_endpoint = rtsp_overwrite;
        }

        let Ok(url) = Url::parse(&*rtsp_endpoint) else {
            return Err(WebrtcClientError::RtspError("Unable to parse to RTSP url".to_string()));
        };

        // convert the password into MD5 and encoded in base64
        let pw_md5_hash = md5::compute(onvif_password.as_str());
        let pw_md5_base64 = general_purpose::STANDARD.encode(*pw_md5_hash);

        let mut tracks = Vec::new();

        let upstream_session_group = Arc::new(retina::client::SessionGroup::default());
        let creds = retina::client::Credentials {
            username: onvif_username.clone(),
            password: pw_md5_base64,
        };

        let mut upstream_session = match retina::client::Session::describe(
            url,
            retina::client::SessionOptions::default()
                .creds(Some(creds))
                .session_group(upstream_session_group.clone())
                .user_agent("VideoAnalytics".to_owned())
                .teardown(retina::client::TeardownPolicy::Auto),
        )
        .await
        {
            Ok(sess) => sess,
            Err(_e) => {
                return Err(WebrtcClientError::RtspError(
                    "Unable to connect to RTSP url".to_string(),
                ))
            }
        };

        let video_track = Arc::new(TrackLocalStaticSample::new(
            RTCRtpCodecCapability { mime_type: MIME_TYPE_H264.to_owned(), ..Default::default() },
            "video".to_string(),
            "video".to_string(),
        ));

        for (i, stream) in upstream_session.streams().iter().enumerate() {
            if stream.media() != "video" && stream.encoding_name() != "h264" {
                // Currently we only support H.264 video.
                continue;
            }

            if tracks.len() <= i {
                tracks.resize(i + 1, None);
            }

            tracks[i] = Some(video_track.clone());
        }

        for i in tracks.iter().enumerate().filter_map(|(i, track)| track.as_ref().map(|_| i)) {
            upstream_session
                .setup(
                    i,
                    SetupOptions::default()
                        .transport(retina::client::Transport::Tcp(Default::default())),
                )
                .await
                .map_err(|e| WebrtcClientError::RtspError(e.to_string()))?;
        }

        let mut upstream_session = upstream_session
            .play(retina::client::PlayOptions::default().ignore_zero_seq(true))
            .await
            .map_err(|e| WebrtcClientError::RtspError(e.to_string()))?
            .demuxed()
            .map_err(|e| WebrtcClientError::RtspError(e.to_string()))?;

        tokio::spawn(async move {
            loop {
                let item = upstream_session.next().await;
                match item {
                    Some(Ok(CodecItem::VideoFrame(frame))) => {
                        if let Some(t) = tracks.get(frame.stream_id()).and_then(Option::as_ref) {
                            let mut nalus: Vec<Vec<u8>> = Vec::with_capacity(3);
                            // Is key frame
                            if frame.is_random_access_point() {
                                info!("Key frame found");
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
                                    }
                                    None => {
                                        info!("Discarding video frame received before parameters");
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
                                let _ = t
                                    .write_sample(&Sample {
                                        data: Bytes::from(nalu),
                                        // From my testing, this doesn't actually do anything. The FPS is included in the
                                        // SPS and the player uses the value there for calculations. Omitting the value entirely
                                        // causes browser-side playback to freeze.
                                        duration: Duration::from_secs(1000),
                                        ..Default::default()
                                    })
                                    .await;
                            }
                        }
                    }
                    Some(Ok(_)) => {
                        info!("No data frame");
                    }
                    Some(Err(e)) => {
                        error!("No data frame {:?}", e);
                    }
                    None => {
                        info!("Process 1 no data frame from packet");
                    }
                }
            }
        });
        Ok(video_track)
    }

    // GRCOV_START_COVERAGE
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

    /// Handler for all incoming WebSocket messages. Handling varies based on the type and content of the message.
    async fn on_message(&mut self, message: Message) -> Result<(), WebrtcClientError> {
        let str_msg = message.to_string();
        let json_msg: serde_json::Value = match serde_json::from_str(&str_msg) {
            Ok(s) => s,
            Err(_) => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to convert message to json".to_string(),
                ));
            }
        };

        let message_type = match json_msg.get("messageType") {
            Some(msg) => msg,
            None => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to retrieve message type from message".to_string(),
                ));
            }
        };

        let sender_client_id = match json_msg.get("senderClientId") {
            Some(msg) => msg,
            None => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to retrieve client id from message".to_string(),
                ));
            }
        }
        .clone();

        let sender_client_id_as_str = match sender_client_id.as_str() {
            Some(msg) => msg,
            None => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to parse sender client as string".to_string(),
                ));
            }
        }
        .to_string();

        let message_payload = match json_msg.get("messagePayload") {
            Some(msg) => msg,
            None => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to retrieve message payload from message".to_string(),
                ));
            }
        };

        let message_payload_as_str = match message_payload.as_str() {
            Some(s) => s,
            None => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to parse message payload as string".to_string(),
                ));
            }
        };
        let message_payload_u8 =
            general_purpose::STANDARD.decode(message_payload_as_str).map_err(|_| {
                WebrtcClientError::ParsingError("Unable to decode message payload".to_string())
            })?;

        let message_payload = match str::from_utf8(&message_payload_u8) {
            Ok(msg) => msg,
            Err(_) => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to convert to utf8".to_string(),
                ));
            }
        };

        let message_type_as_str = match message_type.as_str() {
            Some(msg) => msg,
            None => {
                return Err(WebrtcClientError::ParsingError(
                    "Unable to convert message type to string".to_string(),
                ));
            }
        };

        info!(
            "Received message of type: {} from client id {}",
            message_type_as_str, sender_client_id_as_str
        );

        let mut peer_connections_lock = self.peer_connections.lock().await;

        // If an SDP offer is received from the signaling channel, we use it to set the PeerConnection remote description
        if message_type_as_str == KVSSignalingAction::SdpOffer.as_str() {
            let sdp = match serde_json::from_str::<RTCSessionDescription>(&message_payload) {
                Ok(s) => s,
                Err(err) => return Err(WebrtcClientError::SignalingError(err.to_string())),
            };

            // If a new SDP offer is received, overwrite existing connection
            if peer_connections_lock.contains_key(&sender_client_id_as_str) {
                // Unwrap safe since we checked if key exists
                let existing_peer_connection =
                    match peer_connections_lock.get(&sender_client_id_as_str) {
                        None => return Ok(()),
                        Some(peer) => peer.peer_connection.clone(),
                    }
                    .unwrap();
                let _ = existing_peer_connection.close().await;
                peer_connections_lock.remove(&sender_client_id_as_str);
            }

            let mut new_peer_connection =
                WebrtcSession::new(self.ice_server_configs.clone(), self.video_track.clone());
            match new_peer_connection.create_peer_connection_and_stream(self.region.clone()).await {
                Ok(_) => {
                    info!(
                        "Created new peer connection with client id {:?}",
                        sender_client_id_as_str
                    )
                }
                Err(e) => {
                    return Err(WebrtcClientError::SignalingError(e.to_string()));
                }
            };

            // Check if hashmap size is full, if it is, we reached maximum number of allowed concurrent viewers
            if peer_connections_lock.len() >= MAXIMUM_NUMBER_OF_CONCURRENT_VIEWERS {
                error!("Maximum number of connections has been reached");
                return Err(WebrtcClientError::MaximumConnectionsError(
                    "Maximum number of connections has been reached".to_string(),
                ));
            }

            peer_connections_lock.insert(sender_client_id_as_str.to_string(), new_peer_connection);

            // Unwrap is safe here since we are adding new entry above
            let peer_connection = match peer_connections_lock.get(&sender_client_id_as_str) {
                None => return Ok(()),
                Some(peer) => peer.peer_connection.clone(),
            }
            .unwrap();

            peer_connection.set_remote_description(sdp).await?;
            let sdp_answer = peer_connection.create_answer(None).await?;
            self.send_sdp_answer(&sdp_answer, &*sender_client_id_as_str).await?;
            peer_connection.set_local_description(sdp_answer).await?;
            peer_connection.on_ice_connection_state_change(Box::new(
                move |connection_state: RTCIceConnectionState| {
                    info!("ICE Connection State has changed {connection_state}");
                    Box::pin(async {})
                },
            ));

            let client_id = sender_client_id_as_str.clone();
            let (peer_connection_tx, peer_connection_rx) = channel::<String>(1000);

            let peer_connections = self.peer_connections.clone();

            let _ =
                Self::handle_peer_connection_state_changes(peer_connections, peer_connection_rx)
                    .await;

            peer_connection.on_peer_connection_state_change(Box::new(
                move |peer_connection_state: RTCPeerConnectionState| {
                    let client_id_clone = client_id.clone();
                    info!("Peer Connection State has changed to {peer_connection_state} for client id: {client_id}");
                    if peer_connection_state == RTCPeerConnectionState::Failed {
                        let _ = peer_connection_tx.try_send(client_id_clone);
                    }

                    Box::pin(async {})
                },
            ));

            let pending_candidates: Arc<Mutex<Vec<RTCIceCandidate>>> = Arc::new(Mutex::new(vec![]));
            let pc1 = pending_candidates.clone();
            peer_connection.on_ice_candidate(Box::new(
                move |ice_candidate: Option<RTCIceCandidate>| {
                    let pc = pc1.clone();
                    Box::pin(async move {
                        if let Some(ice_candidate) = ice_candidate {
                            let mut cs = pc.lock().await;
                            cs.push(ice_candidate);
                        }
                    })
                },
            ));

            let mut gather_complete = peer_connection.gathering_complete_promise().await;
            let _ = gather_complete.recv().await;

            let cs = pending_candidates.lock().await;

            for c in &*cs {
                self.send_ice_candidate(c.clone(), &*sender_client_id_as_str).await?;
            }
        } else if message_type_as_str == KVSSignalingAction::IceCandidate.as_str() {
            if !peer_connections_lock.contains_key(&sender_client_id_as_str) {
                return Err(WebrtcClientError::SignalingError(
                    "Peer connection SDP has not yet been set".to_string(),
                ));
            }

            // Unwrap is safe here since we are checking if entry exists in line above
            let peer_connection = match peer_connections_lock.get(&sender_client_id_as_str) {
                None => return Ok(()),
                Some(peer) => peer.peer_connection.clone(),
            }
            .unwrap();

            match peer_connection
                .add_ice_candidate(RTCIceCandidateInit {
                    candidate: message_payload.to_string(),
                    ..Default::default()
                })
                .await
            {
                Ok(()) => {}
                Err(error) => {
                    error!("Error adding ice candidate {error}");
                    return Err(WebrtcClientError::SignalingError(error.to_string()));
                }
            }
        }
        info!("Current number of connections {:?}", peer_connections_lock.len());
        Ok(())
    }

    async fn handle_peer_connection_state_changes(
        peer_connections: Arc<Mutex<HashMap<String, WebrtcSession>>>,
        mut peer_connection_rx: Receiver<String>,
    ) -> JoinHandle<()> {
        tokio::spawn(async move {
            let Some(client_id) = peer_connection_rx.recv().await else {
                error!("Did not get peer connection");
                return;
            };
            let mut peer_connections_lock = peer_connections.lock().await;
            let peer_connection = match peer_connections_lock.get(&client_id) {
                None => {
                    error!("Unable to find peer connection");
                    return;
                }
                Some(peer) => peer.peer_connection.clone(),
            }
            .expect(format!("no peer connection found for {}", client_id).as_str());
            let _ = peer_connection.close().await;

            info!("Closing peer connection for {:?}", client_id);
            peer_connections_lock.remove(&client_id);
        })
    }

    /// Send SDP offer via WebSocket.
    async fn send_sdp_answer(
        &self,
        sdp_answer: &RTCSessionDescription,
        sender_client_id: &str,
    ) -> Result<(), WebrtcClientError> {
        let sdp_answer_str = match serde_json::to_string(sdp_answer) {
            Ok(p) => p,
            Err(err) => {
                error!("unable to send sdp answer {:?}", err);
                return Err(WebrtcClientError::SignalingError(err.to_string()));
            }
        };

        let base64_sdp_answer = general_purpose::STANDARD.encode(sdp_answer_str);
        let _res = self
            .send_signaling_message(
                KVSSignalingAction::SdpAnswer,
                base64_sdp_answer,
                sender_client_id,
            )
            .await;
        Ok(())
    }

    /// Sends a signaling message to the WebSocket.
    async fn send_signaling_message(
        &self,
        action: KVSSignalingAction,
        message_payload: String,
        recipient_client_id: &str,
    ) -> Result<(), WebrtcClientError> {
        let message = serde_json::to_string(&KVSSignalingMessage {
            action: action.as_str(),
            message_payload,
            recipient_client_id,
        })
        .expect("Could not create WebSocket message string.");

        match self.tx.send(message.clone()).await {
            Ok(_) => {}
            Err(error) => {
                error!("failed to send signaling message {:?}", error);
                return Err(WebrtcClientError::SendError(error));
            }
        };
        Ok(())
    }

    /// Send the provided ice candidate to the WebSocket connection
    async fn send_ice_candidate(
        &self,
        ice_candidate: RTCIceCandidate,
        sender_client_id: &str,
    ) -> Result<(), WebrtcClientError> {
        let can = serde_json::to_string(
            &ice_candidate.to_json().expect("Unable to convert ice candidate to json"),
        )
        .expect("Unable to convert ice candidate to string");
        let base64_ice_candidate = general_purpose::STANDARD.encode(can);
        let _err = self
            .send_signaling_message(
                KVSSignalingAction::IceCandidate,
                base64_ice_candidate,
                sender_client_id,
            )
            .await;
        Ok(())
    }
}

/// The client responsible for performing p2p STUN/TURN signaling.
#[derive(Debug)]
pub struct WebrtcSignalingClient {}

impl WebrtcSignalingClient {
    /// WebrtcSignalingClient constructor. Attempts to create a WebSocket connection to the provided
    /// URL and spawns reader/writer threads for signaling.
    #[instrument]
    pub async fn new(
        ice_server_configs: Vec<IceServer>,
        rtsp_url: String,
        region: String,
        onvif_username: String,
        onvif_password: String,
    ) -> Result<Self> {
        // Channel used to communicate with WebSocket reader/writer tasks

        let (tx, rx) = channel::<String>(1000);
        let tx_clone = tx.clone();

        let message_handler = WebrtcPeerConnectionsHandler::new(
            ice_server_configs,
            tx_clone,
            rtsp_url,
            region,
            onvif_username,
            onvif_password,
        )
        .await?;

        let message_handler = Arc::new(Mutex::new(message_handler));

        let _reconnect_handle = Self::start_reconnection_task(rx, message_handler).await?;

        Ok(WebrtcSignalingClient {})
    }

    async fn start_reconnection_task(
        rx: Receiver<String>,
        message_handler: Arc<Mutex<WebrtcPeerConnectionsHandler>>,
    ) -> Result<JoinHandle<()>, WebrtcClientError> {
        let rx = Arc::new(Mutex::new(rx));

        // Reconnection task
        let reconnect_handle = tokio::spawn(async move {
            loop {
                /*
                   TURN server credential lifecycle 5 minutes https://docs.aws.amazon.com/kinesisvideostreams-webrtc-dg/latest/devguide/kvswebrtc-limits.html
                   ConnectAsMaster timeout after 10 minutes
                   therefore we need to fetch need credentials from KVS after every 4 minutes and also need a new connect as master presigned url
                */
                let kvs_client = match KinesisVideoStreamClient::from_conf().await {
                    Ok(kvs_client) => kvs_client,
                    Err(e) => {
                        error!("Unable to make call to KVS {:?}", e);
                        continue;
                    }
                };
                let connect_as_master_url = kvs_client.presigned_url.to_owned();
                message_handler.lock().await.ice_server_configs =
                    kvs_client.ice_server_configs.to_owned();

                let (mut ws_tx, mut ws_rx) = match Self::async_open(&connect_as_master_url).await {
                    Ok(s) => s,
                    Err(_e) => {
                        error!("Websocket connection failed");
                        tokio::time::sleep(Duration::from_secs(20)).await;
                        continue;
                    }
                };
                debug!("Connected to websocket url: {}", connect_as_master_url);

                let rx_clone: Arc<Mutex<Receiver<String>>> = Arc::clone(&rx);
                // WebSocket writer thread. Forwards data from tx to ws_tx.
                let writer_handle = tokio::spawn(async move {
                    loop {
                        let message = rx_clone.lock().await.recv().await;
                        match message {
                            Some(message) => {
                                ws_tx
                                    .send(Message::Text(message.clone()))
                                    .await
                                    .expect("Unable to send data to websocket channel");
                            }
                            None => {
                                debug!("Received empty WebSocket message request.")
                            }
                        }
                    }
                });

                let message_handler_clone = Arc::clone(&message_handler);

                // WebSocket reader thread. Calls message handler for all incoming traffic.
                let reader_handle = tokio::spawn(async move {
                    loop {
                        let Some(msg) =
                            ws_rx.try_next().await.expect("Unable to fetch new message")
                        else {
                            error!("Message from websocket was empty");
                            return;
                        };
                        let _ = message_handler_clone.lock().await.on_message(msg).await;
                    }
                });
                tokio::time::sleep(Duration::from_secs(4 * 60)).await;

                // Abort the previous task so we don't leak resources
                writer_handle.abort();
                reader_handle.abort();
            }
        });

        Ok(reconnect_handle)
    }

    async fn async_open(
        connect_as_master_url: &str,
    ) -> Result<
        (
            SplitSink<WebSocketStream<MaybeTlsStream<TcpStream>>, Message>,
            SplitStream<WebSocketStream<MaybeTlsStream<TcpStream>>>,
        ),
        Error,
    > {
        let (ws, _) = connect_async(connect_as_master_url).await.expect("Unable to connect");
        info!("Connected to new WebSocket URL: {}", connect_as_master_url);
        let (ws_tx, ws_rx) = ws.split();
        Ok((ws_tx, ws_rx))
    }
}

#[cfg(test)]
mod tests {
    use crate::signaling_client::{WebrtcPeerConnectionsHandler, WebrtcSignalingClient};
    #[tokio::test]
    async fn verify_parse_avcc_data() {
        let input = "014d0033ffe10016674d00338a8a503c0113f2cd1000003e80000753004001000468ee3c80";
        let extra_data = hex::decode(input).expect("error decoding");
        let (sps_from, sps_size, pps_from, pps_size) =
            WebrtcPeerConnectionsHandler::parse_avcc_extra_data(&*extra_data);
        assert_eq!(sps_from, 8);
        assert_eq!(sps_size, 22);
        assert_eq!(pps_from, 33);
        assert_eq!(pps_size, 4);
    }

    #[tokio::test]
    async fn verify_create_initial_track_rtsp_invalid_url() {
        let result = WebrtcPeerConnectionsHandler::create_initial_track(
            String::from("url"),
            String::from("user"),
            String::from("pass"),
        )
        .await;
        assert!(result.is_err());
    }
}
