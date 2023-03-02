package io.orangebeard.client.entity.test;

import io.orangebeard.client.entity.Attribute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class StartTest {
    private UUID testRunUUID;
    private UUID suiteUUID;
    private String testName;
    private TestType testType;
    private String description;
    private Set<Attribute> attributes;
    private ZonedDateTime startTime;
}
