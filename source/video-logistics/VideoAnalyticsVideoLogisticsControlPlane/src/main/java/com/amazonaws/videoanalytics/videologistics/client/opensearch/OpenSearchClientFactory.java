package com.amazonaws.videoanalytics.videologistics.client.opensearch;

import dagger.assisted.AssistedFactory;

@AssistedFactory
public interface OpenSearchClientFactory {
    OpenSearchClient create(final String endpoint);
}