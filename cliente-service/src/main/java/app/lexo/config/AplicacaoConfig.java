package app.lexo.config;

import app.lexo.application.ConfiguracaoDoPortal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class AplicacaoConfig {

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }

    @Bean
    ConfiguracaoDoPortal configuracaoDoPortal(@Value("${lexo.portal.token-validade-dias}") long validadeEmDias) {
        return new ConfiguracaoDoPortal(validadeEmDias);
    }
}
