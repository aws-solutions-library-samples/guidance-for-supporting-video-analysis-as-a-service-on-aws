package com.amazonaws.videoanalytics.devicemanagement.workflow.data;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;


@Data
@Builder
public class CreateDeviceData {
    private String jobId;
    private String deviceId;
    private String status;
    private Instant lastUpdatedTime;
    private Instant createTime;
    private String errorMessage;
}
