package app.lexo.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "lexo.rate-limit.replenish-rate=1", "lexo.rate-limit.burst-capacity=2", "lexo.rate-limit.requested-tokens=2"})
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
    @DisplayName("a preflight is never counted against the limit")
    void preflightIsNotLimited() {
        for (int i = 0; i < 6; i++) {
            assertThat(preflight("/api/clientes", "http://localhost:5180").statusCode()).isEqualTo(200);
        }
    }
}
