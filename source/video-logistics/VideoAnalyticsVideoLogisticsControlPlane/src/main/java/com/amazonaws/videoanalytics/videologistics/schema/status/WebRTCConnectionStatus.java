package com.amazonaws.videoanalytics.videologistics.schema.status;

import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.utils.ImmutableMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public enum WebRTCConnectionStatus {

    // TODO: finalize statues and state transitions
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED;

    /**
     * Represents valid state transitions.
     * key: from/origin state
     * value: the set of states that the state represented by the key can transition to
     */
    private static final Map<WebRTCConnectionStatus, Collection<WebRTCConnectionStatus>> STATE_TRANSITION_MAP =
            new ImmutableMap.Builder<WebRTCConnectionStatus, Collection<WebRTCConnectionStatus>>()
                    .put(CONNECTING, ImmutableSet.of(CONNECTED, FAILED))
                    .put(CONNECTED, ImmutableSet.of(DISCONNECTED, FAILED))
                    .put(DISCONNECTED, Collections.emptySet())
                    .put(FAILED, Collections.emptySet())
                    .build();

    public boolean isValidTransitionTo(WebRTCConnectionStatus target) {
        return this.equals(target) || STATE_TRANSITION_MAP.get(this).contains(target);
    }

}
