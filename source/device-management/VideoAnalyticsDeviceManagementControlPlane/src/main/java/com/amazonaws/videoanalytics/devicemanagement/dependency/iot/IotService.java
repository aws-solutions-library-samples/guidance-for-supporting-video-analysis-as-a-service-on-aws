package com.amazonaws.videoanalytics.devicemanagement.dependency.iot;

import com.amazonaws.videoanalytics.devicemanagement.CloudVideoStreamingElement;
import com.amazonaws.videoanalytics.devicemanagement.DeviceConnection;
import com.amazonaws.videoanalytics.devicemanagement.DeviceMetaData;
import com.amazonaws.videoanalytics.devicemanagement.DeviceState;
import com.amazonaws.videoanalytics.devicemanagement.DeviceStatus;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.IpAddress;
import com.amazonaws.videoanalytics.devicemanagement.ShadowMap;
import com.amazonaws.videoanalytics.devicemanagement.UpdateDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.VideoStreamingState;
import com.amazonaws.videoanalytics.devicemanagement.utils.ShadowMapUtils;
import com.amazonaws.videoanalytics.devicemanagement.utils.UpdateDeviceUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.EnumUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.GroupNameAndArn;
import software.amazon.awssdk.services.iot.model.ListThingGroupsForThingRequest;
import software.amazon.awssdk.services.iot.model.ListThingGroupsForThingResponse;
import software.amazon.awssdk.services.iot.model.SearchIndexRequest;
import software.amazon.awssdk.services.iot.model.SearchIndexResponse;
import software.amazon.awssdk.services.iot.model.ThingConnectivity;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import software.amazon.awssdk.utils.StringUtils;
import javax.inject.Inject;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.AI_CHIP_SET_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.AI_MODEL_VERSION_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.AI_SDK_VERSION_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SDK_VERSION_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.FIRMWARE_VERSION_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.IOT_FLEET_INDEXING_INDEX_AWS_THINGS;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.IOT_FLEET_INDEXING_THING_NAME;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.MAC_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.MANUFACTURER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.MAX_THING_GROUP_RESULT_COUNT;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.MODEL_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PRIVATE_IP_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PROFILE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PUBLIC_IP_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.RECORDING;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SD_CARD_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_DESIRED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_METADATA_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_REPORTED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_STATE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.VIDEO_SETTINGS;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.PROVISIONING_SHADOW_NAME;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.VIDEO_ENCODER_SHADOW_NAME;
import software.amazon.awssdk.services.iot.model.RegisterThingRequest;
import com.amazonaws.videoanalytics.devicemanagement.utils.ResourceReader;
import software.amazon.awssdk.services.iot.model.UpdateCertificateRequest;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iot.model.DescribeCertificateRequest;
import software.amazon.awssdk.services.iot.model.CertificateDescription;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ListThingPrincipalsRequest;
import software.amazon.awssdk.services.iot.model.ListThingPrincipalsResponse;
import software.amazon.awssdk.services.iot.model.AttachPolicyRequest;
import com.google.gson.JsonNull;

public class IotService {
    private final IotClient iotClient;
    private final IotDataPlaneClient iotDataPlaneClient;
    private static final Logger LOG = LogManager.getLogger(IotService.class);

    @Inject
    public IotService(final IotClient iotClient,
                      final IotDataPlaneClient iotDataPlaneClient) {
        this.iotClient = iotClient;
        this.iotDataPlaneClient = iotDataPlaneClient;
    }

    public DescribeThingResponse describeThing(String deviceId) {
        DescribeThingRequest describeThingRequest = DescribeThingRequest.builder().thingName(deviceId).build();
        LOG.info("Describing device: %s", deviceId);
        return iotClient.describeThing(describeThingRequest);
    }

    public JSONObject getThingShadow(String deviceIdentifier, String shadowName) 
            throws JSONException, software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException {
        GetThingShadowRequest.Builder getThingShadowRequestBuilder = GetThingShadowRequest.builder();
        getThingShadowRequestBuilder.thingName(deviceIdentifier);

        if (!StringUtils.isBlank(shadowName)) {
            getThingShadowRequestBuilder.shadowName(shadowName);
        }

        GetThingShadowResponse getThingShadowResponse = iotDataPlaneClient.getThingShadow(getThingShadowRequestBuilder.build());
        String shadowStr = getThingShadowResponse.payload().asUtf8String();

        JSONObject shadowObject = new JSONObject(shadowStr);
        return shadowObject;
    }

    /**
     * Get all thing groups assigned to the device
     *
     * @param deviceId device id
     * @return list of thing groups
     */
    public List<GroupNameAndArn> listThingGroupsForThing(String deviceId) {
        LOG.info(String.format("Listing thing groups for device: %s", deviceId));

        // A thing can only present in 10 static thing groups, use static result number for now
        ListThingGroupsForThingResponse response = iotClient.listThingGroupsForThing(
                ListThingGroupsForThingRequest.builder()
                        .thingName(deviceId)
                        .maxResults(MAX_THING_GROUP_RESULT_COUNT)
                        .build());

        return response.thingGroups();
    }

    public GetDeviceResponseContent getDevice(String deviceIdentifier) throws JsonProcessingException, ParseException {
        DescribeThingResponse describeThingResponse = describeThing(deviceIdentifier);

        JSONObject getThingShadowResponseProvision;
        try {
            getThingShadowResponseProvision = getThingShadow(deviceIdentifier, PROVISIONING_SHADOW_NAME);
        } catch (software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException e) {
            getThingShadowResponseProvision = new JSONObject();
        }

        List<GroupNameAndArn> thingGroups = listThingGroupsForThing(deviceIdentifier);

        DeviceMetaData deviceMetaData = generateDeviceMetaData(
                describeThingResponse,
                getThingShadowResponseProvision,
                thingGroups
        );

        Map<String, String> deviceSettings = new HashMap<>();

        // Retrieving videoSettings in videoEncoder iot shadow.
        JSONObject getThingShadowResponseVideoSettings;
        try {
            getThingShadowResponseVideoSettings = getThingShadow(deviceIdentifier, VIDEO_ENCODER_SHADOW_NAME);
        } catch (software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException e) {
            getThingShadowResponseVideoSettings = new JSONObject();
        }
        JSONObject shadowStateReportedVideoSettings = getThingShadowResponseVideoSettings
                .optJSONObject(SHADOW_STATE_KEY, new JSONObject())
                .optJSONObject(SHADOW_REPORTED_KEY);
        if (shadowStateReportedVideoSettings != null && !shadowStateReportedVideoSettings.isEmpty()) {
            JSONObject videoSettings = shadowStateReportedVideoSettings.optJSONObject(VIDEO_SETTINGS);
            if (videoSettings != null && !videoSettings.isEmpty()) {
                ArrayList<String> videoSettingsList = new ArrayList<>();
                for (String key: videoSettings.keySet()) {
                    JSONObject value = videoSettings.optJSONObject(key);
                    value.put(PROFILE_ID, key);
                    videoSettingsList.add(value.toString());
                }
                deviceSettings.put(VIDEO_SETTINGS, videoSettingsList.toString());              
            }
        }

        List<String> deviceGroupId = new ArrayList<>();

        return GetDeviceResponseContent.builder()
                .deviceId(describeThingResponse.thingName())
                .deviceMetaData(deviceMetaData)
                .deviceSettings(deviceSettings)
                .build();
    }

    private DeviceMetaData generateDeviceMetaData(DescribeThingResponse describeThingResponse,
                                                  JSONObject shadowProvision,
                                                  List<GroupNameAndArn> thingGroups)
    {

        DeviceMetaData deviceMetaData = DeviceMetaData.builder().build();
        IpAddress ipAddress = IpAddress.builder().build();
        DeviceConnection deviceConnection = generateDeviceConnection(describeThingResponse);
        DeviceStatus deviceStatus = DeviceStatus.builder().build();
        CloudVideoStreamingElement cloudVideoStreamingElement = CloudVideoStreamingElement.builder().build();

        JSONObject shadowProvisionReported = shadowProvision
                .optJSONObject(SHADOW_STATE_KEY, new JSONObject())
                .optJSONObject(SHADOW_REPORTED_KEY);
        
        JSONObject shadowProvisionMetadataReported = shadowProvision
                .optJSONObject(SHADOW_METADATA_KEY, new JSONObject())
                .optJSONObject(SHADOW_REPORTED_KEY);

        // Get device state based on thing group
        List<String> deviceStatesFromThingGroup = thingGroups.stream()
                .map(GroupNameAndArn::groupName)
                .filter(groupName -> EnumUtils.isValidEnum(UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId.class, groupName))
                .map(groupName -> UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId
                        .valueOf(groupName)
                        .getVideoAnalyticsManagedDeviceState())
                .collect(Collectors.toList());
        
        if (!deviceStatesFromThingGroup.isEmpty()) {
            String videoAnalyticsStatus = deviceStatesFromThingGroup.get(0);
            deviceStatus.setDeviceState(Arrays.stream(DeviceState.values())
                .filter(s -> s.toString().equals(videoAnalyticsStatus))
                .findAny()
                .orElse(null)
            );
        }

        LOG.info("Configuring ipAddress and deviceMetadata");
        if (shadowProvisionReported != null && !shadowProvisionReported.isEmpty()) {
            deviceMetaData.setMac(shadowProvisionReported.optString(MAC_KEY));
            deviceMetaData.setManufacturer(shadowProvisionReported.optString(MANUFACTURER_KEY));
            deviceMetaData.setFirmwareVersion(shadowProvisionReported.optString(FIRMWARE_VERSION_KEY));
            deviceMetaData.setAiModelVersion(shadowProvisionReported.optString(AI_MODEL_VERSION_KEY));
            deviceMetaData.setAiSdkVersion(shadowProvisionReported.optString(AI_SDK_VERSION_KEY));
            deviceMetaData.setModel(shadowProvisionReported.optString(MODEL_KEY));
            ipAddress.setPrivateIpAddress(shadowProvisionReported.optString(PRIVATE_IP_KEY));

            // Streaming status
            Boolean streamingStatus = Boolean.parseBoolean(shadowProvisionReported.optString(RECORDING));
            cloudVideoStreamingElement.setId(describeThingResponse.thingName());
            cloudVideoStreamingElement.setStatus(streamingStatus? VideoStreamingState.CONNECTED : VideoStreamingState.DISCONNECTED);

            cloudVideoStreamingElement.setUpdatedAt(getShadowPropertyUTCTimestamp(
                shadowProvisionMetadataReported,
                RECORDING
            ));

            ipAddress.setPublicIpAddress(shadowProvisionReported.optString(PUBLIC_IP_KEY));
            deviceMetaData.setAiChipset(shadowProvisionReported.optString(AI_CHIP_SET_KEY));
            deviceMetaData.setSdkVersion(shadowProvisionReported.optString(SDK_VERSION_KEY));
        }

        List<CloudVideoStreamingElement> videoStreamingStateList = new ArrayList<>();
        videoStreamingStateList.add(cloudVideoStreamingElement);
        deviceStatus.setDeviceConnection(deviceConnection);
        deviceStatus.setCloudVideoStreaming(videoStreamingStateList);
        deviceMetaData.setIpAddress(ipAddress);
        deviceMetaData.setDeviceStatus(deviceStatus);

        return deviceMetaData;
    }

    private Date getShadowPropertyUTCTimestamp(JSONObject metadataReported, String propertyName) {
        if (metadataReported == null) {
            return null;
        }
        Number propertyTimestampEpoch = metadataReported
                .optJSONObject(propertyName, new JSONObject())
                .optNumber("timestamp", null);

        if (propertyTimestampEpoch == null) {
            LOG.info("Failed to create a timestamp for: " + propertyName);
        }

        return propertyTimestampEpoch == null ? null : new Date(propertyTimestampEpoch.longValue() * 1000);
    }

    private DeviceConnection generateDeviceConnection(DescribeThingResponse describeThingResponse) {
        DeviceConnection deviceConnection = DeviceConnection.builder().status(false).build();
        final String deviceConnectedQuery = IOT_FLEET_INDEXING_THING_NAME + describeThingResponse.thingName();
        SearchIndexResponse searchIndexResponse = searchIndex(IOT_FLEET_INDEXING_INDEX_AWS_THINGS, deviceConnectedQuery);
        LOG.info("searchIndexResponse for deviceConnection: " + searchIndexResponse);
        if(!searchIndexResponse.things().isEmpty()) {
            ThingConnectivity thingConnectivity = searchIndexResponse.things().get(0).connectivity();
            if (thingConnectivity != null) {
                deviceConnection.setStatus(thingConnectivity.connected());
                if (thingConnectivity.timestamp() != 0L) {
                    deviceConnection.setUpdatedAt(
                        Date.from(Instant.ofEpochMilli(thingConnectivity.timestamp()))
                    );
                }
            }
        }
        return deviceConnection;
    }

    public SearchIndexResponse searchIndex(String indexName,
                                           String queryString) {
        LOG.info(String.format("Searching within index: %s", indexName));
        SearchIndexRequest searchIndexRequest = SearchIndexRequest
                .builder()
                .indexName(indexName)
                .queryString(queryString)
                .build();

        return iotClient.searchIndex(searchIndexRequest);
    }

    public GetDeviceShadowResponseContent getDeviceShadow(String deviceId, String shadowName) {
        JSONObject thingShadow = this.getThingShadow(deviceId, shadowName);
        JSONObject desiredShadowObject = thingShadow.optJSONObject(SHADOW_STATE_KEY).optJSONObject(SHADOW_DESIRED_KEY);
        ShadowMap shadowPayload = ShadowMap.builder()
                .stateDocument(desiredShadowObject.toMap())
                .shadowName(shadowName)
                .build();
        return GetDeviceShadowResponseContent.builder()
                .shadowPayload(shadowPayload)
                .build();
    }

    private void updateThingShadow(final String deviceId,
                                   final String shadowName,
                                   final SdkBytes payload) {
        UpdateThingShadowRequest updateThingShadowRequest = UpdateThingShadowRequest
                .builder()
                .payload(payload)
                .thingName(deviceId)
                .shadowName(shadowName)
                .build();
        LOG.info(String.format("Updating thing shadow: %s of %s", shadowName, deviceId));
        iotDataPlaneClient.updateThingShadow(updateThingShadowRequest);
    }

    public UpdateDeviceShadowResponseContent updateDeviceShadow(String deviceId, ShadowMap shadowMap) throws JsonProcessingException {
        if (shadowMap != null) {
            String shadowName = shadowMap.getShadowName();
            SdkBytes payload;
            payload = ShadowMapUtils.serialize(shadowMap);
            updateThingShadow(deviceId, shadowName, payload);
        }
        return UpdateDeviceShadowResponseContent
                .builder()
                .deviceId(deviceId)
                .build();
    }

    public void workflowRegisterDevice(String certificateId, String deviceId) {
        LOG.info(String.format("Registering device: %s", deviceId));

        Map<String, String> parametersForRegisteringThing =
                ImmutableMap.of("ThingName", deviceId, "ThingCertificateId", certificateId);

        String template = ResourceReader.readResourceToString(
                "iot-provisioning-template.json"
        );

        RegisterThingRequest registerThingRequest = RegisterThingRequest
                .builder()
                .parameters(parametersForRegisteringThing)
                .templateBody(template)
                .build();

        iotClient.registerThing(registerThingRequest);
    }

    public void updateCertificate(String certificateId, CertificateStatus newStatus) {
        UpdateCertificateRequest updateCertificateRequest = UpdateCertificateRequest
                .builder()
                .certificateId(certificateId)
                .newStatus(newStatus)
                .build();
        LOG.info("updating certificate to " + newStatus.toString());
        iotClient.updateCertificate(updateCertificateRequest);
    }

    /**
     * This function is used to send messages to "provision" named shadow during device creation. 
     * @param deviceId videoAnalytics Device
     */
    public void publishLogConfigurationToProvisioningShadow(String deviceId) {
        /**
         * The logger configuration template will be:
         * {
         *   "state": {
         *     "desired": {
         *       "loggerSettings": {
         *         "isEnabled": true,
         *         "syncFrequency": 300,
         *         "logLevel": "INFO"
         *       }
         *     }
         *   }
         * }
         */
        // Create logger config
        JsonObject loggerConfiguration = new JsonObject();
        loggerConfiguration.addProperty("isEnabled", true);
        loggerConfiguration.addProperty("syncFrequency", 300);
        loggerConfiguration.addProperty("logLevel", "INFO");

        // Create loggerSettings wrapper
        JsonObject loggerSettings = new JsonObject();
        loggerSettings.add("loggerSettings", loggerConfiguration);

        // Create desired state
        JsonObject desired = new JsonObject();
        desired.add("desired", loggerSettings);

        // Create state wrapper (this was missing before)
        JsonObject messagePayload = new JsonObject();
        messagePayload.add("state", desired);

        UpdateThingShadowRequest updateThingShadowRequest = UpdateThingShadowRequest.builder()
                .thingName(deviceId)
                .shadowName(PROVISIONING_SHADOW_NAME)
                .payload(SdkBytes.fromUtf8String(messagePayload.toString()))
                .build();

        LOG.info("Publishing logger configuration to provisioning shadow: " + messagePayload.toString());
        iotDataPlaneClient.updateThingShadow(updateThingShadowRequest);
    }

    /**
     * Checks if a device with the given ID already exists in IoT Core
     *
     * @param deviceId The device ID to check
     * @return true if the device exists, false otherwise
     */
    public boolean isAnExistingDevice(String deviceId) {
        try {
            describeThing(deviceId);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public CertificateDescription getCertificate(String certId) {
        DescribeCertificateRequest describeCertificateRequest = DescribeCertificateRequest.builder().certificateId(certId).build();
        LOG.info("Describing certificate");
        return iotClient.describeCertificate(describeCertificateRequest).certificateDescription();
    }

    public ListThingPrincipalsResponse listThingPrincipals(String thingIdentifier) {
        ListThingPrincipalsRequest listThingPrincipalsRequest = ListThingPrincipalsRequest.builder()
                .thingName(thingIdentifier)
                .build();
        LOG.info("Listing principals");
        return iotClient.listThingPrincipals(listThingPrincipalsRequest);
    }

    /**
     * This function is used to send messages to "provision" named shadow for the following use cases:
     * - Enabled
     * - Notification condition
     * Field names existence in the shadow is the message.
     * @param deviceId VideoAnalytics Device
     * @param fieldName fieldname to set in the shadow
     */
    public void messageDeviceProvisioningShadow(String deviceId, String fieldName) {
        // Create the field value
        JsonObject fieldValue = new JsonObject();
        fieldValue.addProperty(fieldName, true);

        // Create desired state
        JsonObject desired = new JsonObject();
        desired.add("desired", fieldValue);

        // Create state wrapper
        JsonObject messagePayload = new JsonObject();
        messagePayload.add("state", desired);

        UpdateThingShadowRequest updateThingShadowRequest = UpdateThingShadowRequest.builder()
                .thingName(deviceId)
                .shadowName(PROVISIONING_SHADOW_NAME)
                .payload(SdkBytes.fromUtf8String(messagePayload.toString()))
                .build();

        LOG.info(String.format("Sending message to provisioning shadow for device %s with field %s", deviceId, fieldName));
        iotDataPlaneClient.updateThingShadow(updateThingShadowRequest);
    }

    public void attachPolicy(String policyName, String certificateArn) {
        AttachPolicyRequest attachPolicyRequest = AttachPolicyRequest
                .builder()
                .target(certificateArn)
                .policyName(policyName)
                .build();
        LOG.info("Attaching policy to certificate");
        iotClient.attachPolicy(attachPolicyRequest);
    }

    /**
     * This function is used to clear field in "provision" named shadow for use cases:
     * - Enable
     * - Disable
     * @param deviceId Iot Device
     * @param fieldName fieldname to clear in the shadow
     */
    public void clearDeviceProvisioningShadowField(String deviceId, String fieldName) {
        JsonObject messagePayload = new JsonObject();
        messagePayload.add(fieldName, JsonNull.INSTANCE);

        SdkBytes shadowDoc = ShadowMapUtils.createAnUpdateDesiredStateMessage(messagePayload);

        this.updateThingShadow(deviceId, PROVISIONING_SHADOW_NAME, shadowDoc);
    }

    public void clearDeviceProvisioningShadowFields(String deviceId, JsonObject payload) {
        SdkBytes shadowDoc = ShadowMapUtils.createAnUpdateDesiredStateMessage(payload);

        this.updateThingShadow(deviceId, PROVISIONING_SHADOW_NAME, shadowDoc);
    }

    public void clearDeviceProvisioningShadowReported(String deviceId) {
        SdkBytes shadowDoc = ShadowMapUtils.createAnUpdateReportedStateMessage(JsonNull.INSTANCE);

        this.updateThingShadow(deviceId, PROVISIONING_SHADOW_NAME, shadowDoc);
    }
}
