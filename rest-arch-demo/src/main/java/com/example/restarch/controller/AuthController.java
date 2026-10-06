package com.example.restarch.controller;

import com.example.restarch.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo-only token issuance: trusts whatever clientId is supplied, no
 * credential check. Replace with real auth (OAuth2/OIDC, mTLS, or an
 * internal auth service) before this goes anywhere near production.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Demo token issuance - no credential check")
public class AuthController {

    private final JwtService jwtService;

    @Operation(
            summary = "Issue a JWT for a client",
            description = "Demo-only: any clientId is accepted. Paste the returned token into Authorize.")
    @ApiResponse(responseCode = "200", description = "Token issued")
    @SecurityRequirements  // open endpoint - overrides the global bearerAuth requirement
    @PostMapping("/api/v1/auth/token")
    public Map<String, String> issueToken(
            @Parameter(description = "Identity the token is minted for; also the rate-limit bucket key",
                    example = "demo-client")
            @RequestParam String clientId) {
        return Map.of("token", jwtService.generateToken(clientId));
    }
}
