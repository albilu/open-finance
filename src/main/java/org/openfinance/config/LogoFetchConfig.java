package org.openfinance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration for logo fetching infrastructure.
 *
 * <p>Registers a dedicated {@link RestTemplate} bean with timeouts configured from {@link
 * LogoFetchProperties}. Isolated from any other RestTemplate beans to avoid cross-contamination of
 * timeout settings.
 */
@Configuration
public class LogoFetchConfig {

    /**
     * RestTemplate used exclusively by {@link org.openfinance.service.LogoFetchService}.
     *
     * @param properties logo-fetch configuration
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate logoFetchRestTemplate(LogoFetchProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = properties.getTimeoutSeconds() * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
