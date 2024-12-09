package com.amazonaws.videoanalytics.videologistics.client.opensearch;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import org.apache.http.HttpRequestInterceptor;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.client.RequestOptions;

import javax.inject.Named;
import java.io.IOException;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_BULK_REQUEST;
import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_SEARCH_REQUEST;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.OPENSEARCH_INTERCEPTOR_NAME;

public class OpenSearchClient {
    private final RestHighLevelClientWrapper restClient;
    private final HttpRequestInterceptor interceptor;

    @AssistedInject
    public OpenSearchClient(@Named(OPENSEARCH_INTERCEPTOR_NAME) final HttpRequestInterceptor interceptor,
                            final @Assisted String endpoint) {
        this.interceptor = interceptor;
        this.restClient = create(endpoint);
    }

    public OpenSearchClient(@Named(OPENSEARCH_INTERCEPTOR_NAME) final HttpRequestInterceptor interceptor,
                            final RestHighLevelClientWrapper restClient) {
        this.interceptor = interceptor;
        this.restClient = restClient;
    }

    private RestHighLevelClientWrapper create(final String endpoint) {
        return new RestHighLevelClientWrapper(interceptor, endpoint);
    }

    public SearchResponse search(final ActionRequest searchRequest) throws IOException {
        if (searchRequest == null) {
            throw new RuntimeException(INVALID_SEARCH_REQUEST);
        }
        if (searchRequest.getClass() == SearchRequest.class) {
            return this.search((SearchRequest) searchRequest);
        } else {
            return this.search((SearchScrollRequest) searchRequest);
        }
    }

    private SearchResponse search(final SearchRequest searchRequest) throws IOException {
        return this.restClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    private SearchResponse search(final SearchScrollRequest searchRequest) throws IOException {
        return this.restClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    public BulkResponse bulkIndex(final BulkRequest request) throws IOException {
        if (request == null) {
            throw new RuntimeException(INVALID_BULK_REQUEST);
        }
        return this.restClient.bulkIndex(request);
    }

    public String createPit(final String modelName, final long expirationInSeconds) throws IOException {
        return this.restClient.createPit(modelName, expirationInSeconds);
    }
}