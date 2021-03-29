package io.orangebeard.client.entity;

public enum Status {
    IN_PROGRESS,
    PASSED,
    FAILED,
    STOPPED, //status for manually stopped launches
    SKIPPED,
    TIMED_OUT
}
