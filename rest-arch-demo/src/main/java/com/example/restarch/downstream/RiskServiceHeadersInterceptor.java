package com.example.restarch.downstream;

import com.example.restarch.config.RiskServiceProperties;
import com.example.restarch.correlation.CorrelationContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Applies to every Feign request: shared secret plus the correlation ID from the calling thread's MDC. */
@Component
@RequiredArgsConstructor
public class RiskServiceHeadersInterceptor implements RequestInterceptor {

    private final RiskServiceProperties properties;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Service-Key", properties.getOutboundServiceKey());
        String correlationId = CorrelationContext.get();
        if (correlationId != null) {
            template.header(CorrelationContext.HEADER_NAME, correlationId);
        }
    }
}
