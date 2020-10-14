package io.orangebeard.client;

import io.orangebeard.client.entity.Attachment;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import java.util.Collections;
import java.util.UUID;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

public abstract class AbstractClient implements OrangebeardClient {
    protected final UUID uuid;

    public AbstractClient(UUID uuid){
        this.uuid = uuid;
    }

    protected HttpHeaders getAuthorizationHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((accessToken));
        headers.setContentType(APPLICATION_JSON);
        return headers;
    }

    protected HttpEntity<LinkedMultiValueMap<String, Object>> getMultipartLogRequest(Attachment attachmentLogItem) {
        LinkedMultiValueMap<String, String> filePartHeaders = new LinkedMultiValueMap<>();
        filePartHeaders.add(CONTENT_DISPOSITION, format("form-data; name=\"file\"; filename=\"%s\"", attachmentLogItem.getFile().getName()));
        filePartHeaders.add(CONTENT_TYPE, attachmentLogItem.getFile().getContentType());

        byte[] fileContents = attachmentLogItem.getFile().getContent();
        HttpEntity<byte[]> filePart = new HttpEntity<>(fileContents, filePartHeaders);

        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("json_request_part", Collections.singletonList(attachmentLogItem));
        parts.add("file", filePart);

        HttpHeaders headers = getAuthorizationHeaders(uuid.toString());
        headers.setContentType(MULTIPART_FORM_DATA);

        return new HttpEntity<>(parts, headers);
    }
}
