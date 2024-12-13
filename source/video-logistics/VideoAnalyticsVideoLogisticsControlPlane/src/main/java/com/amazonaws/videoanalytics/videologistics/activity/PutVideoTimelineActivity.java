package com.amazonaws.videoanalytics.videologistics.activity;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.PutVideoTimelineRequestContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.timeline.PutVideoTimelineHandler;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import java.util.Map;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBody;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT;

public class PutVideoTimelineActivity implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private static final Logger LOG = LogManager.getLogger(PutVideoTimelineActivity.class);
    private final PutVideoTimelineHandler putVideoTimelineHandler;
    private final ObjectMapper objectMapper;

    @Inject
    public PutVideoTimelineActivity(
        PutVideoTimelineHandler putVideoTimelineHandler,
        ObjectMapper objectMapper) {
        this.putVideoTimelineHandler = putVideoTimelineHandler;
        this.objectMapper = objectMapper;
    }

    public PutVideoTimelineActivity() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        this.putVideoTimelineHandler = component.getPutVideoTimelineHandler();
        this.objectMapper = component.getObjectMapper();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        LOG.info("Entered PutVideoTimeline method with input: {}", input);
        
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT)
                .build();

        if (input == null) {
            LOG.error("Received null input");
            return serializeResponse(400, exception.toJson());
        }

        try {
            String body = parseBody(input);
            LOG.info("Parsed request body: {}", body);
            
            PutVideoTimelineRequestContent request = PutVideoTimelineRequestContent.fromJson(body);
            LOG.info("Parsed request content: {}", request);
            
            String timestampsJson = objectMapper.writeValueAsString(request.getTimestamps());
            LOG.info("Timestamps JSON: {}", timestampsJson);
            
            String locationString = request.getLocation().toString();
            LOG.info("Location: {}", locationString);
            
            putVideoTimelineHandler.addVideoTimelines(
                request.getDeviceId(),
                timestampsJson,
                locationString
            );
            
            return serializeResponse(200, "{}");
        } catch (Exception e) {
            LOG.error("Failed to process request", e);
            return serializeResponse(400, exception.toJson());
        }
    }

}

