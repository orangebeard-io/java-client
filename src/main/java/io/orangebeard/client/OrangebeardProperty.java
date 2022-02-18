package io.orangebeard.client;

import lombok.Getter;

@Getter
public enum OrangebeardProperty {
    ENDPOINT("orangebeard.endpoint"),
    PROJECT("orangebeard.project"),
    TESTSET("orangebeard.testset"),
    ACCESS_TOKEN("orangebeard.accessToken"),
    DESCRIPTION("orangebeard.description"),
    LOG_LEVEL("orangebeard.logLevel"),
    ATTRIBUTES("orangebeard.attributes"),
    LOGS_AT_END_OF_TEST("orangebeard.logsAtEndOfTest");

    private final String propertyName;

    OrangebeardProperty(String propertyName) {
        this.propertyName = propertyName;
    }
}
