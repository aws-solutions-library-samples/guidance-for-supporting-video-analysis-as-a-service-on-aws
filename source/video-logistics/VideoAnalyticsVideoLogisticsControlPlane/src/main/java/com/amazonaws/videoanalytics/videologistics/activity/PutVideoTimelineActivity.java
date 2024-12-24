package com.amazonaws.videoanalytics.videologistics.activity;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.parseBodyMap;
import static com.amazonaws.videoanalytics.videologistics.utils.LambdaProxyUtils.serializeResponse;

import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.videoanalytics.videologistics.PutVideoTimelineRequestContent;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.timeline.PutVideoTimelineHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        LambdaLogger logger = context.getLogger();
        logger.log(String.format("Entered PutVideoTimeline method with input: %s", input));
        
        ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(INVALID_INPUT)
                .build();

        if (input == null) {
            LOG.error("Received null input");
            return serializeResponse(400, exception.toJson());
        }

        try {
            String body = parseBodyMap(input);
            logger.log(String.format("Parsed request body: %s", body));
            
            PutVideoTimelineRequestContent request = PutVideoTimelineRequestContent.fromJson(body);
            logger.log(String.format("Parsed request content: %s", request));
            
            String timestampsJson = objectMapper.writeValueAsString(request.getTimestamps());
            logger.log(String.format("Timestamps JSON: %s", timestampsJson));
            
            String locationString = request.getLocation().toString();
            logger.log(String.format("Location: %s", locationString));
            
            putVideoTimelineHandler.addVideoTimelines(
                request.getDeviceId(),
                timestampsJson,
                locationString
            );
            
            return serializeResponse(200, "%s");
        } catch (Exception e) {
            LOG.error("Failed to process request", e);
            return serializeResponse(400, exception.toJson());
        }
    }

}

