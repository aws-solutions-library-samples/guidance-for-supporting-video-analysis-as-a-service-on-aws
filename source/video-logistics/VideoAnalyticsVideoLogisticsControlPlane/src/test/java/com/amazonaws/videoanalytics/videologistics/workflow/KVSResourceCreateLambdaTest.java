package com.amazonaws.videoanalytics.videologistics.workflow;

import com.amazonaws.videoanalytics.videologistics.dao.VLRegisterDeviceJobDAO;
import com.amazonaws.videoanalytics.videologistics.schema.VLRegisterDeviceJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.*;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.amazonaws.videoanalytics.videologistics.schema.SchemaConst.JOB_ID;

@ExtendWith(MockitoExtension.class)
public class KVSResourceCreateLambdaTest {

    @Mock
    private VLRegisterDeviceJobDAO vlRegisterDeviceJobDAO;

    @Mock
    private KinesisVideoClient kinesisVideoClient;

    private KVSResourceCreateLambda lambda;

    private static final String TEST_JOB_ID = "test-job-id";
    private static final String TEST_DEVICE_ID = "test-device-id";
    private static final String TEST_STREAM_ARN = "test-stream-arn";
    private static final String TEST_CHANNEL_ARN = "test-channel-arn";

    @BeforeEach
    void setUp() {
        lambda = new KVSResourceCreateLambda(vlRegisterDeviceJobDAO, kinesisVideoClient);
    }

    @Test
    void testHandleRequest_Success() {

        Map<String, Object> input = new HashMap<>();
        input.put(JOB_ID, TEST_JOB_ID);

        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(TEST_JOB_ID)
                .deviceId(TEST_DEVICE_ID)
                .build();

        when(vlRegisterDeviceJobDAO.load(TEST_JOB_ID)).thenReturn(job);

        CreateStreamResponse createStreamResponse = CreateStreamResponse.builder()
                .streamARN(TEST_STREAM_ARN)
                .build();
        when(kinesisVideoClient.createStream(any(CreateStreamRequest.class)))
                .thenReturn(createStreamResponse);

        CreateSignalingChannelResponse createSignalingChannelResponse = CreateSignalingChannelResponse.builder()
                .channelARN(TEST_CHANNEL_ARN)
                .build();
        when(kinesisVideoClient.createSignalingChannel(any(CreateSignalingChannelRequest.class)))
                .thenReturn(createSignalingChannelResponse);


        Map<String, Object> result = lambda.handleRequest(input, null);
        verify(vlRegisterDeviceJobDAO).load(TEST_JOB_ID);
        verify(kinesisVideoClient).createStream(any(CreateStreamRequest.class));
        verify(kinesisVideoClient, times(2)).createSignalingChannel(any(CreateSignalingChannelRequest.class));
        verify(vlRegisterDeviceJobDAO).save(argThat(savedJob -> 
            savedJob.getKvsStreamArn().equals(TEST_STREAM_ARN) &&
            savedJob.getPlaybackSignalingChannelArn().equals(TEST_CHANNEL_ARN) &&
            savedJob.getLiveStreamSignalingChannelArn().equals(TEST_CHANNEL_ARN) &&
            savedJob.getStatus().equals("COMPLETED")
        ));
        
        assertNotNull(result);
    }

    @Test
    void testHandleRequest_ResourceInUseException() {
        Map<String, Object> input = new HashMap<>();
        input.put(JOB_ID, TEST_JOB_ID);

        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(TEST_JOB_ID)
                .deviceId(TEST_DEVICE_ID)
                .build();

        when(vlRegisterDeviceJobDAO.load(TEST_JOB_ID)).thenReturn(job);

                            
        when(kinesisVideoClient.createStream(any(CreateStreamRequest.class)))
                .thenThrow(ResourceInUseException.class);
        
        DescribeStreamResponse describeStreamResponse = DescribeStreamResponse.builder()
                .streamInfo(StreamInfo.builder().streamARN(TEST_STREAM_ARN).build())
                .build();
        when(kinesisVideoClient.describeStream(any(DescribeStreamRequest.class)))
                .thenReturn(describeStreamResponse);

        when(kinesisVideoClient.createSignalingChannel(any(CreateSignalingChannelRequest.class)))
                .thenThrow(ResourceInUseException.class);
        
        DescribeSignalingChannelResponse describeSignalingChannelResponse = DescribeSignalingChannelResponse.builder()
                .channelInfo(ChannelInfo.builder().channelARN(TEST_CHANNEL_ARN).build())
                .build();
        when(kinesisVideoClient.describeSignalingChannel(any(DescribeSignalingChannelRequest.class)))
                .thenReturn(describeSignalingChannelResponse);

        Map<String, Object> result = lambda.handleRequest(input, null);

        verify(vlRegisterDeviceJobDAO).load(TEST_JOB_ID);
        verify(kinesisVideoClient).createStream(any(CreateStreamRequest.class));
        verify(kinesisVideoClient).describeStream(any(DescribeStreamRequest.class));
        verify(kinesisVideoClient, times(2)).createSignalingChannel(any(CreateSignalingChannelRequest.class));
        verify(kinesisVideoClient, times(2)).describeSignalingChannel(any(DescribeSignalingChannelRequest.class));
        verify(vlRegisterDeviceJobDAO).save(argThat(savedJob -> 
            savedJob.getKvsStreamArn().equals(TEST_STREAM_ARN) &&
            savedJob.getPlaybackSignalingChannelArn().equals(TEST_CHANNEL_ARN) &&
            savedJob.getLiveStreamSignalingChannelArn().equals(TEST_CHANNEL_ARN) &&
            savedJob.getStatus().equals("COMPLETED")
        ));
        
        assertNotNull(result);
    }

    @Test
    void testHandleRequest_Error() {
        Map<String, Object> input = new HashMap<>();
        input.put(JOB_ID, TEST_JOB_ID);

        VLRegisterDeviceJob job = VLRegisterDeviceJob.builder()
                .jobId(TEST_JOB_ID)
                .deviceId(TEST_DEVICE_ID)
                .build();

        when(vlRegisterDeviceJobDAO.load(TEST_JOB_ID)).thenReturn(job);
        // Return null for createSignalingChannel to trigger the NPE
        when(kinesisVideoClient.createSignalingChannel(any(CreateSignalingChannelRequest.class)))
                .thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> 
            lambda.handleRequest(input, null)
        );
        assertEquals(
            "Cannot invoke \"software.amazon.awssdk.services.kinesisvideo.model.CreateSignalingChannelResponse.channelARN()\" because \"response\" is null",
            exception.getMessage()
        );

        verify(vlRegisterDeviceJobDAO).save(argThat(savedJob -> {
            boolean statusMatch = "FAILED".equals(savedJob.getStatus());
            boolean errorMatch = exception.getMessage().equals(savedJob.getErrorMessage());
            boolean jobIdMatch = TEST_JOB_ID.equals(savedJob.getJobId());
            boolean deviceIdMatch = TEST_DEVICE_ID.equals(savedJob.getDeviceId());
            
            return statusMatch && errorMatch && jobIdMatch && deviceIdMatch;
        }));
    }
}
