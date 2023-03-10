package io.orangebeard.client;

import io.orangebeard.client.entity.FinishTestRun;
import io.orangebeard.client.entity.Log;
import io.orangebeard.client.entity.Response;
import io.orangebeard.client.entity.StartTestRun;

import io.orangebeard.client.entity.suite.Suite;
import io.orangebeard.client.entity.UpdateTestRun;

import io.orangebeard.client.entity.suite.StartSuiteRQ;

import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;
import io.orangebeard.client.exceptions.ClientVersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.springframework.http.HttpMethod.PUT;

public class OrangebeardV3Client extends LatestAbstractClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardV3Client.class);
    private static final String CONNECTION_FAILED = "The connection with Orangebeard could not be established!";
    private final String endpoint;
    private final RestTemplate restTemplate;
    private final String projectName;
    private boolean connectionWithOrangebeardIsValid;
    protected final UUID accessToken;

    public OrangebeardV3Client(String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        super(uuid);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);

        this.accessToken = uuid;
        this.restTemplate = new RestTemplate(factory);
        this.projectName = projectName;
        this.endpoint = endpoint;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public OrangebeardV3Client(RestTemplate restTemplate, String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        super(uuid);

        this.accessToken = uuid;
        this.restTemplate = restTemplate;
        this.projectName = projectName;
        this.endpoint = endpoint;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public UUID startTestRun(StartTestRun testRun) {
        if (this.connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartTestRun> request = new HttpEntity<>(testRun, this.getAuthorizationHeaders(String.valueOf(accessToken)));
                return this.restTemplate.exchange(
                        String.format("%s/listener/v3/%s/test-run/start", this.endpoint, this.projectName),
                        HttpMethod.POST, request, UUID.class)
                        .getBody();

            } catch (Exception var3) {
                LOGGER.error("The connection with Orangebeard could not be established! Check the properties and try again!");
                this.connectionWithOrangebeardIsValid = false;
            }
        }
        return null;
    }

    @Override
    public void startAnnouncedTestRun(UUID testRunUUID) {
        if (connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartTestRun> request = new HttpEntity<>(getAuthorizationHeaders(accessToken.toString()));
                restTemplate.exchange(format("%s/listener/v3/%s/test-run/start/%s", endpoint, projectName, testRunUUID), PUT, request, Response.class);
            } catch (Exception e) {
                LOGGER.error(CONNECTION_FAILED + "Check the properties and try again!");
                connectionWithOrangebeardIsValid = false;
            }
        }
    }

    @Override
    public void updateTestRun(UUID testRunUUID, UpdateTestRun updateTestRun) {
        throw new ClientVersionException("Method to update an existing a test-run by test-run uuid is not supported in V3 Client!");
    }

    @Override
    public void finishTestRun(UUID testRunUUID, FinishTestRun finishTestRun) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTestRun> request = new HttpEntity<>(finishTestRun, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/test-run/finish/%s", this.endpoint, this.projectName, testRunUUID),
                    HttpMethod.PUT,
                    request,
                    Void.class);
        } else {
            LOGGER.warn(CONNECTION_FAILED);
        }

    }

    public List<Suite> startSuite(StartSuiteRQ suiteRQ) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<StartSuiteRQ> request = new HttpEntity<>(suiteRQ, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            ResponseEntity<Suite[]> response = this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/suite/start", this.endpoint, this.projectName),
                    HttpMethod.POST,
                    request,
                    Suite[].class);

            return Arrays.asList(Objects.requireNonNull(response.getBody()));
        } else {
            LOGGER.warn(CONNECTION_FAILED);
            return Collections.emptyList();
        }
    }

    @Override
    public UUID startTest(UUID suiteId, StartTest startTest) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<StartTest> request = new HttpEntity<>(startTest, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            return this.restTemplate.exchange(
                            String.format("%s/listener/v3/%s/test/start", this.endpoint, this.projectName),
                            HttpMethod.POST, request, UUID.class)
                    .getBody();
        } else {
            LOGGER.warn(CONNECTION_FAILED);
            return null;
        }
    }

    @Override
    public void finishTest(UUID itemId, FinishTest finishTest) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTest> request = new HttpEntity<>(finishTest, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/test/finish/%s", this.endpoint, this.projectName, accessToken),
                    HttpMethod.PUT,
                    request,
                    Void.class);
        } else {
            LOGGER.warn(CONNECTION_FAILED);
        }
    }

    @Override
    public void log(Log log) {
        this.log(Collections.singleton(log));
    }
}
