package com.amazonaws.videoanalytics.videologistics.client.opensearch;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OpenSearchClientProvider {

    private OpenSearchClientFactory openSearchClientFactory;

    private static ConcurrentMap<String, OpenSearchClient> openSearchClientMap = new ConcurrentHashMap<>();

    public OpenSearchClientProvider(final OpenSearchClientFactory openSearchClientFactory) {
        this.openSearchClientFactory = openSearchClientFactory;
    }

    public OpenSearchClient getInstance(final String endpoint) {
        return openSearchClientMap.computeIfAbsent(endpoint, k ->
                openSearchClientFactory.create(endpoint));
    }
}