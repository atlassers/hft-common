# Sessione 15 - SCALPING_SCOUT Momentum5 Cap

Data: 2026-06-11.

Obiettivo:

- Permettere a DocBrown di promuovere profili `TREND_PULLBACK` che richiedono `momentum5 <= soglia`, non solo floor minimi.
- Allineare i parametri promossi al nuovo guard HFT `runtime_entry_momentum5_max`.

Interventi:

- Aggiunta Flyway `V23__add_scalping_scout_momentum5_cap.sql` su `hft.scalping_scout_runtime_config`.
- `run_scalping_scout.py`:
  - auto-allinea la colonna `runtime_entry_momentum5_max`;
  - la legge in `ScoutConfig`;
  - la usa in `candidate_passes`;
  - la salva in `params_json` come `momentum5_max`.
- Aggiunto test `test_candidate_rejects_momentum5_above_runtime_cap`.

Validazione:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout` OK, 20 test.

Evidenza operativa:

- I profili `TREND_PULLBACK` stretti hanno dato `PAPER_WAIT` sul live corrente, ma ora la chain puo' rappresentare correttamente il vincolo `momentum5<=0`.
- I profili PAPER con replay positivo hanno mostrato che frizione tick e stop grace sono determinanti: senza `tick_rate <= stop_loss` e grace `0`, le perdite reali superano la soglia target.

## 2026-06-11 - Profit Feature Set Gate Last-Hour

Obiettivo:

- Collegare la promotion DocBrown ai profit set persistiti da replay last-hour, usando solo soglie DB-backed e profili symbol-level con gross profit superiore alle gross loss.

Interventi:

- Aggiunta Flyway `V24__add_profit_feature_set_gate_runtime.sql`.
- `run_scalping_scout.py`:
  - legge `profit_feature_set_*` da `hft.scalping_scout_runtime_config`;
  - carica i profili attivi da `hft.scalping_scout_profit_feature_set`;
  - filtra i candidati live sulle condizioni JSON del profilo;
  - supporta condizioni `>=` e `<=` su `momentum5`, `momentum15`, `trend`, `volume_ratio`, replay, volume e frizioni;
  - riporta nel JSON `profitFeatureSetGateEnabled` e `profitFeatureSetRejectedSymbols`.
- Fix soglie runtime persistite:
  - con cap `runtime_entry_momentum5_max`, `momentum5_min` resta al floor DB;
  - con gate profit-set, `momentum15_min` e `trend_min` restano ai floor DB invece di diventare il valore puntuale del candidato.

Evidenza:

- Source `last_hour_high_gain` persistita con `12` profili; DocBrown ha promosso `KMNOUSDC`, ma HFT ha bloccato la BUY per decadimento live del momentum/freshness.
- Run successiva con dati sani (`288/288`, `51840` tick) ha prodotto `PAPER_WAIT` con `evaluated=0`; non risultano problemi Influx nella finestra.

Validazione:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout` OK, `25` test.
- `docker compose --env-file ../hft/docker/vpn/.env build docbrown-real-refresh` OK.
