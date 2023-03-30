package io.orangebeard.client.entity.suite;

import io.orangebeard.client.entity.Attribute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class StartSuite {
    private UUID testRunUUID;
    private UUID parentSuiteUUID;
    private String description;
    private Set<Attribute> attributes;
    private List<String> suiteNames;
}
