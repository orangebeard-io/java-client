package io.orangebeard.client.entity.step;

import io.orangebeard.client.entity.test.TestStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class FinishStep {
    private UUID testRunUUID;
    private TestStatus status;
    private ZonedDateTime endTime;
}
