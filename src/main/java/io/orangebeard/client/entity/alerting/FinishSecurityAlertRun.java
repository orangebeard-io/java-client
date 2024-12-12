package io.orangebeard.client.entity.alerting;

import java.time.ZonedDateTime;
import java.util.UUID;

public class FinishSecurityAlertRun extends FinishAlertRun {
    public FinishSecurityAlertRun(UUID alertRunUUID, AlertRunStatus status, ZonedDateTime endTime) {
        super(alertRunUUID, status, endTime);
    }
}
