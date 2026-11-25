package com.ramteja.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
public class RateLimitConfig {

    /**
     * Rate-limits per authenticated user (JWT subject), falling back to client IP.
     * Per-user limits are fairer than per-IP when many users share a NAT or office network.
     */
    @Bean
    @Primary
    KeyResolver userKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth != null && auth.isAuthenticated())
                .map(auth -> "user:" + auth.getName())
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(InetSocketAddress::getHostString)
                        .orElse("unknown")));
    }
}
