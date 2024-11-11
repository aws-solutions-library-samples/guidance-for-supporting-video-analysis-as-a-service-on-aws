package com.amazonaws.videoanalytics.videologistics.validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

public class DeviceValidator {
    private static final Logger LOG = LogManager.getLogger(DeviceValidator.class);

    @Inject
    public DeviceValidator() {
    }

    public void validateDeviceExists(final String deviceId) {
        LOG.info("Received request to validate if device {} exists.", deviceId);
        // TODO: This class relies on GetDeviceInternal which is deprecated for Guidance Solution.
        // TODO: Complete the method once we have GetDevice API ready and client ready for DM side.
//        if (isBlank(deviceId)) {
//            throw new ValidationException(INVALID_VALIDATION_INPUT);
//        }
//        final GetDeviceInternalRequest deviceInternalRequest = GetDeviceInternalRequest
//                .builder()
//                .deviceId(deviceId)
//                .build();
//        try {
//            guidanceClient.getDeviceInternal(deviceInternalRequest);
//        } catch (ResourceNotFoundException e){
//            throw new com.amazonaws.videoanalytics.videologisticsResourceNotFoundException(RESOURCE_NOT_FOUND);
//        }
    }
}
