package com.amazonaws.videoanalytics.devicemanagement.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceReader {
    public static String readResourceToString(String resource) {
        try {
            InputStream resourceAsStream = ResourceReader.class.getClassLoader().getResourceAsStream(resource);
            return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not read resource '%s' to string", resource), e);
        }
    }

    private ResourceReader() {
        // Private default constructor so that JaCoCo marks utility class as covered
    }
}