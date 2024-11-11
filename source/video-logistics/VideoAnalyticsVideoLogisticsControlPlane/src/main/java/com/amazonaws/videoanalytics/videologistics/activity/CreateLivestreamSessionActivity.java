package com.amazonaws.videoanalytics.videologistics.activity;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.client.KvsClient.KvsClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.KvsClient.KvsClientWrapper;
import com.amazonaws.videoanalytics.videologistics.client.KvsSignalingClient.KvsSignalingClientFactory;
import com.amazonaws.videoanalytics.videologistics.client.KvsSignalingClient.KvsSignalingClientWrapper;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dao.LivestreamSessionDAO;
import com.amazonaws.videoanalytics.videologistics.utils.DateTime;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;
import com.amazonaws.videoanalytics.videologistics.CreateLivestreamSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.IceServer;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.kinesisvideo.model.ChannelRole;
import software.amazon.awssdk.services.kinesisvideo.model.SingleMasterChannelEndpointConfiguration;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.amazonaws.videoanalytics.videologistics.exceptions.GuidanceExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.schema.util.GuidanceVLConstants.REGION_NAME;
import static com.amazonaws.videoanalytics.videologistics.schema.util.ResourceNameConversionUtils.getLivestreamSignalingChannelNameFromDeviceId;

import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parsePathParameter;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;
import static software.amazon.awssdk.services.kinesisvideo.model.ChannelProtocol.HTTPS;
import static software.amazon.awssdk.services.kinesisvideo.model.ChannelProtocol.WSS;

/**
 * Class for handling the request for CreateLivestreamSession API.
 */
public class CreateLivestreamSessionActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final Logger LOG = LogManager.getLogger(CreateLivestreamSessionActivity.class);

    private final GuidanceUUIDGenerator guidanceUUIDGenerator;

    private final LivestreamSessionDAO livestreamSessionDAO;

    private final DateTime dateTime;

    private final DeviceValidator deviceValidator;
    private final KvsClientFactory kvsClientFactory;
    private final KvsSignalingClientFactory kvsSignalingClientFactory;
    private final KVSWebRTCUtils kvsWebRTCUtils;
    private final String region;

    @Inject
    public CreateLivestreamSessionActivity(final GuidanceUUIDGenerator guidanceUUIDGenerator,
                                           final LivestreamSessionDAO livestreamSessionDAO,
                                           final DateTime dateTime,
                                           final KvsClientFactory kvsClientFactory,
                                           final KvsSignalingClientFactory kvsSignalingClientFactory,
                                           final DeviceValidator deviceValidator,
                                           final KVSWebRTCUtils kvsWebRTCUtils,
                                           @Named(REGION_NAME) final String region) {
        this.guidanceUUIDGenerator = guidanceUUIDGenerator;
        this.livestreamSessionDAO = livestreamSessionDAO;
        this.kvsClientFactory = kvsClientFactory;
        this.kvsSignalingClientFactory = kvsSignalingClientFactory;
        this.dateTime = dateTime;
        this.deviceValidator = deviceValidator;
        this.kvsWebRTCUtils = kvsWebRTCUtils;
        this.region = region;
    }

    @ExcludeFromJacocoGeneratedReport
    public CreateLivestreamSessionActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.guidanceUUIDGenerator = component.getGuidanceUUIDGenerator();
        this.livestreamSessionDAO = component.getLivestreamSessionDAO();
        this.kvsClientFactory = component.getKvsClientFactory();
        this.kvsSignalingClientFactory = component.getKvsSignalingClientFactory();
        this.dateTime = component.getDateTime();
        this.deviceValidator = component.getDeviceValidator();
        this.kvsWebRTCUtils = component.getKVSWebRTCUtils();
        this.region = String.valueOf(component.getRegion());
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LOG.debug("Entered createLivestreamSessionRequest method");
        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        if (Objects.isNull(input)) {
            return serializeResponse(400, exception.toJson());
        }
        String deviceId;
        try {
            deviceId = parsePathParameter(input, "deviceId");
        } catch (Exception e) {
            LOG.error(e.toString());
            return serializeResponse(400, exception.toJson());
        }

        String clientId;
        try {
            clientId = parsePathParameter(input, "clientId");
        } catch (Exception e) {
            LOG.error(e.toString());
            return serializeResponse(400, exception.toJson());
        }
        deviceValidator.validateDeviceExists(deviceId);

        // Fetch signaling channel endpoint for viewer
        final SingleMasterChannelEndpointConfiguration singleMasterChannelEndpointConfigurationAsViewer =
                SingleMasterChannelEndpointConfiguration
                        .builder()
                        .protocols(Arrays.asList(WSS, HTTPS))
                        .role(ChannelRole.VIEWER)
                        .build();

        final String signalingChannelName = getLivestreamSignalingChannelNameFromDeviceId(deviceId);
        final AwsCredentialsProvider awsCredentialsProvider = DefaultCredentialsProvider.builder().build();

        final KvsClientWrapper kvsClientWrapper = kvsClientFactory.create(awsCredentialsProvider);

        final String channelArn = kvsClientWrapper.getSignalingChannelArnFromName(signalingChannelName);

        final Map<String, String> signalingChannelMapForViewer = kvsClientWrapper
                .getSignalingChannelEndpoint(channelArn, singleMasterChannelEndpointConfigurationAsViewer, deviceId);

        // Presign the URL
        final String presignedUrl = kvsWebRTCUtils.sign(
                signalingChannelMapForViewer.get(WSS.toString()),
                channelArn,
                clientId,
                region,
                awsCredentialsProvider);

        // TURN server logic
        final KvsSignalingClientWrapper kvsSignalingClientWrapperForViewer = kvsSignalingClientFactory.create(
                signalingChannelMapForViewer.get(HTTPS.toString()),
                awsCredentialsProvider
        );
        final List<IceServer> iceServerList = kvsSignalingClientWrapperForViewer.getSyncIceServerConfigs(channelArn, deviceId);

        final CreateLivestreamSessionResponseContent response = CreateLivestreamSessionResponseContent
                .builder()
                .clientId(clientId)
                .signalingChannelURL(presignedUrl)
                .iceServers(iceServerList)
                .build();

        return serializeResponse(200, response);
    }

}
