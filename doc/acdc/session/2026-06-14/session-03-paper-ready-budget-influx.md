# Sessione 3 - PAPER Ready Budget e Influx

Data: 2026-06-14.

## Obiettivo

Completare ACDC verso PAPER READY:

- ogni run deve partire da un budget;
- DRY, SHADOW e PAPER partono da budget simulato `100`;
- ogni execution salva budget a DB;
- SHADOW deve poter leggere da Influx per identificare segnali live;
- REAL resta non avviabile senza autorizzazione e provider Binance abilitato.

## Riferimenti HFT

HFT usa:

- `real_trading_runtime_config.paper_quote_balance` come budget PAPER storico, default `100`;
- `TelegramRuntimeSummaryService` per budget corrente e net giornaliero;
- `InfluxDBService.getLastSymbols()` e microbar, con filtro `not exists r.base`.

## Implementazione ACDC

- Migrazione `V3__add_run_budget_execution.sql`.
- Nuove tabelle:
  - `acdc_run_mode_config`;
  - `acdc_run_execution`.
- Nuovi run mode seedati per `REM_CURRENT`:
  - `DRY`, `SHADOW`, `PAPER`: `default_budget=100`, `budget_source=SIMULATED_DEFAULT`, `status=ACTIVE`;
  - `REAL`: `budget_source=BINANCE`, `status=DISABLED`.
- Nuovo servizio `RunBudgetService`.
- Nuovo endpoint:
  - `GET /acdc/profiles/{profileKey}/run-modes`;
  - `POST /acdc/dry/run/{profileKey}`.
- `RunSummary` ora include:
  - `executionId`;
  - `executionMode`;
  - `dataSource`;
  - `initialBudget`;
  - `currentBudget`;
  - `dailyBudget`.

## Influx SHADOW

- Aggiunto `InfluxSnapshotService`.
- SHADOW senza snapshot nel body usa Influx come sorgente primaria.
- Query allineata a HFT:
  - measurement `tick`;
  - filtro `not exists r.base`;
  - bucket realtime;
  - fallback bucket storico;
  - microbar 5s.
- Feature costruite per le guardie DB correnti:
  - `momentum5`;
  - `momentum10`;
  - `momentum15`;
  - `trend`;
  - `volume_ratio`;
  - `quote_volume_fast`;
  - `distance_from_low`;
  - `pullback_depth`;
  - `entry_friction_quote`.

## Verifica Locale

Profilo: `local`.

Nota: `local` disabilita Influx per run H2 riproducibili.

Run modes DB:

- DRY: `100`;
- SHADOW: `100`;
- PAPER: `100`;
- REAL: `BINANCE`, `DISABLED`.

Risultati:

- DRY:
  - `initialBudget=100`;
  - `currentBudget=100`;
  - action `BUY`;
  - nessuna posizione.
- SHADOW:
  - `initialBudget=100`;
  - `currentBudget=100`;
  - action `BUY`;
  - nessuna posizione.
- PAPER:
  - timeline `BUY -> HOLD -> SELL`;
  - `initialBudget=100`;
  - `currentBudget=101`;
  - `opened=1`;
  - `closed=1`;
  - SELL da `exit_dynamic_trailing`.

## Verifiche

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- `./mvnw -q package -DskipTests -Dquarkus.profile=local` OK.

## Stato

- ACDC e' PAPER READY per run REST controllate con budget persistito.
- SHADOW e' predisposta per sorgente Influx reale nel profilo runtime.
- REAL resta disabilitata: il run mode e' modellato a DB con source `BINANCE`, ma non viene avviato.
