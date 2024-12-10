package com.amazonaws.videoanalytics.videologistics.timeline;

import com.amazonaws.videoanalytics.videologistics.schema.VideoTimeline.TimeIncrementUnits;
import com.google.common.base.Strings;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VideoTimelineUtils {
    private static final String INVALID_ENCODED_TIMESTAMP = "Invalid encoded timestamp";
    private static final String INVALID_TIMESTAMP = "Invalid timestamp: %s";
    private static final long SECONDS_BUCKET_DURATION_MILLIS = 5000L;
    private static final long MILLIS_CONVERSION_UNIT = 1000L;

    public String generateTimelinePartitionKey(final String deviceId, final TimeIncrementUnits timeUnit) {
        return generateKeyValues(deviceId, timeUnit.toString());
    }

    public String generateTimelinePKFromPartialPK(final String partialPK, final TimeIncrementUnits timeUnit) {
        return generateKeyValues(partialPK, timeUnit.toString());
    }

    public String generateRawPartitionKey(final String deviceId) {
        return generateKeyValues(deviceId);
    }

    public String generateKeyValues(String... values) {
        return String.join("#", values);
    }

    public String generateS3Key(String... values) {
        for (String value : values) {
            if (Strings.isNullOrEmpty(value)) {
                throw new RuntimeException("Key value(s) should not be empty!");
            }
        }
        return String.join("/", values);
    }

    public List<Long> encodedTimestampsToList(final String encodedTimestamps) {
        if (encodedTimestamps == null || encodedTimestamps.isEmpty()) {
            throw new RuntimeException(INVALID_ENCODED_TIMESTAMP);
        }

        try {
            // Decode the base64 encoded string
            byte[] decodedBytes = Base64.getDecoder().decode(encodedTimestamps);
            String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

            // Split the decoded string into a list of timestamps
            String[] timestampArray = decodedString.split(",");

            // Convert the array to a list of epochs (Long)
            return Stream.of(timestampArray)
                    .filter(VideoTimelineUtils::isValidLong)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unable to decode timestamps.");
        }
    }

    private static boolean isValidLong(String timestampStr) {
        try {
            Long.parseLong(timestampStr);
            return true;
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format(INVALID_TIMESTAMP, timestampStr));
        }
    }

    /**
     * Calculates the period that follows the period that the current time will belong to for the final Video Timeline
     * eg, if start time is 4am, current time is 11am and time increment is 3 hours, 4am - 7am is period 0,
     * 7am - 10am is period 1, 10am - 1pm is period 2 and so on, so returned value will be 3
     * @param startTime startTime of the VideoTimeline list
     * @param lastEvaluatedTimestamp time for which we need to find the following period
     * @param incrementInMillis time range of periods
     * @return integer value of period that the current time belongs to
     */
    public Long getNextPeriod(Long startTime, Long incrementInMillis, Long lastEvaluatedTimestamp) {
        long nextPeriodStep = (lastEvaluatedTimestamp - startTime) / incrementInMillis;
        // Calculate the next term after lastEvaluatedTimestamp
        return startTime + (nextPeriodStep + 1) * incrementInMillis;
    }

    /**
     * Gets truncated value of timestamp, truncated to the nearest lower unit
     * eg. 09:59:58 gets truncated to 09:59:55 for seconds, 09:59:00 for minutes, 09:00:00 for hours etc.
     * @param timeIncrementUnits - Increment Unit to which time needs to be truncated
     * @param timestamp - start time for which density is to be stored
     * @return Instant
     */
    public Long getUnitTime(TimeIncrementUnits timeIncrementUnits, Long timestamp) {
        if(timeIncrementUnits == TimeIncrementUnits.SECONDS) {
            return (long) (5000 * (Math.floor((double) timestamp / 5000)));
        } else {
            Instant truncatedInstant = Instant.ofEpochMilli(timestamp).truncatedTo(timeIncrementUnits.getChronoUnit());
            return truncatedInstant.toEpochMilli();
        }
    }

    /**
     * Returns bucket duration in milliseconds for given time unit
     * @param timeUnit Unit of time to get duration for
     * @return bucket duration
     */
    public long getBucketDuration(ChronoUnit timeUnit){
        switch (timeUnit) {
            case SECONDS: // 5s in millis
                // The duration is set to 5s as it is a reasonable
                // next unit greater than the pre-configured 4s fragment
                return  SECONDS_BUCKET_DURATION_MILLIS;
            case MINUTES: // 1 minute in  millis
                return 60 * MILLIS_CONVERSION_UNIT;
            case HOURS: // 1 hour in millis
                return 60 * 60 * MILLIS_CONVERSION_UNIT;
            case DAYS: // 1 day in mills
                return 24 * 60 * 60 * MILLIS_CONVERSION_UNIT;
            default:
                throw new IllegalArgumentException("Invalid Time Increment Unit: " + timeUnit);
        }
    }
}
