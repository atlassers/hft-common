# Bollinger-Only Operational Handoff

Data: 2026-06-29.

## Scopo

Manuale operativo compatto per il ciclo Bollinger-only.

## Vincoli

- REAL vietata.
- Validazione operativa solo su MySQL e container deployati.
- `/management` e' l'interfaccia primaria.
- Il contratto runtime usa solo campi `bb_*`.
- La tabella advice runtime e' `hft.acdc_live_bb_advice`.
- WATCH apre BUY solo se il trigger Bollinger setup-specifico e' vero.
- La finestra WATCH autorizza osservazione, non e' una condizione BUY.
- BUY e WATCH non hanno cap numerici concorrenti; il limite effettivo e' budget/exchange sizing al momento della BUY.

## Processo

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

## Setup E Trigger

Setup ammessi:

- `BB_REENTRY_MEAN_REVERSION_LONG`
- `BB_SQUEEZE_BREAKOUT_LONG`

Trigger ammessi:

- `BB_REENTRY_CONFIRMED`
- `BB_UPPER_BREAKOUT_CONFIRMED`

## Endpoint Primari FE

```bash
curl -sS 'http://localhost:5173/backoffice/management/state?profileKey=REM_CURRENT' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs?limit=20' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs/{executionId}' | jq '.'
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:5173/backoffice/management/actions/REFRESH_DIAGNOSTICS' \
  -d '{"profileKey":"REM_CURRENT","payload":{}}' | jq '.'
```

## Action Approvate

- `AUTO_BOLLINGER_START`
- `AUTO_BOLLINGER_STOP`
- `REFRESH_DIAGNOSTICS`
- `UNIVERSE_PREFILTER`
- `RUN_RESEARCH`
- `RESEARCH_STATUS`
- `LIVE_SCORE`
- `ROLLING_VALIDATION`
- `ROLLING_SELECTION_ATTRIBUTION_AUDIT`
- `ROLLING_PROMOTION`
- `PAPER_BOLLINGER_START`
- `PAPER_STOP_BUY`
- `PAPER_STOP`
- `SAVE_MANAGEMENT_CONFIG`

## Diagnostica Standard

1. Leggere `/management/state`.
2. Verificare `bbReady`, blocker, advice attive, PAPER running e posizioni aperte.
3. Se non ci sono PAPER o posizioni aperte, usare `AUTO_BOLLINGER_START` per generare una nuova sequenza.
4. Dopo run PAPER, leggere `/management/runs/{executionId}`.
5. Attribuire ogni BUY/SELL a setup, trigger, reason e PnL.

## Build

Ordine consigliato:

```bash
cd hft-common && mvn -q -DskipTests install
cd ../docbrown && mvn -q -DskipTests package
cd ../acdc && mvn -q -DskipTests package
cd ../kenshiro && mvn -q test && mvn -q -DskipTests package
cd ../hft-fe && npm run check && npm run build
```

## Deploy

Usare i `docker-compose.yml` del modulo o del runtime VPN gia' presenti nel workspace.

Regola: ogni gruppo di codice deployabile va verificato nel container prima di usare la run come evidenza operativa.

## Git

Dopo ogni gruppo coerente:

1. `git status`
2. build/test pertinente
3. commit `MS<n>: <message>`
4. push
