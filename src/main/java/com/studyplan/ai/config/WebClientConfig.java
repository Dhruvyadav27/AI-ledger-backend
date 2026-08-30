package com.studyplan.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient bean pointed at Gemini's base URL. GeminiService injects this
 * and appends the model-specific path + API key per request.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient geminiWebClient(@Value("${gemini.api.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}