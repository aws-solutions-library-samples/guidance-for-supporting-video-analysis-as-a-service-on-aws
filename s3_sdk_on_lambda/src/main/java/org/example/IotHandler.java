package org.example;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Collections;
import java.util.UUID;

public class IotHandler {
    private final IotClient iotClient;

    public IotHandler() {
        iotClient = DependencyFactory.iotClient();
    }

    public void sendRequest() {
        sendIotJobRequest(iotClient);
        // listAllThings(iotClient);
    }

    public void sendIotJobRequest(IotClient iotClient) {

        final String IOT_FLEET_INDEXING_CONNECTIVITY = "connectivity.connected:";
        final String IOT_FLEET_INDEXING_THING_NAME = "thingName:";
        final String IOT_FLEET_INDEXING_INDEX_NAME = "AWS_Things";


        // IoT thingName of the device that will be the target of the remote operation
        final String thingName = "PengSoftwareUpdate0613"; // replace it with the thingName of the device

        // Validate if device exists
        try {
            iotClient.describeThing(DescribeThingRequest.builder().thingName(thingName).build());
        } catch (ResourceNotFoundException e){
            System.out.printf("Thing %s doesn't exist.%n", thingName);
            return;
        }

        System.out.println("Checking if device is connected");
        System.out.printf("Searching within index: %s", IOT_FLEET_INDEXING_INDEX_NAME);

        // generate the query "connectivity.connected:true AND thingName:exampleThing"
        String deviceConnectedQuery = IOT_FLEET_INDEXING_CONNECTIVITY + "true" + " AND " + IOT_FLEET_INDEXING_THING_NAME + thingName;
        SearchIndexRequest searchIndexRequest = SearchIndexRequest
                .builder()
                .indexName(IOT_FLEET_INDEXING_INDEX_NAME)
                .queryString(deviceConnectedQuery)
                .build();

        SearchIndexResponse searchIndexResponse = iotClient.searchIndex(searchIndexRequest);
        // Do nothing if device is not connected. Only proceed to perform remote operations on a device that's connected
        if (searchIndexResponse.things().size() != 1) {
            System.out.println("Device is not connected. No-op for remote operation!");
            return;
        };

        System.out.println("Creating IoT Job for AWS-Run-Command Template");

//        final String region = "us-east-1";
//        final String accountId = "240808650765"; // replace it with customer's 12 digits AWS account ID

        final String region = "us-west-2";
        final String accountId = "422731239694"; // replace it with customer's 12 digits AWS account ID

        String target = String.format("arn:aws:iot:%s:%s:thing/%s",
                region,
                accountId,
                thingName);

        CreateJobRequest jobRequest = CreateJobRequest
                .builder()
                // Required field for IoT API CreaetJob
                //.jobId(UUID.randomUUID().toString())
                .jobId("testing")
                // Required field for IoT API CreaetJob
                .targets(Collections.singletonList(target))
                // Optional field for IoT API CreaetJob
                .targetSelection(TargetSelection.SNAPSHOT)
                // Optional field for IoT API CreaetJob
                .description("This is a job for remotely reboot a device")
                // Optional field for IoT API CreaetJob
                // documentParameters payload for reboot device:
                // TODO: add the payload
                .documentParameters(Collections.singletonMap("command", "REBOOT"))
                // Optional field for IoT API CreaetJob. Set timeout to 10 minutes in this example
                .timeoutConfig(t -> t.inProgressTimeoutInMinutes(10L))
                // Optional field for IoT API CreaetJob
                .jobExecutionsRetryConfig(j -> {
                    j.criteriaList(Collections.singleton(
                                    RetryCriteria.builder()
                                            .failureType(RetryableFailureType.ALL)
                                            // Set retries to 10 in this example
                                            .numberOfRetries(10)
                                            .build()
                            )
                    );
                })
                .jobTemplateArn(String.format("arn:aws:iot:%s::jobtemplate/AWS-Run-Command:1.0", region))
                .build();

        try {
            iotClient.createJob(jobRequest);
        } catch (IotException e) {
            System.out.println(e);
        }

    }

    public static void listAllThings(IotClient iotClient) {
        iotClient.listThingsPaginator(ListThingsRequest.builder()
                        .maxResults(10)
                        .build())
                .stream()
                .flatMap(response -> response.things().stream())
                .forEach(attribute -> {
                    System.out.println("Thing name: " + attribute.thingName());
                    System.out.println("Thing ARN: " + attribute.thingArn());
                });
    }
}
