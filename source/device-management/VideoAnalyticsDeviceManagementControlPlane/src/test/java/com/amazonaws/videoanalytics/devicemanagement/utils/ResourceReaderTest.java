package com.amazonaws.videoanalytics.devicemanagement.utils;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResourceReaderTest {

    @Test
    void readResourceToString_validResource_returnsContent() {
        String testResourceString = ResourceReader.readResourceToString("test-resource.txt");
        assertEquals("Hello World", testResourceString);
    }

    @Test
    void readResourceToString_nonExistentResource_throwsRuntimeException() {
        assertThrows(RuntimeException.class, () -> {
            ResourceReader.readResourceToString("non-existent-resource.json");
        });
    }
}