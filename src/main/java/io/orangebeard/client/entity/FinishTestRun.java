package io.orangebeard.client.entity;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class FinishTestRun {
    @JsonSerialize(using = DateSerializer.class)
    private final LocalDateTime endTime;
    private Status status;

    public FinishTestRun(Status status) {
        this.status = status;
        this.endTime = LocalDateTime.now();
    }

    public FinishTestRun(){
        this.endTime = LocalDateTime.now();
    }

}
