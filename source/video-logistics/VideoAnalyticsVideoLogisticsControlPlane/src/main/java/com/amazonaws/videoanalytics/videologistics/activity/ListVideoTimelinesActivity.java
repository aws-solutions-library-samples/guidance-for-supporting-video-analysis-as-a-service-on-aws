package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.ListVideoTimelinesRequestContent;
import com.amazonaws.videoanalytics.videologistics.ListVideoTimelinesResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.inject.Inject;
import java.util.Map;
import java.time.Duration;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseRequestBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.SECONDS_INCREMENT_ERROR;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.TIME_CHRONOLOGY_MISMATCH;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.TIME_UNIT_ERROR;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.SECONDS_UNIT_ERROR;
import java.util.List;
import com.amazonaws.videoanalytics.videologistics.VideoTimeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import java.math.BigDecimal;

public class ListVideoTimelinesActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final Logger LOG = LogManager.getLogger(ListVideoTimelinesActivity.class);
    private final VideoTimelineDAO videoTimelineDAO;
    private final VideoTimelineUtils videoTimelineUtils;
    public static final Integer HOURS_IN_DAY = 24;

    @Inject
    public ListVideoTimelinesActivity(
        final VideoTimelineDAO videoTimelineDAO,
        final VideoTimelineUtils videoTimelineUtils) {
        this.videoTimelineDAO = videoTimelineDAO;
        this.videoTimelineUtils = videoTimelineUtils;
    }

    public ListVideoTimelinesActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.videoTimelineDAO = component.getVideoTimelineDAO();
        this.videoTimelineUtils = component.getVideoTimelineUtils();
    }

    private Map<String, Object> createErrorResponse(String errorMessage) {
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(errorMessage)
                .build();
        return serializeResponse(400, exception.toJson());
    }

    private Map<String, Object> validateTime(TimeIncrementUnits timeIncrementUnits, Long startTime, Long endTime) {
        if(endTime <= startTime) {
            return createErrorResponse(TIME_CHRONOLOGY_MISMATCH);
        }

        if(!(videoTimelineUtils.getUnitTime(timeIncrementUnits, startTime).equals(startTime) &&
                videoTimelineUtils.getUnitTime(timeIncrementUnits, endTime).equals(endTime))) {
            String validationError = String.format(TIME_UNIT_ERROR, timeIncrementUnits.name().toLowerCase());
            if(timeIncrementUnits.equals(TimeIncrementUnits.SECONDS)) {
                validationError = SECONDS_UNIT_ERROR;
            }
            return createErrorResponse(validationError);
        }
        return null;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LOG.info("Entered ListVideoTimelines method");

        if (input == null) {
            return createErrorResponse(INVALID_INPUT);
        }

        try {
            ListVideoTimelinesRequestContent request = parseRequestBody(input, ListVideoTimelinesRequestContent.class);

            Long startTimeInMillis = request.getStartTime().longValue();
            Long endTimeInMillis = request.getEndTime().longValue();
            String deviceId = request.getDeviceId();
            TimeIncrementUnits timeIncrementUnits = TimeIncrementUnits.valueOf(request.getTimeIncrementUnits().name());
            Integer timeIncrement = request.getTimeIncrement().intValue();
            if(timeIncrementUnits.equals(TimeIncrementUnits.DAYS)) {
                timeIncrementUnits = TimeIncrementUnits.HOURS;
                timeIncrement = HOURS_IN_DAY*timeIncrement;
            }

            Map<String, Object> validationResult = validateTime(timeIncrementUnits, startTimeInMillis, endTimeInMillis);
            if (validationResult != null) {
                return validationResult;
            }

            if(timeIncrementUnits.equals(TimeIncrementUnits.SECONDS) && timeIncrement % 5 != 0) {
                return createErrorResponse(String.format(SECONDS_INCREMENT_ERROR, timeIncrement));
            }

            // convert given time increment to milliseconds using schema version
            Long timeIncrementInMillis = Duration.of(timeIncrement, timeIncrementUnits.getChronoUnit()).toMillis();

            PaginatedListResponse<VideoTimeline> paginatedListTimelineResponse =
                videoTimelineDAO.listVideoTimelines(deviceId,
                        startTimeInMillis,
                        endTimeInMillis,
                        timeIncrementInMillis,
                        timeIncrementUnits,
                        request.getNextToken());

            List<VideoTimeline> videoTimelineList = paginatedListTimelineResponse.getResults();
            String nextToken = paginatedListTimelineResponse.getNextToken();

            ListVideoTimelinesResponseContent response = ListVideoTimelinesResponseContent.builder()
                .deviceId(deviceId)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timeIncrement(BigDecimal.valueOf(timeIncrement))
                .timeIncrementUnits(request.getTimeIncrementUnits())
                .videoTimelines(videoTimelineList)
                .nextToken(nextToken)
                .build();
            return serializeResponse(200, response.toJson());
        } catch (Exception e) {
            LOG.error("Error processing ListVideoTimelines request", e);
            return serializeResponse(500, e.getMessage());
        }
    }
}
