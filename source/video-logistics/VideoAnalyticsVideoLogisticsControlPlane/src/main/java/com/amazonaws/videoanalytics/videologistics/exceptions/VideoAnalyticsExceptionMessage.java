package com.amazonaws.videoanalytics.videologistics.exceptions;

public class VideoAnalyticsExceptionMessage {
    public static final String INVALID_INPUT_EXCEPTION = "Malformed input, please fix the input.";
    public static final String NUMBER_OF_TOTAL_FILTERS_EXCEEDS_EXCEPTION = "Number of total filters exceeds the limit of %d";
    public static final String SUBEXPRESSIONS_DEPTH_EXCEEDS_EXCEPTION = "Depth of nested subexpressions exceeds the limit of %d";
    public static final String INVALID_SEARCH_REQUEST = "Search request received was a null input";
    public static final String INVALID_BULK_REQUEST = "Request received was a null input";
    public static final String INVALID_INPUT = "Input received was a null input";
    public static final String MISSING_RESOURCE = "The resource with ID %s was not found";
    public static final String START_TIME_GREATER_THAN_OR_EQUAL_TO_END_TIME =
            "The start time cannot be greater than or equal to the end time";
    public static final String END_TIME_WITHIN_A_DAY = "The end time value must be within 24 hours of the specified start time";
    public static final String PEER_TO_PEER_PLAYBACK_NOT_ENABLED = "Peer to peer playback is disabled at this time";
    public static final String DEVICE_NOT_REGISTERED = "The device was not registered " +
            "or the KVS stream/signaling channel was deleted out of band after registration";
    public static final String NO_VIDEO_FRAGMENTS = "The cloud has no video fragments between %s and %s for the device: %s";
    public static final String NOT_AUTHORIZED = "Please ensure these permissions are added to the IAM policy calling this API";
    public static final String NO_DATA_RETENTION = "A streaming session was requested for a stream that does not retain data";
    public static final String MAXIMUM_NUMBER_OF_SESSIONS = "The device %s has already reached the " +
            "maximum number of allowed connections: %s";
    public static final String INTERNAL_SERVER_EXCEPTION = "Internal server error";

    public static final String INVALID_TIMESTAMP = "No valid timestamp received. %s is not a POSIX timestamp!";
    public static final String INVALID_ENCODED_TIMESTAMP = "Encoded string is either null or empty!";
    public static final String NO_SCHEMA_FOR_MODEL = "No schema defined for model %s";
    public static final String INFERENCE_NOT_IN_JSON = "%s inference is not in json format: %s";
    public static final String INFERENCE_VALIDATION_FAILURE = "%s inference validation failed: %s in %s";
    public static final String TIME_CHRONOLOGY_MISMATCH = "End time cannot be less than or equal to start time";
    public static final String SECONDS_UNIT_ERROR = "Time should have been in multiples of 5 SECONDS";
    public static final String SECONDS_INCREMENT_ERROR = "Time increments should have been in multiples of 5 SECONDS, " +
            "provided time increment %d is not a multiple of 5!";
    public static final String TIME_UNIT_ERROR = "Time should have been start of the %s time unit!";
    public static final String DENSITY_TIME_MISMATCH_ERROR = "Cancelling the catchup workflow as time being taken away" +
            " from bucket is more than time available in bucket for time %d. Old Time for device in bucket = %d, time to" +
            " be subtracted = %d. This is unexpected behavior, please check aggregation logic sanity.";
    public static final String NO_PARTITION_KEY_ERROR = "Partition Key cannot be null or empty! This is unexpected behavior.";
    public static final String NO_SORT_KEY_ERROR = "Sort Key cannot be null or empty! This is unexpected behavior.";
    public static final String RESOURCE_NOT_FOUND = "Malformed input, Guidance resource you are trying to get does not exist.";
    public static final String DIFFERENT_QUERY_FROM_ORIGINAL = "Query cannot be different from original query, expected %s, but got %s";
    public static final String INVALID_NEXT_TOKEN = "Invalid next token";
    public static final String INVALID_MODEL_VERSION = "Invalid model name or version provided";
    public static final String NO_POINT_IN_TIME_AVAILABLE = "No Point-in-Time available for model {} endpoint {}";
    public static final String INVALID_VALIDATION_INPUT = "Inputs for device validation cannot be null.";
    public static final String ERROR_PARSE_EXCEPTION_MESSAGE = "Error parsing error message cause from step function: %s";
    public static final String TIMELINE_DESERIALIZATION_ERROR = "Failed to deserialize timestamp information for string %s";
    public static final String BATCH_TIMELINE_DESERIALIZATION_ERROR = "Failed to deserialize timeline information";
    public static final String S3_OBJECT_UPLOAD_ERROR = "Error uploading object for bucket %s and key %s";
    public static final String S3_BUCKET_CREATION_ERROR = "Could not create bucket due to inability to set bucket configuration.";
    public static final String S3_BUCKET_NOT_EXIST = "Requested S3 bucket: %s does not exist in the account: %s.";
    public static final String DESERIALIZATION_ERROR = "Failed to deserialize %s object. %s";
    public static final String SERIALIZATION_ERROR = "Failed to serialize %s object. %s";
    public static final String ZIPPING_ERROR = "Error writing to zip file...";
    public static final String UNZIPPING_ERROR = "Exception when unzipping media object...";
    public static final String NOT_SUPPORT_FLATTEN_NESTED_AGGREGATION = "This aggregation is not supported yet";
    public static final String FLATTEN_GROUP_BY_AFTER_NESTED_GROUP_BY =
            "Flatten group-by (not nested) shouldn't follow any nested group-by";
    public static final String FLATTEN_ORDER_BY_AFTER_NESTED_SORT_BY =
            "Flatten sort-by (not nested) shouldn't follow any nested sort-by";
    public static final String EMPTY_GROUP_BY_PROPERTIES = "groupByProperties can not be empty!";
    public static final String EMPTY_AGGREGATION_RESULT = "aggregationResult can not be empty!";
    public static final String EMPTY_INCLUSIVE_PROPERTIES = "inclusiveProperties can not be empty!";
    public static final String INVALID_PROPERTY_IN_AGGREGATION = "Invalid property in aggregation: %s!";
    public static final String INVALID_MAX_INFERENCES_PER_BUCKET = "maxInferencesPerBucket must be positive and not larger than %d!";
    public static final String INVALID_REQUEST_EXPORT_JOB = "Invalid null input for VideoExportJob Request";
    public static final String JOB_ID_NOT_FOUND_MESSAGE = "Job not found for id: %s";
    public static final String NO_VALID_SAMPLE = "No sample found in ACTIVE state for %s. Sample %s is in %s state";
    public static final String NO_VALID_DATASTORE = "No datastore found in ACTIVE state for %s";
    public static final String CODEC_INCONSISTENCY = "Codec inconsistency detected in stream '%s' between timestamps %s and %s. " +
            "This may be due to changes in encoding settings during this time range. " +
            "Try requesting a smaller time range within a single codec configuration period.";
}
