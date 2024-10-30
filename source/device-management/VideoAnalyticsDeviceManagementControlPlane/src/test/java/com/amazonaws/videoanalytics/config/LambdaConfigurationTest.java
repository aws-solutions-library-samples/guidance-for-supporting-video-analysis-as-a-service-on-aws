package com.amazonaws.videoanalytics.config;

import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import static com.amazonaws.videoanalytics.utils.TestConstants.MOCK_AWS_REGION;

public class LambdaConfigurationTest {
    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    LambdaConfigurationTest() {
        environmentVariables.set("AWS_REGION", MOCK_AWS_REGION);
    }

    @Test
    public void getRegion_WhenEnvVarSet_ReturnsRegion() {
        Configuration config = LambdaConfiguration.getInstance();
        assertEquals(MOCK_AWS_REGION, config.getRegion());
    }
}
