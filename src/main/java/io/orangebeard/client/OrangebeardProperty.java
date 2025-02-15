package io.orangebeard.client;

import lombok.Getter;

@Getter
public enum OrangebeardProperty {
    ENDPOINT("orangebeard.endpoint"),
    PROJECT("orangebeard.project"),
    TESTSET("orangebeard.testset"),
    ACCESS_TOKEN("orangebeard.accessToken"), //to be deprecated
    TOKEN("orangebeard.token"),
    DESCRIPTION("orangebeard.description"),
    LOG_LEVEL("orangebeard.logLevel"),
    ATTRIBUTES("orangebeard.attributes"),
    SUT_COMPONENTS("orangebeard.sutComponents"),
    LOGS_AT_END_OF_TEST("orangebeard.logsAtEndOfTest"),
    TEST_RUN_UUID("orangebeard.testRunUUID"),
    REFERENCE_URL("orangebeard.referenceUrl");

    private final String propertyName;

    OrangebeardProperty(String propertyName) {
        this.propertyName = propertyName;
    }
}
