package app.lexo.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "lexo.rate-limit.trusted-proxies=1")
@DisplayName("Gateway client address behind the proxy")
class GatewayClientAddressBehaviorTest extends GatewayHttpTestBase {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        configure(registry, "127.0.0.1", 1);
    }

    @Test
    @DisplayName("the service receives the address the proxy appended, not what the client invented")
    void forwardsTheProxyAddress() {
        web.post().uri("/api/auth/login")
                .header("X-Forwarded-For", "10.9.9.9, 198.51.100.4")
                .exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-Client-Address")).isEqualTo("198.51.100.4");
    }

    @Test
    @DisplayName("an address sent by the client in X-Client-Address is replaced")
    void replacesAForgedHeader() {
        web.post().uri("/api/auth/login")
                .header("X-Forwarded-For", "198.51.100.4")
                .header("X-Client-Address", "6.6.6.6")
                .exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-Client-Address")).isEqualTo("198.51.100.4");
    }

    @Test
    @DisplayName("without X-Forwarded-For nothing the client sent becomes the client address")
    void ignoresAForgedHeaderWithoutForwarding() {
        web.post().uri("/api/auth/login").header("X-Client-Address", "6.6.6.6").exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-Client-Address")).isNotEqualTo("6.6.6.6");
    }

    @Test
    @DisplayName("an authenticated request carries the address too")
    void authenticatedRequestsCarryTheAddress() {
        web.get().uri("/api/clientes").header("Authorization", "Bearer " + validToken("u-1"))
                .header("X-Forwarded-For", "203.0.113.20")
                .exchange().expectStatus().isOk();

        assertThat(DOWNSTREAM.header("X-Client-Address")).isEqualTo("203.0.113.20");
    }
}
