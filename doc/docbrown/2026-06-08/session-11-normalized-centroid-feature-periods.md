# Sessione 11 - Normalized Centroid Feature Periods

Data: 2026-06-08.

## Obiettivo

Allineare Docbrown al nuovo HFT DB-driven: i parametri `SCALPING_SCOUT` promossi devono includere le finestre feature richieste dal BUY live, senza affidarsi a valori cablati in HFT.

## Cambiamenti

- `run_scalping_scout.py` aggiunge e legge da `hft.scalping_scout_runtime_config`:
  - `feature_momentum_fast_period`.
  - `feature_momentum_mid_period`.
  - `feature_momentum_slow_period`.
  - `feature_required_history_floor`.
- I valori vengono salvati nel `params_json` promosso verso `hft.stan_strategy_parameters`.
- HFT usa quei valori nel `MlWalkForwardBuyChecks`; se mancano, il BUY viene bocciato.

## Verifiche

- `python3 -m py_compile python/docbrown_ml/scripts/run_scalping_scout.py python/docbrown_ml/scripts/run_scalping_parameter_optimizer.py python/docbrown_ml/scripts/run_scalping_scout_candidate_dataset.py` OK.

## Stato Operativo

- Nessuna run operativa avviata.
- La modifica prepara il prossimo training/paper run, ma non promuove o abilita REAL.
