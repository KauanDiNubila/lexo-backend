package app.lexo.config;

import app.lexo.application.PortalSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PortalSettings portalSettings(@Value("${lexo.portal.token-validade-dias}") long validityDays) {
        return new PortalSettings(validityDays);
    }
}
