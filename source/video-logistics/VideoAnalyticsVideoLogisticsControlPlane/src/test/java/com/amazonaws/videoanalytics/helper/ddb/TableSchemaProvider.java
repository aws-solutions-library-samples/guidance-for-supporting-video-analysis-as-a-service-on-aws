package com.amazonaws.videoanalytics.helper.ddb;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Provides a table schema for a given class. Implementations of this interface are expected to have
 * a public, no args constructor.
 */
public interface TableSchemaProvider {

    <T> TableSchema<T> getTableSchema(Class<T> type);
}
