# Session 03 - Strategy Replay Endpoint

Date: 2026-06-11

## Goal

Expose the DB-persisted strategy replay evidence required before any new trading strategy/tuning is evaluated in PAPER or REAL.

## Changes

- Added `GET /backoffice/pipeline/replay-evaluations`.
- Default `strategyKey` is `paper_tight_sell_guard`, aligned with `hft/scripts/replay_paper_sell_tuning.py`.
- Reads real rows from `hft.strategy_replay_evaluation`.
- Added optional `executionUuid`: when present, Kenshiro recalculates the replay for that PAPER execution from persisted
  `trade_position` and `paper_trading_event` rows using the latest replay config.
- Response includes baseline/replay win/loss, gross/net profit, profit factor, delta gross/net, avg/max loss, config JSON parsed as an object, `createdAt`, and `sourceTable`.
- Added integration coverage in `BackofficeResourceTest`.

## Verification

- `./mvnw test -Dtest=BackofficeResourceTest`
- `./mvnw package -DskipTests`
- Runtime smoke:
  - `http://localhost:8085/kenshiro/backoffice/pipeline/replay-evaluations?limit=2`
  - `http://localhost:8085/kenshiro/backoffice/pipeline/replay-evaluations?executionUuid=06facc02-18b6-4eaf-94aa-28c7c72adc5f&limit=1`
  - returned real DB rows from `strategy_replay_evaluation`.
  - returned execution-specific replay delta for the selected PAPER execution.

## Runtime

- Rebuilt and restarted `kenshiro-local` from `kenshiro:local`.

## 2026-06-11 Update - Execution Replay Persistito

- `executionUuid` non ricalcola piu' il replay da `trade_position` e `paper_trading_event`.
- Kenshiro legge l'ultima riga disponibile in `hft.strategy_replay_execution_evaluation`.
- `sourceTables` per `executionEvaluation` ora contiene solo `strategy_replay_execution_evaluation`.
- Se la execution non ha replay persistito, `executionEvaluation` resta `null`.
- Rimossa la vecchia logica privata di replay runtime dal service.

Verifica:

- `./mvnw test -Dtest=BackofficeResourceTest` OK.
- `./mvnw package -DskipTests` OK.
- Runtime smoke su `executionUuid=068e4ccc-342b-4aa5-9a30-e0fce86815f9`:
  - baseline `-0.31588032`;
  - replay `+0.071218197`;
  - delta `+0.387098517`;
  - source `strategy_replay_execution_evaluation`.
- Rebuilt e restarted `kenshiro-local`.
