package com.example.serviceb.correlation;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Service 1 always sends X-Correlation-Id, but this still generates one as a
 * fallback so Service B's own logs are traceable even if called directly
 * (e.g. during local testing). Honors an inbound value rather than replacing
 * it, so the ID stays the same across both services for a given request.
 */
@Component
@Order(Integer.MIN_VALUE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(CorrelationContext.HEADER_NAME);
        String correlationId = StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString();

        CorrelationContext.set(correlationId);
        response.setHeader(CorrelationContext.HEADER_NAME, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            CorrelationContext.clear();
        }
    }
}
