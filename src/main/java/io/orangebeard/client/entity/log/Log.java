package io.orangebeard.client.entity.log;

import io.orangebeard.client.entity.LogFormat;

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
public class Log {
    private UUID testRunUUID;
    private UUID testUUID;
    private UUID stepUUID;
    private String message;
    private LogLevel logLevel;
    private ZonedDateTime logTime;
    private LogFormat logFormat;
}
