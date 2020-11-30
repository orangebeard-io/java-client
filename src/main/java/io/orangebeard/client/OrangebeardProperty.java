package io.orangebeard.client;

import lombok.Getter;

@Getter
public enum OrangebeardProperty {
    ENDPOINT("orangebeard.endpoint"),
    PROJECT("orangebeard.project"),
    TESTSET("orangebeard.testset"),
    ACCESS_TOKEN("orangebeard.accessToken"),
    DESCRIPTION("orangebeard.description"),
    ATTRIBUTES("orangebeard.attributes");

    private final String propertyName;

    OrangebeardProperty(String propertyName) {
        this.propertyName = propertyName;
    }
}
