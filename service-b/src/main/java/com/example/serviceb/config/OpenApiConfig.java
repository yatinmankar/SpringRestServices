package com.example.serviceb.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Service B only ever talks to Service 1, so the single scheme is the shared
 * secret ServiceKeyFilter checks on the X-Service-Key header.
 */
@Configuration
public class OpenApiConfig {

    public static final String SERVICE_KEY_SCHEME = "serviceKey";

    @Bean
    public OpenAPI serviceBOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("service-b API")
                        .version("1.0.0")
                        .description("""
                                Downstream async processor. Accepts work from Service 1, processes it
                                off the request thread, and calls back to Service 1 with the result -
                                all carrying the caller's correlation ID."""))
                .components(new Components()
                        .addSecuritySchemes(SERVICE_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Service-Key")
                                .description("Shared secret agreed with Service 1")))
                .addSecurityItem(new SecurityRequirement().addList(SERVICE_KEY_SCHEME));
    }
}
