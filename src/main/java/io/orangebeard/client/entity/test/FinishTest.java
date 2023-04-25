package io.orangebeard.client.entity.test;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Builder
public class FinishTest {
    private UUID testRunUUID;
    private TestStatus status;
    private ZonedDateTime endTime;
}
