package app.lexo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class GeminiStub {

    public record Received(String path, String query, JsonNode body) {
    }

    private record Reply(int status, String body) {
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private volatile Reply reply = new Reply(200, "{}");

    GeminiStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            received.add(new Received(exchange.getRequestURI().getPath(), exchange.getRequestURI().getQuery(),
                    body.isBlank() ? null : JSON.readTree(body)));
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta";
    }

    public void reset() {
        received.clear();
        reply = new Reply(200, "{}");
    }

    public void answers(String text) {
        reply = new Reply(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quoted(text) + "}]}}]}");
    }

    public void answersWith(int status, String body) {
        reply = new Reply(status, body);
    }

    public List<Received> requests() {
        return new ArrayList<>(received);
    }

    public Received onlyRequest() {
        if (received.size() != 1) {
            throw new AssertionError("expected exactly one request to Gemini but got " + received.size());
        }
        return received.get(0);
    }

    private static String quoted(String text) {
        try {
            return JSON.writeValueAsString(text);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
