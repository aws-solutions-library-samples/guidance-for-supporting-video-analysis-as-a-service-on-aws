package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionResponseContent;
import com.google.common.base.Strings;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VideoTimelineUtils {
    // Constants
    private static final long MILLIS_CONVERSION_UNIT = 1000L;
    private static final long SECONDS_BUCKET_DURATION_MILLIS = 5000L;

    public String generateTimelinePartitionKey(final String customerId, final String deviceId, final String timeUnit) {
        return generateKeyValues(customerId, deviceId, timeUnit);
    }

    public String generateTimelinePKFromPartialPK(final String partialPK, final String timeUnit) {
        return generateKeyValues(partialPK, timeUnit);
    }

    public String generateRawPartitionKey(final String customerId, final String deviceId) {
        return generateKeyValues(customerId, deviceId);
    }

    public String generateKeyValues(String... values) {
        for (String value : values) {
            if (Strings.isNullOrEmpty(value)) {
                ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message("Key value(s) should not be empty!")
                    .build();
                throw new RuntimeException(exception.toJson());
            }
        }
        return String.join("#", values);
    }

    public List<Long> encodedTimestampsToList(final String encodedTimestamps) {
        if (encodedTimestamps == null || encodedTimestamps.isEmpty()) {
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(VideoAnalyticsExceptionMessage.INVALID_ENCODED_TIMESTAMP)
                .build();
            throw new RuntimeException(exception.toJson());
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedTimestamps);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] timestampArray = decodedString.split(",");

            return Stream.of(timestampArray)
                    .filter(VideoTimelineUtils::isValidLong)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message("Unable to decode timestamps.")
                .build();
            throw new RuntimeException(exception.toJson());
        }
    }

    private static boolean isValidLong(String timestampStr) {
        try {
            Long.parseLong(timestampStr);
            return true;
        } catch (NumberFormatException e) {
            ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                .message(String.format(VideoAnalyticsExceptionMessage.INVALID_TIMESTAMP, timestampStr))
                .build();
            throw new RuntimeException(exception.toJson());
        }
    }

    public Long getNextPeriod(Long startTime, Long incrementInMillis, Long lastEvaluatedTimestamp) {
        long nextPeriodStep = (lastEvaluatedTimestamp - startTime) / incrementInMillis;
        return startTime + (nextPeriodStep + 1) * incrementInMillis;
    }

    public Long getUnitTime(String timeIncrementUnit, Long timestamp) {
        if("SECONDS".equals(timeIncrementUnit)) {
            return (long) (5000 * (Math.floor((double) timestamp / 5000)));
        } else {
            ChronoUnit unit = ChronoUnit.valueOf(timeIncrementUnit);
            Instant truncatedInstant = Instant.ofEpochMilli(timestamp).truncatedTo(unit);
            return truncatedInstant.toEpochMilli();
        }
    }

    public long getBucketDuration(ChronoUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return SECONDS_BUCKET_DURATION_MILLIS;
            case MINUTES:
                return 60 * MILLIS_CONVERSION_UNIT;
            case HOURS:
                return 60 * 60 * MILLIS_CONVERSION_UNIT;
            case DAYS:
                return 24 * 60 * 60 * MILLIS_CONVERSION_UNIT;
            default:
                ValidationExceptionResponseContent exception = ValidationExceptionResponseContent.builder()
                    .message("Invalid Time Increment Unit: " + timeUnit)
                    .build();
                throw new RuntimeException(exception.toJson());
        }
    }
}
