package com.example.restarch.correlation;

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
 * Runs first in the chain (before auth/rate-limit) so every log line for this
 * request - including auth failures and rate-limit rejections - carries a
 * correlation ID. If the caller already sent one (e.g. an upstream gateway,
 * or a retried request), it's honored rather than replaced, so retries of
 * the same logical request keep the same trace.
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
