// GRCOV_STOP_COVERAGE
use futures_util::stream::{SplitSink, SplitStream};
use futures_util::{SinkExt, StreamExt, TryStreamExt};
use tokio::task::JoinHandle;
use tokio_tungstenite::tungstenite::Message;
use tokio_tungstenite::{connect_async, MaybeTlsStream, WebSocketStream};
use tracing::{error, info, instrument};

use base64::{engine::general_purpose, Engine as _};
use std::str;
use tokio::sync::mpsc::Sender;
use webrtc::ice_transport::ice_candidate::{RTCIceCandidate, RTCIceCandidateInit};
use webrtc::peer_connection::RTCPeerConnection;

use anyhow::Result;
use std::sync::Arc;
use tokio::net::TcpStream;
use tokio::sync::mpsc::channel;
use tracing::debug;
use webrtc::ice_transport::ice_connection_state::RTCIceConnectionState;
use webrtc::peer_connection::sdp::session_description::RTCSessionDescription;
use webrtc::Error;

use serde::Serialize;
use tokio::sync::Mutex;

use crate::error::WebrtcClientError;

// GRCOV_STOP_COVERAGE
// We don't have setup for mock signaling channel URL

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
pub struct WebrtcSignalingMessageHandler {
    tx: Sender<String>,
    client_id: String,
    peer_connection: Arc<RTCPeerConnection>,
    pending_candidates: Arc<Mutex<Vec<RTCIceCandidate>>>,
}

impl WebrtcSignalingMessageHandler {
    /// Returns a new message handler.
    pub fn new(
        tx: Sender<String>,
        client_id: String,
        peer_connection: Arc<RTCPeerConnection>,
        pending_candidates: Arc<Mutex<Vec<RTCIceCandidate>>>,
    ) -> Result<WebrtcSignalingMessageHandler> {
        Ok(WebrtcSignalingMessageHandler { tx, client_id, peer_connection, pending_candidates })
    }

    /// Handler for all incoming WebSocket messages. Handling varies based on the type and content of the message.
    async fn on_message(&self, message: Message) -> Result<()> {
        let str_msg = message.to_string();
        let json_msg: serde_json::Value = match serde_json::from_str(&str_msg) {
            Ok(s) => s,
            Err(_) => {
                return Ok(());
            }
        };

        let message_type =
            json_msg.get("messageType").expect("Unable to retrieve message type from message");
        let message_payload_u8 = general_purpose::STANDARD
            .decode(
                json_msg
                    .get("messagePayload")
                    .expect("Unable to retrieve message payload from message")
                    .as_str()
                    .expect("Unable to convert to string"),
            )
            .expect("Unable to decode message payload");
        let message_payload =
            str::from_utf8(&message_payload_u8).expect("Unable to convert to utf8");

        let message_type_as_str =
            message_type.as_str().expect("Unable to convert message type to string");
        info!("Received message of type: {}", message_type_as_str);

        // If an SDP offer is received from the signaling channel, we use it to set the PeerConnection remote description
        if message_type_as_str == KVSSignalingAction::SdpOffer.as_str() {
            info!(message_payload);
            let sdp = match serde_json::from_str::<RTCSessionDescription>(&message_payload) {
                Ok(s) => s,
                Err(err) => panic!("{}", err),
            };
            self.peer_connection.set_remote_description(sdp).await?;

            let sdp_answer = self.peer_connection.create_answer(None).await?;

            self.send_sdp_answer(&sdp_answer).await?;

            self.peer_connection.set_local_description(sdp_answer).await?;

            self.peer_connection.on_ice_connection_state_change(Box::new(
                move |connection_state: RTCIceConnectionState| {
                    info!("ICE Connection State has changed {connection_state}");
                    if connection_state == RTCIceConnectionState::Failed {}
                    Box::pin(async {})
                },
            ));

            let pc1 = self.pending_candidates.clone();
            self.peer_connection.on_ice_candidate(Box::new(
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

            let mut gather_complete = self.peer_connection.gathering_complete_promise().await;
            let _ = gather_complete.recv().await;
            let cs = self.pending_candidates.lock().await;

            for c in &*cs {
                self.send_ice_candidate(c.clone()).await?;
            }
        } else if message_type_as_str == KVSSignalingAction::IceCandidate.as_str() {
            match self
                .peer_connection
                .add_ice_candidate(RTCIceCandidateInit {
                    candidate: message_payload.to_string(),
                    ..Default::default()
                })
                .await
            {
                Ok(()) => {}
                Err(error) => debug!("Error adding ice candidate {error}"),
            }
        }
        Ok(())
    }

    /// Send SDP offer via WebSocket.
    async fn send_sdp_answer(&self, sdp_answer: &RTCSessionDescription) -> Result<(), Error> {
        let sdp_answer_str = match serde_json::to_string(sdp_answer) {
            Ok(p) => p,
            Err(err) => panic!("{}", err),
        };

        let base64_sdp_answer = general_purpose::STANDARD.encode(sdp_answer_str);
        let _res =
            self.send_signaling_message(KVSSignalingAction::SdpAnswer, base64_sdp_answer).await;
        Ok(())
    }

    /// Sends a signaling message to the WebSocket.
    async fn send_signaling_message(
        &self,
        action: KVSSignalingAction,
        message_payload: String,
    ) -> Result<(), WebrtcClientError> {
        let message = serde_json::to_string(&KVSSignalingMessage {
            action: action.as_str(),
            message_payload,
            recipient_client_id: &self.client_id,
        })
        .expect("Could not create WebSocket message string.");

        match self.tx.send(message).await {
            Ok(_) => {}
            Err(error) => return Err(WebrtcClientError::SendError(error)),
        };
        Ok(())
    }

    /// Send the provided ice candidate to the WebSocket connection
    async fn send_ice_candidate(
        &self,
        ice_candidate: RTCIceCandidate,
    ) -> Result<(), WebrtcClientError> {
        let can = serde_json::to_string(
            &ice_candidate.to_json().expect("Unable to convert ice candidate to json"),
        )
        .expect("Unable to convert ice candidate to string");
        let base64_ice_candidate = general_purpose::STANDARD.encode(can);

        let _err = self
            .send_signaling_message(KVSSignalingAction::IceCandidate, base64_ice_candidate)
            .await;
        Ok(())
    }
}

/// The client responsible for performing p2p STUN/TURN signaling.
#[derive(Debug)]
pub struct WebrtcSignalingClient {
    writer_handle: JoinHandle<()>,
    reader_handle: JoinHandle<()>,
}

impl WebrtcSignalingClient {
    /// WebrtcSignalingClient constructor. Attempts to create a WebSocket connection to the provided
    /// URL and spawns reader/writer threads for signaling.
    #[instrument]
    pub async fn new(
        connect_as_master_url: String,
        peer_connection: Arc<RTCPeerConnection>,
        pending_candidates: Arc<Mutex<Vec<RTCIceCandidate>>>,
        client_id: String,
    ) -> Result<Self> {
        // Channel used to communicate with WebSocket reader/writer tasks
        let (tx, mut rx) = channel::<String>(1000);

        // The actual WebSocket streams.
        let (mut ws_tx, mut ws_rx) = Self::async_open(&connect_as_master_url).await?;

        // WebSocket writer thread. Forwards data from tx to ws_tx.
        let writer_handle = tokio::spawn(async move {
            loop {
                let message = rx.recv().await;
                match message {
                    Some(message) => {
                        ws_tx
                            .send(Message::Text(message))
                            .await
                            .expect("Unable to send data to websocket channel");
                    }
                    None => {
                        debug!("Received empty WebSocket message request.")
                    }
                }
            }
        });

        let peer_connection_clone = peer_connection.clone();
        let pending_candidates_clone = pending_candidates.clone();
        let tx_clone = tx.clone();
        let message_handler = WebrtcSignalingMessageHandler::new(
            tx_clone,
            client_id,
            peer_connection_clone,
            pending_candidates_clone,
        )
        .expect("Unable to create new signaling message handler");

        // WebSocket reader thread. Calls message handler for all incoming traffic.
        let reader_handle = tokio::spawn(async move {
            loop {
                let Some(msg) = ws_rx.try_next().await.expect("Unable to fetch new message") else {
                    error!("Message from websocket was empty");
                    return;
                };
                message_handler.on_message(msg).await.expect("Reader was unable to handle message");
            }
        });

        Ok(WebrtcSignalingClient { reader_handle, writer_handle })
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

impl Drop for WebrtcSignalingClient {
    fn drop(&mut self) {
        self.writer_handle.abort();
        self.reader_handle.abort();
    }
}
