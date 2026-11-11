package app.lexo.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalKeyInterceptor(@Value("${lexo.internal.api-key}") String key) {
        return template -> template.header("X-Internal-Key", key);
    }
}
