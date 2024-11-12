package com.amazonaws.videoanalytics.videologistics.helper.ddb;

import static java.util.stream.Collectors.toList;

import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TableModel {
    @Getter
    @EqualsAndHashCode.Include
    private final String name;
    private final DynamoDbTable<?> table;
    private final CreateTableEnhancedRequest createRequest;

    @Builder
    public TableModel(final DynamoDbTable<?> table) {
        this.name = table.tableName();
        this.table = table;
        this.createRequest = buildCreateRequest(table.tableSchema());
    }

    public void create() {
        table.createTable(createRequest);
    }

    /**
     * Constructs a CreateTableEnhancedRequest from the table's schema. The default
     * {@link DynamoDbTable#createTable()} method does not create the table indices, see
     * https://github.com/aws/aws-sdk-java-v2/issues/1771. This method will inspect the index
     * information from the table's schema to supplement the CreateTableEnhancedRequest. The convention
     * is that global indices should have an explicit partition key value and that local indices should
     * have an empty partition key value (in line with the java doc of the DynamoDbSecondaryPartitionKey
     * and DynamoDbSecondarySortKey annotations).
     * <p>
     * For simplicity, it is assumed that the projection type for the index is "ALL". Supporting other
     * projection types could be a future feature enhancement via supplementary information provided in
     * the DynamoDbLocalSettings annotation.
     *
     * @param tableSchema The table's schema.
     * @return A CreateTableEnhancedRequest which includes index information.
     */
    private CreateTableEnhancedRequest buildCreateRequest(final TableSchema<?> tableSchema) {
        final TableMetadata tableMd = tableSchema.tableMetadata();

        final List<EnhancedGlobalSecondaryIndex> globalIndices = tableMd.indices().stream()
                .filter(indexMd -> !TableMetadata.primaryIndexName().equals(indexMd.name()))
                .filter(indexMd -> indexMd.partitionKey().isPresent())
                .map(indexMd -> EnhancedGlobalSecondaryIndex.builder()
                        .indexName(indexMd.name())
                        .projection(projection -> projection.projectionType(ProjectionType.ALL))
                        .build())
                .collect(toList());

        final List<EnhancedLocalSecondaryIndex> localIndices = tableMd.indices().stream()
                .filter(indexMd -> !TableMetadata.primaryIndexName().equals(indexMd.name()))
                .filter(indexMd -> !indexMd.partitionKey().isPresent())
                .map(indexMd -> EnhancedLocalSecondaryIndex.builder()
                        .indexName(indexMd.name())
                        .projection(projection -> projection.projectionType(ProjectionType.ALL))
                        .build())
                .collect(toList());

        final CreateTableEnhancedRequest.Builder builder = CreateTableEnhancedRequest.builder();
        if (!globalIndices.isEmpty()) {
            builder.globalSecondaryIndices(globalIndices);
        }
        if (!localIndices.isEmpty()) {
            builder.localSecondaryIndices(localIndices);
        }
        return builder.build();
    }
}
