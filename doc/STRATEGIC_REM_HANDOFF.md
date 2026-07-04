# Bollinger Context V1 Operational Handoff

Data: 2026-07-04.

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
- SELL strategica usa candele 1m chiuse per invalidazioni, target, trailing e timeout; microbar 5s non crea segnali
  SELL strategici.
- Loss cap quote-aware puo' usare prezzo eseguibile intraminuto solo come protezione economica meccanica, dichiarata e
  auditata separatamente.
- La finestra WATCH autorizza osservazione, non e' una condizione BUY.
- BUY e WATCH non hanno cap numerici concorrenti; il limite effettivo e' budget/exchange sizing al momento della BUY.
- Dal Consiglio 2026-07-04, prima di nuove RUN PAPER e' vincolante il blocco A0:
  - indicatori, contract, WATCH, BUY e SELL strategica su candele 1m chiuse;
  - microbar 5s solo replay/diagnostica/timing/gap detection/execution observation;
  - `binance-realtime` non decisionale;
  - `binance-microbar` non decisionale;
  - la 1m decisionale non puo' essere ricostruita aggregando realtime o microbar;
  - decision snapshot deve includere candle count, max gap e staleness;
  - no PAPER finche' `1m_alignment_ready` non e' vero.

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
3. Verificare blocco A0:
   - `/management/state` espone `1m_alignment_ready=true`;
   - DocBrown source bucket decisionale = `binance`;
   - ACDC source bucket decisionale = `binance`;
   - interval decisionale = `60`;
   - candle state = `CLOSED`;
   - decision candle count sufficiente per EMA50 e volume ratio 1m/20m;
   - decision max gap entro soglia approvata;
   - decision staleness entro soglia approvata;
   - `binance-realtime` assente dal path BUY;
   - `binance-microbar` assente dal path indicatori/BUY/SELL strategica;
   - SELL decision source bucket = `binance`;
   - SELL decision interval = `60`;
   - SELL decision candle state = `CLOSED`;
   - eventuale SELL execution interval 5s separato dalla reason strategica;
   - replay espone `source_bucket`, `interval_seconds`, `candle_count`, `max_gap_seconds`, `synthetic_backfill`.
4. Se `/management/state` non espone ancora `1m_alignment_ready`, trattarlo come `false`.
5. Verificare count per setup/regime e readiness context.
6. Se A0 non e' pronto, fermarsi: nessuna nuova sequenza PAPER.
7. Se non ci sono PAPER o posizioni aperte e A0 e' pronto, usare l'action approvata per generare una nuova sequenza PAPER.
8. Dopo run PAPER, leggere `/management/runs/{executionId}`.
9. Attribuire ogni BUY/SELL a setup, trigger, regime, gate context, reason e PnL.
10. Separare sempre le metriche di breakout e reentry.
11. Per analisi visiva usare `/trades`: selezione data, execution del giorno, simboli per execution, filtri fase
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
