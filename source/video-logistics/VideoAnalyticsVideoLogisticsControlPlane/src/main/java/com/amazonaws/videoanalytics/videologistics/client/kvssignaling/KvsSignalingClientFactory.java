package com.amazonaws.videoanalytics.videologistics.client.kvssignaling;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;

@AssistedFactory
public interface KvsSignalingClientFactory {

    KvsSignalingClientWrapper create(@Assisted("endpoint") String endpoint);
}
