package com.ramteja.gateway.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Issues short-lived test tokens for local development only. Off unless gateway.dev-tokens=true. */
@RestController
@ConditionalOnProperty(name = "gateway.dev-tokens", havingValue = "true")
public class DevTokenController {

    private final JwtEncoder encoder;

    public DevTokenController(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    @PostMapping("/auth/dev-token")
    public Map<String, String> token(@RequestParam String user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("api-gateway-dev")
                .subject(user)
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return Map.of("access_token", token, "token_type", "Bearer");
    }
}
