package app.lexo.common.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * So se ativa em servicos que tem spring-kafka no classpath (via ConditionalOnClass) —
 * auth-service e ia-service dependem de lexo-common mas nao usam Kafka, entao ficam
 * de fora sem quebrar.
 */
@Configuration
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaErrorHandlingConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaOperations);

        var backOff = new FixedBackOff(1000L, 2L);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("[kafka] tentativa {} falhou para o topico {} — {}",
                        deliveryAttempt, record.topic(), ex.toString()));
        return handler;
    }
}
