package com.amazonaws.videoanalytics.schema.status;

public enum InternalStreamingSessionStatus {
    // TODO: add more internal states
    RUNNING(false),
    COMPLETED(true),
    FAILED(true);

    private final boolean isTerminal;

    public static boolean isTerminal(InternalStreamingSessionStatus status) {
        return status.isTerminal();
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    InternalStreamingSessionStatus(final boolean isTerminal) {
        this.isTerminal = isTerminal;
    }
}
