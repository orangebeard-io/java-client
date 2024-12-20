package io.orangebeard.client;

import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.alerting.FinishAlertRun;
import io.orangebeard.client.entity.alerting.ReportAlert;
import io.orangebeard.client.entity.alerting.StartAlertRun;
import io.orangebeard.client.entity.attachment.Attachment;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.Response;
import io.orangebeard.client.entity.StartTestRun;

import io.orangebeard.client.entity.step.FinishStep;
import io.orangebeard.client.entity.step.StartStep;
import io.orangebeard.client.entity.suite.Suite;

import io.orangebeard.client.entity.suite.StartSuite;

import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@EnableRetry
public class OrangebeardV3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardV3Client.class);
    private static final String CONNECTION_FAILED = "The connection with Orangebeard could not be established!";
    private final String endpoint;
    private final RestTemplate restTemplate;
    private final String projectName;
    private boolean connectionWithOrangebeardIsValid;
    protected final UUID accessToken;

    protected HttpHeaders getAuthorizationHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((accessToken));
        headers.setContentType(APPLICATION_JSON);
        return headers;
    }

    public OrangebeardV3Client(String endpoint, UUID accessToken, String projectName, boolean connectionWithOrangebeardIsValid) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);

        this.accessToken = accessToken;
        this.restTemplate = new RestTemplate(factory);
        this.projectName = projectName;
        this.endpoint = endpoint;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    public OrangebeardV3Client(RestTemplate restTemplate, String endpoint, UUID uuid, String projectName, boolean connectionWithOrangebeardIsValid) {
        this.accessToken = uuid;
        this.restTemplate = restTemplate;
        this.projectName = projectName;
        this.endpoint = endpoint;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public UUID startTestRun(StartV3TestRun testRun) {
        if (this.connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartV3TestRun> request = new HttpEntity<>(testRun, this.getAuthorizationHeaders(String.valueOf(accessToken)));
                ResponseEntity<UUID> response = this.restTemplate.exchange(
                        String.format("%s/listener/v3/%s/test-run/start", this.endpoint, this.projectName),
                        HttpMethod.POST, request, UUID.class);
                return response.getBody() != null ? response.getBody() : null;
            } catch (Exception e) {
                LOGGER.error("The connection with Orangebeard could not be established! Check the properties and try again!");
                this.connectionWithOrangebeardIsValid = false;
            }
        }
        return null;
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
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

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void finishTestRun(UUID testRunUUID, FinishV3TestRun finishTestRun) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishV3TestRun> request = new HttpEntity<>(finishTestRun, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/test-run/finish/%s", this.endpoint, this.projectName, testRunUUID),
                    HttpMethod.PUT,
                    request,
                    Void.class);
        } else {
            LOGGER.warn(CONNECTION_FAILED);
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<Suite> startSuite(StartSuite startSuite) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<StartSuite> request = new HttpEntity<>(startSuite, this.getAuthorizationHeaders(String.valueOf(accessToken)));
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

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public UUID startTest(StartTest startTest) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<StartTest> request = new HttpEntity<>(startTest, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            ResponseEntity<UUID> response = this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/test/start", this.endpoint, this.projectName),
                    HttpMethod.POST, request, UUID.class);
            return response.getBody() != null ? response.getBody() : null;
        } else {
            LOGGER.warn(CONNECTION_FAILED);
            return null;
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void finishTest(UUID testUUID, FinishTest finishTest) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishTest> request = new HttpEntity<>(finishTest, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            String url = String.format("%s/listener/v3/%s/test/finish/%s", this.endpoint, this.projectName, testUUID);
            this.restTemplate.exchange(url, HttpMethod.PUT, request, Void.class);
        } else {
            LOGGER.warn(CONNECTION_FAILED);
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public UUID startStep(StartStep startStep) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<StartStep> request = new HttpEntity<>(startStep, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            return this.restTemplate.exchange(
                            String.format("%s/listener/v3/%s/step/start", this.endpoint, this.projectName),
                            HttpMethod.POST, request, UUID.class)
                    .getBody();
        } else {
            LOGGER.warn(CONNECTION_FAILED);
            return null;
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void finishStep(UUID stepUUID, FinishStep finishStep) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishStep> request = new HttpEntity<>(finishStep, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/step/finish/%s", this.endpoint, this.projectName, stepUUID),
                    HttpMethod.PUT,
                    request,
                    Void.class);
        } else {
            LOGGER.warn(CONNECTION_FAILED);
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public UUID log(Log log) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<Log> request = new HttpEntity<>(log, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            return this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/log", this.endpoint, this.projectName),
                    HttpMethod.POST,
                    request,
                    UUID.class).getBody();
        } else {
            LOGGER.warn(CONNECTION_FAILED);
            return null;
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendLogBatch(List<Log> logs) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<List<Log>> request = new HttpEntity<>(logs, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/log/batch", this.endpoint, this.projectName),
                    HttpMethod.POST,
                    request,
                    Void.class);
        } else {
            LOGGER.warn(CONNECTION_FAILED);
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public UUID sendAttachment(Attachment attachment) {
        if (this.connectionWithOrangebeardIsValid) {
            LinkedMultiValueMap<String, String> filePartHeaders = new LinkedMultiValueMap<>();
            filePartHeaders.add(CONTENT_DISPOSITION, format("form-data; name=\"attachment\"; filename=\"%s\"", attachment.getFile().getName()));
            filePartHeaders.add(CONTENT_TYPE, attachment.getFile().getContentType());

            byte[] fileContents = attachment.getFile().getContent();
            HttpEntity<byte[]> filePart = new HttpEntity<>(fileContents, filePartHeaders);

            LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("json", attachment.getMetaData());
            parts.add("attachment", filePart);

            HttpHeaders headers = getAuthorizationHeaders(String.valueOf(accessToken));
            headers.setContentType(MULTIPART_FORM_DATA);
            HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(parts, headers);

            return this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/attachment", this.endpoint, this.projectName),
                    HttpMethod.POST,
                    request,
                    UUID.class).getBody();
        } else {
            LOGGER.warn(CONNECTION_FAILED);
            return null;
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public UUID startAlertRun(StartAlertRun alertRun) {
        if (this.connectionWithOrangebeardIsValid) {
            try {
                HttpEntity<StartAlertRun> request = new HttpEntity<>(alertRun, this.getAuthorizationHeaders(String.valueOf(accessToken)));
                ResponseEntity<UUID> response = this.restTemplate.exchange(
                        String.format("%s/listener/v3/%s/alert-run/start", this.endpoint, this.projectName),
                        HttpMethod.POST, request, UUID.class);
                return response.getBody() != null ? response.getBody() : null;
            } catch (Exception e) {
                LOGGER.error("The connection with Orangebeard could not be established! Check the properties and try again!");
                this.connectionWithOrangebeardIsValid = false;
            }
        }
        return null;
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void finishAlertRun(FinishAlertRun finishAlertRun) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<FinishAlertRun> request = new HttpEntity<>(finishAlertRun, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            this.restTemplate.exchange(
                    String.format("%s/listener/v3/%s/alert-run/finish", this.endpoint, this.projectName),
                    HttpMethod.PUT,
                    request,
                    Void.class);
        } else {
            LOGGER.warn(CONNECTION_FAILED);
        }
    }

    @Retryable(retryFor = {Exception.class}, maxAttempts = 4, backoff = @Backoff(delay = 2000, multiplier = 2))
    public UUID reportAlert(ReportAlert alert) {
        if (this.connectionWithOrangebeardIsValid) {
            HttpEntity<ReportAlert> request = new HttpEntity<>(alert, this.getAuthorizationHeaders(String.valueOf(accessToken)));
            return this.restTemplate.exchange(
                            String.format("%s/listener/v3/%s/alert-run/report", this.endpoint, this.projectName),
                            HttpMethod.POST, request, UUID.class)
                    .getBody();
        } else {
            LOGGER.warn(CONNECTION_FAILED);
            return null;
        }
    }

}
