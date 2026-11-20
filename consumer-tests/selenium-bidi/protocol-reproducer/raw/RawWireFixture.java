import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class RawWireFixture {
    private record Row(String path, int status, long timestamp) {}
    public static void main(String[] args) throws Exception {
        Path ready = Path.of(args.length > 0 ? args[0] : "target/raw-bidi-diagnostic/fixture-url.txt");
        String bind = args.length > 1 ? args[1] : "127.0.0.1";
        String host = args.length > 2 ? args[2] : "127.0.0.1";
        HttpServer server = HttpServer.create(new InetSocketAddress(bind, 0), 0);
        List<Row> journal = new ArrayList<>();
        boolean[] overflow = {false};
        CountDownLatch shutdown = new CountDownLatch(1);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/__journal")) {
                StringBuilder json = new StringBuilder("{\"overflow\":");
                synchronized (journal) {
                    json.append(overflow[0]).append(",\"rows\":[");
                    for (int i = 0; i < journal.size(); i++) {
                        Row row = journal.get(i);
                        if (i > 0) json.append(',');
                        json.append("{\"path\":\"").append(row.path()).append("\",\"status\":")
                            .append(row.status()).append(",\"timestamp\":").append(row.timestamp()).append('}');
                    }
                }
                byte[] bytes = json.append("]}").toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (exchange) { exchange.getResponseBody().write(bytes); }
                return;
            }
            if (path.equals("/__shutdown") && exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                shutdown.countDown();
                return;
            }
            int status = path.matches("/redirect/[0-9]+") ? 302
                : path.matches("/final/[0-9]+") || path.equals("/probe") ? 200 : 404;
            if (status == 302) exchange.getResponseHeaders().set("Location", path.replace("/redirect/", "/final/"));
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            byte[] body = (path.equals("/probe") ? "<!doctype html><title>Protocol probe</title>" : "controlled response")
                .getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(status, status == 302 ? -1 : body.length);
                synchronized (journal) {
                    if (path.matches("/(redirect|final)/[0-9]+")) {
                        if (journal.size() < 1000) journal.add(new Row(path, status, Instant.now().toEpochMilli()));
                        else overflow[0] = true;
                    }
                }
                if (status != 302) exchange.getResponseBody().write(body);
            } finally { exchange.close(); }
        });
        server.start();
        String origin = "http://" + host + ":" + server.getAddress().getPort();
        if (ready.toAbsolutePath().getParent() != null) Files.createDirectories(ready.toAbsolutePath().getParent());
        Files.writeString(ready, origin, StandardCharsets.UTF_8);
        System.out.println("FIXTURE_URL=" + origin);
        try { shutdown.await(); }
        finally { server.stop(0); }
    }
}
