# rest-arch-demo (Service 1)

A REST request/response service demonstrating auth, validation, idempotency,
rate limiting, fault tolerance, caching, async batched persistence to H2, a
correlation ID that traces a request across services, and Hibernate-audited
order status transitions - built to be pushed to ~10k req/s with the
included k6 script.

This is Service 1. It hands work off asynchronously to a companion project,
**service-b** (a separate deployable, run alongside this one for the full
flow) - see "The full async flow" below.

## Requirements

- Java 21
- Maven 3.9+

## Run it

```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`. H2 console at `/h2-console`
(JDBC URL `jdbc:h2:mem:orders`, user `sa`, empty password).

## Try it manually

```bash
# 1. get a token (demo-only: no real credential check, see AuthController)
TOKEN=$(curl -s -X POST "http://localhost:8080/api/v1/auth/token?clientId=demo-client" | jq -r .token)

# 2. place an order
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"instrumentId":"AAPL","quantity":10,"price":189.50,"side":"BUY"}'

# 3. replay the same Idempotency-Key -> same response, 200 instead of 202
```

Check `GET /actuator/health` and `/actuator/metrics` for liveness/metrics.
Rows land in the `orders` table a few tens of ms later (async batch insert) -
query it from the H2 console to confirm. Every response carries the request's
`correlationId` in the JSON body and as an `X-Correlation-Id` response
header; pass that header on a request yourself to pin a specific ID
(useful for tracing a retry against the same logical request).

## The full async flow (run service-b too)

Service 1 alone will accept and persist orders, but the order will sit in
`CREATED` forever unless **service-b** is also running, since that's what
moves it through `SENT_TO_SERVICE_B -> COMPLETED/FAILED`:

```bash
# terminal 1
cd rest-arch-demo && mvn spring-boot:run          # :8080

# terminal 2
cd service-b && mvn spring-boot:run               # :8081
```

What happens end to end for one order:

1. Client `POST /api/v1/orders` with an `Idempotency-Key`. `CorrelationIdFilter`
   assigns a correlation ID (or honors one the caller supplied) before auth
   even runs, so it's on every log line for this request, success or failure.
2. Order is validated, idempotency-reserved, risk-checked, and queued -
   client gets `202` back immediately with `correlationId` in the body.
   *This response reflects the synchronous risk-check decision only* - the
   order's actual lifecycle keeps moving after the response is sent.
3. `OrderPersistenceQueue`'s batch writer commits the row as `CREATED`
   through Hibernate (not raw JDBC), so Envers records the creation as
   revision 1 in `orders_AUD`.
4. Once committed, `ServiceBClient` posts the order to service-b's
   `/api/v1/process`, forwarding `X-Correlation-Id`. On a successful
   dispatch the order flips to `SENT_TO_SERVICE_B` (revision 2).
5. service-b does its (simulated) work on its own thread and calls back to
   `POST /api/v1/orders/callback` on Service 1 with `{correlationId, status,
   message}`, authenticated by a shared `X-Service-Key` rather than the
   client JWT flow (Service B is a different trust domain).
6. `OrderService.applyServiceBCallback` looks the order up by
   `correlationId`, checks it's actually in `SENT_TO_SERVICE_B` (ignores
   stray/duplicate callbacks otherwise), and flips it to `COMPLETED` or
   `FAILED` (revision 3) - Envers now has the full history of that row.

Query the audit trail directly in the H2 console:

```sql
SELECT id, correlation_id, status, updated_at FROM orders WHERE correlation_id = '...';
SELECT * FROM orders_aud WHERE id = '...' ORDER BY rev;   -- every transition, in order
SELECT * FROM revinfo;                                     -- revision timestamps
```

## Load test: push it to 10k req/s

```bash
cd load-test
./run-load-test.sh                       # defaults: localhost:8080, 50 synthetic clients
BASE_URL=http://host:8080 NUM_CLIENTS=100 ./run-load-test.sh
```

Requires [k6](https://k6.io/docs/get-started/installation/). The script ramps
500 -> 2000 -> 5000 -> 10000 req/s and spreads traffic across many synthetic
client IDs so it stresses the actual pipeline rather than one client's rate
limit bucket. A `wrk` alternative is in `load-test/wrk-post.lua` if you can't
install k6.

**To actually reach 10k rps you generally need more than the app change:**
- Run k6 from a separate, reasonably powerful machine (or `k6 run
  --out ...` distributed across a few machines) - a single laptop's own
  network stack/CPU can bottleneck before the server does.
- Raise file descriptor and ephemeral port limits on both client and server:
  `ulimit -n 65535`, and on Linux widen `net.ipv4.ip_local_port_range` /
  enable `net.ipv4.tcp_tw_reuse` if you're saturating outbound ports.
- Give the JVM enough heap/CPU for the target concurrency (`-Xmx2g` or more,
  a few cores minimum).
- H2 in-memory is fast for a demo but is a single embedded instance with no
  network hop - it will outperform a real network-attached DB. Treat the
  numbers here as validating the *application's* concurrency model, not as a
  production capacity plan.

## What's actually happening on the request path

0. **Correlation ID** - `CorrelationIdFilter` runs before everything else
   (including auth), assigns/honors an `X-Correlation-Id`, and puts it on
   the SLF4J MDC (`logging.pattern.console` includes `%X{correlationId}`),
   so it's on every log line for the request regardless of what happens
   later. It's also stamped on the `orders` row and forwarded to Service B.
1. **Auth** - `JwtAuthFilter` validates a Bearer JWT and puts the client ID in
   the `SecurityContext`. Stateless, so any instance can serve any request.
2. **Rate limiting** - `RateLimitFilter` + `RateLimiterRegistry` enforce a
   per-client token bucket (Bucket4j). Returns `429` with `Retry-After` when
   exceeded.
3. **Validation** - Bean Validation (`@Valid` on `OrderRequest`); failures
   become a `400` with per-field messages via `GlobalExceptionHandler`.
4. **Idempotency** - `OrderService` checks a Caffeine cache first, then tries
   to insert a `PROCESSING` row keyed by `Idempotency-Key` in H2. The unique
   primary key is what makes this safe under concurrent duplicates and across
   multiple app instances (a second insert throws
   `DataIntegrityViolationException`, and that caller polls for the winner's
   result instead of reprocessing).
5. **Risk check (async, fire-and-forget)** - once a batch is committed,
   `RiskServiceClient` POSTs each order to `risk-service` (port 8082) through
   a Feign client (`RiskServiceFeignClient`) on a virtual thread; the request
   path never waits for it. `risk-service` replies `202`, evaluates, and
   POSTs the verdict back to `POST /api/v1/orders/risk-result`
   (`RiskResultController`, shared `X-Service-Key`), which sets
   `riskStatus`/`riskScore`/`riskMessage` on the order. The retry + circuit
   breaker live on that callback, inside `risk-service`
   (`OrderResultPublisher`). If the submit itself fails the order is marked
   `riskStatus=UNAVAILABLE`. Risk does **not** gate the Service B dispatch.
6. **Request saving** - `OrderPersistenceQueue` takes the built `Order`
   (JPA entity, status `CREATED`), enqueues it, and returns immediately.
   Virtual-thread workers drain the queue and write in batches through
   Hibernate (`OrderBatchWriter`: `persist()` per row, one `flush()`/`clear()`
   per batch) - so it's still JDBC-batched under the hood, but now goes
   through the persistence context so Envers can audit it. If the queue is
   full, the request gets a `503` (backpressure) instead of the queue
   growing without bound.
7. **Response** - `202 Accepted` for a new order, `200 OK` with `replay:true`
   for a duplicate `Idempotency-Key`. The order's *persisted* status keeps
   evolving after this response is sent - see "The full async flow" above.
8. **Status tracking + auditing** - `Order` is `@Audited` (Hibernate Envers):
   every insert/update writes a revision to `orders_AUD` + `REVINFO`, so the
   full `CREATED -> SENT_TO_SERVICE_B -> COMPLETED/FAILED` history is queryable
   per order without any hand-rolled audit logging. `@CreatedDate`/
   `@LastModifiedDate` (Spring Data JPA auditing) also stamp the row itself.

## Scaling this beyond one instance

Everything above is written to be correct on a single instance and to *fail
safely* rather than silently break in a cluster, but two pieces are
single-node by construction and need swapping out for real horizontal scale:

- **Rate limiting**: `RateLimiterRegistry` is an in-memory `ConcurrentHashMap`.
  With N instances behind a load balancer, a client could get N x the
  configured limit. Swap to `bucket4j-redis` (or Hazelcast) so the bucket is
  shared cluster-wide.
- **Idempotency cache**: the Caffeine cache is a fast local hint only - the
  actual correctness guarantee is the DB unique constraint, which already
  works across instances. No change needed there, but consider Redis for the
  cache layer too if DB round-trips under load become the bottleneck.

At genuinely high sustained throughput (well beyond what a single DB can
absorb even with batching), the usual next step is to decouple ingestion from
storage entirely: accept -> publish to a durable log (Kafka/Kinesis) ->
consumers batch-write downstream. That removes the DB's write throughput as a
ceiling on the API's accept rate, at the cost of eventual (not immediate)
durability of the "saved" acknowledgment - worth thinking through carefully
for an order-processing use case specifically, since "accepted" vs "durably
recorded" may need to mean different things to callers.

## Where to look if you extend this

- `application.yml` - Tomcat thread pool, HikariCP pool size, resilience4j
  instances, rate limit, async-queue, and `serviceb.*` tuning are all here.
- `OrderPersistenceQueue` - batch size and flush interval directly trade off
  write latency vs. throughput; tune alongside your load test results.
- `SecurityConfig` - swap `AuthController`'s trust-anything token issuance for
  real credential verification before this goes near a real environment; also
  where the callback endpoint's shared-secret exemption from the JWT flow is
  wired up.
- `ServiceBClient` - deliberately lighter-weight fault tolerance than the risk
  check (timeout + single retry via Reactor operators, no circuit breaker).
  Add the `resilience4j-reactor` module + `CircuitBreakerOperator` here if
  Service B's failure modes turn out to need it.
- `OrderCallbackController` - the shared-secret check is the whole
  authentication story for this endpoint today; a duplicate or replayed
  callback is handled (ignored if the order isn't in `SENT_TO_SERVICE_B`),
  but there's no signature/nonce verification, which you'd want for a real
  service-to-service boundary.
