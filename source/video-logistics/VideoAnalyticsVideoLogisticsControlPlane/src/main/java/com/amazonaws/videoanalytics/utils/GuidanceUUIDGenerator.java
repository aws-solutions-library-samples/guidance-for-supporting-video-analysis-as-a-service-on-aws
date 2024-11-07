package com.amazonaws.videoanalytics.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Utility class to generate UUID.
 */
public class GuidanceUUIDGenerator {
    private static final Logger LOG = LogManager.getLogger(GuidanceUUIDGenerator.class);

    public String generateUUIDRandom() {
        LOG.info("Entered the generateUUIDRandom method");
        final String id = UUID.randomUUID().toString();
        LOG.info("UUID generated the ID :- " + id);
        return id;
    }
}
