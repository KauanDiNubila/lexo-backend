package app.lexo.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Anexa a chave interna em toda chamada Feign, autenticando as requisicoes
 * servico-a-servico aos endpoints /internal/** dos outros servicos.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalKeyInterceptor(@Value("${lexo.internal.api-key}") String key) {
        return template -> template.header("X-Internal-Key", key);
    }
}
