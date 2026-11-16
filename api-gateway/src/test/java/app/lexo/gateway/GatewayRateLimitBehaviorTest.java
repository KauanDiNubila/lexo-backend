package app.lexo.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "lexo.rate-limit.replenish-rate=1", "lexo.rate-limit.burst-capacity=2", "lexo.rate-limit.requested-tokens=2", "lexo.rate-limit.trusted-proxies=1"})
@DisplayName("Gateway rate limit over HTTP")
class GatewayRateLimitBehaviorTest extends GatewayHttpTestBase {

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        configure(registry, REDIS.getHost(), REDIS.getMappedPort(6379));
    }

    @Test
    @DisplayName("requests above the burst from the same client get 429 and never reach the service")
    void limitsTheClient() {
        int before = DOWNSTREAM.hits();

        web.get().uri("/api/portal/x").exchange().expectStatus().isOk();
        web.get().uri("/api/portal/x").exchange().expectStatus().isEqualTo(429);

        assertThat(DOWNSTREAM.hits()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("clients behind the proxy are limited one by one, by the address the proxy saw")
    void limitsEachClientBehindTheProxy() {
        web.get().uri("/api/portal/x").header("X-Forwarded-For", "198.51.100.1").exchange().expectStatus().isOk();
        web.get().uri("/api/portal/x").header("X-Forwarded-For", "198.51.100.1").exchange()
                .expectStatus().isEqualTo(429);

        web.get().uri("/api/portal/x").header("X-Forwarded-For", "198.51.100.2").exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("an address the client invents in front of the proxy's own entry does not buy a new limit")
    void inventedAddressesDoNotHelp() {
        web.get().uri("/api/portal/x").header("X-Forwarded-For", "203.0.113.7").exchange().expectStatus().isOk();

        web.get().uri("/api/portal/x").header("X-Forwarded-For", "10.9.9.9, 203.0.113.7").exchange()
                .expectStatus().isEqualTo(429);
        web.get().uri("/api/portal/x").header("X-Forwarded-For", "10.8.8.8, 203.0.113.7").exchange()
                .expectStatus().isEqualTo(429);
    }

    @Test
    @DisplayName("a preflight is never counted against the limit")
    void preflightIsNotLimited() {
        for (int i = 0; i < 6; i++) {
            assertThat(preflight("/api/clientes", "http://localhost:5180").statusCode()).isEqualTo(200);
        }
    }
}
