# REM Blank Generator Clean DB Checklist

Data: 2026-06-19.

Obiettivo: ripartire da DB pulito e implementare il ciclo scientifico con candidate generator osservazionale quasi vuoto, senza soglie predittive premature.

## Stato Generale

- Stato: `DONE_FAIL_SCIENTIFIC`.
- Vincolo charter aggiornato: `DONE`.
- Pulizia DB: `DONE`.
- Candidate generator vuoto: `DONE`.
- Validazione scientifica: `DONE`.
- Risultato finale: `FAIL`.

## DONE

- [x] Decisione scientifica precedente registrata: modello corrente non promuovibile.
- [x] Nuovo obiettivo definito: candidate generator osservazionale quasi vuoto.
- [x] Charter aggiornato con vincolo candidate generator vuoto.
- [x] Writer applicativi fermati prima della pulizia DB.
- [x] Inventario MySQL eseguito.
- [x] Pulizia DB eseguita.
- [x] Verifica post-clean con count esatti.
- [x] Migrazione DocBrown per candidate osservazionali aggiunta.
- [x] Entity/repository candidate osservazionali aggiunti.
- [x] DTO/endpoint DocBrown per candidate generator aggiunti.
- [x] Build DocBrown completata senza test H2 (`./mvnw -DskipTests package`).
- [x] Codice DocBrown committato e pushato: `645e97e Add blank REM candidate generator`.
- [x] Fix timeout transazionale DocBrown committato e pushato: `76c5fdb Extend blank candidate generation transaction`.
- [x] Regole simbolo e baseline volume committate e pushate: `171ab02 Evaluate symbol candidates in blank REM extraction`.
- [x] DocBrown deployato in Docker dopo ogni gruppo codice.
- [x] Endpoint candidate generator eseguito.
- [x] ACDC riavviato dopo pulizia DB.
- [x] Risultato documentato in `doc/session/session-85-blank-generator-result.md`.

## WIP

- [x] Aggiornare charter con vincolo candidate generator vuoto.
- [x] Inventariare schema MySQL e distinguere:
  - schema/Flyway/config da mantenere;
  - runtime data da pulire;
  - ML/advice/candidate data da pulire;
  - tabelle legacy realmente droppabili senza rompere servizi.

## TODO - Pulizia DB

- [x] Fermare o neutralizzare processi che scrivono mentre si pulisce.
- [x] Salvare snapshot diagnostico pre-clean tramite inventario tabelle/count.
- [x] Pulire dati runtime:
  - run execution;
  - paper/shadow positions;
  - decisions;
  - diagnostics;
  - live advice.
- [x] Pulire dati ML/candidate storici:
  - outcome samples;
  - outcome signatures;
  - reversal ML rules;
  - live advice;
  - batch/report storici se presenti.
- [x] Mantenere configurazione essenziale:
  - strategy profiles;
  - guard definitions;
  - runtime config;
  - shared config;
  - Flyway schema history.
- [x] Verificare DB post-clean via MySQL.

### DB Post-Clean Counts

- `acdc_live_ml_advice=0`
- `acdc_outcome_signature=0`
- `acdc_outcome_training_sample=0`
- `acdc_paper_decision=0`
- `acdc_paper_position=0`
- `acdc_paper_run=0`
- `acdc_paper_sell_diagnostics=0`
- `acdc_run_execution=0`
- `acdc_shadow_decision=0`
- `acdc_shadow_position=0`
- `acdc_shadow_run=0`
- `acdc_reversal_ml_rule=0`
- `reversal_event_mining_event=0`
- `scalping_scout_candidate_observation=0`
- `scalping_scout_candidate_sell_policy_outcome=0`
- `paper_trading_event=0`
- `trade_position=0`
- config preservata: `acdc_strategy_profile=1`, `acdc_guard_definition=24`, `acdc_paper_runtime_config=1`, `acdc_shared_runtime_config=60`.

## DONE - Candidate Generator Vuoto

- [x] Implementare endpoint DocBrown per generare candidate reversal minimali.
- [x] Persistenza candidate osservazionali.
- [x] Nessuna soglia predittiva di profittabilita' prima del labeling.
- [x] Labeling netto:
  - MFE;
  - MAE;
  - end-return;
  - cost model;
  - SELL teorica.
- [x] Report split temporale:
  - `DIAGNOSIS_SET`;
  - `SELECTION_SET`;
  - `HOLDOUT_SET`.
- [x] Baseline:
  - `NO_TRADE`;
  - `RANDOM_TOP_VOLUME`;
  - `NAIVE_REVERSAL`;
  - `SCORE_ONLY`;
  - `FRESH_ONLY`;
  - `SYMBOL_ONLY/GLOBAL_ALLOWED` quando applicabili.
- [x] Build DocBrown.
- [x] Commit/push DocBrown.
- [x] Deploy DocBrown container.
- [x] Eseguire endpoint da container.

## DONE - Validazione

- [x] Estrarre modello candidato solo da `SELECTION_SET`.
- [x] Verificare su `HOLDOUT_SET`.
- [x] Non eseguire PAPER perche' il candidato non passa holdout.
- [x] Forensics non applicabile: nessuna PAPER promossa.
- [x] Acceptance finale con `strategicStatus=FAIL`.
- [x] Documentare `PASS_STRATEGIC`, `FAIL` o `INCONCLUSIVE`.

### Risultato Endpoint

- Endpoint: `POST /docbrown/rem/blank-candidates/REM_CURRENT/generate`.
- Batch: `session85-blank-20260619-1530z`.
- Righe persistite: `6912`.
- `DIAGNOSIS_SET=2304`, `SELECTION_SET=2304`, `HOLDOUT_SET=2304`.
- `strategicStatus=FAIL`.
- Universo: `avgEndNetReturn=-0.003368016221182593`, `positiveEndRate=0.14641203703703703`, `zeroMfeRate=0.7407407407407407`.
- Modello estratto da selection: `symbol=REUSDC`.
- Holdout modello: `holdoutAvgEndNetReturn=-0.01804653440989564`, `holdoutPositiveEndRate=0`.
- Decisione: nessuna PAPER automatica, per evitare leakage e regressione.

## Regole Operative

- Nessuna REAL RUN senza autorizzazione esplicita.
- Ogni modifica codice va buildata, committata, pushata e deployata in Docker.
- Ogni prova operativa parte da endpoint.
- Nessuna nuova guardia reattiva fuori dal ciclo scientifico.
