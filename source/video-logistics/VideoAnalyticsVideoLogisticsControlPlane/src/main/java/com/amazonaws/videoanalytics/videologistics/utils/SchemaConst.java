package com.amazonaws.videoanalytics.videologistics.utils;

public final class SchemaConst {

    //    Common Schema Attributes
    public static final String CREATED_AT = "CreatedAt";
    public static final String DEVICE_ID = "DeviceId";
    public static final String LAST_UPDATED = "LastUpdated";
    public static final String WORKFLOW_NAME = "WorkflowName";
    //    Livestream Session Attributes
    public static final String LIVESTREAM_SESSION_TABLE_NAME =
            "LivestreamSessionTable";
    public static final String SESSION_ID = "SessionId";
    public static final String SESSION_STATUS = "SessionStatus";
    public static final String PEER_CONNECTION_STATUS = "PeerConnectionStatus";
    public static final String HLS_URL = "HLSUrl";
    public static final String SIGNALING_CHANNEL_URL = "SignalingChannelUrl";
    public static final String ERROR_CODE = "ErrorCode";
    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String START_TIME = "StartTime";
    public static final String PASSWORD = "Password";
    public static final String USERNAME = "Username";
    public static final String TTL = "TTL";
    public static final String URIS = "Uris";
    public static final String EXPIRATION_TIME = "ExpirationTime";
    public static final String ICE_SERVER = "IceServer";
    public static final String CLIENT_ID = "ClientId";
    public static final String SOURCE = "Source";


    private SchemaConst() {
        // restrict instantiation
    }
}
