package io.orangebeard.client.entity.alerting.codequality;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.alerting.StartAlertRun;
import io.orangebeard.client.entity.alerting.Tool;

import java.time.ZonedDateTime;
import java.util.Set;
import lombok.Getter;

@Getter
public class StartCodeQualityAlertRun extends StartAlertRun {
    public StartCodeQualityAlertRun(String alertSetName, String description, Tool tool, ZonedDateTime startTime, Set<Attribute> attributes) {
        super(alertSetName, description, startTime, attributes, tool);
    }
}
