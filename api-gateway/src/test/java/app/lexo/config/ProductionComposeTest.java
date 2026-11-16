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
    @DisplayName("the proxy overwrites X-Forwarded-For with the address of the connection")
    void caddyOverwritesTheHeader() throws IOException {
        String caddyfile = Files.readString(Path.of("..", "Caddyfile"));

        assertThat(caddyfile).contains("header_up X-Forwarded-For {remote_host}");
    }
}
