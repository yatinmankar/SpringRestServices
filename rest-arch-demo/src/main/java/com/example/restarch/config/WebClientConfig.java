package com.example.restarch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final ServiceBProperties properties;

    @Bean
    public WebClient serviceBWebClient(WebClient.Builder builder) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
