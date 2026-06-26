# Session 02 - SELL policy grid e PAPER optimization

Data: 2026-06-07

## Obiettivo

Estendere il candidate dataset SCALPING_SCOUT con simulazione SELL policy multipla, training soglie BUY x SELL e report robusto train/test/all senza valori operativi hardcoded.

## Sviluppo Docbrown

- Esteso `run_scalping_scout_candidate_dataset.py` con:
  - `--build-sell-policy-outcomes`;
  - `--train-sell-policy-thresholds`;
  - parsing griglie SELL policy da DB;
  - simulazione outcome per `profit_target`, `stop_loss`, `max_hold_minutes`, `stop_grace_minutes`;
  - insert chunked con `sell_policy_insert_batch_size` da DB;
  - training per policy con split train/test/all;
  - log progressivi per dataset, outcome e ogni policy;
  - report `readyCount`, `statusCounts`, `readyBest`, `best`, `top`.
- Fix reporting: il best ora separa candidati READY da candidati WATCH e non presenta un risultato fragile come pronto per REAL.

## PAPER RUN eseguite

1. Discovery wide, dataset 10:
   - 3.794 osservazioni dataset, base net `-20.01056242`;
   - 1.138.200 SELL outcomes, net `-5834.45321803`;
   - best `WATCH_SAMPLE`, non utilizzabile.
2. Discovery tight, dataset 11:
   - 1.951 osservazioni, base net `-11.11534353`;
   - 585.300 SELL outcomes, net `-3253.89276629`;
   - test positivo solo su 2 sample, train/all negativi.
3. Refine short, dataset 12:
   - 3.676 osservazioni, base net `-16.41800851`;
   - 661.680 SELL outcomes, net `-2882.18277599`;
   - best con all positivo ma test negativo: `WATCH_TEST_EV`.
4. Refine runner, dataset 13:
   - 3.524 osservazioni, base net `-17.83362016`;
   - 634.320 SELL outcomes, net `-3502.87002729`;
   - best su 1 solo sample: overfit, `WATCH_SAMPLE`.
5. Final, dataset 14:
   - 4.595 osservazioni, base net `-24.67776995`;
   - 1.148.750 SELL outcomes, net `-5821.21084995`;
   - primo report: best nominale su 26 sample / 3 test, `WATCH_SAMPLE`.
6. Final ranked retraining su dataset 14:
   - 250 policy valutate;
   - `readyCount=0`;
   - `statusCounts={WATCH_SAMPLE: 250}`;
   - best nominale: `pt=0.008|sl=0.004|hold=20|grace=0`, train 9 sample, test 2 sample, all 11 sample, `WATCH_SAMPLE`.

## Validazione

- `python3 -m py_compile python/docbrown_ml/scripts/run_scalping_scout_candidate_dataset.py` OK.
- `python3 -m unittest python.tests.test_centroid_strategy python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK, 29 test.
- `docker compose build docbrown-real-refresh` OK.

## Esito

Non esiste profitto vero robusto nei dati 48h correnti. Tutti i risultati profittevoli sono campioni troppo piccoli oppure falliscono train/test/all. Nessuna REAL RUN autorizzata o avviata.
