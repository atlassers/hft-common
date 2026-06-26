# Session 03 - DB-backed profit factor cap

Data: 2026-06-07 18:10 CEST

## Obiettivo

Rimuovere il sentinel hardcoded `profitFactor=99.0` dal training centroid/SELL-policy.

## Modifiche

- `DatasetConfig` legge `profit_factor_no_loss_cap` da `scalping_scout_candidate_dataset_config`.
- Le statistiche `numpy_stats`, `accumulator_stats` e `stats` usano il cap DB quando ci sono profitti ma zero loss.
- Il cap influenza sia threshold BUY sia threshold BUY x SELL-policy.

## Verifica

- `python3 -m py_compile` OK.
- Unit subset centroid/scalping OK.
