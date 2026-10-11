import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

public final class StudioDiagnosticHttpFixture {
    private record Reply(int status, String body) {}

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 8);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/", StudioDiagnosticHttpFixture::respond);
        server.start();
        Thread.currentThread().join();
    }

    private static void respond(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("/disconnect".equals(path)) return;
            if ("/timeout".equals(path)) {
                try { Thread.sleep(2_200); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            Reply reply = replies().getOrDefault(path, new Reply(404, "{}"));
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(reply.status(), body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private static Map<String, Reply> replies() {
        return Map.ofEntries(
                Map.entry("/ok", new Reply(200, "{\"status\":\"SUCCESS\",\"workflow\":{\"status\":\"READY\"}}")),
                Map.entry("/conflict", new Reply(409, "{\"error\":\"STATE_CONFLICT\",\"stage\":\"WORKFLOW\",\"reasonCode\":\"ACTION_UNAVAILABLE\",\"location\":\"workflow.state\"}")),
                Map.entry("/contract", new Reply(422, "{\"error\":\"AGENT_OUTPUT_INVALID\",\"stage\":\"TEST_IMPLEMENTER\",\"reasonCode\":\"PAGE_OBJECT_API_METHOD_MISSING\",\"location\":\"$.payload.pageObjectApisUsed[item]\",\"metadata\":{\"role\":\"TEST_IMPLEMENTER\",\"profileId\":\"fixture\",\"processStatus\":\"EXITED\",\"durationMillis\":\"17\",\"inputBytes\":\"101\",\"outputBytes\":\"37\"}}")),
                Map.entry("/decoder", new Reply(502, "{\"error\":\"AGENT_OUTPUT_INVALID\",\"stage\":\"TEST_IMPLEMENTER\",\"reasonCode\":\"MALFORMED_JSON\",\"location\":\"$\",\"metadata\":{\"role\":\"TEST_IMPLEMENTER\",\"processStatus\":\"EXITED\",\"durationMillis\":\"19\",\"inputBytes\":\"103\",\"outputBytes\":\"41\"}}")),
                Map.entry("/security", new Reply(502, "{\"error\":\"AGENT_OUTPUT_INVALID\",\"stage\":\"TEST_IMPLEMENTER\",\"reasonCode\":\"JWT\",\"location\":\"$.payload.sourcePatch\",\"metadata\":{\"role\":\"TEST_IMPLEMENTER\",\"processStatus\":\"EXITED\",\"durationMillis\":\"23\",\"inputBytes\":\"107\",\"outputBytes\":\"43\"}}")),
                Map.entry("/capability", new Reply(200, "{\"resultType\":\"PAGE_OBJECT_CAPABILITY_MISSING\",\"status\":\"AGENT_FAILED\"}")),
                Map.entry("/capability-refusal", new Reply(409, "{\"error\":\"PAGE_OBJECT_CAPABILITY_MISSING\",\"stage\":\"TEST_IMPLEMENTER\",\"reasonCode\":\"PAGE_OBJECT_CAPABILITY_MISSING\",\"location\":\"$.payload.capability\",\"capabilityCategory\":\"PAGE_OBJECT_METHOD_MISSING\",\"sourceFingerprintMatched\":true,\"nextAction\":\"REVISE_REQUIREMENT_OR_PAGE_OBJECT\"}")),
                Map.entry("/extension", new Reply(200, "{\"resultType\":\"PAGE_OBJECT_EXTENSION_PROPOSAL\",\"status\":\"AWAITING_REVIEW\"}")),
                Map.entry("/invalid-json", new Reply(502, "{broken")),
                Map.entry("/empty", new Reply(502, " ")),
                Map.entry("/untrusted", new Reply(502, "{\"error\":\"AGENT_OUTPUT_INVALID\",\"stage\":\"caller-canary-7281\",\"reasonCode\":\"MALFORMED_JSON\",\"location\":\"$.payload.synthetic-secret-9137\",\"capabilityCategory\":\"caller-canary-7281\",\"sourceFingerprintMatched\":\"caller-canary-7281\",\"message\":\"untrusted message caller-canary-7281\",\"metadata\":{\"synthetic-secret-key\":\"caller-canary-7281\",\"role\":\"TEST_IMPLEMENTER\",\"profileId\":\"synthetic-secret-9137\",\"processStatus\":\"caller-canary-7281\"}}"))
        );
    }
}
