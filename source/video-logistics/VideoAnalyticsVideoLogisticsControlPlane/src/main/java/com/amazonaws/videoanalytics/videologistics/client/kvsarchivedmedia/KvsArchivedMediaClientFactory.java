package com.amazonaws.videoanalytics.videologistics.client.kvsarchivedmedia;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;

@AssistedFactory
public interface KvsArchivedMediaClientFactory {

    KvsArchivedMediaClientWrapper create(@Assisted("endpoint") String endpoint);
}
