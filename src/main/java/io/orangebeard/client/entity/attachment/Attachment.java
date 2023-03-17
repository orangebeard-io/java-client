package io.orangebeard.client.entity.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class Attachment {

    private AttachmentFile file;
    private AttachmentMetaData metaData;

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public class AttachmentFile {
        private String name;
        private byte[] content;
        private String contentType;
    }

    @AllArgsConstructor
    @Getter
    @NoArgsConstructor
    public class AttachmentMetaData {
        private UUID testRunUUID;
        private UUID testUUID;
        private UUID stepUUID;
        private UUID logUUID;
        private ZonedDateTime attachmentTime;
    }
}
