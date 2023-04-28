package io.orangebeard.client.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import java.time.ZonedDateTime;

@Getter
public class FinishV3TestRun {

    @JsonSerialize(using = DateSerializer.class)
    private final ZonedDateTime endTime;

    public FinishV3TestRun() {
        this.endTime = ZonedDateTime.now();
    }
}
