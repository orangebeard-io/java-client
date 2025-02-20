package io.orangebeard.client.entity.alerting.codequality;


import java.util.Set;
import java.util.UUID;

import io.orangebeard.client.entity.Attribute;

import io.orangebeard.client.entity.alerting.ReportAlert;
import io.orangebeard.client.entity.alerting.Severity;
import lombok.Getter;

@Getter
public class ReportCodeQualityAlert extends ReportAlert {
    private final String smellID;
    private final Severity severity;

    private final String className;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;

    public ReportCodeQualityAlert(UUID alertRunUUID, String definitionId, String description, String message, String remediation, Set<Attribute> attributes,
                                  String smellId, Severity severity,
                                  String className, String methodName, String fileName, int lineNumber) {
        super(alertRunUUID, definitionId, description, message, remediation, attributes);
        this.smellID = smellId;
        this.severity = severity;
        this.className = className;
        this.methodName = methodName;
        this.fileName =fileName;
        this.lineNumber = lineNumber;
    }
}
