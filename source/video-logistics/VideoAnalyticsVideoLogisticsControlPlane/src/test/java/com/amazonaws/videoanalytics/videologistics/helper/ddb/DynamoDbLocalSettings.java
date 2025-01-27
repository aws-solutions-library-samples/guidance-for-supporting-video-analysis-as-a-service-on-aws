package com.amazonaws.videoanalytics.videologistics.helper.ddb;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A method for customizing the behavior of the {@link DynamoDbEnhancedLocalExtension}. This
 * annotation can be present on the test class directly, any parent class of the test class, or
 * via a meta annotation on the test class or any parent class.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamoDbLocalSettings {

    Class<? extends DynamoDbEnhancedClientProvider> enhancedClientProvider()
            default DefaultEnhancedClientProvider.class;

    Class<? extends TableSchemaProvider> tableSchemaProvider()
            default DefaultTableSchemaProvider.class;

    int port() default 0; // a value outside [1, 65535] means random port
}
