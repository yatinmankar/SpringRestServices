# risk-service

Risk evaluator for `rest-arch-demo`. Port **8082**.

```
rest-arch-demo ──POST /api/v1/risk/check (Feign, fire-and-forget)──▶ risk-service
                                                                       │ 202, evaluate async
rest-arch-demo ◀──POST /api/v1/orders/risk-result (Feign, retry + CB)──┘
```

- **Inbound** `POST /api/v1/risk/check` - returns `202` immediately, no body. Needs `X-Service-Key`.
- **Rule** - `quantity * price` above `risk.notional-limit` (default 1,000,000) is `REJECTED`; otherwise `APPROVED`.
  Score is notional as a percentage of the limit, capped at 100.
- **Outbound** `OrderServiceClient` (Feign) posts the verdict to the order service. `OrderResultPublisher`
  wraps it with Resilience4j **Retry** (4 attempts, 200ms exponential backoff, only I/O errors and 5xx)
  around a **CircuitBreaker** (10-call window, opens at 50% failures after 5 calls, 10s open).
  Both are `orderCallback` in `application.yml`; state is visible at `/actuator/circuitbreakers`.

Run: `mvn -f risk-service/pom.xml spring-boot:run`, with `rest-arch-demo` on 8080.

**Known gap:** a verdict whose retries are exhausted, or that hits an open circuit, is logged and dropped.
Persist it to an outbox/DLQ before relying on this.
