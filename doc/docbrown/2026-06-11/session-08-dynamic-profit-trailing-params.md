# Sessione 8 - Dynamic Profit Trailing Params

## Piano 1 - Persistenza Parametri Trailing Profit

Data: 2026-06-08.

Obiettivo:

- Propagare i nuovi parametri HFT di trailing profit dalla config DB Docbrown fino ai `params_json` delle candidate `SCALPING_SCOUT`.
- Non introdurre valori strategici hardcoded nella chain.

Implementazione:

- `scalping_scout_runtime_config` in Docbrown include ora:
  - `profit_trailing_enabled BOOLEAN NOT NULL DEFAULT FALSE`
  - `profit_trailing_drawdown DECIMAL(19,8) NOT NULL DEFAULT 0.00000000`
- `CONFIG_ALTER_COLUMNS` aggiunge le colonne se mancanti.
- `ScoutConfig` legge i nuovi campi da DB.
- `params_for()` scrive nei parametri candidato:
  - `profit_trailing_enabled`
  - `profit_trailing_drawdown`

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK, 26 test.
- `PYTHONPATH=python python3 -m py_compile python/docbrown_ml/scripts/run_scalping_scout.py` OK.
- Rebuild `docbrown-batch:latest` OK.
- Dry-run PAPER ha prodotto candidate con trailing enabled.
- Persist PAPER ha creato `strategy_parameter_id=589` (`EPICUSDC`) e `590` (`LAYERUSDC`) con trailing nei params.

Esito:

- Docbrown non usa valori hardcoded per il trailing: legge DB e propaga nei parametri HFT.
- Nessuna REAL RUN avviata.
# 2026-06-11 - Runtime-aligned tight trailing params

- Updated scalping scout dynamic trailing generation so DB runtime thresholds are respected:
  - `profit_arm_threshold` no longer gets raised above the runtime/profit target floor;
  - `profit_floor_after_arm` stays bounded by arm and runtime config;
  - `absolute_loss_stop` remains independent from `stop_loss`.
- Updated scalping parameter optimizer runtime application/backfill:
  - writes full SELL contract into runtime and `params_json`: trailing enabled, arm, floor, drawdown, giveback, absolute loss, micro-profit fields, max hold.
- Added tests for:
  - tight replay thresholds staying tight;
  - independent absolute loss stop;
  - full runtime/backfill JSON contract.
- Verification:
  - `./mvnw -q test` OK after HFT V99 deploy.
- Operational evidence:
  - optimizer run `1216` produced deployable `CRVUSDC`;
  - HFT PAPER consumed the candidate and closed position `308` at `+0.08217723 USDC`.
