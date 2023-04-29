package io.orangebeard.client.entity.step;

import io.orangebeard.client.entity.test.TestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.UUID;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishStep {
    private UUID testRunUUID;
    private TestStatus status;
    private ZonedDateTime endTime;
}
