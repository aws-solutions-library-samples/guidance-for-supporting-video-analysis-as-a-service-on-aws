package org.example;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.DeleteThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.GetThingShadowRequest;
import software.amazon.awssdk.services.iotdataplane.model.IotDataPlaneException;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import software.amazon.awssdk.services.kinesisvideo.KinesisVideoClient;
import software.amazon.awssdk.services.kinesisvideo.model.DeleteSignalingChannelRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeSignalingChannelResponse;
import software.amazon.awssdk.services.kinesisvideo.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesisvideo.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesisvideo.model.KinesisVideoException;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;

public class Handler {
    private final IotClient iotClient;
    private final IotDataPlaneClient iotDataPlaneClient;
    private final KinesisVideoClient kvsClient;

    public Handler() {
        iotClient = DependencyFactory.iotClient();
        iotDataPlaneClient = DependencyFactory.iotDataPlaneClient();
        kvsClient = DependencyFactory.kinesisVideoClient();
    }

    public void sendRequest() {
        updateShadowForConfiguration("AnnieThing1210");
    }

    public void deleteDevice(final String deviceId) {
        // Validate if device exists
        System.out.println("Checking if device exist.");
        try {
            iotClient.describeThing(DescribeThingRequest.builder().thingName(deviceId).build());
        } catch (ResourceNotFoundException e){
            System.out.printf("Thing %s doesn't exist.%n", deviceId);
            return;
        }

        System.out.println("Deleting thing shadows");

        // null represents classic shadow which does not have a name
        // If more shadows are created for additional features, add them here
        List<String> shadowNames = Arrays.asList(null, "provision", "snapshot", "videoEncoder");

        for (String shadowName: shadowNames) {
            try {
                GetThingShadowRequest getThingShadowRequest = GetThingShadowRequest.builder()
                    .thingName(deviceId)
                    .shadowName(shadowName)
                    .build();
                iotDataPlaneClient.getThingShadow(getThingShadowRequest);
                // if ResourceNotFoundException is not thrown, shadow exists
                try {
                    DeleteThingShadowRequest deleteThingShadowRequest = DeleteThingShadowRequest.builder()
                        .thingName(deviceId)
                        .shadowName(shadowName)
                        .build();
                    iotDataPlaneClient.deleteThingShadow(deleteThingShadowRequest);
                } catch (IotDataPlaneException e) {
                    System.out.println(e);
                }
            } catch (software.amazon.awssdk.services.iotdataplane.model.ResourceNotFoundException e) {
                // shadow does not exist, no-op
            }
        }

        ListThingPrincipalsRequest listThingPrincipalsRequest = ListThingPrincipalsRequest.builder()
            .thingName(deviceId)
            .build();
        try {
            List<String> principals = iotClient.listThingPrincipals(listThingPrincipalsRequest).principals();

            System.out.println("Detaching cert from device");

            for (String principal: principals) {
                DetachThingPrincipalRequest detachThingPrincipalRequest = DetachThingPrincipalRequest.builder()
                    .principal(principal)
                    .thingName(deviceId)
                    .build();
                iotClient.detachThingPrincipal(detachThingPrincipalRequest);
    
                Arn arn = Arn.fromString(principal);
                ArnResource arnResource = arn.resource();
                if (arnResource.resourceType().orElse("").equals("cert")) {
                    ListAttachedPoliciesRequest listAttachedPoliciesRequest = ListAttachedPoliciesRequest.builder()
                        .target(principal)
                        .build();
                    List<Policy> policies = iotClient.listAttachedPolicies(listAttachedPoliciesRequest).policies();

                    System.out.println("Detaching policies from cert");

                    for (Policy policy: policies) {
                        DetachPolicyRequest detachPolicyRequest = DetachPolicyRequest.builder()
                            .policyName(policy.policyName())
                            .target(principal)
                            .build();
                        iotClient.detachPolicy(detachPolicyRequest);
                    }

                    // Certificate must be deactivated before being deleted
                    System.out.println("Deactivating cert");

                    String certificateId = arnResource.resource();
                    UpdateCertificateRequest updateCertificateRequest = UpdateCertificateRequest.builder()
                        .certificateId(certificateId)
                        .newStatus(CertificateStatus.INACTIVE)
                        .build();
                    iotClient.updateCertificate(updateCertificateRequest);

                    System.out.println("Deleting cert");

                    DeleteCertificateRequest deleteCertificateRequest = DeleteCertificateRequest.builder()
                        .certificateId(certificateId)
                        .build();
                    iotClient.deleteCertificate(deleteCertificateRequest);
                }
            }
        } catch (IotException e) {
            System.out.println(e);
        }

        System.out.println("Deleting KVS stream");

        // By default, only 1 KVS stream (name=deviceId) is created.
        // If more are created, add them here
        try {
            DescribeStreamRequest describeStreamRequest = DescribeStreamRequest.builder()
                    .streamName(deviceId)
                    .build();
            DescribeStreamResponse describeStreamResponse = kvsClient.describeStream(describeStreamRequest);
            DeleteStreamRequest deleteStreamRequest = DeleteStreamRequest.builder()
                .streamARN(describeStreamResponse.streamInfo().streamARN())
                .build();
            kvsClient.deleteStream(deleteStreamRequest);
        } catch (KinesisVideoException e) {
            System.out.println(e);
        }

        System.out.println("Deleting KVS signaling channels");

        // By default, only 2 KVS signaling channels are created with deviceId and the following suffixes.
        // If more are created, add them here
        List<String> channelSuffixes = List.of("%s-LiveStreamSignalingChannel", "%s-PlaybackSignalingChannel");

        for (String suffix: channelSuffixes) {  
            String signalingChannelName = String.format(suffix, deviceId);  
            try {
                DescribeSignalingChannelRequest describeSignalingChannelRequest = DescribeSignalingChannelRequest.builder()
                    .channelName(signalingChannelName)
                    .build();
                DescribeSignalingChannelResponse describeSignalingChannelResponse = kvsClient.describeSignalingChannel(describeSignalingChannelRequest);
                DeleteSignalingChannelRequest deleteSignalingChannelRequest = DeleteSignalingChannelRequest.builder()
                    .channelARN(describeSignalingChannelResponse.channelInfo().channelARN())
                    .build();
                kvsClient.deleteSignalingChannel(deleteSignalingChannelRequest);
            } catch (KinesisVideoException e) {
                System.out.println(e);
            }
        }

        System.out.println("Deleting thing");
        DeleteThingRequest deleteThingRequest = DeleteThingRequest.builder()
            .thingName(deviceId)
            .build();
        try {
            iotClient.deleteThing(deleteThingRequest);
        } catch (IotException e) {
            System.out.println(e);
        }
    }

    public void updateShadowForConfiguration(final String thingName) {
        // Validate if device exists
        System.out.println("Checking if device exist.");
        try {
            iotClient.describeThing(DescribeThingRequest.builder().thingName(thingName).build());
        } catch (ResourceNotFoundException e){
            System.out.printf("Thing %s doesn't exist.%n", thingName);
            return;
        }

        System.out.println("Updating IoT Device Shadow");

        // Set this value to the shadow name
        String shadowName = "videoEncoder";

        // Creating a json object with the desired configuration
        // Recommended to validate desired configuration input if not hardcoded
        JsonObject configurationJson = new JsonObject();
        JsonObject videoSettings = new JsonObject();
        JsonObject vec1 = new JsonObject();
        vec1.addProperty("name", "GuidanceConfiguration");
        vec1.addProperty("bitRateType", "CBR");
        vec1.addProperty("bitRate", 512);
        vec1.addProperty("frameRate", 15);
        vec1.addProperty("gopRange", 30);
        vec1.addProperty("resolution", "1920x1080");
        videoSettings.add("vec1", vec1);
        configurationJson.add("videoSettings", videoSettings);

        // Create desired state
        JsonObject desired = new JsonObject();
        desired.add("desired", configurationJson);

        // Create state document
        JsonObject messagePayload = new JsonObject();
        messagePayload.add("state", desired);

        UpdateThingShadowRequest updateThingShadowRequest = UpdateThingShadowRequest.builder()
            .thingName(thingName)
            .shadowName(shadowName)
            .payload(SdkBytes.fromUtf8String(messagePayload.toString()))
            .build();

        try {
            iotDataPlaneClient.updateThingShadow(updateThingShadowRequest);
        } catch (IotDataPlaneException e) {
            System.out.println(e);
        }
    }
}
