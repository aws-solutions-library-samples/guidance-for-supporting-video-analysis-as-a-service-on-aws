package com.amazonaws.videoanalytics.videologistics.utils;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INTERNAL_SERVER_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.schema.util.GuidanceVLConstants.NEW_LINE_DELIMITER;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.CREDENTIALS_PROVIDER;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.REGION_NAME;
import static com.google.common.hash.Hashing.sha256;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

public class KVSWebRTCUtils {
    private final AwsCredentialsProvider awsCredentialsProvider;
    private final String region;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String AWS4_REQUEST_TYPE = "aws4_request";
    private static final String SERVICE = "kinesisvideo";
    private static final String METHOD = "GET";
    private static final String X_AMZ_ALGORITHM = "X-Amz-Algorithm";
    private static final String X_AMZ_CREDENTIAL = "X-Amz-Credential";
    private static final String X_AMZ_DATE = "X-Amz-Date";
    private static final String X_AMZ_EXPIRES = "X-Amz-Expires";
    private static final String X_AMZ_SECURITY_TOKEN = "X-Amz-Security-Token";
    private static final String X_AMZ_SIGNATURE = "X-Amz-Signature";
    private static final String X_AMZ_SIGNED_HEADERS = "X-Amz-SignedHeaders";
    private static final String EXPIRATION_SECONDS = "300";
    private static final String HMACSHA256 = "HmacSHA256";
    private static final String ALGORITHM_AWS4_HMAC_SHA_256 = "AWS4-HMAC-SHA256";
    private static final String AWSV4 = "AWS4";

    @Inject
    public KVSWebRTCUtils(@Named(CREDENTIALS_PROVIDER) AwsCredentialsProvider awsCredentialsProvider,
                          @Named(REGION_NAME) String region) {

        this.awsCredentialsProvider = awsCredentialsProvider;
        this.region = region;
    }

    /**
     * This function manually creates v4 signed url without using existing library.
     * Existing library only supports https endpoints whereas webrtc connection requires wss endpoint
     */
    public String sign(final String endpoint,
                       final String channelArn,
                       final String clientId) {

        String endpointQueryParams = "X-Amz-ChannelARN=" + channelArn;
        if (clientId.length() > 0) {
            endpointQueryParams += "&X-Amz-ClientId=" + clientId;
        }
        URI canonicalEndpoint = URI.create(endpoint + "?" + endpointQueryParams);
        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC);
        String amzDate = timestamp.format(dateTimeFormatter);
        String datestamp = timestamp.format(dateFormatter);

        AwsCredentials awsCredentials = awsCredentialsProvider.resolveCredentials();
        if (!(awsCredentials instanceof AwsSessionCredentials)) {
            throw new RuntimeException("Assumed role credentials are long-lived credentials");
        }
        AwsSessionCredentials awsSessionCredentials = (AwsSessionCredentials) awsCredentials;
        String canonicalUri = canonicalEndpoint.getPath().isEmpty() ? "/" : canonicalEndpoint.getPath();
        String canonicalHeaders = "host:" + canonicalEndpoint.getHost() + NEW_LINE_DELIMITER;
        String signedHeaders = "host";
        String credentialScope = String.join("/", datestamp, region, SERVICE, AWS4_REQUEST_TYPE);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(X_AMZ_ALGORITHM, ALGORITHM_AWS4_HMAC_SHA_256);
        queryParams.put(X_AMZ_CREDENTIAL, awsSessionCredentials.accessKeyId() + "/" + credentialScope);
        queryParams.put(X_AMZ_DATE, amzDate);
        queryParams.put(X_AMZ_EXPIRES, EXPIRATION_SECONDS);
        queryParams.put(X_AMZ_SIGNED_HEADERS, signedHeaders);
        queryParams.put(X_AMZ_SECURITY_TOKEN, awsSessionCredentials.sessionToken());

        if (!canonicalEndpoint.getQuery().isEmpty()) {
            final String[] params = canonicalEndpoint.getQuery().split("&");
            for (final String param : params) {
                final int index = param.indexOf('=');
                if (index > 0) {
                    queryParams.put(param.substring(0, index), param.substring(index + 1));
                }
            }
        }

        String canonicalQuerystring = queryParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    try {
                        return entry.getKey() + "=" + urlEncode(entry.getValue());
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining("&"));
        String payloadHash = sha256().hashString(EMPTY, UTF_8).toString();
        String canonicalRequest = String.join(NEW_LINE_DELIMITER,
                METHOD,
                canonicalUri,
                canonicalQuerystring,
                canonicalHeaders,
                signedHeaders,
                payloadHash);
        String stringToSign = String.join(NEW_LINE_DELIMITER,
                ALGORITHM_AWS4_HMAC_SHA_256,
                amzDate,
                credentialScope,
                sha256().hashString(canonicalRequest, UTF_8).toString());

        final byte[] signatureKey;
        try {
            signatureKey = getSignatureKey(awsSessionCredentials.secretAccessKey(),
                    datestamp,
                    region,
                    SERVICE);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(INTERNAL_SERVER_EXCEPTION);
        }
        final String signature;
        try {
            signature = encodeHexString(hmacSha256(stringToSign, signatureKey));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(INTERNAL_SERVER_EXCEPTION);
        }
        final String signedCanonicalQueryString = canonicalQuerystring + "&" + X_AMZ_SIGNATURE + "=" + signature;

        return canonicalEndpoint.getScheme() +
                "://" +
                canonicalEndpoint.getRawAuthority() +
                canonicalUri + "?" + signedCanonicalQueryString;
    }

    private String urlEncode(final String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, UTF_8);
    }

    private byte[] hmacSha256(final String data,
                              final byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        final Mac mac;
        mac = Mac.getInstance(HMACSHA256);
        mac.init(new SecretKeySpec(key, HMACSHA256));
        return mac.doFinal(data.getBytes(UTF_8));
    }

    // https://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html#signature-v4-examples-java
    private byte[] getSignatureKey(
            final String key,
            final String dateStamp,
            final String regionName,
            final String serviceName) throws NoSuchAlgorithmException, InvalidKeyException {
        final byte[] kSecret = (AWSV4 + key).getBytes(UTF_8);
        final byte[] kDate = hmacSha256(dateStamp, kSecret);
        final byte[] kRegion = hmacSha256(regionName, kDate);
        final byte[] kService = hmacSha256(serviceName, kRegion);
        final byte[] kSigning = hmacSha256(AWS4_REQUEST_TYPE, kService);
        return kSigning;
    }
}
