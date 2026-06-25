package app.lexo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;

/**
 * Habilita o cache com Redis como backend. As entradas expiram pelo TTL configurado
 * (lexo.cache.ttl-minutes) e valores nulos nao sao cacheados. A serializacao de valores
 * usa o padrao (JDK) — por isso os DTOs cacheados implementam Serializable.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration cacheConfiguration(
            @Value("${lexo.cache.ttl-minutes}") long ttlMinutes) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMinutes))
                .disableCachingNullValues();
    }
}
