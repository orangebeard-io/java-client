package io.orangebeard.client.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@JsonInclude(Include.NON_NULL)
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartTestRun {
    private String name;
    private String description;
    @JsonSerialize(using = DateSerializer.class)
    private LocalDateTime startTime;
    private Set<Attribute> attributes;
    private Set<ChangedComponent> changedComponents;

    public StartTestRun(String name, String description, Set<Attribute> attributes) {
        this.name = name;
        this.description = description;
        this.startTime = LocalDateTime.now();
        this.attributes = attributes;
    }

    public StartTestRun(String name, String description, Set<Attribute> attributes, Set<ChangedComponent> changedComponents) {
        this.name = name;
        this.description = description;
        this.startTime = LocalDateTime.now();
        this.attributes = attributes;
        this.changedComponents = changedComponents;
    }
}
