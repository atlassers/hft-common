# Sessione 2 - SHADOW/PAPER BUY SELL

Data: 2026-06-14.

## Obiettivo

Eseguire una SHADOW RUN e una PAPER RUN ACDC con guardie lette da DB, arrivando a decisioni operative leggibili come `BUY` e `SELL`, senza avviare REAL RUN.

## Setup

- Profilo runtime: `local`.
- DB: H2 in memoria.
- Migrazioni Flyway applicate:
  - `V1__create_acdc_runtime.sql`;
  - `V2__seed_rem_current_profile.sql`.
- Profilo strategico: `REM_CURRENT`.
- Guardie DB verificate via REST:
  - 11 ENTRY attive;
  - 3 EXIT attive.

## Modifiche Applicate

- Aggiunto H2 come dependency runtime per run locali controllate.
- Aggiunto profilo Quarkus `local` in `application.yml`.
- Aggiunto `DecisionAction`:
  - `BUY`;
  - `SELL`;
  - `HOLD`;
  - `REJECT`.
- Aggiornato `DecisionResult` per esporre `action`.
- Corretto `PaperRunService`:
  - senza posizione aperta valuta ENTRY;
  - con posizione aperta valuta EXIT;
  - evita BUY ripetuti mentre la posizione e' gia' aperta.
- Aggiornati JUnit per verificare azioni `BUY`, `HOLD`, `SELL`.

## Input Run

Simbolo: `TLMUSDC`.

Feature ENTRY valide:

- `momentum5=0`;
- `momentum10=0`;
- `momentum15=0`;
- `trend=0`;
- `volume_ratio=1.20`;
- `quote_volume_fast=25`;
- `distance_from_low=0.005`;
- `pullback_depth=0`;
- `entry_friction_quote=0.010`.

Sequenza PAPER:

1. prezzo `1.00000000`: ENTRY accettata, apertura PAPER.
2. prezzo `1.01200000`: posizione aperta, EXIT non scatta.
3. prezzo `1.01000000`: trailing dinamico scatta dopo max return `0.012`.

## Risultati

SHADOW RUN:

- `runId=1`;
- `type=SHADOW`;
- `evaluated=1`;
- `accepted=1`;
- `rejected=0`;
- decisione: `action=BUY`, `phase=ENTRY`, `reason=ACCEPTED`.

PAPER RUN:

- `runId=1`;
- `type=PAPER`;
- `evaluated=3`;
- `accepted=2`;
- `rejected=1`;
- `opened=1`;
- `closed=1`;
- `netProfitQuote=1.00000000`;
- timeline:
  - `BUY`, `ENTRY`, `ACCEPTED`;
  - `HOLD`, `EXIT`, `EXIT_HOLD`;
  - `SELL`, `EXIT`, `EXIT_DYNAMIC_TRAILING`, `guardKey=exit_dynamic_trailing`.

## Verifiche

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests -Dquarkus.profile=local` OK.

## Stato Operativo

- Nessuna REAL RUN avviata.
- SHADOW non apre posizioni.
- PAPER usa solo ledger simulato ACDC.
- Le decisioni BUY/SELL derivano da guardie DB del profilo `REM_CURRENT`.
