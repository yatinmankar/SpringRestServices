package com.example.serviceb.security;

import com.example.serviceb.config.ServiceAProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Demo-only service-to-service auth: a static shared secret checked against
 * the X-Service-Key header. Service B is a different trust domain from
 * Service 1's clients, so it deliberately doesn't reuse their JWT flow.
 * Use mTLS or a real service-to-service token exchange in production.
 */
@Component
@RequiredArgsConstructor
@Order(0)
public class ServiceKeyFilter extends OncePerRequestFilter {

    private final ServiceAProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/process");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader("X-Service-Key");
        if (key == null || !key.equals(properties.getInboundServiceKey())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid service key\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
