# API Gateway: JWT Auth, Redis Rate Limiting, Circuit Breakers

A reactive edge gateway built with **Java 21, Spring Boot 3, Spring Cloud Gateway, Spring
Security (OAuth2 resource server), Redis and Resilience4j**. It sits in front of backend services
(for example [ledger-service](https://github.com/ramteja1045/ledger-service) and [order-events-outbox](https://github.com/ramteja1045/order-events-outbox))
and handles cross-cutting concerns once, at the edge.

## Request pipeline

```
client ──► RequestIdFilter ──► JWT validation ──► Redis token-bucket rate limiter
                                     │ 401              │ 429
                                     ▼                  ▼
                           route match (/api/orders/**, /api/accounts/**, ...)
                                     │
                                     ▼
                      Retry (GET, 502/503, backoff) ──► CircuitBreaker ──► downstream service
                                                             │ open / timeout / error
                                                             ▼
                                                     /fallback/{service} → 503 ProblemDetail
```

## Features

- **JWT authentication at the edge.** Spring Security reactive resource server. Requests without a
  valid token get `401` before they touch any backend.
- **Distributed rate limiting.** Redis token-bucket limiter shared by all gateway instances, keyed
  **per user** (JWT subject) with a per-IP fallback. Replenish rate and burst are configurable.
- **Circuit breakers + time limits.** Resilience4j opens the circuit when a service is failing and
  returns a clean `503` problem response instead of piling up slow requests.
- **Safe retries.** Only idempotent `GET` requests are retried, with exponential backoff.
- **Request tracing.** Every request gets an `X-Request-Id` (or keeps the caller's). It is forwarded
  downstream and logged with status and latency.
- **Integration tests** with a real Redis via Testcontainers: covers 401s, fallback responses and
  per-user rate limiting.

## Run locally

```bash
docker compose up -d                 # Redis
mvn spring-boot:run                  # gateway on :8080

TOKEN=$(curl -s -X POST 'localhost:8080/auth/dev-token?user=alice' | jq -r .access_token)
curl -i localhost:8080/api/orders/123 -H "Authorization: Bearer $TOKEN"
```

Set `ORDERS_URI` / `LEDGER_URI` to point at real services. Set `DEV_TOKENS=false` and a strong
`JWT_SECRET` (or an identity provider's `issuer-uri`) outside local development.

## Tests

```bash
mvn verify   # needs Docker running
```
