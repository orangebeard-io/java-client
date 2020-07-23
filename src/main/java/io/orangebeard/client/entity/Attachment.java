package io.orangebeard.client.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private UUID itemUuid;
    @JsonProperty("launchUuid")
    private UUID testRunUUID;

    @JsonSerialize(using = DateSerializer.class)
    private LocalDateTime time;
    private String message;
    @JsonProperty("level")
    private LogLevel logLevel;
    @JsonProperty("file")
    private File file;

    public Attachment(UUID testRunUUID, UUID testItemUUID, LogLevel logLevel, String fileName, File file) {
        this.file = file;
        this.itemUuid = testItemUUID;
        this.testRunUUID = testRunUUID;
        this.logLevel = logLevel;
        this.time = LocalDateTime.now();
        this.message = fileName;
    }

    @Getter
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class File {
        private final String name;
        @JsonIgnore
        private final byte[] content;
        @JsonIgnore
        private final String contentType;
    }
}
