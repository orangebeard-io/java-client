package io.orangebeard.client.entity.alerting;

import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FinishAlertRun {
    private UUID alertRunUUID;
    private AlertRunStatus status;
    private ZonedDateTime endTime;
}

