
package org.example;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.regions.Region;

/**
 * The module containing all dependencies required by the {@link Handler}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of IotClient
     */
    public static IotClient iotClient() {
        return IotClient.builder()
                .region(Region.US_WEST_2)
                .build();
    }

    /**
     * @return an instance of IotDataPlaneClient
     */
    public static IotDataPlaneClient iotDataPlaneClient() {
        return IotDataPlaneClient.builder()
                .region(Region.US_WEST_2)
                .build();
    }

    /**
     * @return an instance of KinesisVideoClient
     */
    public static KinesisVideoClient kinesisVideoClient() {
        return KinesisVideoClient.builder()
                .region(Region.US_WEST_2)
                .build();
    }
}
