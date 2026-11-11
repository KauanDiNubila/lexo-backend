package app.lexo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
