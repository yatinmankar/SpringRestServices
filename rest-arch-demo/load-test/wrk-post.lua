-- Alternative to k6 using wrk (https://github.com/wg/wrk), for environments
-- where installing k6 isn't an option. wrk reports raw throughput/latency
-- well but doesn't do k6's staged ramp-up or per-VU token cycling, so this
-- is coarser: one token, fixed concurrency.
--
-- 1) get a token first:
--      TOKEN=$(curl -s -X POST "http://localhost:8080/api/v1/auth/token?clientId=wrk-client" | jq -r .token)
-- 2) run wrk with that token baked in (env vars aren't available inside the
--    Lua script, so pass it via -e or edit the line below):
--      wrk -t8 -c400 -d30s -s wrk-post.lua --latency http://localhost:8080/api/v1/orders
--
-- NOTE: raise app.rate-limit.* in application.yml for the "wrk-client" id
-- if you want to push this past ~200 req/s, since wrk reuses one token/client.

local token = os.getenv("TOKEN") or "REPLACE_WITH_TOKEN"

wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.headers["Authorization"] = "Bearer " .. token

request = function()
  local key = string.format("%d-%d", os.time(), math.random(1, 1000000))
  wrk.headers["Idempotency-Key"] = key
  local body = string.format(
    '{"instrumentId":"AAPL","quantity":%d,"price":%.2f,"side":"%s"}',
    math.random(1, 100),
    100 + math.random() * 10,
    (math.random() > 0.5) and "BUY" or "SELL"
  )
  return wrk.format(nil, nil, nil, body)
end
