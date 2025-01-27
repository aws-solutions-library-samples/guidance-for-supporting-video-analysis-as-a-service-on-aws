package com.amazonaws.videoanalytics.videologistics.validator;

import static software.amazon.awssdk.utils.StringUtils.isBlank;

import javax.inject.Inject;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.videoanalytics.videologistics.dependency.apig.ApigService;

import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;

public class DeviceValidator {
    private final ApigService apigService;

    @Inject
    public DeviceValidator(ApigService apigService) {
        this.apigService = apigService;
    }

    public boolean validateDeviceExists(final String deviceId, LambdaLogger logger) {
        logger.log(String.format("Received request to validate if device %s exists.", deviceId));
        if (isBlank(deviceId)) {
            return false;
        }
        try {
            // Invoke get-device to check if device exists
            HttpExecuteResponse response = apigService.invokeGetDevice(
                deviceId,
                null,  // headers 
                null   // body 
            );

            // Close response InputStream
            AbortableInputStream abortableStream = response.responseBody().get();
            abortableStream.delegate().close();
            abortableStream.abort();
            
            return response.httpResponse().isSuccessful();
        }
        catch (Exception e) {
            logger.log(e.getMessage());
            return false;
        }
    }
}
