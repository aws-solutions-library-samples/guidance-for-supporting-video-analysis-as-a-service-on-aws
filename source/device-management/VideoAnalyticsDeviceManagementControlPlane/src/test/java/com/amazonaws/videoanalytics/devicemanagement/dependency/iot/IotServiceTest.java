package com.amazonaws.videoanalytics.devicemanagement.dependency.iot;

import com.amazonaws.videoanalytics.devicemanagement.CloudVideoStreamingElement;
import com.amazonaws.videoanalytics.devicemanagement.DeviceConnection;
import com.amazonaws.videoanalytics.devicemanagement.DeviceMetaData;
import com.amazonaws.videoanalytics.devicemanagement.DeviceState;
import com.amazonaws.videoanalytics.devicemanagement.DeviceStatus;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.GetDeviceShadowResponseContent;
import com.amazonaws.videoanalytics.devicemanagement.IpAddress;
import com.amazonaws.videoanalytics.devicemanagement.StorageElement;
import com.amazonaws.videoanalytics.devicemanagement.StorageState;
import com.amazonaws.videoanalytics.devicemanagement.VideoStreamingState;
import com.amazonaws.videoanalytics.devicemanagement.utils.UpdateDeviceUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.GroupNameAndArn;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListThingGroupsForThingRequest;
import software.amazon.awssdk.services.iot.model.ListThingGroupsForThingResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.SearchIndexRequest;
import software.amazon.awssdk.services.iot.model.SearchIndexResponse;
import software.amazon.awssdk.services.iot.model.ThingConnectivity;
import software.amazon.awssdk.services.iot.model.ThingDocument;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowResponse;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.AI_CHIP_SET_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.AI_MODEL_VERSION_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.AI_SDK_VERSION_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.AI_SETTINGS;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.FIRMWARE_VERSION_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.IOT_FLEET_INDEXING_INDEX_AWS_THINGS;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.MAC_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.MANUFACTURER_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.MODEL_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PRIVATE_IP_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.PUBLIC_IP_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.RECORDING;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_DESIRED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_METADATA_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_REPORTED_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.AWSVideoAnalyticsServiceLambdaConstants.SHADOW_STATE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.AI_CHIP_SET_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.AI_MODEL_VERSION_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.AI_SDK_VERSION_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DATE_LONG_IN_MILLIS;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DATE_LONG;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DATE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.DEVICE_TYPE_NAME;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.EXPECTED_DEVICE_SETTINGS_STRING;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.EXPECTED_DEVICE_SETTINGS_WITH_ADDITIONAL_SETTINGS_STRING;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.FIRMWARE_VERSION_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.FLEET_INDEXING_QUERY_STRING;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.MAC_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.MANUFACTURER_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.MODEL_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.PRIVATE_IP_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.PUBLIC_IP_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SD_CARD_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SD_CARD_TOTAL_CAPACITY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SD_CARD_USED_CAPACITY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.SHADOW_NAME;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.TEST_ATTRIBUTE_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.TEST_ATTRIBUTE_VALUE;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.TEST_DEVICE_CAP_KEY;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.TEST_DEVICE_CAP_VAL;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.THING_ARN;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.THING_ID;
import static com.amazonaws.videoanalytics.devicemanagement.utils.TestConstants.TIMESTAMP;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.AI_SHADOW_NAME;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.PROVISIONING_SHADOW_NAME;
import static com.amazonaws.videoanalytics.devicemanagement.utils.WorkflowConstants.VIDEO_ENCODER_SHADOW_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IotServiceTest {
    @Mock
    private IotClient iotClient;
    @Mock
    private IotDataPlaneClient iotDataPlaneClient;
    private IotService iotService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        iotService = new IotService(
                iotClient,
                iotDataPlaneClient
        );
    }

    @Test
    public void listThingGroupsForThing_WhenValidRequest_ReturnsResponse() {
        List<GroupNameAndArn> testThingGroups = ImmutableList.of(GroupNameAndArn.builder()
                .groupName(UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId.SpecialGroup_EnabledState.name())
                .build());
        when(iotClient.listThingGroupsForThing(any(ListThingGroupsForThingRequest.class)))
                .thenReturn(ListThingGroupsForThingResponse.builder()
                        .thingGroups(testThingGroups)
                        .build());

        List<GroupNameAndArn> thingGroups = iotService.listThingGroupsForThing(DEVICE_ID);

        verify(iotClient).listThingGroupsForThing(ListThingGroupsForThingRequest.builder()
                .thingName(DEVICE_ID)
                .maxResults(20)
                .build());
        assertThat(thingGroups, is(testThingGroups));
    }

    @Test
    public void describeThing_WhenValidRequest_ReturnsResponse() {
        Map<String, String> iotAttribute = new HashMap<>();
        iotAttribute.put(TEST_ATTRIBUTE_KEY, TEST_ATTRIBUTE_VALUE);
        DescribeThingResponse describeThingResponse = DescribeThingResponse
                .builder()
                .thingName(DEVICE_ID)
                .thingId(THING_ID)
                .thingArn(THING_ARN)
                .thingTypeName(DEVICE_TYPE_NAME)
                .attributes(iotAttribute)
                .build();
        when(iotClient.describeThing(any(DescribeThingRequest.class))).thenReturn(describeThingResponse);

        DescribeThingResponse responseFromIotService = iotService.describeThing(DEVICE_ID);
        assertEquals(DEVICE_ID, responseFromIotService.thingName());
        assertEquals(THING_ID, responseFromIotService.thingId());
        assertEquals(THING_ARN, responseFromIotService.thingArn());
        assertEquals(DEVICE_TYPE_NAME, responseFromIotService.thingTypeName());
        assertEquals(1, responseFromIotService.attributes().size());
        assertEquals(TEST_ATTRIBUTE_VALUE, responseFromIotService.attributes().get(TEST_ATTRIBUTE_KEY));
    }

    @Test
    public void getDevice_WhenValidRequest_ReturnsResponse() throws ParseException, JsonProcessingException {
        setupDeviceHappyCase(StorageState.NORMAL.toString());
        when(iotClient.listThingGroupsForThing(any(ListThingGroupsForThingRequest.class)))
                .thenReturn(ListThingGroupsForThingResponse.builder()
                        .thingGroups(ImmutableList.of(GroupNameAndArn.builder()
                                .groupName(UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId.SpecialGroup_EnabledState.name())
                                .build()))
                        .build());
        Map<String, String> expectedDeviceCapabilities = new HashMap<>();
        DeviceMetaData expectedMetaData = buildExpectedDeviceMetaData();

        GetDeviceResponseContent responseFromIotService = iotService.getDevice(DEVICE_ID);
        List<String> deviceGroupIdsList = new ArrayList<>();
        assertEquals(DEVICE_ID, responseFromIotService.getDeviceId());
        assertEquals(DEVICE_TYPE_NAME, responseFromIotService.getDeviceType());
        assertEquals(deviceGroupIdsList, responseFromIotService.getDeviceGroupIds());
        assertEquals(expectedMetaData, responseFromIotService.getDeviceMetaData());
        assertEquals(expectedDeviceCapabilities, responseFromIotService.getDeviceCapabilities());
        assertEquals(EXPECTED_DEVICE_SETTINGS_WITH_ADDITIONAL_SETTINGS_STRING, responseFromIotService.getDeviceSettings().toString());
    }

    @Test
    public void getDevice_WhenNoSDCard_ReturnsResponse() throws ParseException, JsonProcessingException {
        setupDeviceHappyCase(StorageState.NO_CARD.toString());
        when(iotClient.listThingGroupsForThing(any(ListThingGroupsForThingRequest.class)))
                .thenReturn(ListThingGroupsForThingResponse.builder()
                        .thingGroups(ImmutableList.of(GroupNameAndArn.builder()
                                .groupName(UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId.SpecialGroup_EnabledState.name())
                                .build()))
                        .build());
        Map<String, String> expectedDeviceCapabilities = new HashMap<>();

        IpAddress expectedIpAddress = IpAddress
                .builder()
                .privateIpAddress(PRIVATE_IP_VALUE)
                .publicIpAddress("")
                .build();

        DeviceConnection expectedDeviceConnection = DeviceConnection
                .builder()
                .status(true)
                .updatedAt(new Date(DATE))
                .build();

        StorageElement expectedStorageState = StorageElement
                .builder()
                .status(StorageState.NO_CARD)
                .id(SD_CARD_ID)
                .totalCapacity("0")
                .usedCapacity("0")
                .updatedAt(new Date(DATE))
                .build();

        List<StorageElement> storageList = new ArrayList<>();
        storageList.add(expectedStorageState);

        CloudVideoStreamingElement expectedVideoStreamingState = CloudVideoStreamingElement
                .builder()
                .id(DEVICE_ID)
                .status(VideoStreamingState.CONNECTED)
                .updatedAt(new Date(DATE))
                .build();
        List<CloudVideoStreamingElement> videoStreamingStateList = new ArrayList<>();
        videoStreamingStateList.add(expectedVideoStreamingState);

        DeviceStatus expectedDeviceStatus = DeviceStatus
                .builder()
                .deviceState(DeviceState.ENABLED)
                .deviceConnection(expectedDeviceConnection)
                .storage(storageList)
                .cloudVideoStreaming(videoStreamingStateList)
                .build();

        DeviceMetaData expectedMetaData = DeviceMetaData
                .builder()
                .aiChipset("")
                .aiModelVersion(AI_MODEL_VERSION_VALUE)
                .aiSdkVersion(AI_SDK_VERSION_VALUE)
                .mac(MAC_VALUE)
                .firmwareVersion(FIRMWARE_VERSION_VALUE)
                .sdkVersion("")
                .manufacturer(MANUFACTURER_VALUE)
                .model(MODEL_VALUE)
                .ipAddress(expectedIpAddress)
                .deviceStatus(expectedDeviceStatus)
                .build();

        GetDeviceResponseContent responseFromIotService = iotService.getDevice(DEVICE_ID);
        List<String> deviceGroupIdsList = new ArrayList<>();
        assertEquals(DEVICE_ID, responseFromIotService.getDeviceId());
        assertEquals(DEVICE_TYPE_NAME, responseFromIotService.getDeviceType());
        assertEquals(deviceGroupIdsList, responseFromIotService.getDeviceGroupIds());
        assertEquals(expectedMetaData, responseFromIotService.getDeviceMetaData());
        assertEquals(expectedMetaData.getDeviceStatus().getStorage().get(0).getTotalCapacity(), "0");
        assertEquals(expectedMetaData.getDeviceStatus().getStorage().get(0).getUsedCapacity(), "0");
        assertEquals(expectedDeviceCapabilities, responseFromIotService.getDeviceCapabilities());
        assertEquals(EXPECTED_DEVICE_SETTINGS_WITH_ADDITIONAL_SETTINGS_STRING, responseFromIotService.getDeviceSettings().toString());
    }

    @Test
    public void getDevice_WhenStatusNoTimestamp_ReturnsResponse() throws ParseException, JsonProcessingException {
        when(iotClient.listThingGroupsForThing(any(ListThingGroupsForThingRequest.class)))
                .thenReturn(ListThingGroupsForThingResponse.builder()
                        .thingGroups(ImmutableList.of(GroupNameAndArn.builder()
                                .groupName(UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId.SpecialGroup_EnabledState.name())
                                .build()))
                        .build());
        Map<String, String> iotAttribute = new HashMap<>();
        DescribeThingResponse describeThingResponse = DescribeThingResponse
                .builder()
                .thingName(DEVICE_ID)
                .thingId(THING_ID)
                .thingArn(THING_ARN)
                .thingTypeName(DEVICE_TYPE_NAME)
                .attributes(iotAttribute)
                .build();
        when(iotClient.describeThing(any(DescribeThingRequest.class))).thenReturn(describeThingResponse);

        GetThingShadowRequest getThingShadowRequestProvision = GetThingShadowRequest
                .builder()
                .thingName(DEVICE_ID)
                .shadowName(PROVISIONING_SHADOW_NAME)
                .build();
        GetThingShadowResponse getThingShadowResponseProvision = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildProvisionShadowPayload(StorageState.NORMAL.toString())))
                .build();
        when(iotDataPlaneClient.getThingShadow(getThingShadowRequestProvision)).thenReturn(getThingShadowResponseProvision);

        GetThingShadowRequest getThingShadowRequestVideoEncoder = GetThingShadowRequest
                .builder()
                .thingName(DEVICE_ID)
                .shadowName(VIDEO_ENCODER_SHADOW_NAME)
                .build();    
        GetThingShadowResponse getThingShadowResponseVideoEncoder = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildVideoEncoderShadowPayload()))
                .build();                
        when(iotDataPlaneClient.getThingShadow(getThingShadowRequestVideoEncoder)).thenReturn(getThingShadowResponseVideoEncoder);

        GetThingShadowRequest getThingShadowRequestAi = GetThingShadowRequest
                .builder()
                .thingName(DEVICE_ID)
                .shadowName(AI_SHADOW_NAME)
                .build();    
        GetThingShadowResponse getThingShadowResponseAi = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildAiShadowPayload()))
                .build();                
        when(iotDataPlaneClient.getThingShadow(getThingShadowRequestAi)).thenReturn(getThingShadowResponseAi);

        Map<String, String> expectedDeviceCapabilities = new HashMap<>();

        IpAddress expectedIpAddress = IpAddress
                .builder()
                .privateIpAddress(PRIVATE_IP_VALUE)
                .publicIpAddress("")
                .build();

        ThingConnectivity thingConnectivity = ThingConnectivity.builder().connected(true).timestamp(DATE_LONG).build();
        ThingDocument thingDocument = ThingDocument.builder().connectivity(thingConnectivity).build();
        when(iotClient.searchIndex(any(SearchIndexRequest.class))).thenReturn(
                SearchIndexResponse.builder().things(thingDocument).build()
        );
        DeviceConnection expectedDeviceConnection = DeviceConnection
                .builder()
                .status(true)
                .updatedAt(new Date(DATE_LONG))
                .build();

        StorageElement expectedStorageState = StorageElement
                .builder()
                .status(StorageState.NORMAL)
                .id(SD_CARD_ID)
                .totalCapacity(String.valueOf(SD_CARD_TOTAL_CAPACITY))
                .usedCapacity(String.valueOf(SD_CARD_USED_CAPACITY))
                // this field is pulled from onvif shadow, for this test onvif shadow has timestamps,
                // the UpdatedAt field is set
                .updatedAt(new Date(DATE))
                .build();

        List<StorageElement> storageList = new ArrayList<>();
        storageList.add(expectedStorageState);

        CloudVideoStreamingElement expectedVideoStreamingState = CloudVideoStreamingElement
                .builder()
                .id(DEVICE_ID)
                .status(VideoStreamingState.CONNECTED)
                .updatedAt(new Date(DATE))
                .build();
        List<CloudVideoStreamingElement> videoStreamingStateList = new ArrayList<>();
        videoStreamingStateList.add(expectedVideoStreamingState);

        DeviceStatus expectedDeviceStatus = DeviceStatus
                .builder()
                .deviceState(DeviceState.ENABLED)
                .deviceConnection(expectedDeviceConnection)
                .storage(storageList)
                .cloudVideoStreaming(videoStreamingStateList)
                .build();

        DeviceMetaData expectedMetaData = DeviceMetaData
                .builder()
                .aiChipset("")
                .aiSdkVersion(AI_SDK_VERSION_VALUE)
                .aiModelVersion(AI_MODEL_VERSION_VALUE)
                .mac(MAC_VALUE)
                .firmwareVersion(FIRMWARE_VERSION_VALUE)
                .sdkVersion("")
                .manufacturer(MANUFACTURER_VALUE)
                .model(MODEL_VALUE)
                .ipAddress(expectedIpAddress)
                .deviceStatus(expectedDeviceStatus)
                .build();

        GetDeviceResponseContent responseFromIotService = iotService.getDevice(DEVICE_ID);
        List<String> deviceGroupIdsList = new ArrayList<>();
        assertEquals(DEVICE_ID, responseFromIotService.getDeviceId());
        assertEquals(DEVICE_TYPE_NAME, responseFromIotService.getDeviceType());
        assertEquals(deviceGroupIdsList, responseFromIotService.getDeviceGroupIds());
        assertEquals(expectedMetaData, responseFromIotService.getDeviceMetaData());
        assertEquals(expectedDeviceCapabilities, responseFromIotService.getDeviceCapabilities());
        assertEquals(EXPECTED_DEVICE_SETTINGS_STRING, responseFromIotService.getDeviceSettings().toString());
    }

    @Test
    public void getDevice_WhenInvalidRequest_ThrowsException() {
        when(iotClient.describeThing(any(DescribeThingRequest.class))).thenThrow(InvalidRequestException.builder().build());

        assertThrows(InvalidRequestException.class, () -> {
            iotService.getDevice(DEVICE_ID);
        });
    }

    @Test
    public void describeThing_WhenResourceNotFound_ThrowsResourceNotFoundException() {
        when(iotClient.describeThing(any(DescribeThingRequest.class))).thenThrow(ResourceNotFoundException.class);
        assertThrows(ResourceNotFoundException.class, () -> {
           iotService.describeThing(DEVICE_ID);
        });
    }

    @Test
    public void describeThing_WhenInvalidRequest_ThrowsInvalidRequestException() {
        when(iotClient.describeThing(
                any(DescribeThingRequest.class)
        )).thenThrow(InvalidRequestException.builder().build());

        assertThrows(InvalidRequestException.class, () -> {
            iotService.describeThing(DEVICE_ID);
        });
    }

    @Test
    public void searchIndex_WhenValidRequest_ReturnsResponse() {
        SearchIndexRequest searchIndexRequest = SearchIndexRequest
                .builder()
                .indexName(IOT_FLEET_INDEXING_INDEX_AWS_THINGS)
                .queryString(FLEET_INDEXING_QUERY_STRING)
                .build();
        SearchIndexResponse searchIndexResponse = SearchIndexResponse
                .builder()
                .build();
        when(iotClient.searchIndex(any(SearchIndexRequest.class))).thenReturn(searchIndexResponse);

        iotService.searchIndex(IOT_FLEET_INDEXING_INDEX_AWS_THINGS, FLEET_INDEXING_QUERY_STRING);
        verify(iotClient).searchIndex(eq(searchIndexRequest));
    }

    @Test
    public void getDeviceShadow_WhenShadowNameExists_ReturnsResponse() {
        GetThingShadowResponse getThingShadowResponse = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildEmptyDesiredStateShadow()))
                .build();

        when(iotDataPlaneClient.getThingShadow(any(GetThingShadowRequest.class))).thenReturn(getThingShadowResponse);
        
        GetDeviceShadowResponseContent response = iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME);
        JSONObject expectedJsonObject = new JSONObject();
        assertTrue(expectedJsonObject.similar(response.getShadowPayload().getStateDocument()));
        assertEquals(response.getShadowPayload().getShadowName(), SHADOW_NAME);
    }

    @Test
    public void getDeviceShadow_WhenNullShadowName_ReturnsResponse() {
        GetThingShadowResponse getThingShadowResponse = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildEmptyDesiredStateShadow()))
                .build();

        when(iotDataPlaneClient.getThingShadow(any(GetThingShadowRequest.class))).thenReturn(getThingShadowResponse);

        GetDeviceShadowResponseContent response = iotService.getDeviceShadow(DEVICE_ID, null);
        JSONObject expectedJsonObject = new JSONObject();
        assertTrue(expectedJsonObject.similar(response.getShadowPayload().getStateDocument()));
        assertEquals(response.getShadowPayload().getShadowName(), null);
    }

    @Test
    public void getDeviceShadow_WhenResourceNotFound_ThrowsResourceNotFoundException() {
        when(iotDataPlaneClient.getThingShadow(any(GetThingShadowRequest.class))).thenThrow(
                ResourceNotFoundException.class
        );

        assertThrows(ResourceNotFoundException.class, () -> {
                iotService.getDeviceShadow(DEVICE_ID, SHADOW_NAME);
        });
    }

    private String buildProvisionShadowPayload(String sdCardStatus) {
        JSONObject provisionShadow = new JSONObject();

        JSONObject sdCardObject = new JSONObject();
        sdCardObject.put("status", sdCardStatus);
        sdCardObject.put("totalCapacity", SD_CARD_TOTAL_CAPACITY);
        sdCardObject.put("usedCapacity", SD_CARD_USED_CAPACITY);
        sdCardObject.put("updatedAt", DATE_LONG_IN_MILLIS);
        sdCardObject.put("id", SD_CARD_ID);
        
        // provision shadow's state.reported
        JSONObject capabilitiesObject = new JSONObject();
        capabilitiesObject.put(TEST_DEVICE_CAP_KEY, TEST_DEVICE_CAP_VAL);
        capabilitiesObject.put(PUBLIC_IP_KEY, PUBLIC_IP_VALUE);
        capabilitiesObject.put(AI_CHIP_SET_KEY, AI_CHIP_SET_VALUE);
        capabilitiesObject.put(AI_SDK_VERSION_KEY, AI_SDK_VERSION_VALUE);

        JSONObject reportedStateObject = new JSONObject();
        reportedStateObject.put("sdCard", sdCardObject);
        reportedStateObject.put("capabilities", capabilitiesObject);
        reportedStateObject.put(MAC_KEY, MAC_VALUE);
        reportedStateObject.put(AI_MODEL_VERSION_KEY, AI_MODEL_VERSION_VALUE);
        reportedStateObject.put(AI_SDK_VERSION_KEY, AI_SDK_VERSION_VALUE);
        reportedStateObject.put(FIRMWARE_VERSION_KEY, FIRMWARE_VERSION_VALUE);
        reportedStateObject.put(MODEL_KEY, MODEL_VALUE);
        reportedStateObject.put(MANUFACTURER_KEY, MANUFACTURER_VALUE);
        reportedStateObject.put(PRIVATE_IP_KEY, PRIVATE_IP_VALUE);
        reportedStateObject.put(RECORDING, true);

        JSONObject reportedState = new JSONObject();
        reportedState.put(SHADOW_REPORTED_KEY, reportedStateObject);
        provisionShadow.put(SHADOW_STATE_KEY, reportedState);

        // provision shadow's metadata.reported
        JSONObject timestamp = new JSONObject();
        timestamp.put("timestamp", TIMESTAMP);
        JSONObject reportedMetadataObject = new JSONObject();
        reportedMetadataObject.put(TEST_DEVICE_CAP_KEY, timestamp);
        reportedMetadataObject.put(RECORDING, timestamp);
        JSONObject reportedMetadata = new JSONObject();
        reportedMetadata.put(SHADOW_REPORTED_KEY, reportedMetadataObject);
        provisionShadow.put(SHADOW_METADATA_KEY, reportedMetadata);

        provisionShadow.put("timestamp", DATE);
        provisionShadow.put("clientToken", "token");
        provisionShadow.put("version",  "version");
        return provisionShadow.toString();
    }

    private String buildVideoEncoderShadowPayload(){
        // videoEncoder shadow reported contains two vec profiles and imagingSettings
        /*
        {
        "state": {
         "reported": {
          "videoSettings": {
           "vec1": {'profileOneVideoSettings'},
           "vec2": {'profileTwoVideoSettings'}
          }
          "imagingSettings": {
            "reversal": OFF | MIRROR | V-FLIP | ROTATE
          }
         }
        }
         */
        JSONObject videoEncoderShadow = new JSONObject();

        JSONObject resolutionForProfileOne = new JSONObject();
        resolutionForProfileOne.put("width", 1280);
        resolutionForProfileOne.put("height", 720);
        JSONObject profileOneVideoSettings = new JSONObject();
        profileOneVideoSettings.put("codec", "H264");
        profileOneVideoSettings.put("bitRateType", "CBR");
        profileOneVideoSettings.put("frameRateLimit", 30);
        profileOneVideoSettings.put("bitRateLimit", 800);
        profileOneVideoSettings.put("gopLength", 10);
        profileOneVideoSettings.put("resolution", resolutionForProfileOne);

        JSONObject resolutionForProfileTwo = new JSONObject();
        resolutionForProfileTwo.put("width", 320);
        resolutionForProfileTwo.put("height", 240);

        JSONObject profileTwoVideoSettings = new JSONObject();
        profileTwoVideoSettings.put("codec", "H264");
        profileTwoVideoSettings.put("bitRateType", "VBR");
        profileTwoVideoSettings.put("frameRateLimit", 15.0);
        profileTwoVideoSettings.put("bitRateLimit", 128);
        profileTwoVideoSettings.put("gopLength", 30);
        profileTwoVideoSettings.put("resolution", resolutionForProfileTwo);

        JSONObject videoSettings = new JSONObject();
        videoSettings.put("vec1", profileOneVideoSettings);
        videoSettings.put("vec2", profileTwoVideoSettings);

        JSONObject imagingSettings = new JSONObject();
        imagingSettings.put("reversal", "V-FLIP");

        JSONObject streamingSettings = new JSONObject();
        streamingSettings.put("type", "CONTINUOUS");

        JSONObject allSettings = new JSONObject();
        allSettings.put("videoSettings", videoSettings);
        allSettings.put("imagingSettings", imagingSettings);
        allSettings.put("streamingSettings", streamingSettings);

        JSONObject videoEncoderReportedShadow = new JSONObject();
        videoEncoderReportedShadow.put(SHADOW_REPORTED_KEY, allSettings);
        videoEncoderShadow.put(SHADOW_STATE_KEY, videoEncoderReportedShadow);

        return videoEncoderShadow.toString();
    }

    private String buildAiShadowPayload(){
        // ai shadow reported contains one profile
        /*
        {
        "state": {
         "reported": {
          "aiSettings": {
           <settings>
          }
         }
        }
         */
        JSONObject aiSettings = new JSONObject();
        aiSettings.put("mode", "intrusion");

        JSONObject aiProfiles = new JSONObject();
        aiProfiles.put(AI_SETTINGS, aiSettings);

        JSONObject aiReportedShadow = new JSONObject();
        aiReportedShadow.put(SHADOW_REPORTED_KEY, aiProfiles);

        JSONObject aiShadow = new JSONObject();
        aiShadow.put(SHADOW_STATE_KEY, aiReportedShadow);

        return aiShadow.toString();
    }

    private String buildEmptyDesiredStateShadow() {
        JSONObject emptyReported = new JSONObject();
        JSONObject reportedStateObject = new JSONObject();
        JSONObject reportedState = new JSONObject();

        reportedState.put(SHADOW_DESIRED_KEY, reportedStateObject);
        emptyReported.put(SHADOW_STATE_KEY, reportedState);
        emptyReported.put("timestamp", DATE);
        emptyReported.put("clientToken", "token");
        emptyReported.put("version",  "version");

        return emptyReported.toString();
    }

    private void setupDeviceHappyCase(String sdCardStatus) {
        Map<String, String> iotAttribute = new HashMap<>();
        DescribeThingResponse describeThingResponse = DescribeThingResponse
                .builder()
                .thingName(DEVICE_ID)
                .thingId(THING_ID)
                .thingArn(THING_ARN)
                .thingTypeName(DEVICE_TYPE_NAME)
                .attributes(iotAttribute)
                .build();
        when(iotClient.describeThing(any(DescribeThingRequest.class))).thenReturn(describeThingResponse);

        ThingConnectivity thingConnectivity = ThingConnectivity.builder().connected(true).timestamp(DATE_LONG).build();
        ThingDocument thingDocument = ThingDocument.builder().connectivity(thingConnectivity).build();
        when(iotClient.searchIndex(any(SearchIndexRequest.class))).thenReturn(
                SearchIndexResponse
                    .builder()
                    .things(thingDocument)
                    .build()
        );

        when(iotClient.listThingGroupsForThing(any(ListThingGroupsForThingRequest.class)))
                .thenReturn(ListThingGroupsForThingResponse.builder()
                        .thingGroups(ImmutableList.of(GroupNameAndArn.builder()
                                .groupName(UpdateDeviceUtils.VideoAnalyticsManagedDeviceGroupId.SpecialGroup_EnabledState.name())
                                .build()))
                        .build());

        GetThingShadowRequest getThingShadowRequestProvision = GetThingShadowRequest
                .builder()
                .thingName(DEVICE_ID)
                .shadowName(PROVISIONING_SHADOW_NAME)
                .build();
        GetThingShadowResponse getThingShadowResponseProvision = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildProvisionShadowPayload(sdCardStatus)))
                .build();
        when(iotDataPlaneClient.getThingShadow(getThingShadowRequestProvision)).thenReturn(getThingShadowResponseProvision);

        GetThingShadowRequest getThingShadowRequestVideoEncoder = GetThingShadowRequest
                .builder()
                .thingName(DEVICE_ID)
                .shadowName(VIDEO_ENCODER_SHADOW_NAME)
                .build();    

        GetThingShadowResponse getThingShadowResponseVideoEncoder = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildVideoEncoderShadowPayload()))
                .build();                
        when(iotDataPlaneClient.getThingShadow(getThingShadowRequestVideoEncoder)).thenReturn(getThingShadowResponseVideoEncoder);

        GetThingShadowRequest getThingShadowRequestAi = GetThingShadowRequest
                .builder()
                .thingName(DEVICE_ID)
                .shadowName(AI_SHADOW_NAME)
                .build();    

        GetThingShadowResponse getThingShadowResponseAi = GetThingShadowResponse
                .builder()
                .payload(SdkBytes.fromUtf8String(buildAiShadowPayload()))
                .build();                
        when(iotDataPlaneClient.getThingShadow(getThingShadowRequestAi)).thenReturn(getThingShadowResponseAi);
    }

    private DeviceMetaData buildExpectedDeviceMetaData() {
        IpAddress expectedIpAddress = IpAddress
                .builder()
                .privateIpAddress(PRIVATE_IP_VALUE)
                .publicIpAddress("")
                .build();

        DeviceConnection expectedDeviceConnection = DeviceConnection
                .builder()
                .status(true)
                .updatedAt(new Date(DATE))
                .build();

        StorageElement expectedStorageState = StorageElement
                .builder()
                .status(StorageState.NORMAL)
                .id(SD_CARD_ID)
                .totalCapacity(String.valueOf(SD_CARD_TOTAL_CAPACITY))
                .usedCapacity(String.valueOf(SD_CARD_USED_CAPACITY))
                .updatedAt(new Date(DATE))
                .build();

        List<StorageElement> storageList = new ArrayList<>();
        storageList.add(expectedStorageState);

        CloudVideoStreamingElement expectedVideoStreamingState = CloudVideoStreamingElement
                .builder()
                .id(DEVICE_ID)
                .status(VideoStreamingState.CONNECTED)
                .updatedAt(new Date(DATE))
                .build();
        List<CloudVideoStreamingElement> videoStreamingStateList = new ArrayList<>();
        videoStreamingStateList.add(expectedVideoStreamingState);

        DeviceStatus expectedDeviceStatus = DeviceStatus
                .builder()
                .deviceState(DeviceState.ENABLED)
                .deviceConnection(expectedDeviceConnection)
                .storage(storageList)
                .cloudVideoStreaming(videoStreamingStateList)
                .build();

        return DeviceMetaData
                .builder()
                .aiChipset("")
                .aiModelVersion(AI_MODEL_VERSION_VALUE)
                .aiSdkVersion(AI_SDK_VERSION_VALUE)
                .mac(MAC_VALUE)
                .firmwareVersion(FIRMWARE_VERSION_VALUE)
                .sdkVersion("")
                .manufacturer(MANUFACTURER_VALUE)
                .model(MODEL_VALUE)
                .ipAddress(expectedIpAddress)
                .deviceStatus(expectedDeviceStatus)
                .build();
    }
}
