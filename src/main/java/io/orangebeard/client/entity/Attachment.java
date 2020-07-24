package io.orangebeard.client.entity;

import java.io.IOException;
import java.nio.file.Files;
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
    @JsonProperty("launchUuid")
    private UUID testRunUUID;
    private UUID itemUuid;

    @JsonProperty("level")
    private LogLevel logLevel;
    private String message;
    @JsonProperty("file")
    private File file;
    @JsonSerialize(using = DateSerializer.class)
    private LocalDateTime time;

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

        public File (java.io.File file) throws IOException {
            name = file.getName();
            content = Files.readAllBytes(file.toPath());
            contentType = Files.probeContentType(file.toPath());
        }
    }
}
