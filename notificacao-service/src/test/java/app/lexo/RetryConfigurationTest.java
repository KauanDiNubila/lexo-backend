package app.lexo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Production retry policy for e-mail delivery")
class RetryConfigurationTest {

    private static Properties production() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(in);
        }
        return properties;
    }

    @Test
    @DisplayName("keeps trying for about a minute before giving up on a task")
    void backsOffForAboutAMinute() throws IOException {
        Properties p = production();
        String prefix = "spring.rabbitmq.listener.simple.retry.";

        int attempts = Integer.parseInt(p.getProperty(prefix + "max-attempts"));
        long first = Long.parseLong(p.getProperty(prefix + "initial-interval"));
        double multiplier = Double.parseDouble(p.getProperty(prefix + "multiplier"));
        long max = Long.parseLong(p.getProperty(prefix + "max-interval"));

        long waited = 0;
        long interval = first;
        for (int i = 1; i < attempts; i++) {
            waited += Math.min(interval, max);
            interval = (long) (interval * multiplier);
        }
        assertThat(waited).isBetween(45_000L, 90_000L);
    }
}
