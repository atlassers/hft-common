# Session 06 - Strategy Replay FE

Date: 2026-06-11

## Goal

Make the mandatory strategy replay visible in the FE after the DB-persisted replay rule was introduced.

## Changes

- Updated `BUSINESS_REQUIRIMENTS.md` with the replay endpoint contract.
- Added SvelteKit proxy:
  - `GET /backoffice/pipeline/replay-evaluations`
- Added FE API/types for `PipelineReplayEvaluationResponse`.
- Added replay evidence cards to:
  - `/pipeline`
  - `/pipeline/flow`
- The UI passes the selected `executionUuid` and shows execution-specific replay first when available.
- The UI shows sample size, baseline net, replay net, delta net, replay win/loss, replay profit factor, avg loss, max loss, and readable config dialog.
- Global replay remains available as fallback/context and is visually labelled differently from `Execution selezionata`.
- No mock/fallback was added for replay data.

## Verification

- `npm run check`
- `npm run build`
- Runtime smoke:
  - `http://localhost:5173/backoffice/pipeline/replay-evaluations?limit=2`
  - `http://localhost:5173/backoffice/pipeline/replay-evaluations?executionUuid=06facc02-18b6-4eaf-94aa-28c7c72adc5f&limit=1`
  - browser check on `http://localhost:5173/pipeline/flow` confirms `EXECUTION SELEZIONATA`, baseline `-0.24399244`, replay net `+0.03426802`, and delta `+0.27826046` are rendered.

## Runtime

- Rebuilt and restarted `hft-fe-local` from `hft-fe:local`.

## 2026-06-11 Update - Replay Per Execution Da Tabella

- Aggiornato `BUSINESS_REQUIRIMENTS.md`: `executionEvaluation` e' letta da
  `hft.strategy_replay_execution_evaluation`.
- Il FE continua a passare `executionUuid`, ma non presume piu' un calcolo runtime di Kenshiro.
- Etichetta sorgente aggiornata a `strategy replay execution evaluation`.
- Il comportamento resta fail-closed: se non esiste riga persistita per la execution, viene mostrato solo il replay globale.

Verifica:

- `npm run check` OK.
- `npm run build` OK.
- Runtime smoke `http://localhost:5173/pipeline/flow` OK, response HTML `7772` byte.
- Rebuilt e restarted `hft-fe-local`.
