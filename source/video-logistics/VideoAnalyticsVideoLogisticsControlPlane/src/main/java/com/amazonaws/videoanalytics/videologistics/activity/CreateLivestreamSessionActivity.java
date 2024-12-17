package com.amazonaws.videoanalytics.videologistics.activity;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.RESOURCE_NOT_FOUND;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;
import static com.amazonaws.videoanalytics.videologistics.utils.ResourceNameConversionUtils.getLivestreamSignalingChannelNameFromDeviceId;
import static software.amazon.awssdk.services.kinesisvideo.model.ChannelProtocol.HTTPS;
import static software.amazon.awssdk.services.kinesisvideo.model.ChannelProtocol.WSS;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.CreateLivestreamSessionRequestContent;
import com.amazonaws.videoanalytics.videologistics.CreateLivestreamSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.IceServer;
import com.amazonaws.videoanalytics.videologistics.InternalServerExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ResourceNotFoundExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dependency.kvs.KvsService;
import com.amazonaws.videoanalytics.videologistics.exceptions.ExceptionTranslator;
import com.amazonaws.videoanalytics.videologistics.utils.KVSWebRTCUtils;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;

import software.amazon.awssdk.services.kinesisvideo.model.ChannelRole;
import software.amazon.awssdk.services.kinesisvideo.model.KinesisVideoException;
import software.amazon.awssdk.services.kinesisvideo.model.SingleMasterChannelEndpointConfiguration;
import software.amazon.awssdk.services.kinesisvideosignaling.model.KinesisVideoSignalingException;

/**
 * Class for handling the request for CreateLivestreamSession API.
 */
public class CreateLivestreamSessionActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DeviceValidator deviceValidator;
    private final KvsService kvsService;
    private final KVSWebRTCUtils kvsWebRTCUtils;

    @Inject
    public CreateLivestreamSessionActivity(final KvsService kvsService,
                                           final DeviceValidator deviceValidator,
                                           final KVSWebRTCUtils kvsWebRTCUtils) {
        this.kvsService = kvsService;
        this.deviceValidator = deviceValidator;
        this.kvsWebRTCUtils = kvsWebRTCUtils;
    }

    public CreateLivestreamSessionActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.kvsService = component.getKvsService();
        this.deviceValidator = component.getDeviceValidator();
        this.kvsWebRTCUtils = component.getKVSWebRTCUtils();
    }

    // used for unit tests
    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (kvsService == null || deviceValidator == null || kvsWebRTCUtils == null) {
            throw new AssertionError("private field is null");
        }
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered CreateLivestreamSession method");

        final ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .reason(ValidationExceptionReason.FIELD_VALIDATION_FAILED)
                .build();

        if (Objects.isNull(input)) {
            return serializeResponse(400, exception.toJson());
        }

        String deviceId;
        String clientId;
        try {
            CreateLivestreamSessionRequestContent request = CreateLivestreamSessionRequestContent.fromJson(parseBody(input));
            deviceId = request.getDeviceId();
            clientId = request.getClientId();
        } catch (Exception e) {
            logger.log(e.toString());
            return serializeResponse(400, exception.toJson());
        }

        if (!deviceValidator.validateDeviceExists(deviceId, logger)) {
            ResourceNotFoundExceptionResponseContent resourceNotFoundException = ResourceNotFoundExceptionResponseContent.builder()
                .message(RESOURCE_NOT_FOUND)
                .build();
            return serializeResponse(404, resourceNotFoundException.toJson());
        }

        // Fetch signaling channel endpoint for viewer
        final SingleMasterChannelEndpointConfiguration singleMasterChannelEndpointConfigurationAsViewer =
                SingleMasterChannelEndpointConfiguration
                        .builder()
                        .protocols(Arrays.asList(WSS, HTTPS))
                        .role(ChannelRole.VIEWER)
                        .build();

        String channelArn;
        Map<String, String> signalingChannelMapForViewer;
        String presignedUrl;
        List<IceServer> iceServerList;
        try {
            final String signalingChannelName = getLivestreamSignalingChannelNameFromDeviceId(deviceId);

            channelArn = kvsService.getSignalingChannelArnFromName(signalingChannelName);

            signalingChannelMapForViewer = kvsService
                    .getSignalingChannelEndpoint(channelArn, singleMasterChannelEndpointConfigurationAsViewer);
        } catch (KinesisVideoException e) {
            logger.log(e.toString());
            return ExceptionTranslator.translateKvsExceptionToLambdaResponse(e);
        }

        try {
            // Presign the URL
            presignedUrl = kvsWebRTCUtils.sign(
                    signalingChannelMapForViewer.get(WSS.toString()),
                    channelArn,
                    clientId);
        } catch (RuntimeException e) {
            logger.log(e.toString());
            InternalServerExceptionResponseContent internalServerException = InternalServerExceptionResponseContent.builder()
                    .message(INTERNAL_SERVER_EXCEPTION)
                    .build();
            return serializeResponse(500, internalServerException.toJson());
        }

        try {
            // TURN server logic
            iceServerList = kvsService.getSyncIceServerConfigs(
                signalingChannelMapForViewer.get(HTTPS.toString()),
                channelArn
            );
        } catch (KinesisVideoSignalingException e) {
            logger.log(e.toString());
            return ExceptionTranslator.translateKvsExceptionToLambdaResponse(e);
        }

        final CreateLivestreamSessionResponseContent response = CreateLivestreamSessionResponseContent
                .builder()
                .clientId(clientId)
                .signalingChannelURL(presignedUrl)
                .iceServers(iceServerList)
                .build();

        return serializeResponse(200, response.toJson());
    }
}
