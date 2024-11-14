package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.CreatePlaybackSessionRequestContent;
import com.amazonaws.videoanalytics.videologistics.CreatePlaybackSessionResponseContent;
import com.amazonaws.videoanalytics.videologistics.Status;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.schema.PlaybackSession.PlaybackSession;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.amazonaws.videoanalytics.videologistics.utils.GuidanceUUIDGenerator;
import com.amazonaws.videoanalytics.videologistics.validator.DeviceValidator;

import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dao.PlaybackSessionDAO;

import javax.inject.Inject;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.END_TIME_WITHIN_A_DAY;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.START_TIME_GREATER_THAN_OR_EQUAL_TO_END_TIME;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.MILLIS_TO_HOURS;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

/**
 * Class for handling the request for CreatePlaybackSession API.
 */
public class CreatePlaybackSessionActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private final DeviceValidator deviceValidator;
    private final PlaybackSessionDAO playbackSessionDAO;
    private final GuidanceUUIDGenerator guidanceUUIDGenerator;

    @Inject
    public CreatePlaybackSessionActivity(final DeviceValidator deviceValidator,
                                         final PlaybackSessionDAO playbackSessionDAO,
                                         final GuidanceUUIDGenerator guidanceUUIDGenerator) {
        this.deviceValidator = deviceValidator;
        this.playbackSessionDAO = playbackSessionDAO;
        this.guidanceUUIDGenerator = guidanceUUIDGenerator;
    }

    public CreatePlaybackSessionActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.deviceValidator = component.getDeviceValidator();
        this.playbackSessionDAO = component.getPlaybackSessionDAO();
        this.guidanceUUIDGenerator = component.getGuidanceUUIDGenerator();
    }

    // used for unit tests
    @ExcludeFromJacocoGeneratedReport
    public void assertPrivateFieldNotNull() {
        if (deviceValidator == null || playbackSessionDAO == null || guidanceUUIDGenerator == null) {
            throw new AssertionError("private field is null");
        }
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Entered CreatePlaybackSession method");

        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT_EXCEPTION)
                .build();

        if (Objects.isNull(input)) {
            return serializeResponse(400, exception.toJson());
        }

        String deviceId;
        Date startTime;
        Date endTime;
        try {
            CreatePlaybackSessionRequestContent request = CreatePlaybackSessionRequestContent.fromJson(parseBody(input));
            deviceId = request.getDeviceId();
            startTime = request.getStartTime();
            endTime = request.getEndTime();
        } catch (Exception e) {
            logger.log(e.toString());
            return serializeResponse(400, exception.toJson());
        }

        deviceValidator.validateDeviceExists(deviceId);

        if (startTime.after(endTime) || startTime.equals(endTime)) {
            exception = ValidationExceptionResponseContent.builder()
                .message(START_TIME_GREATER_THAN_OR_EQUAL_TO_END_TIME)
                .build();
            return serializeResponse(400, exception.toJson());
        }

        if (timeDifferenceIsGreaterThanADay(startTime, endTime)) {
            exception = ValidationExceptionResponseContent.builder()
                .message(END_TIME_WITHIN_A_DAY)
                .build();
            return serializeResponse(400, exception.toJson());
        }

        PlaybackSession playbackSession = PlaybackSession.builder()
                .deviceId(deviceId)
                .endTime(endTime)
                .sessionStatus(Status.RUNNING.toString())
                .startTime(startTime)
                .sessionId(guidanceUUIDGenerator.generateUUIDRandom())
                .workflowName(guidanceUUIDGenerator.generateUUIDRandom())
                .build();
    
        playbackSessionDAO.save(playbackSession);

        CreatePlaybackSessionResponseContent response = CreatePlaybackSessionResponseContent
                .builder()
                .sessionId(playbackSession.getSessionId())
                .build();

        return serializeResponse(200, response.toJson());
    }

    private Boolean timeDifferenceIsGreaterThanADay(final Date startTime,
                                                    final Date endTime) {
        long diffInMillis = Math.abs(startTime.getTime() - endTime.getTime());
        return diffInMillis / MILLIS_TO_HOURS >= 24;
    }
}
