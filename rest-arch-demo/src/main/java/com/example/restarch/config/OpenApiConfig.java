package com.example.restarch.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Two distinct trust domains show up as two schemes:
 * bearerAuth (client JWT from /api/v1/auth/token) is applied globally, while
 * serviceKey (the shared secret Service B presents on its callback) is opt-in
 * per operation via @SecurityRequirement.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";
    public static final String SERVICE_KEY_SCHEME = "serviceKey";

    @Bean
    public OpenAPI restArchOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("rest-arch-demo API")
                        .version("1.0.0")
                        .description("""
                                REST request/response architecture demo: JWT auth, bean validation,
                                per-client rate limiting, idempotent order submission, resilient
                                downstream calls to Service B, caching and async batched persistence.

                                To try the secured endpoints: call POST /api/v1/auth/token with a
                                clientId, then paste the returned token into Authorize."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token issued by POST /api/v1/auth/token"))
                        .addSecuritySchemes(SERVICE_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Service-Key")
                                .description("Shared secret presented by Service B / the risk service on their callbacks")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
