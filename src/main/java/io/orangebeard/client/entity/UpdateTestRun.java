package io.orangebeard.client.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Set;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
public class UpdateTestRun {
    private final String description;
    private final Set<Attribute> attributes;
}
