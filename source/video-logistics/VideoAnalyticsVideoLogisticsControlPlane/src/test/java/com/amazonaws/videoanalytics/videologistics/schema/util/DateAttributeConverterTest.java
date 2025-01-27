package com.amazonaws.videoanalytics.videologistics.schema.util;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;

import java.util.Date;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateAttributeConverterTest {

    private final DateAttributeConverter converter = new DateAttributeConverter();

    @Test
    public void transformFrom_validDate_correctAttributeValue() {
        Date date = Date.from(Instant.parse("2023-10-01T00:00:00Z"));
        AttributeValue attributeValue = converter.transformFrom(date);
        assertEquals("2023-10-01T00:00:00Z", attributeValue.s());
    }

    @Test
    public void transformTo_validAttributeValue_correctDate() {
        AttributeValue attributeValue = AttributeValue.builder().s("2023-10-01T00:00:00Z").build();
        Date date = converter.transformTo(attributeValue);
        assertEquals(Date.from(Instant.parse("2023-10-01T00:00:00Z")), date);
    }

    @Test
    public void type_always_correctType() {
        assertEquals(Date.class, converter.type().rawClass());
    }

    @Test
    public void attributeValueType_always_correctAttributeValueType() {
        assertEquals(AttributeValueType.S, converter.attributeValueType());
    }
}