package io.orangebeard.client.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * In-process stand-in for the Orangebeard listener API. Records every request and, unless told otherwise,
 * answers like the real server: new UUIDs for "start" calls, the suite path for suite starts, empty 200s for the rest.
 */
public class FakeOrangebeard implements AutoCloseable {
    public static final String PROJECT = "my-project";
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public record Request(String method, String path, String authorization, String contentType, byte[] body) {
        public String bodyText() {
            return new String(body, StandardCharsets.UTF_8);
        }

        public JsonNode json() {
            return MAPPER.readTree(body);
        }

        /** Path relative to the project, e.g. "/test/start". */
        public String apiPath() {
            return path.substring(("/listener/v3/" + PROJECT).length());
        }
    }

    public record Reply(int status, String body, long delayMillis) {
        public static Reply ok(String body) {
            return new Reply(200, body, 0);
        }

        public static Reply status(int status) {
            return new Reply(status, "", 0);
        }
    }

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile Function<Request, Reply> responder = request -> null;

    public FakeOrangebeard() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "fake-orangebeard");
            thread.setDaemon(true);
            return thread;
        }));
        server.createContext("/", this::handle);
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Overrides the default answer; return null from the function to fall back to the default. */
    public void respondWith(Function<Request, Reply> responder) {
        this.responder = responder;
    }

    public List<Request> requests() {
        return new ArrayList<>(requests);
    }

    public List<Request> requestsTo(String apiPath) {
        return requests.stream().filter(request -> request.apiPath().equals(apiPath)).toList();
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        Request request = new Request(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getRawPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Content-Type"),
                body);
        requests.add(request);

        Reply reply = responder.apply(request);
        if (reply == null) {
            reply = defaultReply(request);
        }
        if (reply.delayMillis() > 0) {
            try {
                Thread.sleep(reply.delayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] responseBody = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(reply.status(), responseBody.length == 0 ? -1 : responseBody.length);
        if (responseBody.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(responseBody);
            }
        }
        exchange.close();
    }

    private Reply defaultReply(Request request) {
        if (!request.method().equals("POST")) {
            return Reply.status(200);
        }
        return switch (request.apiPath()) {
            case "/suite/start" -> Reply.ok(suitesFor(request.json()));
            case "/log/batch" -> Reply.status(200);
            default -> Reply.ok("\"" + UUID.randomUUID() + "\"");
        };
    }

    private String suitesFor(JsonNode startSuite) {
        List<String> names = new ArrayList<>();
        startSuite.get("suiteNames").forEach(name -> names.add(name.asString()));
        StringBuilder json = new StringBuilder("[");
        UUID parent = null;
        for (int i = 0; i < names.size(); i++) {
            UUID suite = UUID.randomUUID();
            json.append(i == 0 ? "" : ",")
                    .append("{\"suiteUUID\":\"").append(suite).append("\",")
                    .append("\"parentUUID\":").append(parent == null ? "null" : "\"" + parent + "\"").append(",")
                    .append("\"localSuiteName\":\"").append(names.get(i)).append("\",")
                    .append("\"fullSuitePath\":[").append(String.join(",", names.subList(0, i + 1).stream().map(n -> "\"" + n + "\"").toList())).append("]}");
            parent = suite;
        }
        return json.append("]").toString();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
