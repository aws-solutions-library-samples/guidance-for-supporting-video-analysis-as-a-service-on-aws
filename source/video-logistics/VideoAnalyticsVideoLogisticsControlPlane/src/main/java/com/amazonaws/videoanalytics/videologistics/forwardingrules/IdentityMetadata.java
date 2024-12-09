package com.amazonaws.videoanalytics.videologistics.forwardingrules;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class IdentityMetadata {
    @JsonProperty
    private String accountId;

    public IdentityMetadata(){}

    public IdentityMetadata(final String accountId) {
        this.accountId = accountId;
    }
}