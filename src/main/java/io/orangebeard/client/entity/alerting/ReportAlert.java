package io.orangebeard.client.entity.alerting;

import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.orangebeard.client.entity.Attribute;

import io.orangebeard.client.entity.alerting.codequality.ReportCodeQualityAlert;
import io.orangebeard.client.entity.alerting.security.ReportSecurityAlert;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ReportSecurityAlert.class, name = "ReportSecurityAlert"),
        @JsonSubTypes.Type(value = ReportCodeQualityAlert.class, name = "ReportCodeQualityAlert")
})
@Getter
@Setter
@AllArgsConstructor
public abstract class ReportAlert {
    private UUID alertRunUUID;
    private String definitionId;
    private String description;
    private String message;
    private String remediation;
    private Set<Attribute> attributes;
}
