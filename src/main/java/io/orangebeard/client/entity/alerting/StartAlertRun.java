package io.orangebeard.client.entity.alerting;

import java.time.ZonedDateTime;
import java.util.Set;

import io.orangebeard.client.entity.Attribute;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StartAlertRun {
    private String alertSetName;
    private String description;
    private ZonedDateTime startTime;
    private Set<Attribute> attributes;
    private Tool tool;
}
