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
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

public class OrangebeardClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardClient.class);
    private final String endpoint;
    private final RestTemplate restTemplate;
    private final UUID uuid;
    private final String projectName;
    private boolean connectionWithOrangebeardIsValid;

    public OrangebeardClient(String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        this.restTemplate = new RestTemplate();
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

    public UUID startSuite(StartTestItem testItem) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<StartTestItem> request = new HttpEntity<>(testItem, getAuthorizationHeaders(uuid.toString()));
            return restTemplate.exchange(format("%s/api/v1/%s/item", endpoint, projectName), POST, request, Response.class).getBody().getId();
        }
        return null;
    }

    public UUID startSuite(UUID suiteId, StartTestItem testItem) {
        return suiteId == null ? startSuite(testItem) : startTest(suiteId, testItem);
    }

    public UUID startTest(UUID suiteId, StartTestItem testItem) {
        if (connectionWithOrangebeardIsValid) {

            HttpEntity<StartTestItem> request = new HttpEntity<>(testItem, getAuthorizationHeaders(uuid.toString()));
            return restTemplate.exchange(format("%s/api/v1/%s/item/%s", endpoint, projectName, suiteId), POST, request, Response.class).getBody().getId();
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
            HttpEntity<Log> request = new HttpEntity<>(log, getAuthorizationHeaders(uuid.toString()));
            restTemplate.exchange(format("%s/api/v1/%s/log", endpoint, projectName), POST, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public void attachment(Attachment attachment) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<LinkedMultiValueMap<String, Object>> request = multipartLogRequest(attachment);
            restTemplate.exchange(format("%s/api/v1/%s/log", endpoint, projectName), POST, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    private HttpHeaders getAuthorizationHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((accessToken));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> multipartLogRequest(Attachment attachmentLogItem) {
        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        LinkedMultiValueMap<String, String> filePartHeaders = new LinkedMultiValueMap<>();
        filePartHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"file\"; filename=\"" + attachmentLogItem.getFile().getName() + "\"");
        filePartHeaders.add(HttpHeaders.CONTENT_TYPE, attachmentLogItem.getFile().getContentType());
        byte[] fileContents = attachmentLogItem.getFile().getContent();

        HttpEntity<byte[]> filePart = new HttpEntity<>(fileContents, filePartHeaders);

        parts.add("json_request_part", Collections.singletonList(attachmentLogItem));
        parts.add("file", filePart);

        HttpHeaders headers = getAuthorizationHeaders(uuid.toString());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(parts, headers);
    }
}
