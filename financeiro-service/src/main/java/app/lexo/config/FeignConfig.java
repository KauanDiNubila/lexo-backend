package app.lexo.config;

import app.lexo.common.security.InternalKeyFilter;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalKeyInterceptor(@Value("${lexo.internal.api-key}") String key) {
        return template -> template.header(InternalKeyFilter.H_INTERNAL_KEY, key);
    }
}
