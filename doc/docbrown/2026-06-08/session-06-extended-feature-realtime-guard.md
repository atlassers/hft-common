# Sessione 6 - Extended Feature Realtime Guard

## Piano 1 - Refit Esteso Dataset 18

Data: 2026-06-08.

Obiettivo: chiarire e potenziare il processo `offline training -> DB promotion -> realtime guard` senza avviare REAL RUN.

Modifiche:

- `run_scalping_scout_candidate_dataset.py` legge da DB `threshold_feature_set_csv`.
- La ricerca soglie non e' piu' limitata a trend, quote volume, replay net return e replay trades.
- Feature abilitate sul DB operativo: `trend,fast_quote_volume,replay_net_return,replay_trades,momentum5,volume_ratio`.
- La griglia usa `threshold_quantile_count=4` e `threshold_search_max_combinations=20000`.
- Le soglie estese vengono salvate su BUY threshold run e SELL policy threshold run.

Esecuzione:

- Dataset: `18`.
- Osservazioni BUY: `25096`.
- Outcome SELL simulati: `7496100`.
- Policy SELL valutate: `300`.
- Combinazioni per policy: `16384`.
- Output: `target/real_ready_search_05_extended_features.json`.

Risultato:

- BUY threshold: `WATCH_WIN_RATE`, `appliedToRuntime=false`.
- SELL policy: `readyCount=0`.
- Status SELL: `WATCH_TRAIN_EV=300`.
- Migliore SELL per all expected value: `pt=0.003|sl=0.008|hold=5|grace=2`, `all_expected_value=-0.00503872`, `test_expected_value=-0.00484642`, `test_profit_factor=0.04042711`.
- Migliore policy nel report robusto: `pt=0.003|sl=0.008|hold=20|grace=2`, `all_expected_value=-0.00512365`, `test_expected_value=-0.00462900`, `profit_factor=0.06185697`.

Decisione:

- Nessun pattern e' promuovibile.
- La REAL RUN resta non autorizzata e non pronta.
- Il risultato indica che aggiungere soglie multidimensionali semplici non basta: il problema e' probabilmente assenza di edge nel segnale/uscita su questa finestra con i costi correnti, non solo mancanza di un gate.
