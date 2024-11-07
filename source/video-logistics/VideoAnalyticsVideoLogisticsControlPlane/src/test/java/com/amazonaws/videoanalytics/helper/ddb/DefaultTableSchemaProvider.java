package com.amazonaws.videoanalytics.helper.ddb;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Default implementation of a {@link TableSchemaProvider} which provides a table schema through the
 * standard mechanism of introspection of an annotated class.
 */
public class DefaultTableSchemaProvider implements TableSchemaProvider {

    @Override
    public <T> TableSchema<T> getTableSchema(final Class<T> annotatedClass) {
        return TableSchema.fromClass(annotatedClass);
    }
}
