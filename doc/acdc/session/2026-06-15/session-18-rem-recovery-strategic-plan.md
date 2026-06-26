# Sessione 18 - REM Recovery Strategic Plan

Data: 2026-06-15.

## Obiettivo

Fissare un piano vincolante per recuperare il filone REM outcome-first e portare ACDC a stato PAPER-ready senza tornare a soglie manuali.

## Piano Sacro

Il piano e' salvato in:

- `doc/STRATEGIC_REM_RECOVERY_PLAN.md`

Regola operativa:

- non si cambia piano senza chiedere conferma;
- non si avvia PAPER finche' il piano non raggiunge `PAPER_READY`;
- non si avvia REAL.

## Prima Diagnosi

La deviazione critica e' `V14`:

- ha sostituito guardie candidate-specific con soglie statiche globali;
- era esplorativa;
- non puo' essere base della prossima PAPER RUN.

## Prossimo Step Obbligatorio

Ripristinare ENTRY REM candidate-specific e aggiornare i test di configurazione.

## Implementazione

- Aggiunta migration `V17__restore_rem_candidate_specific_entry_guards.sql`.
- Aggiunti diagnostics:
  - `GET /diagnostics/acdc/rem/parity`;
  - `GET /diagnostics/acdc/rem/lifecycle-replay`;
  - `GET /diagnostics/acdc/rem/readiness`.
- Aggiunto script ML ACDC:
  - `scripts/acdc-run-rem-ml.sh`.
- Il miner ML usa DocBrown come motore outcome-first e scrive `target/rem-ml/latest.json`.
- Docker ACDC monta `target/rem-ml` in read-only per readiness.

## Stato ML

- Eseguito ML REM dry-run su bucket `binance-microbar`.
- Report: `target/rem-ml/latest.json`.
- Output osservato:
  - `symbols=287`;
  - `ticks=76059`;
  - `events=58839`;
  - `GOOD_REVERSAL=2499`;
  - `BAD_REVERSAL=49637`;
  - `NEUTRAL_REVERSAL=6703`;
  - `signatures=12`;
  - `promoted=0`.

## Nota Operativa

Il report contiene firme, ma la readiness finale dipende anche da parity e lifecycle replay.
La PAPER non viene avviata automaticamente.

## Validazione Finale

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- `docker build -f docker/Dockerfile.jvm -t acdc:latest .` OK.
- Container `acdc-vpn` ricreato.
- Flyway MySQL `hft` aggiornato a v17.
- Readiness finale:
  - `PAPER_READY_WAITING_SIGNAL`;
  - blockers vuoti;
  - warning `WAITING_LIVE_SIGNAL`.

## Interpretazione

ACDC e' pronto per una PAPER RUN dal FE.
Non c'e' un BUY immediato perche' il live corrente non rispetta le firme REM candidate-specific promosse.
Questo e' comportamento corretto e fail-closed: la PAPER puo' partire e attendere un segnale valido, ma non deve forzare BUY.
