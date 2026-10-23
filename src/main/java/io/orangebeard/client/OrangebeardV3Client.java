package io.orangebeard.client;

import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.alerting.FinishAlertRun;
import io.orangebeard.client.entity.alerting.ReportAlert;
import io.orangebeard.client.entity.alerting.StartAlertRun;
import io.orangebeard.client.entity.attachment.Attachment;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.step.FinishStep;
import io.orangebeard.client.entity.step.StartStep;
import io.orangebeard.client.entity.suite.StartSuite;
import io.orangebeard.client.entity.suite.Suite;
import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;
import io.orangebeard.client.exceptions.OrangebeardClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;

/**
 * Synchronous client for the Orangebeard listener v3 API, built on the JDK {@link HttpClient}.
 * Failed requests are retried (connection problems, 5xx, 408 and 429) before an
 * {@link OrangebeardClientException} is thrown. Listeners should normally use the non-blocking
 * {@link io.orangebeard.client.v3.OrangebeardAsyncV3Client} instead.
 */
public class OrangebeardV3Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrangebeardV3Client.class);
    private static final String CONNECTION_FAILED = "The connection with Orangebeard could not be established! Check the properties and try again!";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final RetryPolicy retryPolicy;
    private final Duration requestTimeout;
    private final String baseUrl;
    private final String authorization;
    private final AtomicBoolean invalidWarningLogged = new AtomicBoolean();
    private volatile boolean connectionWithOrangebeardIsValid;
    protected final UUID accessToken;

    public OrangebeardV3Client(String endpoint, UUID accessToken, String projectName, boolean connectionWithOrangebeardIsValid) {
        this(endpoint, accessToken, projectName, connectionWithOrangebeardIsValid,
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), REQUEST_TIMEOUT, RetryPolicy.DEFAULT);
    }

    /**
     * @param maxAttempts    total number of tries per request (1 disables retrying)
     * @param retryDelay     wait before the first retry; doubles for every following retry
     * @param requestTimeout maximum time to wait for a single response
     */
    public OrangebeardV3Client(String endpoint, UUID accessToken, String projectName, boolean connectionWithOrangebeardIsValid,
                               int maxAttempts, Duration retryDelay, Duration requestTimeout) {
        this(endpoint, accessToken, projectName, connectionWithOrangebeardIsValid,
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), requestTimeout,
                new RetryPolicy(maxAttempts, retryDelay, 2.0));
    }

    OrangebeardV3Client(String endpoint, UUID accessToken, String projectName, boolean connectionWithOrangebeardIsValid,
                        HttpClient httpClient, Duration requestTimeout, RetryPolicy retryPolicy) {
        this.accessToken = accessToken;
        this.connectionWithOrangebeardIsValid = connectionWithOrangebeardIsValid;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
        this.retryPolicy = retryPolicy;
        String trimmedEndpoint = endpoint == null ? "" : endpoint.replaceAll("/+$", "");
        this.baseUrl = trimmedEndpoint + "/listener/v3/" + encodePathSegment(projectName);
        this.authorization = "Bearer " + accessToken;
    }

    public UUID startTestRun(StartV3TestRun testRun) {
        if (!connectionIsValid()) {
            return null;
        }
        try {
            return readBody(post("/test-run/start", testRun), UUID.class);
        } catch (OrangebeardClientException e) {
            return startFailed(e);
        }
    }

    public void startAnnouncedTestRun(UUID testRunUUID) {
        if (!connectionIsValid()) {
            return;
        }
        try {
            send(request("/test-run/start/" + testRunUUID).PUT(BodyPublishers.noBody()).build());
        } catch (OrangebeardClientException e) {
            startFailed(e);
        }
    }

    public void finishTestRun(UUID testRunUUID, FinishV3TestRun finishTestRun) {
        if (connectionIsValid()) {
            put("/test-run/finish/" + testRunUUID, finishTestRun);
        }
    }

    public List<Suite> startSuite(StartSuite startSuite) {
        if (!connectionIsValid()) {
            return Collections.emptyList();
        }
        Suite[] suites = readBody(post("/suite/start", startSuite), Suite[].class);
        return suites == null ? Collections.emptyList() : Arrays.asList(suites);
    }

    public UUID startTest(StartTest startTest) {
        return connectionIsValid() ? readBody(post("/test/start", startTest), UUID.class) : null;
    }

    public void finishTest(UUID testUUID, FinishTest finishTest) {
        if (connectionIsValid()) {
            put("/test/finish/" + testUUID, finishTest);
        }
    }

    public UUID startStep(StartStep startStep) {
        return connectionIsValid() ? readBody(post("/step/start", startStep), UUID.class) : null;
    }

    public void finishStep(UUID stepUUID, FinishStep finishStep) {
        if (connectionIsValid()) {
            put("/step/finish/" + stepUUID, finishStep);
        }
    }

    public UUID log(Log log) {
        return connectionIsValid() ? readBody(post("/log", log), UUID.class) : null;
    }

    public void sendLogBatch(List<Log> logs) {
        if (connectionIsValid()) {
            post("/log/batch", logs);
        }
    }

    public UUID sendAttachment(Attachment attachment) {
        if (!connectionIsValid()) {
            return null;
        }
        String boundary = "orangebeard-" + UUID.randomUUID();
        Attachment.AttachmentFile file = attachment.getFile();
        byte[] body = multipart(boundary, Json.mapper().writeValueAsBytes(attachment.getMetaData()), file);
        HttpRequest request = request("/attachment")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofByteArray(body))
                .build();
        return readBody(send(request), UUID.class);
    }

    public UUID startAlertRun(StartAlertRun alertRun) {
        if (!connectionIsValid()) {
            return null;
        }
        try {
            return readBody(post("/alert-run/start", alertRun), UUID.class);
        } catch (OrangebeardClientException e) {
            return startFailed(e);
        }
    }

    public void finishAlertRun(FinishAlertRun finishAlertRun) {
        if (connectionIsValid()) {
            put("/alert-run/finish", finishAlertRun);
        }
    }

    public UUID reportAlert(ReportAlert alert) {
        return connectionIsValid() ? readBody(post("/alert-run/report", alert), UUID.class) : null;
    }

    private boolean connectionIsValid() {
        if (!connectionWithOrangebeardIsValid && invalidWarningLogged.compareAndSet(false, true)) {
            LOGGER.warn("Orangebeard reporting is disabled: {}", CONNECTION_FAILED);
        }
        return connectionWithOrangebeardIsValid;
    }

    /**
     * Starting a run is the first call; when it fails nothing that follows can succeed,
     * so the client stops trying instead of retrying every following call.
     */
    private UUID startFailed(OrangebeardClientException e) {
        LOGGER.error(CONNECTION_FAILED + " ({})", e.getMessage());
        connectionWithOrangebeardIsValid = false;
        return null;
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header("Authorization", authorization);
    }

    private HttpResponse<byte[]> post(String path, Object body) {
        return send(request(path).header("Content-Type", "application/json").POST(jsonBody(body)).build());
    }

    private HttpResponse<byte[]> put(String path, Object body) {
        return send(request(path).header("Content-Type", "application/json").PUT(jsonBody(body)).build());
    }

    private BodyPublisher jsonBody(Object body) {
        try {
            return BodyPublishers.ofByteArray(Json.mapper().writeValueAsBytes(body));
        } catch (JacksonException e) {
            throw new OrangebeardClientException("Could not serialize request: " + e.getMessage(), e);
        }
    }

    private <T> T readBody(HttpResponse<byte[]> response, Class<T> type) {
        byte[] body = response.body();
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            return Json.mapper().readValue(body, type);
        } catch (JacksonException e) {
            throw new OrangebeardClientException("Could not read response: " + e.getMessage(), e);
        }
    }

    private HttpResponse<byte[]> send(HttpRequest request) {
        OrangebeardClientException lastFailure = null;
        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            if (attempt > 1) {
                pause(retryPolicy.delayBefore(attempt));
            }
            try {
                HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response;
                }
                lastFailure = new OrangebeardClientException(format("%s %s failed with HTTP %d", request.method(), request.uri().getPath(), status));
                if (!isRetryable(status)) {
                    throw lastFailure;
                }
            } catch (IOException e) {
                lastFailure = new OrangebeardClientException(format("%s %s failed: %s", request.method(), request.uri().getPath(), e), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OrangebeardClientException("Interrupted while calling Orangebeard", e);
            }
            LOGGER.debug("Attempt {}/{} failed: {}", attempt, retryPolicy.maxAttempts(), lastFailure.getMessage());
        }
        throw lastFailure;
    }

    private static boolean isRetryable(int status) {
        return status >= 500 || status == 408 || status == 429;
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OrangebeardClientException("Interrupted while waiting to retry", e);
        }
    }

    private static byte[] multipart(String boundary, byte[] json, Attachment.AttachmentFile file) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(json.length + file.getContent().length + 512);
        write(out, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"json\"\r\nContent-Type: application/json\r\n\r\n");
        out.writeBytes(json);
        write(out, "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"attachment\"; filename=\""
                + file.getName().replace("\"", "%22").replace("\r", "").replace("\n", "") + "\"\r\nContent-Type: " + file.getContentType() + "\r\n\r\n");
        out.writeBytes(file.getContent());
        write(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String encodePathSegment(String segment) {
        return URLEncoder.encode(segment == null ? "" : segment, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
