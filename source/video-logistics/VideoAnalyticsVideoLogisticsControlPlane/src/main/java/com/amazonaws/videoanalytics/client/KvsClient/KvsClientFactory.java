package com.amazonaws.videoanalytics.client.KvsClient;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@AssistedFactory
public interface KvsClientFactory {
    KvsClientWrapper create(@Assisted("credentialsProvider") final AwsCredentialsProvider credentialsProvider);
}
