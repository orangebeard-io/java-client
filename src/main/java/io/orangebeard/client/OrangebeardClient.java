package io.orangebeard.client;

import io.orangebeard.client.entity.Attachment;
import io.orangebeard.client.entity.FinishTestItem;
import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.Response;
import io.orangebeard.client.entity.StartTestItem;
import io.orangebeard.client.entity.StartTestRun;

import java.util.Collections;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

public class OrangebeardClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardClient.class);
    private final String endpoint;
    private final RestTemplate restTemplate;
    private final UUID uuid;
    private final String projectName;
    private boolean connectionWithOrangebeardIsValid;

    public OrangebeardClient(String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);

        this.restTemplate = new RestTemplate(factory);
        this.endpoint = endpoint;
        this.uuid = uuid;
        this.projectName = projectName;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public OrangebeardClient(RestTemplate restTemplate, String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        this.restTemplate = restTemplate;
        this.endpoint = endpoint;
        this.uuid = uuid;
        this.projectName = projectName;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public UUID startTestRun(StartTestRun testRun) {
        if (connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartTestRun> request = new HttpEntity<>(testRun, getAuthorizationHeaders(uuid.toString()));
                return restTemplate.exchange(format("%s/api/v1/%s/launch", endpoint, projectName), POST, request, Response.class).getBody().getId();
            } catch (Exception e) {
                LOGGER.error("The connection with Orangebeard could not be established! Check the properties and try again!");
                connectionWithOrangebeardIsValid = false;
            }
        }
        return null;
    }

    public UUID startTestItem(UUID suiteId, StartTestItem testItem) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<StartTestItem> request = new HttpEntity<>(testItem, getAuthorizationHeaders(uuid.toString()));
            if (suiteId == null) {
                return restTemplate.exchange(format("%s/api/v1/%s/item", endpoint, projectName), POST, request, Response.class).getBody().getId();
            } else {
                return restTemplate.exchange(format("%s/api/v1/%s/item/%s", endpoint, projectName, suiteId), POST, request, Response.class).getBody().getId();
            }
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
        return null;
    }

    public void finishTestItem(UUID itemId, FinishTestItem finishTestItem) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestItem> request = new HttpEntity<>(finishTestItem, getAuthorizationHeaders(uuid.toString()));
            restTemplate.exchange(format("%s/api/v1/%s/item/%s", endpoint, projectName, itemId), PUT, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public void finishTestRun(UUID testRunUUID, FinishTestRun finishTestRun) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestRun> request = new HttpEntity<>(finishTestRun, getAuthorizationHeaders(uuid.toString()));
            restTemplate.exchange(format("%s/api/v1/%s/launch/%s/finish", endpoint, projectName, testRunUUID), PUT, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public void log(Log log) {
        if (connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<Log> request = new HttpEntity<>(log, getAuthorizationHeaders(uuid.toString()));
                restTemplate.exchange(format("%s/api/v1/%s/log", endpoint, projectName), POST, request, Response.class);
            } catch (ResourceAccessException e){
                LOGGER.error("Log cannot be reported to Orangebeard. Uuid=[{}]; loglevel=[{}]; message=[{}]", log.getItemUuid(), log.getLogLevel(), log.getMessage(), e);
            }
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public void sendAttachment(Attachment attachment) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<LinkedMultiValueMap<String, Object>> request = getMultipartLogRequest(attachment);
            restTemplate.exchange(format("%s/api/v1/%s/log", endpoint, projectName), POST, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    private HttpHeaders getAuthorizationHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((accessToken));
        headers.setContentType(APPLICATION_JSON);
        return headers;
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> getMultipartLogRequest(Attachment attachmentLogItem) {
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
