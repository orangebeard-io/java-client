package io.orangebeard.client.entity.alerting;

import java.time.ZonedDateTime;
import java.util.Set;

import io.orangebeard.client.entity.Attribute;

import lombok.Getter;

@Getter
public class StartCodeQualityAlertRun extends StartAlertRun {

    private final CodeQualityTool tool;

    public StartCodeQualityAlertRun(String alertSetName, String description, CodeQualityTool tool, ZonedDateTime startTime, Set<Attribute> attributes) {
        super(alertSetName, description, startTime, attributes);
        this.tool = tool;
    }
}
