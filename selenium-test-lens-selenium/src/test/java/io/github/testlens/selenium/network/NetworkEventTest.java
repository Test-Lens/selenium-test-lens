package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NetworkEventTest {

    @Test
    void requestFactoryStoresRequest() {
        NetworkRequest request = NetworkRequest.of("GET", "https://app.example.com/api/orders");

        NetworkEvent event = NetworkEvent.request(request);

        assertNotNull(event.id());
        assertEquals(NetworkEventType.REQUEST, event.type());
        assertEquals("https://app.example.com/api/orders", event.url());
    }

    @Test
    void responseFactoryStoresStatus() {
        NetworkEvent event = NetworkEvent.response(NetworkResponse.of("1", "https://app.example.com/api/orders", 500));

        assertEquals(NetworkEventType.RESPONSE, event.type());
        assertEquals(500, event.response().status());
    }
}

