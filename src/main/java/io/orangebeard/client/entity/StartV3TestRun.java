package io.orangebeard.client.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartV3TestRun {

    private String testSetName;
    private String description;
    @JsonSerialize(using = DateSerializer.class)
    private LocalDateTime startTime;
    private Set<Attribute> attributes;
    private Set<ChangedComponent> changedComponents;

    public StartV3TestRun(String testSetName, String description, Set<Attribute> attributes) {
        this.testSetName = testSetName;
        this.description = description;
        this.startTime = LocalDateTime.now();
        this.attributes = attributes;
    }

    public StartV3TestRun(String testSetName, String description, Set<Attribute> attributes, Set<ChangedComponent> changedComponents) {
        this.testSetName = testSetName;
        this.description = description;
        this.startTime = LocalDateTime.now();
        this.attributes = attributes;
        this.changedComponents = changedComponents;
    }
}
