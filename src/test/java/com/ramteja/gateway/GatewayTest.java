package com.ramteja.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "RATE_REPLENISH=1",
        "RATE_BURST=2",
        // Point routes at a closed port so every downstream call fails and trips the fallback.
        "ORDERS_URI=http://localhost:1",
        "LEDGER_URI=http://localhost:1"
})
@AutoConfigureWebTestClient
@Testcontainers
class GatewayTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebTestClient client;

    private String tokenFor(String user) {
        @SuppressWarnings("unchecked")
        Map<String, String> body = client.post().uri("/auth/dev-token?user=" + user)
                .exchange().expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();
        return body.get("access_token");
    }

    @Test
    void rejects_requests_without_a_token() {
        client.get().uri("/api/orders/123").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void returns_fallback_when_downstream_is_down() {
        client.get().uri("/api/orders/123")
                .headers(h -> h.setBearerAuth(tokenFor("fallback-user")))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().exists("X-Request-Id")
                .expectBody().jsonPath("$.service").isEqualTo("orders");
    }

    @Test
    void rate_limits_per_user_after_burst() {
        String token = tokenFor("burst-user");
        int tooMany = 0;
        for (int i = 0; i < 6; i++) {
            int status = client.get().uri("/api/accounts/abc")
                    .headers(h -> h.setBearerAuth(token))
                    .exchange().returnResult(String.class).getStatus().value();
            if (status == 429) {
                tooMany++;
            }
        }
        assertThat(tooMany).isPositive();

        // A different user has their own bucket and is not affected.
        client.get().uri("/api/accounts/abc")
                .headers(h -> h.setBearerAuth(tokenFor("other-user")))
                .exchange().expectStatus().isEqualTo(503);
    }
}
