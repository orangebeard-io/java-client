package io.orangebeard.client.entity.alerting.security;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.alerting.StartAlertRun;

import io.orangebeard.client.entity.alerting.Tool;

import lombok.Getter;
import java.time.ZonedDateTime;
import java.util.Set;

@Getter
public class StartSecurityAlertRun extends StartAlertRun {
    public StartSecurityAlertRun(String alertSetName, String description, Tool tool, ZonedDateTime startTime, Set<Attribute> attributes) {
        super(alertSetName, description, startTime, attributes, tool);
    }
}
