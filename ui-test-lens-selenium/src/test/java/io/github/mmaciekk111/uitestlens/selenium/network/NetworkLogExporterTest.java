package io.github.mmaciekk111.uitestlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkLogExporterTest {

    @Test
    void exportsEscapedJson() {
        NetworkEvent event = NetworkEvent.request(new NetworkRequest(
                "1",
                "POST",
                "https://app.example.com/api/orders?note=\"x\"",
                "xhr",
                null,
                Map.of("X-Test", "line\nvalue")
        ));

        String json = new NetworkLogExporter().export(List.of(event));

        assertTrue(json.startsWith("["));
        assertTrue(json.contains("\\\"x\\\""));
        assertTrue(json.contains("line\\nvalue"));
    }
}
