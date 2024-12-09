package com.amazonaws.videoanalytics.videologistics.timeline;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoTimeline {
    private Float cloudDensity = 0F;
    private Float deviceDensity = 0F;

    public static VideoTimelineBuilder builder() {
        return new VideoTimelineBuilder();
    }

    public static class VideoTimelineBuilder {
        private Float cloudDensity = 0F;
        private Float deviceDensity = 0F;

        public VideoTimelineBuilder withCloudDensity(Float cloudDensity) {
            this.cloudDensity = cloudDensity;
            return this;
        }

        public VideoTimelineBuilder withDeviceDensity(Float deviceDensity) {
            this.deviceDensity = deviceDensity;
            return this;
        }

        public VideoTimeline build() {
            return new VideoTimeline(cloudDensity, deviceDensity);
        }
    }
}