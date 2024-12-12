package io.orangebeard.client.entity.alerting;

import java.time.ZonedDateTime;
import java.util.UUID;

public class FinishCodeQualityAlertRun extends FinishAlertRun {
    public FinishCodeQualityAlertRun(UUID alertRunUUID, AlertRunStatus status, ZonedDateTime endTime) {
        super(alertRunUUID, status, endTime);
    }
}
