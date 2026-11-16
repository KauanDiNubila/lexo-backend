package app.lexo.config;

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer.Target;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.Map;

@Configuration
class EmailRetryConfig {

    @Bean
    RabbitRetryTemplateCustomizer retryOnlyServerFailures(RabbitProperties rabbit) {
        int maxAttempts = rabbit.getListener().getSimple().getRetry().getMaxAttempts();
        return (target, template) -> {
            if (target == Target.LISTENER) {
                template.setRetryPolicy(new SimpleRetryPolicy(maxAttempts, Map.of(MailSendException.class, true), true, false));
            }
        };
    }
}
