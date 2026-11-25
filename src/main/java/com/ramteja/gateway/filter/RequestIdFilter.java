package com.ramteja.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adds an X-Request-Id to every request (or keeps the caller's), passes it downstream for tracing,
 * echoes it on the response and logs method, path, status and latency.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String requestId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
        long start = System.nanoTime();

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HEADER, requestId)))
                .build();
        mutated.getResponse().getHeaders().set(HEADER, requestId);

        return chain.filter(mutated).doFinally(signal -> log.info("{} {} {} -> {} in {} ms",
                requestId,
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                mutated.getResponse().getStatusCode(),
                (System.nanoTime() - start) / 1_000_000));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
