package com.amazonaws.videoanalytics.devicemanagement.utils;

import com.amazonaws.videoanalytics.devicemanagement.DeviceState;

public class UpdateDeviceUtils {

    public enum VideoAnalyticsManagedDeviceGroupId {
        SpecialGroup_CreatedState(DeviceState.CREATED.toString()),
        SpecialGroup_EnabledState(DeviceState.ENABLED.toString()),
        SpecialGroup_DisabledState(DeviceState.DISABLED.toString());

        private String videoAnalyticsManagedDeviceState;

        VideoAnalyticsManagedDeviceGroupId(String deviceState) {
            this.videoAnalyticsManagedDeviceState = deviceState;
        }

        public String getVideoAnalyticsManagedDeviceState() {
            return this.videoAnalyticsManagedDeviceState;
        }
    }

    private UpdateDeviceUtils() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
