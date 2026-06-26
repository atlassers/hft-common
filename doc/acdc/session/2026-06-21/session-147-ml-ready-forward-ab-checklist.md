# Session 147 - ML_READY To Forward A/B Checklist

Data: 2026-06-21.

## Scopo

Portare il ciclo REM dalla condizione corrente `BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE` a una run breve `FORWARD_AB_98`, senza REAL, senza tuning manuale e senza promozioni contaminate.

## Vincoli

- `STRATEGIC_REM_RECOVERY_PLAN.md` prevale su questa checklist.
- `STRATEGIC_REM_HANDOFF.md` contiene endpoint, payload e ordine diagnostico dettagliato.
- REAL vietata.
- H2 vietato per validazione operativa.
- MySQL e container deployati obbligatori.
- PAPER validativa vietata se `ML_READY=false`.
- Ogni SHADOW/PAPER validativa sulla baseline `98` deve essere `FORWARD_AB_98`.
- Naming commit: `MS<n>: <message>`, con `n = max(MS nei log workspace coinvolti) + 1`.

## Piano Del Consiglio

Saggio ascoltatore:

- non inseguire una PAPER finche' il sistema dichiara fail-closed;
- trattare l'assenza di advice fresche come stato sano, non come errore da bypassare.

Scienziato severo:

- non promuovere candidate con `FAIL_SELECTION_BIAS`;
- non accettare advice residue, scadute o con source generation ambigua;
- non giudicare la baseline `98` senza confronto A/B sulla stessa finestra realtime.

Mediano pragmatico:

- eseguire cicli brevi DocBrown scoring/rolling;
- appena `ML_READY=true`, partire con A/B breve e stop-buy/drain ordinato;
- se `ML_READY` resta falso, salvare output e diagnosi senza avviare PAPER.

Decisione unica:

1. Preflight runtime.
2. Scoring live leggero DocBrown.
3. Rolling validation/promotion solo se scientificamente promuovibile.
4. Check `ML_READY`.
5. Se `ML_READY=true`, run breve `FORWARD_AB_98`.
6. Se `ML_READY=false`, nessuna PAPER: aggiornare evidenza e prossimo blocker.

## Checklist

- [x] Verificare git pulito nei repo coinvolti o annotare modifiche preesistenti.
- [x] Determinare prossimo `MS<n>` dai log workspace se verranno fatti commit.
- [x] Verificare container base: `acdc-vpn`, `docbrown`, `mysql_container`, `influxer`, `influxdb`, `grafana`.
- [x] Verificare `paperRunning=false` e `openPositions=0`.
- [x] Verificare DocBrown research status.
- [x] Eseguire scoring live leggero DocBrown.
- [x] Eseguire rolling validation/promotion solo se il verdict non e' `FAIL_SELECTION_BIAS`.
- [x] Verificare `GET /diagnostics/acdc/ml-readiness?profileKey=REM_CURRENT`.
- [x] Se `ML_READY=false`, fermarsi prima della PAPER e registrare blocker.
- [ ] Se `ML_READY=true`, avviare run `FORWARD_AB_98` con stesso `forwardAbGroupId`.
- [ ] Raccogliere diagnostica A/B e forensics secondo handoff.
- [x] Far valutare il risultato al Consiglio.
- [x] Commit/push di eventuali modifiche coerenti con naming `MS<n>: <message>`.

## Stato Iniziale

Ultimo stato verificato:

- `ML_READY=false`;
- blocker: `LIVE_ADVICE_ACTIVE_MISSING`, `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- advice attive: `0`;
- PAPER running: `false`;
- open positions: `0`;
- DocBrown research run `2` completata.

Stato checklist:

`BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`.

## Evidenza Sessione 147

Commit naming:

- massimo `MS` rilevato nei log workspace: `704`;
- commit checklist/handoff: `MS705: add REM forward A/B operating checklist`.

Preflight:

- container base up;
- ACDC git pulito dopo commit `MS705`;
- PAPER running: `false`;
- open positions: `0`.

DocBrown research:

- status `COMPLETED`;
- runId `2`;
- simboli processati `288`;
- sample processati `17280`;
- errori: nessuno.

ML readiness prima dello scoring:

- `ready=false`;
- blocker: `LIVE_ADVICE_ACTIVE_MISSING`, `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- advice attive: `0`;
- `PAPER_ELIGIBLE` contract-active: `0`.

Scoring live leggero:

- output: `/tmp/session147_docbrown_live_score.json`;
- rules `136`;
- snapshots `222`;
- matched symbols `5`;
- saved advice `5`;
- tutte `PURE_REVERSAL_OBSERVED`;
- nessuna advice `PAPER_ELIGIBLE`.

ML readiness dopo scoring:

- output: `/tmp/session147_ml_readiness_after_score.json`;
- `ready=false`;
- blocker: `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- advice attive `4`;
- `PAPER_ELIGIBLE` contract-active `0`;
- PAPER running `false`;
- open positions `0`.

Rolling validation breve:

- output: `/tmp/session147_rolling_validation.json`;
- batch `session147-rolling-20260621T083711Z`;
- finestra `60m`, window `10m`, holdout `1`;
- persistedRows `5760`;
- selectionWindows `5`;
- holdoutRows `960`;
- strategicStatus `FAIL_SELECTION_BIAS`;
- selectedCandidate `symbol=1000CATUSDC`;
- selectionAvgEndNetReturn `-0.0028`;
- holdoutAvgEndNetReturn `-0.0028`;
- reasons:
  - `expectancy selection non positiva`;
  - `almeno una finestra selection negativa`;
  - `expectancy holdout non positiva`;
  - `Wilson holdout non supera 50%`.

Decisione:

- nessuna rolling paper promotion;
- nessuna PAPER;
- nessuna run `FORWARD_AB_98`;
- stato confermato: `BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`.
