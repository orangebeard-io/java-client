package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.alerting.AlertRunStatus;
import io.orangebeard.client.entity.alerting.Tool;
import io.orangebeard.client.entity.alerting.security.FinishSecurityAlertRun;
import io.orangebeard.client.entity.alerting.security.StartSecurityAlertRun;
import io.orangebeard.client.entity.attachment.Attachment;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.log.LogLevel;
import io.orangebeard.client.entity.step.FinishStep;
import io.orangebeard.client.entity.step.StartStep;
import io.orangebeard.client.entity.suite.StartSuite;
import io.orangebeard.client.entity.suite.Suite;
import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;
import io.orangebeard.client.entity.test.TestStatus;
import io.orangebeard.client.entity.test.TestType;
import io.orangebeard.client.exceptions.OrangebeardClientException;
import io.orangebeard.client.support.FakeOrangebeard;
import io.orangebeard.client.support.FakeOrangebeard.Reply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrangebeardV3ClientTest {
    private static final UUID TOKEN = UUID.fromString("722f2956-4ef9-455f-a8aa-2be5408ec6d0");
    private static final UUID RUN = UUID.randomUUID();
    private static final UUID TEST = UUID.randomUUID();
    private static final UUID STEP = UUID.randomUUID();
    private static final ZonedDateTime NOW = ZonedDateTime.now();

    private FakeOrangebeard server;
    private OrangebeardV3Client client;

    @BeforeEach
    void setUp() {
        server = new FakeOrangebeard();
        client = clientWith(true, 3, Duration.ofMillis(10), Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private OrangebeardV3Client clientWith(boolean valid, int attempts, Duration retryDelay, Duration timeout) {
        return new OrangebeardV3Client(server.url(), TOKEN, FakeOrangebeard.PROJECT, valid, attempts, retryDelay, timeout);
    }

    @Test
    void startTestRun_posts_the_run_with_bearer_token_and_returns_the_uuid() {
        UUID expected = UUID.randomUUID();
        server.respondWith(request -> Reply.ok("\"" + expected + "\""));

        UUID actual = client.startTestRun(new StartV3TestRun("set", "desc", Set.of(new Attribute("k", "v"))));

        assertThat(actual).isEqualTo(expected);
        FakeOrangebeard.Request request = server.requestsTo("/test-run/start").get(0);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN);
        assertThat(request.contentType()).isEqualTo("application/json");
        assertThat(request.json().get("testSetName").asString()).isEqualTo("set");
        assertThat(request.json().get("startTime").isNumber()).isTrue();
    }

    @Test
    void startAnnouncedTestRun_puts_without_body() {
        UUID announced = UUID.randomUUID();

        client.startAnnouncedTestRun(announced);

        FakeOrangebeard.Request request = server.requestsTo("/test-run/start/" + announced).get(0);
        assertThat(request.method()).isEqualTo("PUT");
        assertThat(request.body()).isEmpty();
    }

    @Test
    void finishTestRun_puts_the_end_time() {
        client.finishTestRun(RUN, new FinishV3TestRun(NOW));

        FakeOrangebeard.Request request = server.requestsTo("/test-run/finish/" + RUN).get(0);
        assertThat(request.method()).isEqualTo("PUT");
        assertThat(request.json().has("endTime")).isTrue();
    }

    @Test
    void startSuite_returns_the_suites_created_by_the_server() {
        List<Suite> suites = client.startSuite(new StartSuite(RUN, null, null, Set.of(), List.of("a", "b")));

        assertThat(suites).extracting(Suite::getLocalSuiteName).containsExactly("a", "b");
        assertThat(suites.get(1).getParentUUID()).isEqualTo(suites.get(0).getSuiteUUID());
    }

    @Test
    void test_step_and_log_lifecycle_uses_the_expected_endpoints() {
        UUID test = client.startTest(new StartTest(RUN, UUID.randomUUID(), "t", TestType.TEST, null, Set.of(), NOW));
        client.finishTest(test, new FinishTest(RUN, TestStatus.PASSED, NOW));
        UUID step = client.startStep(new StartStep(RUN, TEST, null, "s", null, NOW));
        client.finishStep(step, new FinishStep(RUN, TestStatus.PASSED, NOW));
        UUID log = client.log(new Log(RUN, TEST, STEP, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
        client.sendLogBatch(List.of(new Log(RUN, TEST, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT)));

        assertThat(test).isNotNull();
        assertThat(step).isNotNull();
        assertThat(log).isNotNull();
        assertThat(server.requests()).extracting(r -> r.method() + " " + r.apiPath()).containsExactly(
                "POST /test/start",
                "PUT /test/finish/" + test,
                "POST /step/start",
                "PUT /step/finish/" + step,
                "POST /log",
                "POST /log/batch");
        assertThat(server.requestsTo("/log/batch").get(0).json().isArray()).isTrue();
    }

    @Test
    void alert_run_lifecycle_uses_the_expected_endpoints() {
        UUID alertRun = client.startAlertRun(new StartSecurityAlertRun("alerts", "desc", Tool.ZAP, NOW, Set.of()));
        client.finishAlertRun(new FinishSecurityAlertRun(alertRun, AlertRunStatus.COMPLETED, NOW));

        assertThat(alertRun).isNotNull();
        assertThat(server.requestsTo("/alert-run/start").get(0).json().get("type").asString()).isEqualTo("StartSecurityAlertRun");
        assertThat(server.requestsTo("/alert-run/finish").get(0).method()).isEqualTo("PUT");
    }

    @Test
    void sendAttachment_posts_multipart_with_json_metadata_and_file() {
        byte[] content = "file-content".getBytes(StandardCharsets.UTF_8);
        Attachment attachment = new Attachment(
                new Attachment.AttachmentFile("report.txt", content, "text/plain"),
                new Attachment.AttachmentMetaData(RUN, TEST, STEP, UUID.randomUUID(), NOW));

        UUID id = client.sendAttachment(attachment);

        assertThat(id).isNotNull();
        FakeOrangebeard.Request request = server.requestsTo("/attachment").get(0);
        assertThat(request.contentType()).startsWith("multipart/form-data; boundary=");
        String body = request.bodyText();
        assertThat(body).contains("name=\"json\"", "Content-Type: application/json", "\"testRunUUID\":\"" + RUN + "\"");
        assertThat(body).contains("name=\"attachment\"; filename=\"report.txt\"", "Content-Type: text/plain", "file-content");
    }

    @Test
    void server_errors_are_retried_until_they_succeed() {
        AtomicInteger calls = new AtomicInteger();
        server.respondWith(request -> calls.incrementAndGet() < 3 ? Reply.status(503) : null);

        UUID result = client.startTest(new StartTest(RUN, UUID.randomUUID(), "t", TestType.TEST, null, Set.of(), NOW));

        assertThat(result).isNotNull();
        assertThat(server.requests()).hasSize(3);
    }

    @Test
    void rate_limiting_is_retried() {
        AtomicInteger calls = new AtomicInteger();
        server.respondWith(request -> calls.incrementAndGet() < 2 ? Reply.status(429) : null);

        assertThat(client.log(new Log(RUN, TEST, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT))).isNotNull();
    }

    @Test
    void client_errors_are_not_retried() {
        server.respondWith(request -> Reply.status(400));

        assertThatThrownBy(() -> client.startTest(new StartTest(RUN, UUID.randomUUID(), "t", TestType.TEST, null, Set.of(), NOW)))
                .isInstanceOf(OrangebeardClientException.class)
                .hasMessageContaining("HTTP 400");
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void gives_up_after_the_configured_number_of_attempts() {
        server.respondWith(request -> Reply.status(500));

        assertThatThrownBy(() -> client.log(new Log(RUN, TEST, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT)))
                .isInstanceOf(OrangebeardClientException.class)
                .hasMessageContaining("HTTP 500");
        assertThat(server.requests()).hasSize(3);
    }

    @Test
    void a_stalled_server_triggers_the_request_timeout() {
        server.respondWith(request -> new Reply(200, "", 2000));
        OrangebeardV3Client impatient = clientWith(true, 1, Duration.ofMillis(1), Duration.ofMillis(200));

        assertThatThrownBy(() -> impatient.log(new Log(RUN, TEST, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT)))
                .isInstanceOf(OrangebeardClientException.class);
    }

    @Test
    void failing_to_start_a_run_disables_the_client() {
        server.respondWith(request -> Reply.status(401));

        assertThat(client.startTestRun(new StartV3TestRun("set", "desc", Set.of()))).isNull();
        int requestsAfterStart = server.requests().size();
        assertThat(client.log(new Log(RUN, TEST, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT))).isNull();

        assertThat(server.requests()).hasSize(requestsAfterStart);
    }

    @Test
    void a_client_that_is_not_valid_does_not_send_anything() {
        OrangebeardV3Client invalid = clientWith(false, 3, Duration.ofMillis(10), Duration.ofSeconds(1));

        assertThat(invalid.startTestRun(new StartV3TestRun("set", "desc", Set.of()))).isNull();
        assertThat(invalid.startSuite(new StartSuite(RUN, null, null, Set.of(), List.of("a")))).isEmpty();
        assertThat(invalid.log(new Log(RUN, TEST, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT))).isNull();
        invalid.finishTestRun(RUN, new FinishV3TestRun(NOW));
        invalid.startAnnouncedTestRun(RUN);

        assertThat(server.requests()).isEmpty();
    }

    @Test
    void project_names_with_special_characters_are_encoded_in_the_url() {
        OrangebeardV3Client spaced = new OrangebeardV3Client(server.url() + "/", TOKEN, "my project", true, 1, Duration.ofMillis(1), Duration.ofSeconds(5));

        spaced.finishTestRun(RUN, new FinishV3TestRun(NOW));

        assertThat(server.requests().get(0).path()).isEqualTo("/listener/v3/my%20project/test-run/finish/" + RUN);
    }
}
