package com.amazonaws.videoanalytics.devicemanagement.config;

import java.util.Objects;

public class LambdaConfiguration implements Configuration {
    private static volatile LambdaConfiguration config;
    private final String region;

    private LambdaConfiguration() {
        // On Lambda, the region code is always set to AWS_REGION environment variable
        // https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
        this.region = Objects.requireNonNull(System.getenv("AWS_REGION"));
    }

    /**
     * @return Config - An singleton instance of Config.
     */
    public static LambdaConfiguration getInstance() {
        if (null == config) {
            synchronized (LambdaConfiguration.class) {
                if (null == config) {
                    config = new LambdaConfiguration();
                }
            }
        }
        return config;
    }

    public String getRegion() {
        return this.region;
    }
}
