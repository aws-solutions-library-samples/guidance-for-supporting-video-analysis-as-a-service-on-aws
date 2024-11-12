package com.amazonaws.videoanalytics.devicemanagement.utils;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResourceReaderTest {

    @Test
    void readResourceToStringTest() {
        String testResourceString = ResourceReader.readResourceToString("test-resource.txt");
        assertEquals("Hello World", testResourceString);
    }

    @Test
    void readingNonExistentTestResourceThrowsException() {
        assertThrows(RuntimeException.class, () -> {
            ResourceReader.readResourceToString("non-existent-resource.json");
        });
    }
}