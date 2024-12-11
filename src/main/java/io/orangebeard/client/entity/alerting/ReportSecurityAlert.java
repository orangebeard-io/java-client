package io.orangebeard.client.entity.alerting;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.alerting.security.Confidence;
import io.orangebeard.client.entity.alerting.security.Evidence;

import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportSecurityAlert extends ReportAlert {
    private final String definitionID;
    private final Confidence confidence;
    private final Evidence evidence;
    private final Severity severity;

    public ReportSecurityAlert(UUID alertRunUUID, String definitionID, String description, String message, String remediation, Set<Attribute> attributes,
                               Confidence confidence, Evidence evidence, Severity severity) {
        super(alertRunUUID, definitionID, description, message, remediation, attributes);
        this.definitionID = definitionID;
        this.confidence = confidence;
        this.evidence = evidence;
        this.severity = severity;
    }
}
