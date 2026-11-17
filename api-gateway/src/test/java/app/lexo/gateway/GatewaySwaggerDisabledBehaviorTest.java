package app.lexo.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@DisplayName("Gateway with the API docs turned off, as in production")
class GatewaySwaggerDisabledBehaviorTest extends GatewayHttpTestBase {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        configure(registry, "127.0.0.1", 1);
    }

    @Test
    @DisplayName("the Swagger UI and the aggregated spec are not served")
    void swaggerIsNotServed() {
        web.get().uri("/swagger-ui.html").exchange().expectStatus().isNotFound();
        web.get().uri("/swagger-ui/index.html").exchange().expectStatus().isNotFound();
        web.get().uri("/v3/api-docs").exchange().expectStatus().isNotFound();
    }
}
