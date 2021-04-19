package io.orangebeard.client.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import java.util.Set;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateTestRun {
    private final String description;
    private final Set<Attribute> attributes;

    public UpdateTestRun(String description, Set<Attribute> attributes) {
        this.description = description;
        this.attributes = attributes;
    }



}
