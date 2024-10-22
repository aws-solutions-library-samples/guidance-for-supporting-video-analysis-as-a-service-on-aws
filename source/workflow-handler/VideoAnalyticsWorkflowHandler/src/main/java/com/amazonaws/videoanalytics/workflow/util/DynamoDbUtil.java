package com.amazonaws.videoanalytics.workflow.util;

import java.util.Optional;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import static com.amazonaws.videoanalytics.workflow.util.Constants.DATASET_WORKFLOW_NAME;
import static com.amazonaws.videoanalytics.workflow.util.Constants.NULL_ATTRIBUTE_KEY_METRIC;

public class DynamoDbUtil {
    private MetricsLogger metricsLogger;
    private static final Logger LOG = LogManager.getLogger(DynamoDbUtil.class);

    public DynamoDbUtil(MetricsLogger metricsLogger) {
        this.metricsLogger = metricsLogger;
    }

    public Optional<String> getValueFromRecord(final DynamodbStreamRecord record,
                                               final String attributeKey) {
        /* default: try to get item as string */
        Optional<String> value = Optional.ofNullable(record.getDynamodb())
                .map(dynamo -> dynamo.getNewImage())
                .map(attributes -> {
                    if (attributeKey.equals(Constants.WORKFLOW_NAME)) {
                        /* this is for dataset ddb tables as they use different naming convention */
                        return attributes.getOrDefault(attributeKey, attributes.get(DATASET_WORKFLOW_NAME));
                    }
                    return attributes.get(attributeKey);
                })
                .map(attribute -> attribute.getS());

        /* otherwise, try to get item as number */
        if (value.isEmpty()) {
            value = Optional.ofNullable(record.getDynamodb())
                    .map(dynamo -> dynamo.getNewImage())
                    .map(attributes -> attributes.get(attributeKey))
                    .map(attribute -> attribute.getN());
        }

        if (value.isEmpty()) {
            publishMetric(NULL_ATTRIBUTE_KEY_METRIC + attributeKey);
        }
        return value;
    }

    private void publishMetric(final String metricName) {
        LOG.info("Publishing metric {} to CloudWatch.", metricName);
        try {
            metricsLogger.putMetric(metricName, 1, Unit.COUNT);
        } catch (InvalidMetricException e) {
            LOG.error("InvalidMetricException while publishing metric: ", e);
        } finally {
            metricsLogger.flush();
        }
    }
}
