# Sessione 09 - DB-Backed Scalping Runtime Residues

## Piano 1 - Persistenza Residui Operativi SCALPING_SCOUT

Data: 2026-06-08.

Obiettivo: fare in modo che Docbrown non persista piu' parametri runtime `SCALPING_SCOUT` con numeri decisionali hardcoded.

Interventi Docbrown:

- `ScoutConfig` estesa con i nuovi campi DB-backed di `scalping_scout_runtime_config`.
- `run_scalping_scout.py` legge/crea/assicura le nuove colonne runtime.
- I periodi `feature_window`, SMA, volatilita', volume, range, pullback, `sma_period` e `sma_limit` vengono persistiti da DB.
- Gli artifact `negative_mean_json` e `deviations_json` usano valori DB-backed invece di soglie locali.
- `params_json` include i nuovi campi, cosi' HFT puo' validare la coerenza runtime.
- Profit factor di validazione in assenza di loss usa `validation_profit_factor_no_loss_cap` DB-backed.

Verifiche:

- `PYTHONPATH=python python3 -m py_compile python/docbrown_ml/scripts/run_scalping_scout.py python/docbrown_ml/scripts/run_scalping_scout_candidate_dataset.py`: OK.
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer python.tests.test_centroid_strategy`: OK.
- Build Docker `docbrown-batch:latest`: OK.

Nessuna REAL RUN avviata.
