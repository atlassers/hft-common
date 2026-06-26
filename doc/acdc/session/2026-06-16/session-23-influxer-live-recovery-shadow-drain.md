# Session 23 - Influxer Live Recovery and Shadow Drain

Date: 2026-06-16

## Scope

- Keep ACDC aligned with HFT live-source semantics:
  - `binance` is historical/48h only;
  - `binance-realtime` and `binance-microbar` are the only live BUY/SELL sources;
  - no fallback from ACDC live SHADOW/PAPER to historical `binance`.
- Restore live short-retention data by fixing `influxer`.
- Verify SHADOW BUY/SELL simulation with DB guards on live USDC-only data.

## Influxer Fix

- Root cause:
  - the Binance WebSocket client stopped reconnecting after finite retries;
  - the `influxer` process remained alive but short-retention buckets stopped receiving fresh points.
- Code changes:
  - removed finite max retry stop;
  - reconnect now continues indefinitely with capped exponential backoff;
  - duplicate retry scheduling is suppressed with an atomic pending flag;
  - retry state resets after successful connection.
- Tests:
  - `./mvnw -q test` passed;
  - added coverage for repeated close/failure events scheduling only one pending reconnect.
- Runtime:
  - `./mvnw -q package -DskipTests`;
  - restarted `influxer`;
  - verified runtime config `Quote currencies: [USDC]`;
  - verified WebSocket subscribed to `288` Binance USDC `1m` streams.

## ACDC Verification

- `GET /diagnostics/acdc/rem/parity` after recovery:
  - `dataSource=INFLUX`;
  - `snapshots=131`;
  - `evaluated=20`;
  - `accepted=11`;
  - `status=NO_HFT_CANDIDATES` because HFT comparison had no candidate rows, not because ACDC lacked live data.

## SHADOW Results

- Existing drain:
  - `executionId=22`;
  - closed stale positions from the previous session;
  - `closed=3`;
  - `realizedProfitQuote=-2.299924712937200000`;
  - all sells were loss exits, mainly `EXIT_ABSOLUTE_LOSS`;
  - execution stopped with zero reserved budget.
- Clean live SHADOW:
  - `executionId=23`;
  - opened `3` simulated positions;
  - stop-buy applied immediately;
  - second cycle closed `2` positions;
  - realized PnL after second cycle: `-0.214997047723000000`;
  - third cycle kept `BCHUSDC` open in `EXIT_HOLD`.
- Position details:
  - `EPICUSDC`: closed by `EXIT_ABSOLUTE_LOSS`, net `-0.139196428126000000`;
  - `CUSDC`: closed by `EXIT_QUOTE_LOSS_CAP`, net `-0.075800619597000000`;
  - `BCHUSDC`: still open, buy price `224.000000000000000000`, reserved quote about `25.024967968`.

## Strategic Status

- ACDC now reads live buckets correctly and does not mix historical data into live BUY/SELL.
- SHADOW lifecycle is active: BUY, stop-buy/drain, EXIT HOLD, SELL and budget updates all executed.
- The remaining issue is strategy quality:
  - entries are still too weak/noisy under lowered SHADOW filters;
  - exits are active and currently cut loss quickly;
  - PAPER remains blocked until SHADOW/ML cost-aware evidence is positive.
