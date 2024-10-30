package com.amazonaws.videoanalytics.utils;

import com.amazonaws.videoanalytics.DeviceState;
import org.apache.commons.lang3.EnumUtils;
import software.amazon.awssdk.services.iot.model.GroupNameAndArn;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        public static Optional<VideoAnalyticsManagedDeviceGroupId> from(String deviceState) {
            return Arrays.stream(VideoAnalyticsManagedDeviceGroupId.values())
                    .filter(validMessages -> validMessages.videoAnalyticsManagedDeviceState.equals(deviceState))
                    .findFirst();
        }
    }

    public static boolean isSpecialThingGroupsExist(List<GroupNameAndArn> thingGroups) {
        List<String> deviceStatesFromThingGroup = thingGroups.stream()
                .map(GroupNameAndArn::groupName)
                .filter(groupName -> EnumUtils.isValidEnum(UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId.class, groupName))
                .collect(Collectors.toList());
        return deviceStatesFromThingGroup.size() == Arrays.asList(VideoAnalyticsManagedDeviceGroupId.values()).size();
    }

    private UpdateDeviceUtils() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}
