package io.orangebeard.client.entity.alerting.security;

import io.orangebeard.client.entity.alerting.AlertRunStatus;
import io.orangebeard.client.entity.alerting.FinishAlertRun;

import java.time.ZonedDateTime;
import java.util.UUID;

public class FinishSecurityAlertRun extends FinishAlertRun {
    public FinishSecurityAlertRun(UUID alertRunUUID, AlertRunStatus status, ZonedDateTime endTime) {
        super(alertRunUUID, status, endTime);
    }
}
