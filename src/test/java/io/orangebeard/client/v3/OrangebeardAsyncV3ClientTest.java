package io.orangebeard.client.v3;

import io.orangebeard.client.OrangebeardProperties;
import io.orangebeard.client.OrangebeardV3Client;
import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.alerting.AlertRunStatus;
import io.orangebeard.client.entity.alerting.Severity;
import io.orangebeard.client.entity.alerting.Tool;
import io.orangebeard.client.entity.alerting.security.Confidence;
import io.orangebeard.client.entity.alerting.security.FinishSecurityAlertRun;
import io.orangebeard.client.entity.alerting.security.ReportSecurityAlert;
import io.orangebeard.client.entity.alerting.security.StartSecurityAlertRun;
import io.orangebeard.client.entity.attachment.Attachment;
import io.orangebeard.client.entity.log.Log;
import io.orangebeard.client.entity.log.LogLevel;
import io.orangebeard.client.entity.step.FinishStep;
import io.orangebeard.client.entity.step.StartStep;
import io.orangebeard.client.entity.suite.StartSuite;
import io.orangebeard.client.entity.test.FinishTest;
import io.orangebeard.client.entity.test.StartTest;
import io.orangebeard.client.entity.test.TestStatus;
import io.orangebeard.client.entity.test.TestType;
import io.orangebeard.client.support.FakeOrangebeard;
import io.orangebeard.client.support.FakeOrangebeard.Reply;
import io.orangebeard.client.support.FakeOrangebeard.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(30)
class OrangebeardAsyncV3ClientTest {
    private static final ZonedDateTime NOW = ZonedDateTime.now();

    private FakeOrangebeard server;
    private OrangebeardAsyncV3Client client;

    @BeforeEach
    void setUp() {
        server = new FakeOrangebeard();
        client = new OrangebeardAsyncV3Client(syncClient(), Executors.newFixedThreadPool(4), Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private OrangebeardV3Client syncClient() {
        return new OrangebeardV3Client(server.url(), UUID.randomUUID(), FakeOrangebeard.PROJECT, true, 2, Duration.ofMillis(10), Duration.ofSeconds(5));
    }

    private UUID startRun() {
        return client.startTestRun(new StartV3TestRun("set", "desc", Set.of(new Attribute("k", "v"))));
    }

    private UUID startTest(UUID run) {
        UUID suite = client.startSuite(new StartSuite(run, null, null, Set.of(), List.of("suite"))).get(0);
        return client.startTest(new StartTest(run, suite, "test", TestType.TEST, null, Set.of(), NOW));
    }

    @Test
    void full_hierarchy_is_reported_with_the_real_uuids_in_order() {
        UUID run = startRun();
        UUID suite = client.startSuite(new StartSuite(run, null, "d", Set.of(), List.of("a", "b"))).get(1);
        UUID test = client.startTest(new StartTest(run, suite, "test", TestType.TEST, null, Set.of(), NOW));
        UUID step = client.startStep(new StartStep(run, test, null, "step", null, NOW));
        UUID log = client.log(new Log(run, test, step, "message", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
        client.sendAttachment(new Attachment(
                new Attachment.AttachmentFile("a.txt", new byte[]{1}, "text/plain"),
                new Attachment.AttachmentMetaData(run, test, step, log, NOW)));
        client.finishStep(step, new FinishStep(run, TestStatus.PASSED, NOW));
        client.finishTest(test, new FinishTest(run, TestStatus.PASSED, NOW));
        client.finishTestRun(run, new FinishV3TestRun(NOW));

        List<String> calls = server.requests().stream().map(Request::apiPath).map(path -> path.replaceAll("/[0-9a-f-]{36}$", "")).toList();
        assertThat(calls).containsExactlyInAnyOrder(
                "/test-run/start", "/suite/start", "/test/start", "/step/start", "/log", "/attachment",
                "/step/finish", "/test/finish", "/test-run/finish");
        assertThat(calls.get(0)).isEqualTo("/test-run/start");
        assertThat(calls.get(calls.size() - 1)).isEqualTo("/test-run/finish");

        String realRun = server.requestsTo("/suite/start").get(0).json().get("testRunUUID").asString();
        assertThat(realRun).isNotEqualTo(run.toString());
        assertThat(server.requestsTo("/test/start").get(0).json().get("testRunUUID").asString()).isEqualTo(realRun);
        assertThat(server.requestsTo("/step/start").get(0).json().get("testRunUUID").asString()).isEqualTo(realRun);
        assertThat(server.requests().get(server.requests().size() - 1).apiPath()).isEqualTo("/test-run/finish/" + realRun);

        String realSuiteB = server.requestsTo("/test/start").get(0).json().get("suiteUUID").asString();
        assertThat(realSuiteB).isNotEqualTo(suite.toString());
        String realTest = server.requestsTo("/step/start").get(0).json().get("testUUID").asString();
        assertThat(server.requestsTo("/log").get(0).json().get("testUUID").asString()).isEqualTo(realTest);
        assertThat(server.requestsTo("/attachment").get(0).bodyText()).contains(realTest);
    }

    @Test
    void sub_suites_use_the_real_uuid_of_their_parent_suite() {
        UUID run = startRun();
        List<UUID> parents = client.startSuite(new StartSuite(run, null, null, Set.of(), List.of("a", "b")));
        client.startSuite(new StartSuite(run, parents.get(0), null, Set.of(), List.of("c")));
        client.finishTestRun(run, new FinishV3TestRun(NOW));

        List<Request> suiteStarts = server.requestsTo("/suite/start");
        String parentOfFirst = suiteStarts.stream().filter(r -> r.json().get("parentSuiteUUID").isNull()).findFirst().orElseThrow().bodyText();
        assertThat(parentOfFirst).contains("\"a\"");
        Request child = suiteStarts.stream().filter(r -> !r.json().get("parentSuiteUUID").isNull()).findFirst().orElseThrow();
        assertThat(child.json().get("parentSuiteUUID").asString()).isNotEqualTo(parents.get(0).toString());
    }

    @Test
    void calls_return_immediately_while_the_server_is_slow() {
        server.respondWith(request -> request.apiPath().equals("/log") ? new Reply(200, "\"" + UUID.randomUUID() + "\"", 50) : null);
        UUID run = startRun();
        UUID test = startTest(run);

        long start = System.nanoTime();
        for (int i = 0; i < 200; i++) {
            client.log(new Log(run, test, null, "message " + i, LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
        }
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(millis).as("time spent on the caller thread").isLessThan(500);
        client.finishTestRun(run, new FinishV3TestRun(NOW));
        assertThat(server.requestsTo("/log")).hasSize(200);
    }

    @Test
    void a_call_made_after_its_parent_completed_still_does_not_block_the_caller() throws Exception {
        server.respondWith(request -> request.apiPath().equals("/step/start") ? new Reply(200, "\"" + UUID.randomUUID() + "\"", 500) : null);
        UUID run = startRun();
        UUID test = startTest(run);
        Thread.sleep(300); // let the parents complete

        long start = System.nanoTime();
        client.startStep(new StartStep(run, test, null, "step", null, NOW));
        long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(millis).isLessThan(200);
        client.finishTestRun(run, new FinishV3TestRun(NOW));
    }

    @Test
    void a_failed_parent_skips_its_children_and_finishing_does_not_hang() {
        server.respondWith(request -> request.apiPath().equals("/test/start") ? Reply.status(400) : null);
        UUID run = startRun();
        UUID test = startTest(run);

        client.startStep(new StartStep(run, test, null, "step", null, NOW));
        client.log(new Log(run, test, null, "message", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
        client.finishTest(test, new FinishTest(run, TestStatus.FAILED, NOW));
        client.finishTestRun(run, new FinishV3TestRun(NOW));

        assertThat(server.requestsTo("/step/start")).isEmpty();
        assertThat(server.requestsTo("/log")).isEmpty();
        assertThat(server.requests()).anyMatch(r -> r.apiPath().startsWith("/test-run/finish/"));
    }

    @Test
    void unknown_parents_do_not_throw_on_the_caller_thread() {
        UUID unknown = UUID.randomUUID();

        client.startTest(new StartTest(unknown, unknown, "t", TestType.TEST, null, Set.of(), NOW));
        client.log(new Log(unknown, unknown, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
        client.finishStep(unknown, new FinishStep(unknown, TestStatus.PASSED, NOW));
        client.finishTestRun(unknown, new FinishV3TestRun(NOW));

        assertThat(server.requests()).isEmpty();
    }

    @Test
    void an_unreachable_server_does_not_hang_the_run() {
        server.respondWith(request -> Reply.status(500));
        UUID run = startRun();

        UUID test = startTest(run);
        client.log(new Log(run, test, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));

        client.finishTestRun(run, new FinishV3TestRun(NOW));
    }

    @Test
    void finishing_gives_up_waiting_after_the_timeout_but_still_returns() {
        server.respondWith(request -> request.apiPath().equals("/log") ? new Reply(200, "\"" + UUID.randomUUID() + "\"", 3000) : null);
        OrangebeardAsyncV3Client impatient = new OrangebeardAsyncV3Client(syncClient(), Executors.newFixedThreadPool(2), Duration.ofMillis(300));
        UUID run = impatient.startTestRun(new StartV3TestRun("set", "desc", Set.of()));
        UUID suite = impatient.startSuite(new StartSuite(run, null, null, Set.of(), List.of("s"))).get(0);
        UUID test = impatient.startTest(new StartTest(run, suite, "t", TestType.TEST, null, Set.of(), NOW));
        impatient.log(new Log(run, test, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));

        long start = System.nanoTime();
        impatient.finishTestRun(run, new FinishV3TestRun(NOW));

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(2500);
        assertThat(server.requests()).anyMatch(r -> r.apiPath().startsWith("/test-run/finish/"));
    }

    @Test
    void concurrent_reporting_from_many_threads_reports_everything() throws Exception {
        UUID run = startRun();
        UUID test = startTest(run);
        int threads = 8;
        int logsPerThread = 50;
        ExecutorService callers = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            callers.submit(() -> {
                go.await();
                for (int i = 0; i < logsPerThread; i++) {
                    client.log(new Log(run, test, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
                }
                return null;
            });
        }
        go.countDown();
        callers.shutdown();
        assertThat(callers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        client.finishTestRun(run, new FinishV3TestRun(NOW));

        assertThat(server.requestsTo("/log")).hasSize(threads * logsPerThread);
    }

    @Test
    void logs_of_a_finished_test_are_forgotten_but_late_logs_for_the_test_itself_still_work() {
        UUID run = startRun();
        UUID test = startTest(run);
        UUID log = client.log(new Log(run, test, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
        client.finishTest(test, new FinishTest(run, TestStatus.PASSED, NOW));
        awaitRequests("/test/finish", 1);
        sleep(200); // eviction happens right after the finish request completes

        // The log was evicted: an attachment for it can no longer be linked and is skipped.
        client.sendAttachment(new Attachment(
                new Attachment.AttachmentFile("a.txt", new byte[]{1}, "text/plain"),
                new Attachment.AttachmentMetaData(run, test, null, log, NOW)));
        client.finishTestRun(run, new FinishV3TestRun(NOW));

        assertThat(server.requestsTo("/attachment")).isEmpty();
    }

    @Test
    void alert_run_lifecycle_translates_uuids() {
        UUID alertRun = client.startAlertRun(new StartSecurityAlertRun("alerts", "d", Tool.ZAP, NOW, Set.of()));
        client.reportAlert(new ReportSecurityAlert(alertRun, "def", "d", "m", "r", Set.of(), Confidence.FIRM, null, Severity.HIGH));
        client.finishAlertRun(new FinishSecurityAlertRun(alertRun, AlertRunStatus.COMPLETED, NOW));

        assertThat(server.requestsTo("/alert-run/start")).hasSize(1);
        String reported = server.requestsTo("/alert-run/report").get(0).json().get("alertRunUUID").asString();
        assertThat(reported).isNotEqualTo(alertRun.toString());
        assertThat(server.requestsTo("/alert-run/finish").get(0).json().get("alertRunUUID").asString()).isEqualTo(reported);
    }

    @Test
    void announced_test_runs_are_started_with_their_own_uuid() {
        UUID announced = UUID.randomUUID();

        client.startAnnouncedTestRun(announced);
        UUID suite = client.startSuite(new StartSuite(announced, null, null, Set.of(), List.of("s"))).get(0);
        client.finishTestRun(announced, new FinishV3TestRun(NOW));

        assertThat(suite).isNotNull();
        assertThat(server.requestsTo("/test-run/start/" + announced)).hasSize(1);
        assertThat(server.requestsTo("/suite/start").get(0).json().get("testRunUUID").asString()).isEqualTo(announced.toString());
        assertThat(server.requestsTo("/test-run/finish/" + announced)).hasSize(1);
    }

    @Test
    void a_run_uuid_from_the_configuration_is_used_as_is() {
        UUID configured = UUID.randomUUID();
        OrangebeardProperties properties = new OrangebeardProperties(server.url(), UUID.randomUUID(), FakeOrangebeard.PROJECT, "set");
        properties.setTestRunUUID(configured);
        OrangebeardAsyncV3Client configuredClient = new OrangebeardAsyncV3Client(properties);

        configuredClient.startSuite(new StartSuite(configured, null, null, Set.of(), List.of("s")));
        configuredClient.finishTestRun(configured, new FinishV3TestRun(NOW));

        assertThat(server.requestsTo("/suite/start").get(0).json().get("testRunUUID").asString()).isEqualTo(configured.toString());
    }

    @Test
    void sendLogBatch_forwards_to_single_logs() {
        UUID run = startRun();
        UUID test = startTest(run);

        client.sendLogBatch(List.of(
                new Log(run, test, null, "1", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT),
                new Log(run, test, null, "2", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT)));
        client.finishTestRun(run, new FinishV3TestRun(NOW));

        assertThat(server.requestsTo("/log")).hasSize(2);
    }

    @Test
    void an_invalid_configuration_never_calls_the_server() {
        OrangebeardAsyncV3Client disabled = new OrangebeardAsyncV3Client(server.url(), UUID.randomUUID(), FakeOrangebeard.PROJECT, false);

        UUID run = disabled.startTestRun(new StartV3TestRun("set", "desc", Set.of()));
        UUID test = disabled.startTest(new StartTest(run, run, "t", TestType.TEST, null, Set.of(), NOW));
        disabled.log(new Log(run, test, null, "m", LogLevel.INFO, NOW, LogFormat.PLAIN_TEXT));
        disabled.finishTestRun(run, new FinishV3TestRun(NOW));

        assertThat(server.requests()).isEmpty();
    }

    @Test
    void the_blocking_helpers_for_the_cli_talk_to_the_server_directly() {
        UUID run = client.startTestRunAndAwaitRealUUID(new StartV3TestRun("set", "desc", Set.of()));
        client.finishTestRunWithRealUUID(run, new FinishV3TestRun(NOW));
        UUID alertRun = client.startAlertRunAndAwaitRealUUID(new StartSecurityAlertRun("alerts", "d", Tool.ZAP, NOW, Set.of()));
        client.finishAlertRunWithRealUUID(new FinishSecurityAlertRun(alertRun, AlertRunStatus.INTERRUPTED, NOW));

        assertThat(run).isNotNull();
        assertThat(server.requestsTo("/test-run/finish/" + run)).hasSize(1);
        assertThat(server.requestsTo("/alert-run/finish")).hasSize(1);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void awaitRequests(String apiPathPrefix, int count) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (server.requests().stream().filter(r -> r.apiPath().startsWith(apiPathPrefix)).count() < count) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for " + apiPathPrefix);
            }
            Thread.onSpinWait();
        }
    }
}
