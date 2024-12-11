package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.ListVideoTimelinesRequestContent;
import com.amazonaws.videoanalytics.videologistics.ListVideoTimelinesResponseContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dao.videotimeline.VideoTimelineDAO;
import com.amazonaws.videoanalytics.videologistics.timeline.VideoTimelineUtils;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.inject.Inject;
import java.util.Map;

import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT;

public class ListVideoTimelinesActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final Logger LOG = LogManager.getLogger(ListVideoTimelinesActivity.class);
    private final VideoTimelineDAO videoTimelineDAO;
    private final VideoTimelineUtils videoTimelineUtils;

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

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LOG.info("Entered ListVideoTimelines method");

        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT)
                .build();

        if (input == null) {
            return serializeResponse(400, exception.toJson());
        }

        try {
            ListVideoTimelinesRequestContent request = ListVideoTimelinesRequestContent.fromJson(parseBody(input));

            ListVideoTimelinesResponseContent response = ListVideoTimelinesResponseContent.builder()
                .deviceId(request.getDeviceId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timeIncrement(request.getTimeIncrement())
                .build();

            return serializeResponse(200, response.toJson());
        } catch (Exception e) {
            LOG.error("Error processing ListVideoTimelines request", e);
            return serializeResponse(500, e.getMessage());
        }
    }
}