# Sessione 13 - DB Query Alignment Audit

Data: 2026-06-08.

## Piano 2 - Recent Loss E Config Sync Coerenti Con DB

Richiesta: verificare query DB e correggere tabelle/campi disallineati.

Interventi Docbrown:

- `run_scalping_scout.py`: `recent_loss_symbols()` ora include anche `hft.trade_position` PAPER, oltre a `hft.paper_trading_run`.
- `promote_current_scalping_probe.py`: ramo PAPER di `recent_loss_symbols()` ora include anche `hft.trade_position` PAPER.
- `run_real_candidate_refresh_batch.py`: `sync_scalping_scout_runtime_config()` usa i valori DB esatti per `max_symbols`, `probe_symbol_limit`, `ttl_minutes`; rimosso `GREATEST(...)` che impediva di abbassare i valori da DB.
- Aggiunti test sui due casi: recent loss PAPER da `trade_position` e sync config senza `GREATEST`.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe -v` OK, 61 test.
- `python3 -m py_compile` sugli script modificati OK.
- Query reali MySQL recent-loss eseguite con successo.
- Rebuild `docbrown-real-refresh:latest` OK.
- Restart `docbrown-real-refresh` OK.

Risultato runtime:

- Renewal partito da DB per simboli attivi sotto soglia.
- Universo USDC coerente.
- Nessun candidato REAL promosso nel ciclo osservato (`selectedSymbols=[]`).
- Nessuna REAL RUN avviata.

## Piano 3 - Candidate Dataset Streaming Refinement

Data: 2026-06-09.

Richiesta: generare dati utili per soglie PAPER profittevoli, anche simulando una giornata/finestra ampia di acquisti.

Interventi Docbrown:

- `run_scalping_scout_candidate_dataset.py`: `build_sell_policy_outcomes()` ora persiste gli outcome SELL a batch durante l'elaborazione usando `sell_policy_insert_batch_size` DB.
- `train_sell_policy_thresholds()` ora legge la lista policy e allena una policy per volta, evitando di caricare in RAM 6,9M outcome.
- Il training SELL mantiene il resume sui threshold gia' persistiti.

Verifiche:

- `python3 -m py_compile python/docbrown_ml/scripts/run_scalping_scout_candidate_dataset.py` OK.
- Rebuild `docbrown-real-refresh:latest` OK.
- Restart `docbrown-real-refresh` OK.

Risultato dataset 19:

- Dataset BUY: `23121` osservazioni, `6297` win, `16824` loss, net return `-52.49648397`.
- Outcome SELL: `6936300` righe persistite.
- Policy trainate: `300`.
- `READY_FOR_PAPER=0`; tutte `WATCH_TRAIN_EV`.
- Migliore policy: `pt=0.004|sl=0.008|hold=5|grace=0`, all EV `-0.00206971`, test EV `-0.00225929`.

Conclusione:

- Nessun pattern profittevole nella griglia corrente su questa finestra.
- Il risultato corretto e' fail-closed: non promuovere soglie e non avviare REAL.

## Piano 4 - Trailing Dinamico Per Candidate SCALPING_SCOUT

Data: 2026-06-11.

Richiesta: collegare le soglie del trailing dinamico ai parametri di esecuzione/filtro usati, partendo dalle evidenze optimizer `949` e `951`, e ottimizzarle per massimizzare il guadagno senza abbassare i guard.

Interventi Docbrown:

- `params_for()` ora calcola soglie trailing per-candidato con `dynamic_profit_trailing_params()`.
- I parametri derivano da `profit_target`, `stop_loss`, `min_profit_target`, `fee_rate`, `execution_slippage_rate` e replay edge medio (`replay_net_return / replay_trades`).
- I `params_json` includono `dynamic_profit_trailing`, `dynamic_profit_trailing_replay_average` e `dynamic_profit_trailing_round_trip_cost`.
- Con config delle run `949`/`951` (`pt=0.004`, `sl=0.005`, fee `0.00095`, slippage `0`) il calcolo produce:
  - `OPGUSDC` run `949`: arm/floor `0.003`, drawdown `0.001`, giveback `0.004`.
  - `HEMIUSDC` run `951`: arm/floor `0.003`, drawdown circa `0.00065457`, giveback `0.003`.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer -v` OK, 35 test.

Conclusione:

- Docbrown non copia piu' soglie trailing piatte dalla config DB nei parametri HFT: le pinna per-candidato in modo coerente con costi, target/stop e robustezza replay.
- Nessuna REAL RUN avviata.

## Piano 5 - Realtime-Only E Data Quality Reason

Data: 2026-06-11.

Contesto:

- Le run `952+` restituivano molti `NO_SYMBOLS`.
- L'audit Influx ha mostrato intervalli da `8670` punti su 30 minuti, pari a `289 simboli * 30 minuti`: era il repair storico USDC, non il realtime sano.
- Docbrown leggeva anche le serie storiche taggate `base`/`quote`; HFT runtime invece le escludeva gia'.

Interventi Docbrown:

- `InfluxTickRepository.symbols()`, `ticks()` e `latest_timestamp()` leggono solo serie realtime non taggate: `not exists r.base and not exists r.quote`.
- `run_scalping_scout.py` ora produce `dataQuality` con `NO_REALTIME_DATA`, `SPARSE_REALTIME_SYMBOLS` o `DATA_QUALITY_OK`.
- `run_scalping_parameter_optimizer.py` conserva i motivi data-quality quando non ci sono simboli selezionati, evitando di salvare tutto come `NO_SYMBOLS`.
- Aggiunti test sui filtri Flux realtime-only e sulle tre classi data-quality.

Verifiche:

- `PYTHONPATH=python python3 -m unittest discover -s python/tests -v` OK, 216 test.
- `python3 -m py_compile` sui file modificati OK.
- Rebuild e recreate `docbrown-real-refresh:latest` OK.

Evidenze runtime post-fix:

- Influx ultimi 5 minuti: `719` simboli realtime non taggati, `36` simboli storici taggati durante repair.
- Run optimizer pre-fix: `994`-`996` ancora `NO_SYMBOLS`.
- Run optimizer post-fix: `997` e `998` `DEPLOYABLE`, `288` simboli USDC probed, `6/6` trade replay, candidato `ZROUSDC`.
- Nessuna REAL RUN avviata.
