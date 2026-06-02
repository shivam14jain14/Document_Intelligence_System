package com.docint.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Increases HTTP timeouts for outbound calls (Anthropic / OpenAI).
 * Agentic RAG requests involve multiple round-trips (tool calls), so the
 * default ~10s read timeout is too aggressive and causes SocketTimeoutException.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder.requestFactory(
                ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(Duration.ofSeconds(30))
                                .withReadTimeout(Duration.ofSeconds(180))));
    }
}
