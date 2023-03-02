package io.orangebeard.client;

import io.orangebeard.client.entity.Response;
import io.orangebeard.client.entity.StartTestRun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static java.lang.String.format;
import static org.springframework.http.HttpMethod.PUT;

public class OrangebeardV3Client extends OrangebeardV2Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardV3Client.class);
    private final String endpoint;
    private final RestTemplate restTemplate;
    private final String projectName;
    private boolean connectionWithOrangebeardIsValid;

    public OrangebeardV3Client(String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        super(endpoint, uuid, projectName, connectionWithOrangebeardIsValid);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);

        this.restTemplate = new RestTemplate(factory);
        this.projectName = projectName;
        this.endpoint = endpoint;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public OrangebeardV3Client(RestTemplate restTemplate, String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        super(restTemplate, endpoint, uuid, projectName, connectionWithOrangebeardIsValid);

        this.restTemplate = restTemplate;
        this.projectName = projectName;
        this.endpoint = endpoint;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    @Override
    public void startTestRunAfterAnnouncement(UUID testRunUUID) {
        if (connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartTestRun> request = new HttpEntity<>(getAuthorizationHeaders(uuid.toString()));
                restTemplate.exchange(format("%s/listener/v3/%s/test-run/start/%s", endpoint, projectName, testRunUUID), PUT, request, Response.class);
            } catch (Exception e) {
                LOGGER.error("The connection with Orangebeard could not be established! Check the properties and try again!");
                connectionWithOrangebeardIsValid = false;
            }
        }
    }
}
