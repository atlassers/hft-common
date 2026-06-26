# Sessione 16 - Best Winner Window Model

## Piano 19 - Signature Model Multi-Finestra

Obiettivo: derivare firme operative dai best winner osservati su una finestra massima parametrica, validandole su finestre piu' brevi prima della promozione.

Modifiche:

- aggiunta migration `V25__add_best_winner_window_model.sql`;
- aggiunto script `python/docbrown_ml/scripts/run_best_winner_window_model.py`;
- integrato il batch `run_real_candidate_refresh_batch.py` con step `bestWinnerWindow`;
- aggiunti test `python/tests/test_best_winner_window_model.py`.

Pipeline:

1. legge `hft.best_winner_window_config`;
2. deriva finestre `memory/discovery/regimeFast/regimeSlow/economics/kill/freshBuy`;
3. carica osservazioni da `scalping_scout_candidate_observation`;
4. estrae top winner nella discovery window;
5. costruisce firme simbolo con feature primarie `momentum15`, `trend`, `volume_ratio`, `momentum5` e replay;
6. promuove solo firme positive su regime/economia e non degradate su kill;
7. persiste su `best_winner_signature` e `scalping_scout_profit_feature_set`.

Verifiche:

- `python3 -m py_compile python/docbrown_ml/scripts/run_real_candidate_refresh_batch.py python/docbrown_ml/scripts/run_best_winner_window_model.py` OK;
- `PYTHONPATH=python python3 -m unittest python.tests.test_best_winner_window_model python.tests.test_scalping_scout` OK, `28` test.

Evidenza live PAPER:

- dataset `33`: `25946` osservazioni, `288` simboli;
- top-winner model `NO_SIGNATURES`;
- causa: le firme candidate avevano discovery positiva ma non conferma economica su `60m` e/o regime su `3h/6h`;
- comportamento corretto: nessuna promozione e nessun candidato scout forzato.

## Piano 20 - Refinement Parametri Best-Winner

Obiettivo: trovare parametri operativi per la prossima PAPER RUN usando replay/model validation sulle osservazioni salvate, senza avviare una run dal backend.

Modifiche:

- aggiunto `python/docbrown_ml/scripts/run_best_winner_window_refinement.py` come tool di analisi offline sulle osservazioni salvate;
- aggiunti test `python/tests/test_best_winner_window_refinement.py`;
- verificato che il refinement ausiliario deve essere subordinato al modello runtime reale, perche' `top_winner_limit` e la generazione effettiva delle firme possono cambiare il risultato.

Evidenze su dataset `31`, `32`, `33`:

- `top_winner_limit=80` tagliava via troppe firme utili e produceva `NO_SIGNATURES` sul dataset `33`;
- `top_winner_limit=0` fa valutare tutti i top winner sopra percentile e ripristina firme READY;
- il profilo corto `120m / percentile 0.90 / PF 1.5` e' il piu' difensivo: netto `1.62271414`, gross profit `1.79336326`, gross loss `-0.17064912`, `63` firme READY aggregate;
- il profilo operativo scelto e' `360m / percentile 0.90 / PF 1.5 / topWinnerLimit=0 / minBest=1`: netto `2.37586070`, gross profit `2.69123333`, gross loss `-0.31537263`, `209` firme READY aggregate su `31/32/33`;
- il profilo a PF `1.2` massimizza appena di piu' il netto (`2.39751993`) ma quasi raddoppia la gross loss (`-0.57357203`), quindi non e' stato scelto.

Config DB applicata su `hft.best_winner_window_config`:

- `economics_window_minutes_override=360`;
- `regime_slow_window_minutes_override=360`;
- `top_winner_percentile=0.90`;
- `top_winner_limit=0`;
- `min_best_winner_samples=1`;
- `min_discovery_samples=1`;
- `min_regime_samples=1`;
- `min_profit_factor=1.5`.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_best_winner_window_refinement python.tests.test_best_winner_window_model` OK, `6` test;
- dry-run runtime reale su dataset `33`: `status=READY`, `evaluated=227`, `ready=96`, finestre `memory=2880`, `discovery=1440`, `regimeFast=360`, `regimeSlow=360`, `economics=360`, `kill=20`;
- persistite `96` firme su `best_winner_signature` e `scalping_scout_profit_feature_set` per dataset `33`;
- nessuna PAPER RUN avviata da backend; prossimo step: avvio manuale da FE `/best-winner`.

## Piano 21 - Contratto Parametri Per BUY Microbar 5s

Obiettivo: rendere esplicita nei parametri generati da DocBrown la separazione tra analisi `1m` e validazione BUY HFT su microbarre `5s`.

Regola strategica:

- DocBrown continua a estrarre segnali profittevoli dal bucket `binance`, con barre chiuse `1m`.
- HFT valida l'ingresso usando il bucket `binance-microbar`, con barre `5s`.
- HFT gestisce SELL/trailing/max-loss sul realtime.

Modifiche:

- `run_scalping_scout.py` aggiunge in ogni `params_json`:
  - `analysis_data_source=binance`;
  - `analysis_bar_seconds=60`;
  - `entry_data_source=binance-microbar`;
  - `entry_microbar_seconds=5`;
  - `runtime_strategy_rule=DocBrown selects profitable 1m signatures; HFT validates BUY on 5s microbars and manages SELL on realtime ticks.`
- `test_scalping_scout.py` verifica che il contratto sia presente nei parametri.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout` OK, `30` test.

Nota operativa:

- La prossima PAPER RUN deve rigenerare parametri DocBrown dopo questa modifica; parametri gia' esistenti non dimostrano la nuova BUY validation.
