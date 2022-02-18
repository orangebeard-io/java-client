package io.orangebeard.client;

import io.orangebeard.client.entity.Attachment;
import io.orangebeard.client.entity.FinishTestItem;
import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.Response;
import io.orangebeard.client.entity.StartTestItem;
import io.orangebeard.client.entity.StartTestRun;
import io.orangebeard.client.entity.UpdateTestRun;

import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

public class OrangebeardV2Client extends AbstractClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardV2Client.class);
    private final String endpoint;
    private final RestTemplate restTemplate;
    private final String projectName;
    private boolean connectionWithOrangebeardIsValid;

    public OrangebeardV2Client(String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        super(uuid);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);

        this.restTemplate = new RestTemplate(factory);
        this.endpoint = endpoint;
        this.projectName = projectName == null ? null : projectName.toLowerCase();
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public OrangebeardV2Client(RestTemplate restTemplate, String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        super(uuid);
        this.restTemplate = restTemplate;
        this.endpoint = endpoint;
        this.projectName = projectName == null ? null : projectName.toLowerCase();
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public UUID startTestRun(StartTestRun testRun) {
        if (connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartTestRun> request = new HttpEntity<>(testRun, getAuthorizationHeaders(uuid.toString()));
                return restTemplate.exchange(format("%s/listener/v2/%s/launch", endpoint, projectName), POST, request, Response.class).getBody().getId();
            } catch (Exception e) {
                LOGGER.error("The connection with Orangebeard could not be established! Check the properties and try again!");
                connectionWithOrangebeardIsValid = false;
            }
        }
        return null;
    }

    public void updateTestRun(UUID testRunUUID, UpdateTestRun updateTestRun) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<UpdateTestRun> request = new HttpEntity<>(updateTestRun, getAuthorizationHeaders(uuid.toString()));
            restTemplate.exchange(format("%s/listener/v2/%s/launch/%s/update", endpoint, projectName, testRunUUID), PUT, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public UUID startTestItem(UUID suiteId, StartTestItem testItem) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<StartTestItem> request = new HttpEntity<>(testItem, getAuthorizationHeaders(uuid.toString()));
            if (suiteId == null) {
                return restTemplate.exchange(format("%s/listener/v2/%s/item", endpoint, projectName), POST, request, Response.class).getBody().getId();
            } else {
                return restTemplate.exchange(format("%s/listener/v2/%s/item/%s", endpoint, projectName, suiteId), POST, request, Response.class).getBody().getId();
            }
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
        return null;
    }

    public void finishTestItem(UUID itemId, FinishTestItem finishTestItem) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestItem> request = new HttpEntity<>(finishTestItem, getAuthorizationHeaders(uuid.toString()));
            restTemplate.exchange(format("%s/listener/v2/%s/item/%s", endpoint, projectName, itemId), PUT, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public void finishTestRun(UUID testRunUUID, FinishTestRun finishTestRun) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestRun> request = new HttpEntity<>(finishTestRun, getAuthorizationHeaders(uuid.toString()));
            restTemplate.exchange(format("%s/listener/v2/%s/launch/%s/finish", endpoint, projectName, testRunUUID), PUT, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public void log(Log log) {
        log(singleton(log));
    }

    @Override
    public void log(Set<Log> logs) {
        if (connectionWithOrangebeardIsValid && logs!=null && !logs.isEmpty()) {
            try {
                HttpEntity<Set<Log>> request = new HttpEntity<>(logs, getAuthorizationHeaders(uuid.toString()));
                restTemplate.exchange(format("%s/listener/v2/%s/log", endpoint, projectName), POST, request, Response.class);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                Log anyLog = logs.iterator().next();
                LOGGER.error("Logs cannot be reported to Orangebeard. One of the logs that cannot be reported Uuid=[{}]; loglevel=[{}]; message=[{}]", anyLog.getItemUuid(), anyLog.getLogLevel(), anyLog.getMessage(), e);
            }
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }

    public void sendAttachment(Attachment attachment) {
        if (connectionWithOrangebeardIsValid) {
            HttpEntity<LinkedMultiValueMap<String, Object>> request = getMultipartLogRequest(attachment);
            restTemplate.exchange(format("%s/listener/v2/%s/log", endpoint, projectName), POST, request, Response.class);
        } else {
            LOGGER.warn("The connection with Orangebeard could not be established!");
        }
    }
}
