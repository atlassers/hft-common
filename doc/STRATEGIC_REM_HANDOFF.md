# Bollinger Context V1 Operational Handoff

Data: 2026-06-30.

## Scopo

Manuale operativo compatto per il ciclo `BOLLINGER_CONTEXT_V1`.

Il charter strategico operativo primario e':

```text
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md
```

La base scientifica di riferimento e':

```text
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md
```

## Vincoli

- REAL vietata.
- Validazione operativa solo su MySQL e container deployati.
- `/management` e' l'interfaccia primaria.
- La tabella advice runtime resta `hft.acdc_live_bb_advice`.
- Bollinger resta obbligatorio: ogni advice deve avere `bb_setup`, `bb_trigger` e contratto `bb_*`.
- Context V1 richiede feature esplicite di regime, trend, momentum, volume e risk.
- WATCH apre BUY solo se passano trigger Bollinger e gate Context V1.
- SELL fase 1 resta quello Bollinger-only, senza nuove logiche fino a evidenza PAPER.
- La finestra WATCH autorizza osservazione, non e' una condizione BUY.
- BUY e WATCH non hanno cap numerici concorrenti; il limite effettivo e' budget/exchange sizing al momento della BUY.

## Processo

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

## Setup, Trigger E Regimi

Setup ammessi:

- `BB_REENTRY_MEAN_REVERSION_LONG`
- `BB_SQUEEZE_BREAKOUT_LONG`

Trigger ammessi:

- `BB_REENTRY_CONFIRMED`
- `BB_UPPER_BREAKOUT_CONFIRMED`

Regimi Context V1:

- `REGIME_RANGE`
- `REGIME_SQUEEZE`
- `REGIME_EXPANSION`
- `REGIME_TREND_UP`
- `REGIME_TREND_DOWN`
- `REGIME_CHAOS`

## Endpoint Primari FE

```bash
curl -sS 'http://localhost:5173/backoffice/management/state?profileKey=REM_CURRENT' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs?limit=20' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs/{executionId}' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades/executions?date=YYYY-MM-DD&limit=100' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades?profileKey=REM_CURRENT&limit=50' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades?profileKey=REM_CURRENT&executionId={executionId}&limit=300' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades/{executionId}/{symbol}' | jq '.'
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:5173/backoffice/management/actions/REFRESH_DIAGNOSTICS' \
  -d '{"profileKey":"REM_CURRENT","payload":{}}' | jq '.'
```

## Action Approvate

Operative gia' esistenti:

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

Da introdurre solo quando DocBrown e ACDC sono compatibili:

- `APPLY_BOLLINGER_ONLY`
- `APPLY_BOLLINGER_CONTEXT_V1`

## Diagnostica Standard

1. Leggere `/management/state`.
2. Verificare strategy family, `bbReady`, blocker, advice attive, PAPER running e posizioni aperte.
3. Verificare count per setup/regime e readiness context.
4. Se non ci sono PAPER o posizioni aperte, usare l'action approvata per generare una nuova sequenza PAPER.
5. Dopo run PAPER, leggere `/management/runs/{executionId}`.
6. Attribuire ogni BUY/SELL a setup, trigger, regime, gate context, reason e PnL.
7. Separare sempre le metriche di breakout e reentry.
8. Per analisi visiva usare `/trades`: selezione data, execution del giorno, simboli per execution, filtri fase
   WATCH/BUY/SELL, replay candle persistito e replay live Influx con refresh 1s.

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
