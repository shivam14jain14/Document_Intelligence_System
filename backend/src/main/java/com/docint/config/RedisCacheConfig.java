package com.docint.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Distributed cache backed by Redis — used in production where multiple app
 * instances share one cache. Activated by {@code app.cache.provider=redis}.
 *
 * Connection settings come from Spring Boot's standard properties:
 *   spring.data.redis.host, spring.data.redis.port, spring.data.redis.password
 *
 * On AWS, point spring.data.redis.host at:
 *   - the Redis ECS service name (self-hosted container), OR
 *   - an EC2 instance running Redis, OR
 *   - an ElastiCache primary endpoint (managed).
 *
 * Spring Boot auto-configures the RedisConnectionFactory (Lettuce) from those
 * properties; we only customize the cache manager (TTL + JSON serialization).
 */
@Configuration
@ConditionalOnProperty(prefix = "app.cache", name = "provider", havingValue = "redis")
public class RedisCacheConfig {

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                // Chat answers live longer (content updates are infrequent)
                .withCacheConfiguration(CacheConfig.CHAT_ANSWERS, base.entryTtl(Duration.ofMinutes(10)))
                .build();
    }
}
