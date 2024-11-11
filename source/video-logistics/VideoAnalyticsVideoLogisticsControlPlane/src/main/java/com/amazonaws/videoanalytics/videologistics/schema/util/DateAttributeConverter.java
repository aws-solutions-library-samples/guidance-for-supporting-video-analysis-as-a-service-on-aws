package com.amazonaws.videoanalytics.videologistics.schema.util;


import java.util.Date;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.InstantAsStringAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class DateAttributeConverter implements AttributeConverter<Date> {

    private final InstantAsStringAttributeConverter instantAsStringAttributeConverter = InstantAsStringAttributeConverter.create();

    @Override
    public AttributeValue transformFrom(final Date input) {
        return instantAsStringAttributeConverter.transformFrom(input.toInstant());
    }

    @Override
    public Date transformTo(final AttributeValue input) {
        return Date.from(instantAsStringAttributeConverter.transformTo(input));
    }

    @Override
    public EnhancedType<Date> type() {
        return new EnhancedType<Date>() {
        };
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
