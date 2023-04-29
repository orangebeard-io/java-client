package io.orangebeard.client.entity;

import lombok.Getter;
import java.time.ZonedDateTime;

@Getter
public class FinishV3TestRun {

    private final ZonedDateTime endTime;

    public FinishV3TestRun() {
        this.endTime = ZonedDateTime.now();
    }
}
