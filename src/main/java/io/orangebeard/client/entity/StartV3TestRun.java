package io.orangebeard.client.entity;

import java.time.ZonedDateTime;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartV3TestRun {

    private String testSetName;
    private String description;
    private ZonedDateTime startTime;
    private Set<Attribute> attributes;
    private Set<ChangedComponent> changedComponents;

    public StartV3TestRun(String testSetName, String description, Set<Attribute> attributes) {
        this.testSetName = testSetName;
        this.description = description;
        this.startTime = ZonedDateTime.now();
        this.attributes = attributes;
    }

    public StartV3TestRun(String testSetName, String description, Set<Attribute> attributes, Set<ChangedComponent> changedComponents) {
        this.testSetName = testSetName;
        this.description = description;
        this.startTime = ZonedDateTime.now();
        this.attributes = attributes;
        this.changedComponents = changedComponents;
    }
}
