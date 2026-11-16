package app.lexo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.ListenerRetry;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Production retry policy for e-mail delivery")
class RetryConfigurationTest {

    @Test
    @DisplayName("tries six times with a growing wait, about a minute in all")
    void backsOffForAboutAMinute() throws IOException {
        var production = new MapConfigurationPropertySource(
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties")));

        ListenerRetry retry = new Binder(production)
                .bind("spring.rabbitmq.listener.simple.retry", ListenerRetry.class).get();

        assertThat(retry.isEnabled()).isTrue();
        assertThat(retry.getMaxAttempts()).isEqualTo(6);
        assertThat(retry.getInitialInterval()).isEqualTo(Duration.ofSeconds(2));
        assertThat(retry.getMultiplier()).isEqualTo(2.0);
        assertThat(retry.getMaxInterval()).isEqualTo(Duration.ofSeconds(30));
    }
}
