# Session 22 - Shadow lifecycle ledger

Data: 2026-06-16

## Correzione semantica

La SHADOW ACDC era stata implementata come sola signal/parity run:

- leggeva Influx;
- applicava guardie ENTRY SHADOW;
- salvava `acdc_shadow_decision`;
- non apriva posizioni;
- non simulava EXIT/SELL.

Questo non replica correttamente il comportamento operativo atteso da HFT.

Nuova semantica confermata:

- `DRY`: solo valutazione, nessun ledger;
- `SHADOW`: BUY/SELL simulati su ledger shadow separato, ENTRY a filtri abbassati, EXIT con guardie DB;
- `PAPER`: BUY/SELL simulati con soglie validate;
- `REAL`: ordini Binance, vietato senza autorizzazione.

## Implementazione

- Aggiunta migration `V18__add_shadow_lifecycle_ledger.sql`.
- Aggiunta tabella `acdc_shadow_position`.
- Estesa `acdc_shadow_run` con:
  - `opened`;
  - `closed`;
  - `net_profit_quote`.
- Estesa `acdc_shadow_decision` con:
  - `execution_id`;
  - `action`;
  - `guard_key`;
  - `price`;
  - `quantity`;
  - `quote_quantity`.
- Aggiunta entity `ShadowPosition`.
- Aggiunto repository `ShadowPositionRepository`.
- `RunBudgetService` ora espone `startOrResumeShadow`.
- `ShadowRunService` ora:
  - mantiene una execution SHADOW running;
  - apre posizioni simulate su ENTRY accettata;
  - rivaluta EXIT sulle posizioni aperte;
  - chiude posizioni simulate su SELL;
  - aggiorna current/reserved/realized budget;
  - supporta `stop` e `stopBuy`.
- Aggiunti endpoint:
  - `POST /acdc/shadow/stop/{profileKey}`;
  - `POST /acdc/shadow/stop-buy/{profileKey}`.
- `BestWinnerActionService` ora gestisce `stop-run SHADOW`:
  - con posizioni aperte: `BUY_STOPPED_DRAINING`;
  - senza posizioni aperte: `STOPPED`.

## Test

- Aggiornato `AcdcRunServiceTest`:
  - SHADOW ora apre trade simulati;
  - aggiunto test SHADOW BUY -> HOLD -> SELL across execution steps.
- Comando:
  - `./mvnw -q test`.
- Esito: OK.

## Runtime

- Eseguito:
  - `./mvnw -q package -DskipTests`;
  - `docker build -f docker/Dockerfile.jvm -t acdc:latest .`;
  - restart container `acdc-vpn`.
- Flyway MySQL:
  - schema da version `17` a `18`;
  - migration `18 - add shadow lifecycle ledger` applicata.

## SHADOW Live

- Avviata da endpoint FE/ACDC:
  - `POST /backoffice/best-winner/actions/start-run`;
  - `executionMode=SHADOW`.
- Prima run:
  - `executionId=22`;
  - `evaluated=200`;
  - `accepted=3`;
  - `opened=3`;
  - `closed=0`;
  - `currentBudget=24.945038916497600000`;
  - `reservedBudget=75.054961083502400000`.
- Posizioni aperte:
  - `BABYUSDC`;
  - `BTCUSDC`;
  - `PYTHUSDC`.
- Seconda run sulla stessa execution:
  - `evaluated=200`;
  - `accepted=0`;
  - `opened=0`;
  - `closed=0`;
  - `EXIT_HOLD=3`.
- Eseguito `stop-run SHADOW`:
  - output `BUY_STOPPED_DRAINING`;
  - nuovi BUY bloccati;
  - posizioni aperte restano drenabili da future valutazioni EXIT.

## Vincoli rispettati

- Nessuna REAL.
- Nessuna PAPER.
- Nessun ordine Binance.
- SELL runtime non forzata con snapshot artificiali su dati live; il ramo SELL e' coperto da JUnit dedicato.

## Chiarimento sorgenti Influx

- ACDC non deve usare `binance` per SHADOW/PAPER live.
- `binance` resta bucket storico/48h.
- SHADOW/PAPER live devono usare:
  - `binance-realtime`;
  - `binance-microbar`.
- Allineamento a HFT:
  - mantenuto filtro `not exists r.base` sui percorsi realtime/microbar;
  - rimosso fallback storico da `latestShadowSnapshots()`;
  - rimosso fallback microbar verso `binance` da `buildSnapshot()`.
- Se i bucket live sono vuoti, ACDC resta fail-closed con `NO_SNAPSHOTS` e non testa BUY/SELL con dati storici.
