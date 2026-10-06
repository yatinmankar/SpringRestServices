package com.example.serviceb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient serviceAWebClient(WebClient.Builder builder) {
        // No fixed base URL: ServiceAProperties.callbackUrl is a full URL.
        return builder.build();
    }
}
