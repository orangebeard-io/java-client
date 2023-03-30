package io.orangebeard.client.entity.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class StartStep {
    private UUID testRunUUID;
    private UUID testUUID;
    private UUID parentStepUUID;
    private String stepName;
    private String description;
    private ZonedDateTime startTime;
}
