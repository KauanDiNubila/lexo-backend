package app.lexo.config;

import app.lexo.application.AiSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationConfig {

    @Bean
    AiSettings aiSettings(@Value("${lexo.ia.limite-por-minuto}") int requestsPerMinute) {
        return new AiSettings(requestsPerMinute);
    }
}
