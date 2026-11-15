package app.lexo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kafka configuration of the service")
class KafkaConfigurationTest {

    private static Properties production() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(in);
        }
        return properties;
    }

    @Test
    @DisplayName("a message that is not JSON cannot stop the consumer: the JSON reader is wrapped in ErrorHandlingDeserializer")
    void consumerSurvivesMalformedMessages() throws IOException {
        Properties properties = production();

        assertThat(properties.getProperty("spring.kafka.consumer.value-deserializer"))
                .isEqualTo(ErrorHandlingDeserializer.class.getName());
        assertThat(properties.getProperty("spring.kafka.consumer.properties.spring.deserializer.value.delegate.class"))
                .isEqualTo(JsonDeserializer.class.getName());
    }

    @Test
    @DisplayName("the producer writes events as JSON, so the dead-letter topic can receive them")
    void producerWritesJson() throws IOException {
        assertThat(production().getProperty("spring.kafka.producer.value-serializer"))
                .isEqualTo(JsonSerializer.class.getName());
    }
}
