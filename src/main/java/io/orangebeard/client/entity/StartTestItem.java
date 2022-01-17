package io.orangebeard.client.entity;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Start a new test item
 * As of 1.2.0 the countAsTest (api: hasStats) property is introduced. If this property is false, the item will be
 * displayed as a sub-item below a test (or step), cannot be individually classified and will not be counted towards pass/fail counts
 * This is useful to make tests more readable (log grouping).
 *
 * Example:
 * Suite [countAsTest=true]
 *  - Test (or scenario)[countAsTest=true]
 *    - Given: Some starting point [countAsTest=false]
 *      - Log 1..n
 *    - When: Some interaction [countAsTest=false]
 *      - Log 1..n
 *
 * This allows to visualize the test (scenario) as a test (with its own view and defect type on fail), while the given/when/etc
 * become collapsible sections containing log entries (groups). By default, steps are reported as log groups, instead of
 * separate test items as of 1.2.1
 */

@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartTestItem {
    @JsonProperty("launchUuid")
    private UUID testRunUUID;
    private TestItemType type;

    private String name;
    private String codeRef;
    private String description;
    @JsonSerialize(using = DateSerializer.class)
    private LocalDateTime startTime;
    private Set<Attribute> attributes;
    @JsonProperty("hasStats")
    private boolean countAsTestItem;

    public StartTestItem(UUID testRunUUID, String name, TestItemType type, String description, Set<Attribute> attributes) {
        this.testRunUUID = testRunUUID;
        this.name = name;
        this.type = type;
        this.description = description;
        this.startTime = LocalDateTime.now();
        this.attributes = attributes;
        countAsTestItem = type != TestItemType.STEP;
    }

    public StartTestItem(UUID testRunUUID, String name, TestItemType type) {
        this.testRunUUID = testRunUUID;
        this.name = name;
        this.type = type;
        this.startTime = LocalDateTime.now();
        countAsTestItem = type != TestItemType.STEP;
    }
}
