package io.orangebeard.client;

import io.orangebeard.client.entity.Attribute;
import io.orangebeard.client.entity.ChangedComponent;
import io.orangebeard.client.entity.FinishV3TestRun;
import io.orangebeard.client.entity.LogFormat;
import io.orangebeard.client.entity.StartV3TestRun;
import io.orangebeard.client.entity.alerting.AlertRunStatus;
import io.orangebeard.client.entity.alerting.Tool;
import io.orangebeard.client.entity.alerting.security.Confidence;
import io.orangebeard.client.entity.alerting.security.Evidence;
import io.orangebeard.client.entity.alerting.security.FinishSecurityAlertRun;
import io.orangebeard.client.entity.alerting.security.ReportSecurityAlert;
import io.orangebeard.client.entity.alerting.security.StartSecurityAlertRun;
import io.orangebeard.client.entity.alerting.Severity;
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
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the JSON wire format of the request entities (dates as epoch seconds, UUIDs and enums by name).
 */
class WireFormatTest {
    private static final UUID RUN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUITE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TEST = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID STEP = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID LOG = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final ZonedDateTime TIME = ZonedDateTime.of(2024, 1, 2, 3, 4, 5, 678_000_000, ZoneOffset.UTC);

    private static Set<Attribute> attributes() {
        Set<Attribute> attributes = new LinkedHashSet<>();
        attributes.add(new Attribute("key", "value"));
        attributes.add(new Attribute("tag"));
        return attributes;
    }

    private static List<Object> allRequests() {
        Evidence evidence = new Evidence("http://u", "GET", 200, Map.of("a", "b"), "req", Map.of("c", "d"), "resp", "other");
        return List.of(
                new StartV3TestRun("set", "desc", TIME, attributes(), Set.of(new ChangedComponent("comp", "1.0"))),
                new FinishV3TestRun(TIME),
                new StartSuite(RUN, null, "desc", attributes(), List.of("a", "b")),
                new StartTest(RUN, SUITE, "test", TestType.TEST, "desc", attributes(), TIME),
                new FinishTest(RUN, TestStatus.PASSED, TIME),
                new StartStep(RUN, TEST, null, "step", "desc", TIME),
                new FinishStep(RUN, TestStatus.FAILED, TIME),
                new Log(RUN, TEST, STEP, "message", LogLevel.INFO, TIME, LogFormat.PLAIN_TEXT),
                new Attachment.AttachmentMetaData(RUN, TEST, STEP, LOG, TIME),
                new StartSecurityAlertRun("alerts", "desc", Tool.ZAP, TIME, attributes()),
                new FinishSecurityAlertRun(RUN, AlertRunStatus.COMPLETED, TIME),
                new ReportSecurityAlert(RUN, "def", "desc", "msg", "fix", attributes(), Confidence.FIRM, evidence, Severity.HIGH)
        );
    }

    @Test
    void every_request_serializes_with_numeric_times() throws Exception {
        for (Object request : allRequests()) {
            String json = Json.mapper().writeValueAsString(request);
            assertThat(json).as(request.getClass().getSimpleName()).doesNotContain("2024-01-02");
        }
    }

    @Test
    void times_are_written_as_epoch_seconds_with_nanos() throws Exception {
        assertThat(Json.mapper().writeValueAsString(new FinishV3TestRun(TIME)))
                .isEqualTo("{\"endTime\":1704164645.678000000}");
    }

    @Test
    void test_status_log_and_uuid_fields_are_serialized_by_name() throws Exception {
        assertThat(Json.mapper().readTree(Json.mapper().writeValueAsString(new FinishTest(RUN, TestStatus.PASSED, TIME))))
                .isEqualTo(Json.mapper().readTree("{\"testRunUUID\":\"00000000-0000-0000-0000-000000000001\",\"status\":\"PASSED\",\"endTime\":1704164645.678000000}"));
    }

    @Test
    void alert_requests_carry_a_type_discriminator() throws Exception {
        assertThat(Json.mapper().writeValueAsString(new FinishSecurityAlertRun(RUN, AlertRunStatus.COMPLETED, TIME)))
                .contains("\"type\":\"FinishSecurityAlertRun\"");
    }

    @Test
    void suite_response_is_deserialized() throws Exception {
        String json = "[{\"suiteUUID\":\"" + SUITE + "\",\"parentUUID\":null,\"localSuiteName\":\"a\",\"fullSuitePath\":[\"a\"],\"unknown\":1}]";

        Suite[] suites = Json.mapper().readValue(json, Suite[].class);

        assertThat(suites).hasSize(1);
        assertThat(suites[0].getSuiteUUID()).isEqualTo(SUITE);
        assertThat(suites[0].getFullSuitePath()).containsExactly("a");
    }

    @Test
    void uuid_response_is_deserialized() throws Exception {
        assertThat(Json.mapper().readValue("\"" + TEST + "\"", UUID.class)).isEqualTo(TEST);
    }
}
