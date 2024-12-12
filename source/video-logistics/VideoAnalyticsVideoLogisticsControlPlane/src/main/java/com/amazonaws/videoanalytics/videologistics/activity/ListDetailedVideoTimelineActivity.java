package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.videoanalytics.videologistics.Timeline;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.PaginatedListResponse;
import com.amazonaws.videoanalytics.videologistics.DetailedVideoTimeline;
import com.amazonaws.videoanalytics.videologistics.ListDetailedVideoTimelineRequestContent;
import com.amazonaws.videoanalytics.videologistics.ListDetailedVideoTimelineResponseContent;
import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.DetailedTimelinePaginatedResponse;
import com.amazonaws.videoanalytics.videologistics.timeline.DetailedVideoTimelineGenerator;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseRequestBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;
import javax.inject.Inject;
import java.util.Map;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.TIME_CHRONOLOGY_MISMATCH;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT;

public class ListDetailedVideoTimelineActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final Logger LOG = LogManager.getLogger(ListDetailedVideoTimelineActivity.class);
    private final DetailedVideoTimelineGenerator detailedVideoTimelineGenerator;

    @Inject
    public ListDetailedVideoTimelineActivity(DetailedVideoTimelineGenerator detailedVideoTimelineGenerator) {
        this.detailedVideoTimelineGenerator = detailedVideoTimelineGenerator;
    }

    public ListDetailedVideoTimelineActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.detailedVideoTimelineGenerator = component.getDetailedVideoTimelineGenerator();
    }

    private Map<String, Object> createErrorResponse(String errorMessage) {
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(errorMessage)
                .build();
        return serializeResponse(400, exception.toJson());
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LOG.info("Entered ListDetailedVideoTimeline method");

        if (input == null) {
            return createErrorResponse(INVALID_INPUT);
        }

        try {
            ListDetailedVideoTimelineRequestContent request = parseRequestBody(input, ListDetailedVideoTimelineRequestContent.class);

            Long startTimeInMillis = request.getStartTime().longValue();
            Long endTimeInMillis = request.getEndTime().longValue();
            String deviceId = request.getDeviceId();

            // validate time
            if(endTimeInMillis <= startTimeInMillis) {
                return createErrorResponse(TIME_CHRONOLOGY_MISMATCH);
            }

            DetailedTimelinePaginatedResponse timelinePaginatedResponse =
                detailedVideoTimelineGenerator.getDetailedVideoTimeLine(deviceId,
                        startTimeInMillis,
                        endTimeInMillis,
                        request.getNextToken());

            DetailedVideoTimeline detailedVideoTimeline = DetailedVideoTimeline.builder()
                .device(timelinePaginatedResponse.getDeviceTimeline())
                .cloud(timelinePaginatedResponse.getCloudTimeline())
                .build();

            ListDetailedVideoTimelineResponseContent response = ListDetailedVideoTimelineResponseContent.builder()
                .deviceId(deviceId)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .detailedVideoTimeline(detailedVideoTimeline)
                .nextToken(timelinePaginatedResponse.getNextToken())
                .build();

            return serializeResponse(200, response.toJson());

        } catch (Exception e) {
            LOG.error("Error processing ListDetailedVideoTimelines request", e);
            return serializeResponse(500, e.getMessage());
        }

    }

}
