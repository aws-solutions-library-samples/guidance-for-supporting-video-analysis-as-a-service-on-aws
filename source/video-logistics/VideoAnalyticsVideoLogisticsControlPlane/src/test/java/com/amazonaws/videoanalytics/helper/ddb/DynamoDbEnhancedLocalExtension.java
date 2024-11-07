package com.amazonaws.videoanalytics.helper.ddb;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;


/**
 * Junit5 extension for functionally testing classes that use DynamoDBEnhanced client. Injects an instance of DynamoDbEnhancedClient
 * and/or DynamoDbClient that are connected to a locally-running instance of DynamoDb. The extension takes care of the following:
 *
 * <ul>
 *     <li>Any parameter of type {@link DynamoDbTable} will be automatically created.</li>
 *     <li>Before each test method, all of the tables specified in the constructor are deleted and re-created.</li>
 *     <li>When the test suite completes, the local DynamoDb server is shut down.</li>
 * </ul>
 */
public class DynamoDbEnhancedLocalExtension implements
        BeforeAllCallback, AfterAllCallback, BeforeEachCallback, ParameterResolver {

    private static final String REFLECTIVE_CONSTRUCTOR_ERROR_MESSAGE = "Could not construct instance of %s. Please make"
            + " sure it has a public, no args constructor.";

    private static final Set<Class<?>> SUPPORTED_TYPES = ImmutableSet.of(DynamoDbTable.class,
            DynamoDbAsyncTable.class,
            DynamoDbEnhancedClient.class,
            DynamoDbEnhancedAsyncClient.class,
            DynamoDbClient.class,
            DynamoDbAsyncClient.class);

    private final Map<String, TableModel> tables = new HashMap<>();

    private TableSchemaProvider tableSchemaProvider;
    private LocalDynamoDb localDynamoDb;
    private DynamoDbClient dynamoDbClient;
    private DynamoDbAsyncClient dynamoDbAsyncClient;
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    /**
     * Start the server and create the DynamoDb clients for re-use across all tests.
     */
    @Override
    public void beforeAll(final ExtensionContext context) {
        final Optional<DynamoDbLocalSettings> settings = getSettings(context);
        final DynamoDbEnhancedClientProvider enhancedClientProvider = getEnhancedClientProvider(settings);
        tableSchemaProvider = getTableSchemaProvider(settings);
        final int port = getPort(settings);

        localDynamoDb = new LocalDynamoDb(port);
        localDynamoDb.start();

        dynamoDbClient = localDynamoDb.createClient();
        dynamoDbEnhancedClient = enhancedClientProvider.getEnhancedClient(dynamoDbClient);

        dynamoDbAsyncClient = localDynamoDb.createAsyncClient();
        dynamoDbEnhancedAsyncClient = enhancedClientProvider.getEnhancedAsyncClient(dynamoDbAsyncClient);
    }

    /**
     * Clean up if necessary and create the tables that were injected as parameters.
     */
    @Override
    public void beforeEach(final ExtensionContext context) {
        tables.values().forEach(this::deleteIfExists);
        tables.values().forEach(TableModel::create);
    }

    /**
     * Cleanup and stop the dynamodb local server.
     */
    @Override
    public void afterAll(final ExtensionContext context) {
        localDynamoDb.stop();
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {

        return SUPPORTED_TYPES.contains(parameterContext.getParameter().getType());
    }

    /**
     * Resolves the three types of supported parameters: {@link DynamoDbEnhancedClient}, {@link DynamoDbClient} and any number of
     * {@link DynamoDbTable}. Stores references to the tables, so that they can be created in the actual database once the tests are run.
     */
    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {

        final Class<?> type = parameterContext.getParameter().getType();
        if (type.equals(DynamoDbTable.class) || type.equals(DynamoDbAsyncTable.class)) {
            // Extract DynamoDB bean from the parametrized DynamoDbTable type
            final ParameterizedType parameterizedType = (ParameterizedType) parameterContext.getParameter().getParameterizedType();
            Preconditions.checkArgument(
                    parameterizedType.getActualTypeArguments().length == 1,
                    "DynamoDbTable should have 1 type argument"
            );
            final Class<?> bean = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            final String tableName = parameterContext.findAnnotation(Table.class)
                    .map(Table::tableName)
                    .orElse(bean.getSimpleName());

            final TableSchema<?> tableSchema = tableSchemaProvider.getTableSchema(bean);
            final DynamoDbTable<?> table = dynamoDbEnhancedClient.table(tableName, tableSchema);
            tables.putIfAbsent(tableName, new TableModel(table));
            return type.equals(DynamoDbAsyncTable.class) ?
                    dynamoDbEnhancedAsyncClient.table(tableName, tableSchema) :
                    table;
        } else if (type.equals(DynamoDbClient.class)) {
            return dynamoDbClient;
        } else if (type.equals(DynamoDbEnhancedClient.class)) {
            return dynamoDbEnhancedClient;
        } else if (type.equals(DynamoDbAsyncClient.class)) {
            return dynamoDbAsyncClient;
        } else if (type.equals(DynamoDbEnhancedAsyncClient.class)) {
            return dynamoDbEnhancedAsyncClient;
        }
        throw new IllegalArgumentException(String.format("Unsupported parameter type '%s'", type.getName()));
    }

    private Optional<DynamoDbLocalSettings> getSettings(final ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getElement(), DynamoDbLocalSettings.class);
    }

    private DynamoDbEnhancedClientProvider getEnhancedClientProvider(final Optional<DynamoDbLocalSettings> settings) {
        if (!settings.isPresent()) {
            return new DefaultEnhancedClientProvider();
        }
        final Class<? extends DynamoDbEnhancedClientProvider> providerClass = settings.get().enhancedClientProvider();
        try {
            final Constructor<? extends DynamoDbEnhancedClientProvider> constructor = providerClass.getConstructor();
            return constructor.newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalArgumentException(String.format(REFLECTIVE_CONSTRUCTOR_ERROR_MESSAGE,
                    providerClass.getName()), e);
        }
    }

    private TableSchemaProvider getTableSchemaProvider(final Optional<DynamoDbLocalSettings> settings) {
        if (!settings.isPresent()) {
            return new DefaultTableSchemaProvider();
        }
        final Class<? extends TableSchemaProvider> providerClass = settings.get().tableSchemaProvider();
        try {
            final Constructor<? extends TableSchemaProvider> constructor = providerClass.getConstructor();
            return constructor.newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new IllegalArgumentException(String.format(REFLECTIVE_CONSTRUCTOR_ERROR_MESSAGE,
                    providerClass.getName()), e);
        }
    }

    private int getPort(final Optional<DynamoDbLocalSettings> settings) {
        if (!settings.isPresent()) {
            return 0; // a value outside [1, 65535] means random port
        }
        return settings.get().port();
    }

    private void deleteIfExists(final TableModel table) {
        final List<String> currentTableNames = dynamoDbClient.listTables().tableNames();
        if (currentTableNames.contains(table.getName())) {
            dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(table.getName()).build());
        }
    }
}
