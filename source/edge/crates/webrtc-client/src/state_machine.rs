use async_trait::async_trait;
use mockall::automock;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::error::Error;
use std::str::FromStr;
use tokio::sync::mpsc::Sender;

use crate::client::{PeerConnection, PeerStreaming, TurnServerInfo, WebrtcClient};
use crate::constants::{CLIENT_ID, STATE};

pub enum StreamingPeerConnectionState {
    Connecting,
    Connected,
    Failed,
    Disconnected,
}

impl FromStr for StreamingPeerConnectionState {
    type Err = ();
    fn from_str(input: &str) -> Result<StreamingPeerConnectionState, Self::Err> {
        match input {
            "Connecting" => Ok(StreamingPeerConnectionState::Connecting),
            "Connected" => Ok(StreamingPeerConnectionState::Connected),
            "Failed" => Ok(StreamingPeerConnectionState::Failed),
            "Disconnected" => Ok(StreamingPeerConnectionState::Disconnected),
            _ => Err(()),
        }
    }
}

#[derive(Serialize, Deserialize)]
struct StreamingPeerConnectionShadowEntry {
    #[serde(rename = "ClientId")]
    client_id: String,
    #[serde(rename = "SignalingChannelUrl")]
    signaling_channel_url: String,
    #[serde(rename = "ExpirationTime")]
    expiration_time: String,
    #[serde(rename = "State")]
    state: String,
    #[serde(rename = "IceServerList")]
    ice_server_list: Vec<TurnServerInfo>,
    #[serde(rename = "StartTime")]
    start_time: Option<String>,
    #[serde(rename = "EndTime")]
    end_time: Option<String>,
}

/// The top-level handler for streaming on the device. This state machine should only be instantiated once,
/// and contains references to every outstanding p2p connection. The state machine should be notified
/// via the `handle_state_machine` callback whenever there is an update to the device shadow that
/// modifieds a streaming connection.
#[derive(Debug)]
pub struct WebrtcStateMachine {
    pub active_peer_connections: Vec<Box<WebrtcClient>>,
    signal_tx: Sender<String>,
    rtsp_url: String,
    username: String,
    password: String,
    region: String,
}

#[automock]
#[async_trait]
pub trait StateMachineWebrtcClient {
    fn create_new_webrtc_client(
        &self,
        connect_as_master_url: String,
        client_id: String,
    ) -> Box<WebrtcClient>;
    async fn connect_peer_and_stream(
        &mut self,
        webrtc_client: &mut WebrtcClient,
    ) -> Result<(), Box<dyn Error>>;
}

#[async_trait]
impl StateMachineWebrtcClient for WebrtcStateMachine {
    fn create_new_webrtc_client(
        &self,
        connect_as_master_url: String,
        client_id: String,
    ) -> Box<WebrtcClient> {
        Box::new(
            WebrtcClient::new(client_id, connect_as_master_url)
                .expect("Unable to create webrtc client"),
        )
    }

    async fn connect_peer_and_stream(
        &mut self,
        webrtc_client: &mut WebrtcClient,
    ) -> Result<(), Box<dyn Error>> {
        webrtc_client
            .connect_peer(self.signal_tx.clone(), self.region.clone())
            .await
            .expect("Unable to connect to peer");

        webrtc_client
            .stream_from_rtsp(self.rtsp_url.clone(), self.username.clone(), self.password.clone())
            .await
            .expect("Unable to stream from RTSP");

        Ok(())
    }
}

impl WebrtcStateMachine {
    /// Initializes a new WebrtcStateMachine. There should only be one state machine per device.
    pub fn new(
        signal_tx: Sender<String>,
        rtsp_url: String,
        username: String,
        password: String,
        region: String,
    ) -> Result<WebrtcStateMachine, ()> {
        Ok(WebrtcStateMachine {
            active_peer_connections: vec![],
            signal_tx,
            rtsp_url,
            username,
            password,
            region,
        })
    }

    /// Handler for all shadow updates that modify a streaming connection.
    pub async fn handle_state_update(&mut self, update: Value) -> anyhow::Result<()> {
        let new_state_str = match update[STATE].as_str() {
            Some(state) => state,
            None => {
                anyhow::bail!("No state found");
            }
        };

        let new_state: StreamingPeerConnectionState =
            match StreamingPeerConnectionState::from_str(new_state_str) {
                Ok(state) => state,
                Err(_err) => {
                    anyhow::bail!("Invalid state passed in {}", new_state_str);
                }
            };

        match new_state {
            StreamingPeerConnectionState::Connecting => {
                let _res = self.handle_state_change_connecting(update).await;
            }
            StreamingPeerConnectionState::Connected => {
                // do nothing
            }
            StreamingPeerConnectionState::Failed => {
                self.handle_state_change_disconnected_or_failed(update);
            }
            StreamingPeerConnectionState::Disconnected => {
                self.handle_state_change_disconnected_or_failed(update);
            }
        }
        Ok(())
    }

    fn handle_state_change_disconnected_or_failed(&mut self, update: Value) {
        let client_id = update[CLIENT_ID].as_str().expect("Unable to retrieve client id");

        if let Some(pos) = self
            .active_peer_connections
            .iter()
            .position(|webrtc_client| webrtc_client.client_id == client_id)
        {
            self.active_peer_connections.remove(pos);
        }
    }

    // GRCOV_STOP_COVERAGE
    // We don't have setup to mock RTSP server or signaling channel
    async fn handle_state_change_connecting(&mut self, update: Value) -> Result<(), ()> {
        let connection_details =
            match serde_json::from_value::<StreamingPeerConnectionShadowEntry>(update) {
                Ok(streaming_details) => streaming_details,
                Err(_) => {
                    return Ok(());
                }
            };

        let connect_as_master_url = connection_details.signaling_channel_url;
        let client_id = connection_details.client_id;

        let mut webrtc_client =
            self.create_new_webrtc_client(connect_as_master_url, client_id.clone());
        let _ = self.connect_peer_and_stream(webrtc_client.as_mut()).await;
        self.active_peer_connections.push(webrtc_client);
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use crate::state_machine::WebrtcStateMachine;
    use serde_json::json;
    use tokio::sync::mpsc::channel;

    #[tokio::test]
    async fn verify_handle_state_update_for_disconnection() {
        let (tx, rx) = channel(10);
        let (iot_shadow_client_tx, iot_shadow_client_rx) = channel::<String>(10000);
        let mut state_machine = WebrtcStateMachine::new(
            tx,
            "rtsp://url.com".parse().unwrap(),
            "username".parse().unwrap(),
            "password".parse().unwrap(),
            "region".to_string(),
        )
        .unwrap();
        let state_update = json!({"State": "Disconnected", "ClientId": "fail"});
        state_machine
            .handle_state_update(state_update)
            .await
            .expect("Failed to handle state update change");
        assert_eq!(state_machine.active_peer_connections.len(), 0);
    }

    #[tokio::test]
    async fn verify_handle_state_update_for_new_connection_without_fields() {
        let (tx, rx) = channel(10);
        let (iot_shadow_client_tx, iot_shadow_client_rx) = channel::<String>(10000);
        let mut state_machine = WebrtcStateMachine::new(
            tx,
            "rtsp://url.com".parse().unwrap(),
            "username".parse().unwrap(),
            "password".parse().unwrap(),
            "region".to_string(),
        )
        .unwrap();
        let state_update = json!({"State": "Connecting"});
        let _ = state_machine.handle_state_update(state_update).await;
        assert_eq!(state_machine.active_peer_connections.len(), 0);
    }

    #[tokio::test]
    async fn verify_handle_state_update_for_new_connection_fails() {
        let (tx, rx) = channel(10);
        let (iot_shadow_client_tx, iot_shadow_client_rx) = channel::<String>(10000);
        let mut state_machine = WebrtcStateMachine::new(
            tx,
            "rtsp://url.com".parse().unwrap(),
            "username".parse().unwrap(),
            "password".parse().unwrap(),
            "region".to_string(),
        )
        .unwrap();
        let state_update = json!({"Fail": "Fail"});
        assert!(state_machine.handle_state_update(state_update).await.is_err());
    }
}
