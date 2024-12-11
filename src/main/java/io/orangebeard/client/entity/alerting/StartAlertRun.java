package io.orangebeard.client.entity.alerting;

import java.time.ZonedDateTime;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.orangebeard.client.entity.Attribute;

import lombok.AllArgsConstructor;
import lombok.Getter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StartSecurityAlertRun.class, name = "StartSecurityAlertRun"),
        @JsonSubTypes.Type(value = StartCodeQualityAlertRun.class, name = "StartCodeQualityAlertRun")
})
@Getter
@AllArgsConstructor
public abstract class StartAlertRun {
    // Note that AlertRunId and UserFacingId are determined later.
    private String alertSetName;
    private String description;
    private ZonedDateTime startTime;
    private Set<Attribute> attributes;
}
