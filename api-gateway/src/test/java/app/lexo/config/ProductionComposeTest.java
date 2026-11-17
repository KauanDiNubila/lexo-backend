package app.lexo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Production compose for the gateway")
class ProductionComposeTest {

    private static final Pattern SERVICE_HEADER = Pattern.compile("^ {2}[a-z][a-z0-9-]*:\\s*$", Pattern.MULTILINE);

    private static String serviceBlock(String compose, String service) {
        Matcher start = Pattern.compile("^ {2}" + service + ":\\s*$", Pattern.MULTILINE).matcher(compose);
        assertThat(start.find()).as("service %s in the compose file", service).isTrue();
        Matcher next = SERVICE_HEADER.matcher(compose);
        int end = next.find(start.end()) ? next.start() : compose.length();
        return compose.substring(start.start(), end);
    }

    @Test
    @DisplayName("trusts exactly one proxy, the Caddy in front of it, so each client has its own rate limit")
    void trustsTheCaddy() throws IOException {
        String compose = Files.readString(Path.of("..", "docker-compose.prod.yml"));

        assertThat(serviceBlock(compose, "api-gateway")).contains("LEXO_RATE_LIMIT_TRUSTED_PROXIES: \"1\"");
    }

    @Test
    @DisplayName("trusts no proxy unless the deployment says so")
    void trustsNoProxyByDefault() throws IOException {
        String properties = Files.readString(Path.of("src", "main", "resources", "application.properties"));

        assertThat(properties).contains("lexo.rate-limit.trusted-proxies=${LEXO_RATE_LIMIT_TRUSTED_PROXIES:0}");
    }

    @Test
    @DisplayName("does not publish the gateway port, so only the proxy can reach it")
    void doesNotPublishThePort() throws IOException {
        String compose = Files.readString(Path.of("..", "docker-compose.prod.yml"));

        assertThat(serviceBlock(compose, "api-gateway")).doesNotContain("ports:");
    }

    @Test
    @DisplayName("turns the API docs and the Swagger UI off in the environment shared by every service")
    void swaggerIsOffInProduction() throws IOException {
        String compose = Files.readString(Path.of("..", "docker-compose.prod.yml"));
        String sharedEnvironment = compose.substring(compose.indexOf("x-infra-env:"), compose.indexOf("x-db-env:"));

        assertThat(sharedEnvironment).contains("SPRINGDOC_API_DOCS_ENABLED: \"false\"")
                .contains("SPRINGDOC_SWAGGER_UI_ENABLED: \"false\"");
    }

    @Test
    @DisplayName("every service that serves API docs takes that shared environment")
    void everyDocumentedServiceUsesTheSharedEnvironment() throws IOException {
        String compose = Files.readString(Path.of("..", "docker-compose.prod.yml"));

        for (String service : new String[] {"api-gateway", "auth-service", "cliente-service", "processo-service",
                "financeiro-service", "auditoria-service", "ia-service"}) {
            assertThat(serviceBlock(compose, service)).as("%s environment", service).contains("<<: *infra-env");
        }
    }

    @Test
    @DisplayName("gives every service the same RabbitMQ user and password the broker is created with")
    void servicesUseTheBrokerCredentials() throws IOException {
        String compose = Files.readString(Path.of("..", "docker-compose.prod.yml"));
        String sharedEnvironment = compose.substring(compose.indexOf("x-infra-env:"), compose.indexOf("x-db-env:"));

        assertThat(serviceBlock(compose, "rabbitmq"))
                .contains("RABBITMQ_DEFAULT_USER: ${RABBIT_USER:?defina RABBIT_USER no .env.prod}")
                .contains("RABBITMQ_DEFAULT_PASS: ${RABBIT_PASSWORD:?defina RABBIT_PASSWORD no .env.prod}");
        assertThat(sharedEnvironment)
                .contains("LEXO_RABBIT_USER: ${RABBIT_USER:?defina RABBIT_USER no .env.prod}")
                .contains("LEXO_RABBIT_PASS: ${RABBIT_PASSWORD:?defina RABBIT_PASSWORD no .env.prod}");
    }

    @Test
    @DisplayName("every service that talks to RabbitMQ takes that shared environment")
    void everyRabbitServiceUsesTheSharedEnvironment() throws IOException {
        String compose = Files.readString(Path.of("..", "docker-compose.prod.yml"));

        for (String service : new String[] {"auth-service", "processo-service", "notificacao-service"}) {
            assertThat(serviceBlock(compose, service)).as("%s environment", service).contains("<<: *infra-env");
        }
    }

    @Test
    @DisplayName("the proxy overwrites X-Forwarded-For with the address of the connection")
    void caddyOverwritesTheHeader() throws IOException {
        String caddyfile = Files.readString(Path.of("..", "Caddyfile"));

        assertThat(caddyfile).contains("header_up X-Forwarded-For {remote_host}");
    }
}
