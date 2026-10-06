# service-b

A minimal downstream processor for the `rest-arch-demo` project. Accepts
work from Service 1, does simulated async processing, and calls back with a
result - all correlated by the `X-Correlation-Id` header/`correlationId`
field Service 1 sends.

## Run it

```bash
mvn spring-boot:run
```

Starts on `http://localhost:8081`. Requires Service 1 to be reachable at
the URL configured in `application.yml` under `servicea.callback-url`
(defaults to `http://localhost:8080/api/v1/orders/callback`) for the
callback to succeed - run Service 1 first, or expect callback errors in the
logs until it's up.

## What it does

1. `POST /api/v1/process` - validates the `X-Service-Key` header against
   `servicea.inbound-service-key`, then returns `202 Accepted` immediately.
2. The actual work happens on a separate virtual thread: a simulated delay
   (`servicea.min-latency-ms` to `max-latency-ms`), then a
   `servicea.failure-percentage` chance of resolving as `FAILED` instead of
   `COMPLETED` (so you can exercise Service 1's failure path without
   needing a real flaky dependency).
3. Calls back to Service 1's `/api/v1/orders/callback` with
   `{correlationId, status, message}`, carrying the same correlation ID it
   was given.

## Config

All in `application.yml` under `servicea.*`:

| Property | Meaning |
|---|---|
| `callback-url` | Full URL of Service 1's callback endpoint |
| `outbound-service-key` | Sent on the callback call |
| `inbound-service-key` | Expected on Service 1's inbound dispatch call |
| `min-latency-ms` / `max-latency-ms` | Simulated processing time range |
| `failure-percentage` | % of orders that resolve as `FAILED` |

Both service keys are demo-only shared secrets, matching what's configured
in Service 1's `serviceb.*` properties. Swap for mTLS or a real
service-to-service token exchange before this goes anywhere real.
