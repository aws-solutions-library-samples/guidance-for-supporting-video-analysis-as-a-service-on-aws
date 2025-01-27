package com.amazonaws.videoanalytics.videologistics.client.opensearch;

import com.amazonaws.videoanalytics.videologistics.utils.InferenceUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.action.search.CreatePitRequest;
import org.opensearch.action.search.CreatePitResponse;
import org.opensearch.action.search.DeletePitRequest;
import org.opensearch.action.search.DeletePitResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.core.rest.RestStatus;

import java.io.IOException;

import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.CONNECTION_TIMEOUT;

// The reason for wrapping the RestHighLevelClient is due to RestHighLevelClient final modifiers
public class RestHighLevelClientWrapper {
    private final RestHighLevelClient client;
    private final HttpRequestInterceptor interceptor;

    public RestHighLevelClientWrapper(final HttpRequestInterceptor interceptor,
                                      final String endpoint) {
        this.interceptor = interceptor;
        this.client = create(String.format("https://%s", endpoint));
    }

    public SearchResponse search(final SearchRequest searchRequest, final RequestOptions options) throws IOException {
        return client.search(searchRequest, options);
    }
    public BulkResponse bulkIndex(final BulkRequest request) throws IOException {
            return client.bulk(request, RequestOptions.DEFAULT);
    }

    public SearchResponse search(final SearchScrollRequest searchRequest, final RequestOptions options) throws IOException {
        return client.scroll(searchRequest, options);
    }

    public String createPit(final String modelName, final long expirationInSeconds) throws IOException {
        String dataStream = InferenceUtils.getOpenSearchDataStream(modelName, "*");
        CreatePitRequest request = new CreatePitRequest(TimeValue.timeValueSeconds(expirationInSeconds), false, dataStream);
        CreatePitResponse response = client.createPit(request, RequestOptions.DEFAULT);
        return response.getId();
    }

    public boolean deletePit(String pitId) throws IOException {
        DeletePitRequest request = new DeletePitRequest(pitId);
        DeletePitResponse response = client.deletePit(request, RequestOptions.DEFAULT);
        return RestStatus.OK.equals(response.status());
    }


    private RestHighLevelClient create(final String endpoint) {
        return new RestHighLevelClient(
                RestClient.builder(HttpHost.create(endpoint))
                        .setHttpClientConfigCallback(
                                httpClientConfigCallback -> httpClientConfigCallback.addInterceptorLast(interceptor))
                        .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(CONNECTION_TIMEOUT)
                        .setSocketTimeout(CONNECTION_TIMEOUT)));
    }
}