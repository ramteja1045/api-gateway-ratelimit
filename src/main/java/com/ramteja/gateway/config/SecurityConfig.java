package com.ramteja.gateway.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Validates HS256 JWTs at the edge so downstream services never see unauthenticated traffic.
 * In production you would point {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}
 * at an identity provider (Okta, Auth0, Cognito) and drop the shared secret.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final SecretKey key;

    public SecurityConfig(@Value("${gateway.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("gateway.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/health", "/auth/dev-token", "/fallback/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(o -> o.jwt(jwt -> { }))
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }
}
