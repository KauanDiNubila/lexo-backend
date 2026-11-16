package app.lexo.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DisplayName("Gateway with Redis unreachable")
class GatewayRedisDownBehaviorTest extends GatewayHttpTestBase {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        configure(registry, "127.0.0.1", 1);
    }

    @Test
    @DisplayName("the request goes through when the session cutoff and the rate limit cannot be checked")
    void failsOpen() {
        web.get().uri("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken("u-1"))
                .exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("a request without a token is still refused")
    void stillRefusesWithoutToken() {
        web.get().uri("/api/clientes").exchange().expectStatus().isUnauthorized();
    }
}
