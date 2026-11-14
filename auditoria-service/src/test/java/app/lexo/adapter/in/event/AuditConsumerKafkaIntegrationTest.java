package app.lexo.adapter.in.event;

import app.lexo.IntegrationTestBase;
import app.lexo.common.event.DomainEvent;
import app.lexo.common.kafka.EventPublisher;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "spring.kafka.listener.auto-startup=true")
@DisplayName("Audit consumer with a real Kafka broker")
class AuditConsumerKafkaIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "lexo.eventos";
    private static final String DEAD_LETTER_TOPIC = "lexo.eventos-dlt";
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-12T10:15:30.123456Z");

    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.1");

    static {
        KAFKA.start();
        try (AdminClient admin = AdminClient.create(Map.<String, Object>of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1), new NewTopic(DEAD_LETTER_TOPIC, 1, (short) 1)))
                    .all().get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void kafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    private EventPublisher publisher;

    private DomainEvent event(String orgId, String userName) {
        return new DomainEvent("CLIENTE_CRIADO", orgId, "CLIENTE", "c-42", "Cliente criado: Maria Souza", "u-7",
                userName, OCCURRED_AT);
    }

    private KafkaConsumer<String, String> deadLetterConsumer() {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-reader-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        consumer.subscribe(List.of(DEAD_LETTER_TOPIC));
        return consumer;
    }

    @Test
    @DisplayName("an event published to the topic becomes an entry dated when the event happened")
    void consumesAnEvent() {
        String org = UUID.randomUUID().toString();

        publisher.publish(event(org, "Ana Advogada"));

        await().atMost(Duration.ofSeconds(30)).until(() -> !entriesOf(org).isEmpty());
        Map<String, Object> row = entriesOf(org).get(0);
        assertThat(row.get("action")).isEqualTo("CLIENTE_CRIADO");
        assertThat(row.get("user_name")).isEqualTo("Ana Advogada");
        assertThat(((Timestamp) row.get("created_at")).toInstant()).isEqualTo(OCCURRED_AT);
    }

    @Test
    @DisplayName("an event that cannot be stored goes to the dead-letter topic and the consumer keeps going")
    void deadLettersAnEventThatCannotBeStored() {
        String badOrg = UUID.randomUUID().toString();
        String goodOrg = UUID.randomUUID().toString();

        try (KafkaConsumer<String, String> reader = deadLetterConsumer()) {
            publisher.publish(event(badOrg, null));
            publisher.publish(event(goodOrg, "Ana Advogada"));

            await().atMost(Duration.ofSeconds(60)).until(() -> !entriesOf(goodOrg).isEmpty());
            ConsumerRecord<String, String> dead = null;
            long deadline = System.currentTimeMillis() + 60_000;
            while (dead == null && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : reader.poll(Duration.ofSeconds(1))) {
                    if (badOrg.equals(record.key())) {
                        dead = record;
                    }
                }
            }
            assertThat(dead).isNotNull();
            assertThat(dead.value()).contains(badOrg);
        }
        assertThat(entriesOf(badOrg)).isEmpty();
    }
}
