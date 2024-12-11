package io.orangebeard.client.entity.alerting;


import java.time.ZonedDateTime;
import java.util.Set;

import io.orangebeard.client.entity.Attribute;

import io.orangebeard.client.entity.alerting.security.Tool;

import lombok.Getter;

@Getter
public class StartSecurityAlertRun extends StartAlertRun {

    private final Tool tool;

    public StartSecurityAlertRun(String alertSetName, String description, Tool tool, ZonedDateTime startTime, Set<Attribute> attributes) {
        super(alertSetName, description, startTime, attributes);
        this.tool = tool;
    }
}
