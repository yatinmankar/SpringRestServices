package com.example.restarch.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless bearer-token auth. Runs before Spring Security's own auth filter
 * so downstream filters (rate limiting) and controllers can rely on
 * SecurityContextHolder having the authenticated clientId.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth") || path.equals("/api/v1/orders/callback")
                || path.equals("/api/v1/orders/risk-result")
                || path.startsWith("/actuator/health") || path.startsWith("/h2-console")
                || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            reject(response, "missing or malformed Authorization header");
            return;
        }

        try {
            String token = header.substring(7);
            String clientId = jwtService.extractClientId(token);
            var auth = new UsernamePasswordAuthenticationToken(clientId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            reject(response, "invalid or expired token");
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
