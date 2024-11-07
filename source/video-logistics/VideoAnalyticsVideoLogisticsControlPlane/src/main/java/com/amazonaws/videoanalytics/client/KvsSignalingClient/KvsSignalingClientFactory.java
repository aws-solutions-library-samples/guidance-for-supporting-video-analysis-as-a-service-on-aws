package com.amazonaws.videoanalytics.client.KvsSignalingClient;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@AssistedFactory
public interface KvsSignalingClientFactory {

    KvsSignalingClientWrapper create(@Assisted("endpoint") final String endpoint,
                                     @Assisted("credentialsProvider") final AwsCredentialsProvider credentialsProvider);
}
