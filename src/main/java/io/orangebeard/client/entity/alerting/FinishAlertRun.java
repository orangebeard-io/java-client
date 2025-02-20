package io.orangebeard.client.entity.alerting;

import java.time.ZonedDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.orangebeard.client.entity.alerting.codequality.FinishCodeQualityAlertRun;
import io.orangebeard.client.entity.alerting.security.FinishSecurityAlertRun;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FinishSecurityAlertRun.class, name = "FinishSecurityAlertRun"),
        @JsonSubTypes.Type(value = FinishCodeQualityAlertRun.class, name = "FinishCodeQualityAlertRun")
})
@Getter
@Setter
@AllArgsConstructor
public abstract class FinishAlertRun {
    private UUID alertRunUUID;
    private AlertRunStatus status;
    private ZonedDateTime endTime;
}


