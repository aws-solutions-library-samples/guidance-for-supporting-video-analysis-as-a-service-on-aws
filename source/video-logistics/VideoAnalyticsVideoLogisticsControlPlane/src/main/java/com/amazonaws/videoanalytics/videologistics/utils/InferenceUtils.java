package com.amazonaws.videoanalytics.videologistics.utils;

import com.google.common.base.Strings;

public class InferenceUtils {
    private InferenceUtils() {}

    public static String getOpenSearchDataStream(String modelName, String modelVersion) {
        // Open Search only allows lowercase data stream name
        return String.format("%s-%s", modelName, modelVersion).toLowerCase();
    }

    public static String getOpenSearchDataStreamForQuery(final String modelName, final String modelVersion) {
        if (Strings.isNullOrEmpty(modelVersion)) {
            return String.format("%s-*", modelName).toLowerCase();
        }

        return getOpenSearchDataStream(modelName, modelVersion);
    }
}