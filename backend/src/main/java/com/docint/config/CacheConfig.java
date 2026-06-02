package com.docint.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caching layer. Provider is chosen by the property {@code app.cache.provider}:
 *
 *   app.cache.provider = caffeine  (default)  → in-memory, single JVM, zero infra. Local dev.
 *   app.cache.provider = redis                → distributed, shared across all app instances. Production / AWS.
 *
 * The @Cacheable / @CacheEvict annotations on the services are IDENTICAL for both —
 * only this config and one property change. (See RedisCacheConfig for the Redis bean.)
 *
 * Cache names & TTLs:
 *  - vectorSearch (60s) : retrieval results keyed by (query, category, topK). Saves the OpenAI
 *                         embedding call AND the pgvector query on repeated questions.
 *  - categories   (60s) : category list + counts.
 *  - stats        (60s) : admin dashboard stats.
 *  - chatAnswers  (10m) : full LLM answers keyed by (question, category). A repeated identical
 *                         question returns instantly WITHOUT calling Claude. Evicted whenever
 *                         documents change (upload of new content / delete).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String VECTOR_SEARCH = "vectorSearch";
    public static final String CATEGORIES = "categories";
    public static final String STATS = "stats";
    public static final String CHAT_ANSWERS = "chatAnswers";

    /** Default cache manager: Caffeine (in-memory). Active unless app.cache.provider=redis. */
    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "provider", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        // Per-cache TTLs (Caffeine needs each cache registered with its own spec)
        manager.registerCustomCache(VECTOR_SEARCH,
                Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(1_000).build());
        manager.registerCustomCache(CATEGORIES,
                Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(50).build());
        manager.registerCustomCache(STATS,
                Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(10).build());
        manager.registerCustomCache(CHAT_ANSWERS,
                Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(500).build());
        return manager;
    }
}
