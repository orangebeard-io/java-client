package io.orangebeard.client.entity;

public enum Status {
    IN_PROGRESS,
    PASSED,
    FAILED,
    STOPPED, //status for manually stopped launches
    SKIPPED,
    INTERRUPTED,
    RESETED, //status for items with deleted descendants
    CANCELLED
}
