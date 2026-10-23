package io.orangebeard.client;

import io.orangebeard.client.support.FakeOrangebeard;
import io.orangebeard.client.support.FakeOrangebeard.Reply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CliTest {
    private static final UUID TOKEN = UUID.fromString("722f2956-4ef9-455f-a8aa-2be5408ec6d0");

    private FakeOrangebeard server;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void setUp() {
        server = new FakeOrangebeard();
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private OrangebeardProperties config() {
        return new OrangebeardProperties(server.url(), TOKEN, FakeOrangebeard.PROJECT, "set");
    }

    private int run(OrangebeardProperties config, String... args) {
        return Cli.run(args, config, new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8).trim();
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void start_prints_the_uuid_of_the_new_test_run() {
        UUID expected = UUID.randomUUID();
        server.respondWith(request -> Reply.ok("\"" + expected + "\""));

        int exit = run(config(), "-x", "start", "-d", "my description", "-a", "key:value");

        assertThat(exit).isZero();
        assertThat(out()).isEqualTo(expected.toString());
        assertThat(server.requestsTo("/test-run/start").get(0).json().get("description").asString()).isEqualTo("my description");
    }

    @Test
    void command_line_options_override_the_configuration_used_by_the_client() {
        OrangebeardProperties wrong = new OrangebeardProperties("http://127.0.0.1:1", UUID.randomUUID(), "other-project", "set");

        int exit = run(wrong, "-x", "start", "-e", server.url(), "-t", TOKEN.toString(), "-p", FakeOrangebeard.PROJECT);

        assertThat(exit).isZero();
        FakeOrangebeard.Request request = server.requestsTo("/test-run/start").get(0);
        assertThat(request.authorization()).isEqualTo("Bearer " + TOKEN);
    }

    @Test
    void finish_finishes_the_given_test_run() {
        UUID run = UUID.randomUUID();

        int exit = run(config(), "-x", "finish", "-id", run.toString());

        assertThat(exit).isZero();
        assertThat(out()).isEqualTo("Orangebeard report Finished!");
        assertThat(server.requestsTo("/test-run/finish/" + run)).hasSize(1);
    }

    @Test
    void security_alert_runs_can_be_started_and_finished() {
        UUID expected = UUID.randomUUID();
        server.respondWith(request -> request.apiPath().equals("/alert-run/start") ? Reply.ok("\"" + expected + "\"") : null);

        assertThat(run(config(), "-x", "start", "-k", "security", "-at", "zap")).isZero();
        assertThat(out()).isEqualTo(expected.toString());
        assertThat(server.requestsTo("/alert-run/start").get(0).json().get("tool").asString()).isEqualTo("ZAP");

        assertThat(run(config(), "-x", "finish", "-k", "security", "-id", expected.toString(), "-as", "INTERRUPTED")).isZero();
        assertThat(server.requestsTo("/alert-run/finish").get(0).json().get("status").asString()).isEqualTo("INTERRUPTED");
    }

    @Test
    void starting_an_alert_run_requires_a_known_tool() {
        assertThat(run(config(), "-x", "start", "-k", "security")).isEqualTo(1);
        assertThat(err()).contains("tool arg is required");

        assertThat(run(config(), "-x", "start", "-k", "security", "-at", "nessus")).isEqualTo(1);
        assertThat(err()).contains("Unknown alert tool");
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void a_run_that_cannot_be_started_fails_the_command() {
        server.respondWith(request -> Reply.status(401));

        assertThat(run(config(), "-x", "start")).isEqualTo(1);
        assertThat(out()).isEmpty();
        assertThat(err()).contains("could not be started");
    }

    @Test
    void invalid_input_prints_usage_or_an_error_and_fails() {
        assertThat(run(config())).isEqualTo(1);
        assertThat(out()).contains(Cli.CLI_NAME);

        assertThat(run(config(), "-x", "dance")).isEqualTo(1);
        assertThat(run(config(), "--bogus")).isEqualTo(1);
        assertThat(run(config(), "-x", "finish")).isEqualTo(1);
        assertThat(run(config(), "-x", "finish", "-id", "not-a-uuid")).isEqualTo(1);
        assertThat(err()).contains("not valid");
        assertThat(run(config(), "-x", "start", "-t", "not-a-uuid")).isEqualTo(1);
        assertThat(server.requests()).isEmpty();
    }

    @Test
    void a_failing_finish_fails_the_command() {
        server.respondWith(request -> Reply.status(404));

        assertThat(run(config(), "-x", "finish", "-id", UUID.randomUUID().toString())).isEqualTo(1);
        assertThat(err()).contains("could not be finished");
    }
}
