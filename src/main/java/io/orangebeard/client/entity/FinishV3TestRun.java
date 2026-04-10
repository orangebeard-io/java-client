package io.orangebeard.client.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class FinishV3TestRun {

    private final ZonedDateTime endTime;

    public FinishV3TestRun() {
        this.endTime = ZonedDateTime.now();
    }
}
