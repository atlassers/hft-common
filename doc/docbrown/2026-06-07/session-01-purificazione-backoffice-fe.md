# Sessione 1 - Purificazione Backoffice FE

## Piano 1 - Rimozione Endpoint Backoffice FE

User story: `DOC-S01-P01-US-001`.

Decisioni:

- Rimossi controller, DTO, service e test backoffice introdotti per integrazione FE.
- Gli aggregati/config backoffice restano responsabilita' di Kenshiro, che legge direttamente lo schema `back_test`.
- Lasciati invariati gli endpoint Docbrown preesistenti.

Verifiche:

- `./mvnw test`: 6 test OK.
- `rg "Backoffice|backoffice/config|/backoffice|dto.backoffice|service.Backoffice" hft/src docbrown/src`: nessun riferimento residuo in HFT/Docbrown.

Esito: completato.

## Piano 2 - Restart Runtime Locale

User story: `DOC-S01-P02-US-002`.

Decisioni:

- Docbrown riavviato come processo locale da package Quarkus su porta `8083`.
- Il runtime usa il datasource configurato in `application.yml`, attualmente `jdbc:mysql://localhost:3306/hft`.
- Le migrazioni Docbrown risultano applicate sulla tabella Flyway dedicata `docbrown_schema_history` fino a V19.

Verifiche:

- `./mvnw -DskipTests package`: OK.
- Processo Java attivo con PID registrato in `/tmp/docbrown.pid`.
- Porta `8083` in ascolto.

Esito: completato.

## Piano 3 - Esclusione Perdite REAL Da Promotion

User story: `DOC-S01-P03-US-003`.

Problema rilevato:

- `ALLOUSDC` e' stata ripromossa come `REAL_ELIGIBLE` nonostante perdite REAL recenti.
- Il promotore tattico escludeva solo loser PAPER interrogando `hft.paper_trading_run`.
- Le perdite REAL persistono invece in `hft.trade_position`.

Decisioni:

- Aggiunto `--exclude-recent-loss-hours` a `promote_current_scalping_probe.py`.
- Mantenuto `--exclude-recent-paper-loss-hours` come alias compatibile.
- In `execution_mode=REAL`, il filtro esclude simboli presenti in una UNION tra:
  - `hft.paper_trading_run` con chiusura netta negativa recente.
  - `hft.trade_position` con `execution_mode='REAL'`, `status='CLOSED'` e `net_profit_quote < 0`.
- In `execution_mode=PAPER`, il filtro resta scoped a `hft.paper_trading_run`.

Verifiche:

```text
PYTHONPATH=python python3 -m unittest python.tests.test_promote_current_scalping_probe
Ran 21 tests in 0.007s
OK
```

Esito: completato.

Verifica runtime collegata:

```text
ALLOUSDC latest shadow decision:
status=BLOCKED
execution_mode=REAL
live_eligible=false
valid_until=2026-06-03 06:08:29.512000 UTC
reasons_json=["RECENT_REAL_LOSS_COOLDOWN"]
```

Nota operativa:

- La remediation DB e' stata applicata per allineare subito lo stato runtime alla nuova regola; i refresh successivi applicano lo stesso criterio via codice.

## Piano 4 - Batch Docker Refresh REAL Da DB

User story: `DOC-S01-P04-US-004`.

Problema:

- Il refresh candidate REAL non deve essere uno step manuale o un processo host separato.
- I parametri operativi del refresh non devono essere passati al container in fase di creazione.

Decisioni:

- Creato un batch Python containerizzato `docbrown-real-refresh`.
- Il batch legge `hft.real_trading_runtime_config` a ogni ciclo.
- Env Docker limitate a connessioni e secret: MySQL e Influx.
- Se `refresh_enabled=false`, il batch resta vivo e dorme senza promuovere candidate.
- Quando abilitato, esegue `promote_current_scalping_probe.py` con criteri presi dal DB e ripete secondo `refresh_interval_seconds`.

Verifiche:

```text
python3 -m py_compile python/run_real_candidate_refresh_batch.py python/docbrown_ml/scripts/run_real_candidate_refresh_batch.py
PYTHONPATH=python python3 -m unittest python.tests.test_promote_current_scalping_probe
Ran 21 tests in 0.008s
OK
docker compose --env-file ../hft/docker/vpn/.env build docbrown-real-refresh
Service docbrown-real-refresh Built
```

Esito: completato.

## Piano 5 - Confidence Mode BALANCED Per Promotion REAL

User story: `DOC-S01-P05-US-005`.

Problema:

- La promotion REAL `STRICT` puo' restare in `REAL_WAIT` quando non trova simboli sopra soglia hard.
- Serve accettare setup 2/3 o 3/4 win senza tornare a candidate permissive o non profittevoli.

Decisioni:

- Aggiunto `--confidence-mode` con valori `STRICT` e `BALANCED`.
- `STRICT` conserva le soglie esistenti.
- `BALANCED` accetta un canale alternativo se il campione ha:
  - almeno 3 trade;
  - almeno 2 win;
  - win rate minimo 0.66666667;
  - net profit positivo;
  - profit factor minimo 1.2;
  - average trade return minimo 0.0015.
- La stessa logica vale anche per la probation recente, cosi' un 2/3 positivo non viene bloccato automaticamente da `min_probation_win_rate=0.75`.

Verifiche:

```text
python3 -m py_compile python/run_real_candidate_refresh_batch.py python/docbrown_ml/scripts/run_real_candidate_refresh_batch.py python/docbrown_ml/scripts/promote_current_scalping_probe.py
PYTHONPATH=python python3 -m unittest python.tests.test_promote_current_scalping_probe
Ran 24 tests in 0.006s
OK
```

Esito: completato.

## Piano 6 - Probe Universe USDC Prima Del Limit Finale

User story: `DOC-S01-P06-US-006`.

Problema:

- Anche con `confidence_mode=BALANCED`, Docbrown restava in `REAL_WAIT`.
- Il report mostrava `probedSymbols` limitati alla prima fascia alfabetica A/B.
- Root cause: `InfluxTickRepository.symbols(..., limit=120)` applicava il limit prima del filtro quote; dopo il filtro `USDC` restavano circa 47 simboli, non tutto l'universo desiderato.

Decisioni:

- Per la promotion tattica, caricare un pool grezzo 10x prima del filtro quote.
- Applicare `probe_symbol_limit` solo dopo il filtro `USDC`, cosi' il limite resta il numero finale di simboli candidabili.
- Applicare la stessa logica anche alla sync live bucket.

Verifiche:

```text
python3 -m py_compile python/docbrown_ml/scripts/promote_current_scalping_probe.py
PYTHONPATH=python python3 -m unittest python.tests.test_promote_current_scalping_probe
Ran 24 tests in 0.006s
OK
```

Esito: completato.

## Piano 7 - Probation Neutrale Per BALANCED

User story: `DOC-S01-P07-US-007`.

Problema:

- `confidence_mode=BALANCED` accettava piccoli campioni positivi sul periodo principale, ma la probation ultra-recente poteva azzerarli quando la finestra recente era sterile o con pochi trade non negativi.
- Questo rendeva possibile restare in `PAPER_WAIT`/`REAL_WAIT` anche con evidenze 2/3 o 3/4 positive sul periodo corrente.

Decisioni:

- In `BALANCED`, una probation con meno trade di `balanced_min_trades` viene trattata come neutrale solo se:
  - il candidato principale passa i criteri balanced;
  - la probation non contiene loss;
  - il net profit probation non e' negativo.
- Una probation con loss recente o net negativo continua a bloccare il simbolo.
- Il report ora espone `probationNeutral` e `probationRejectReason` sui simboli.

Verifiche:

```text
PYTHONPATH=python python3 -m unittest python.tests.test_promote_current_scalping_probe
Ran 26 tests in 0.009s
OK

PYTHONPATH=python python3 -m py_compile python/docbrown_ml/scripts/promote_current_scalping_probe.py python/docbrown_ml/scripts/run_real_candidate_refresh_batch.py
OK

docker compose --env-file ../hft/docker/vpn/.env build docbrown-real-refresh
OK
```

PAPER check operativo:

- PAPER tattica mirata con `evaluation-hours=6`: `PAPER_WAIT`, `evaluatedRows=0`, nessuna candidate.
- PAPER tattica mirata con `evaluation-hours=12`: `PAPER_WAIT`, `evaluatedRows=0`, nessuna candidate.
- PAPER full scan 120 senza sync: timeout a 240s, nessuna candidate persistita.
- Esito: REAL non avviata perche' la PAPER non ha prodotto un risultato corretto.

Esito: completato lato codice; REAL non eseguita.

## Piano 8 - Scalping Scout PAPER-Only

User story: `DOC-S01-P08-US-008`.

Problema:

- Il ciclo tattico storico puo' restare in `PAPER_WAIT` quando non ci sono trade validati nella finestra, anche se il mercato live ha micro-momentum comprabile.
- Non e' accettabile promuovere in REAL senza una prova PAPER misurabile.

Decisioni:

- Creato `run_scalping_scout.py`, PAPER-only, che legge configurazione da `hft.scalping_scout_runtime_config`.
- Lo scout legge direttamente Influx e DB, non contatta HFT o Docbrown via HTTP.
- Seleziona candidati USDC usando momentum 5/15m, trend, volume ratio, volatilita' e distanza dal minimo recente.
- Persiste artifact minimi ma coerenti per `ML_WALK_FORWARD`, record `hft.stan_strategy_parameters`, `back_test.shadow_trading_decision` in `PAPER_PROBATION` e una policy market-regime run-specific.
- Deattiva solo i precedenti parametri `SCALPING_SCOUT`, senza toccare candidate REAL esistenti.

Verifiche:

```text
PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_promote_current_scalping_probe
Ran 29 tests in 0.007s
OK

PYTHONPATH=python python3 -m py_compile python/docbrown_ml/scripts/run_scalping_scout.py python/run_scalping_scout.py
OK

docker compose --env-file ../hft/docker/vpn/.env build docbrown-real-refresh
OK
```

Esito: completato lato codice; PAPER run da avviare dopo commit/push.

Dry-run operativo prima del commit:

```text
PYTHONPATH=python python3 python/run_scalping_scout.py --config /tmp/scalping-scout.yml --dry-run
operationalStatus: PAPER_READY
selectedSymbols: HEIUSDC, EPICUSDC, 1000CATUSDC, AUSDC, ENJUSDC
persisted.strategyParameters: 0
```

Nota schema: il DB corrente usa `hft.paper_trading_run.net_profit_quote` e non espone ancora `execution_mode`; il cooldown loss dello scout rileva le colonne da `INFORMATION_SCHEMA`.

## Piano 9 - Scout Fee-Aware Con Replay E Frizioni

User story: `DOC-S01-P09-US-009`.

Problema:

- La PAPER scout precedente ha comprato spike gia' maturi e ha chiuso net negative.
- Il filtro di selezione non scartava ex-ante strumenti con frizioni Binance sfavorevoli o replay recente non profittevole dopo fee.

Decisioni:

- Lo scout ora legge da DB anche:
  - `assumed_quote_budget`
  - `max_buy_tick_rate`
  - `max_buy_dust_rate`
  - `replay_lookback_minutes`
  - `replay_stride_minutes`
  - `min_replay_trades`
  - `min_replay_win_rate`
  - `min_replay_net_return`
- Prima di persistere, ogni candidato deve passare:
  - friction check Binance `tickSize/price` e dust stimato su budget configurato;
  - replay recente fee-aware con target/stop/max-hold correnti;
  - soglia minima di trade, win-rate e net return nel replay.
- I record persistiti usano le metriche reali del replay invece del precedente 2/3 sintetico.

Verifiche:

```text
PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_promote_current_scalping_probe
Ran 32 tests
OK

PYTHONPATH=python python3 python/run_scalping_scout.py --config /tmp/scalping-scout.yml --dry-run
operationalStatus: PAPER_READY
selectedSymbols: IOUSDC, DYDXUSDC
IOUSDC replay: 3/4, net 0.0015463973269737369, tickRate 0.0005455537370430988, dustRate 0.00004458823529403128
DYDXUSDC replay: 4/5, net 0.0025485150579443137, tickRate 0.00005488172987212557, dustRate 0.00006010196078425978
```

Esito: pronto per PAPER dopo commit/push; REAL resta subordinata ad autorizzazione esplicita.

## Piano 10 - Scout ASCII E Robust Gate Runtime

User story: `DOC-S01-P10-US-010`.

Problema:

- Durante un loop robusto e' stato promosso un simbolo Binance non ASCII, rappresentato dal client MySQL come `????USDC`.
- Il simbolo era formalmente profittevole nel replay ma non deve entrare nella pipeline HFT, perche' rompe leggibilita', controlli operativi e tracciabilita' FE/DB.
- La validation `180` e i relativi parametri sono stati disattivati prima di qualsiasi HFT run.

Decisioni:

- Lo scout accetta solo simboli `^[A-Z0-9]+$` che terminano con la quote configurata.
- La validazione avviene prima di calcolare metriche, replay e persistenza.
- I gate robusti ora restano quelli a DB:
  - `momentum5_min=0.00500000`
  - `volume_ratio_min=1.20000000`
  - `min_replay_win_rate=0.75000000`
  - `min_replay_net_return=0.00200000`

Verifiche:

```text
PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_promote_current_scalping_probe
Ran 33 tests in 11.308s
OK
```

Esito runtime:

- Loop PAPER-only dopo MS395: 8 tentativi, nessun candidato persistito.
- Report finale: `operationalStatus=PAPER_WAIT`, `selectedSymbols=[]`, `evaluated=0`, `probedSymbols=289`.
- Nessuna REAL RUN avviata; la REAL resta subordinata ad almeno una PAPER positiva e ad autorizzazione esplicita utente.

## Piano 11 - SCALPING_SCOUT Parameter Optimizer

User story: `DOC-S01-P11-US-011`.

Problema:

- I gate robusti evitavano BUY ma non esisteva un tuning automatico e tracciabile dei parametri.
- `binance-history` risultava stale alle 12:07 CEST e limitato a 120 simboli USDC, mentre `binance` live era aggiornato e conteneva circa 290 simboli USDC.

Decisioni:

- Aggiunto `run_scalping_parameter_optimizer.py`.
- Il batch valuta una griglia DB-driven su:
  - `momentum5_min`
  - `volume_ratio_min`
  - `min_replay_win_rate`
  - `min_replay_net_return`
  - `profit_target`
  - `stop_loss`
  - `replay_lookback_minutes`
- Se `binance-history` non produce candidati, il batch fa fallback controllato sul bucket live `binance` e lo registra nel report/DB.
- Il batch applica la config solo se:
  - esistono simboli;
  - `trades >= 4`;
  - `win_rate >= 0.70`;
  - `net_return > 0`;
  - `average_trade_return > 0`.
- `docbrown-real-refresh` ora esegue l'optimizer all'inizio di ogni ciclo prima del refresh candidate.

Verifiche:

```text
PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe
Ran 36 tests
OK

PYTHONPATH=python python3 -m py_compile python/docbrown_ml/scripts/run_scalping_parameter_optimizer.py python/run_scalping_parameter_optimizer.py python/docbrown_ml/scripts/run_real_candidate_refresh_batch.py
OK
```

Esito runtime:

- Primo tuning su `binance-history`: `NO_SYMBOLS`.
- Tuning esteso con fallback live: `DEPLOYABLE`, applicato a DB.
- Run id: `3`.
- Best symbols: `DASHUSDC`, `STRKUSDC`, `PUMPUSDC`.
- Aggregate replay: `11` trade, `9` win, win rate `0.81818182`, net return `0.004340898138694653`.
## Piano 8 - Preserva Candidate REAL Su Cicli Vuoti

Data: 2026-06-03 17:13 CEST

Obiettivo:

- Evitare che un refresh Docbrown `REAL_WAIT`/`NO_SYMBOLS` svuoti HFT disattivando tutte le candidate REAL ancora valide.

Intervento:

- Corretto `promote_current_scalping_probe.py`: `deactivate_other_hft_symbols([])` ora e' no-op.
- La disattivazione degli altri simboli resta attiva solo quando esiste almeno un nuovo set selezionato che puo' sostituire quello precedente.
- Aggiunto test dedicato per impedire regressioni.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_promote_current_scalping_probe` -> 27 OK.
- `python3 -m py_compile python/docbrown_ml/scripts/promote_current_scalping_probe.py` OK.

## Piano 9 - Disattiva Candidate In Recent Loss

Data: 2026-06-03 18:08 CEST

Obiettivo:

- Evitare che una candidate REAL appena chiusa in perdita resti attiva fino a TTL anche se HFT la rigetta correttamente per cooldown.

Intervento:

- `promote_current_scalping_probe.py` disattiva in modo mirato i simboli in recent-loss nello stesso `pipeline_stage`.
- La protezione del Piano 8 resta valida: una selezione vuota non svuota tutti i parametri attivi.
- Aggiunti test per scope `pipeline_stage` e no-op su lista vuota.

Verifiche previste:

- `PYTHONPATH=python python3 -m unittest python.tests.test_promote_current_scalping_probe`.
- Build/restart `docbrown-real-refresh`.

## Piano 10 - Timeout Refresh REAL Da DB E Optimizer Recent-Loss

Data: 2026-06-03 19:25 CEST

Problema:

- Il batch `docbrown-real-refresh` andava in timeout dopo `600s` durante `promote_current_scalping_probe.py`.
- La causa era il riuso di `refresh_interval_seconds` come timeout di processo: con `probe_symbol_limit=720` il calcolo puo' essere piu' lungo dell'intervallo nominale.
- L'optimizer `run_scalping_parameter_optimizer.py` poteva inoltre ripromuovere un simbolo in perdita REAL recente, perche' il filtro recent-loss era presente nella promotion tattica ma non nella fase optimizer.

Intervento:

- `run_real_candidate_refresh_batch.py` legge `refresh_step_timeout_seconds` da DB e lo usa come timeout step; se la colonna non esiste resta compatibile con il vecchio moltiplicatore sull'intervallo.
- `safe_log_config` espone anche `refresh_step_timeout_seconds` nei log.
- `run_scalping_parameter_optimizer.py` calcola i simboli recent-loss con la stessa funzione della promotion tattica e li esclude prima della selezione/persistenza.
- Il report e `metrics_json` includono `excludedRecentLossSymbols`.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` -> 36 OK.
- `python3 -m py_compile python/docbrown_ml/scripts/run_real_candidate_refresh_batch.py python/docbrown_ml/scripts/run_scalping_parameter_optimizer.py` OK.

Aggiornamento:

- `refresh_step_timeout_seconds=0` disabilita il timeout di `subprocess.run`.
- Se la colonna non esiste, resta il comportamento legacy basato su `refresh_interval_seconds`.

## Piano 11 - Runtime Entry Thresholds Rilassate

Data: 2026-06-03 20:05 CEST

Problema:

- Lo scout selezionava candidate robuste, ma persisteva in HFT soglie minime pari al valore osservato al momento della selezione.
- In REAL questo produceva reject continui appena il simbolo usciva dal picco istantaneo.

Intervento Docbrown:

- Aggiunti parametri DB su `hft.scalping_scout_runtime_config`:
  - `runtime_entry_momentum_ratio=0.70`
  - `runtime_entry_trend_ratio=0.50`
  - `runtime_entry_volume_ratio_min=0.70`
  - `runtime_entry_pullback_multiplier=1.50`
- La selezione Docbrown resta invariata e robusta.
- La persistenza HFT usa soglie live rilassate; i valori candidati originali restano in `params_json` come `candidate_*`.


Aggiornamento Piano 11:

- Aggiunto gate di quote volume assoluto per evitare candidate con volume relativo alto ma book/flusso reale troppo sottile.
- Nuovo parametro DB `runtime_entry_min_quote_volume_multiplier`, default `10.0`.
- Docbrown richiede `fast_quote_volume >= assumed_quote_budget * runtime_entry_min_quote_volume_multiplier` sia nella selezione corrente sia nel replay.
- HFT riceve `min_quote_volume` in `params_json` e lo ricontrolla live prima di accettare BUY.


## 2026-06-03 21:12 CEST - MS414 stop-loss grace nei parametri scout

- Aggiunto `stop_loss_grace_seconds` alla config runtime scout, default `120` secondi.
- `run_scalping_scout.py` crea/auto-allinea la colonna se manca, la legge da DB e la persiste in `stan_strategy_parameters.params_json`.
- Aggiunta migration Docbrown `V20__add_scalping_scout_stop_loss_grace.sql` per mantenere coerente il contratto DB lato modulo.
- Test: `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout` OK; `python3 -m py_compile python/docbrown_ml/scripts/run_scalping_scout.py` OK.

## 2026-06-03 - Allineamento refresh REAL su optimizer

Contesto:
- La REAL RUN era attiva ma senza nuovi BUY: HFT riceveva buffer da Influx, mentre `docbrown-real-refresh` chiudeva i cicli con `REAL_WAIT` dopo l'optimizer.
- Il batch eseguiva in sequenza optimizer, scout legacy e promotion legacy; un ciclo legacy vuoto rendeva poco chiara la sorgente effettiva dei candidati e poteva mascherare l'output optimizer.

Intervento:
- `run_real_candidate_refresh_batch.py` sincronizza `hft.scalping_scout_runtime_config` dai valori DB REAL (`max_symbols`, `probe_symbol_limit`, `ttl_minutes`) prima di lanciare l'optimizer.
- Se l'optimizer termina con exit `0`, il batch considera applicati i candidati REAL e salta scout/promotion legacy.
- Scout/promotion legacy restano fallback solo quando l'optimizer fallisce.
- Aggiunti test per il comportamento fallback.

Verifiche:
- `PYTHONPATH=python python3 -m py_compile python/docbrown_ml/scripts/run_real_candidate_refresh_batch.py python/run_real_candidate_refresh_batch.py` OK.
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` -> 39 OK.

## 2026-06-03 - Grid optimizer DB-backed per frequenza segnali REAL

Contesto:
- Il primo ciclo post-fix ha confermato che `docbrown-real-refresh` gira e invoca l'optimizer, ma la run `scalping_scout_optimizer_run` `id=24` ha prodotto `NO_SYMBOLS`.
- Il collo di bottiglia era nel grid optimizer: volume ratio candidato partiva sempre da `1.0` e replay lookback massimo `90` minuti, ignorando la soglia runtime HFT gia' rilassata a `0.7`.

Intervento:
- `config_grid` include ora i valori DB di `momentum5_min`, `volume_ratio_min` e `replay_lookback_minutes`.
- Aggiunti al grid `volume_ratio` intermedi `0.8`/`0.9` e replay lookback `120`/`180`, mantenendo min replay trades, win-rate 2/3 e net positivo.
- Aggiunto test sul fatto che il grid includa i valori DB-backed.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` -> 40 OK.

## 2026-06-03 - Riduzione costo grid optimizer REAL

Contesto:
- Il grid DB-backed introdotto per aumentare frequenza segnali includeva troppe combinazioni e il primo ciclo superava il ciclo operativo nominale.

Intervento:
- Ridotto il grid a combinazioni compatte DB-backed:
  - `momentum5_min`: valore DB, `0.0025`, `0.0050`.
  - `volume_ratio_min`: valore DB, `1.0`, `1.2`.
  - `replay_lookback_minutes`: valore DB e `90`.
- Restano invariati trade minimi, win-rate minimo, net positivo, profit target/stop loss search.

## 2026-06-03 - Fallback legacy solo se optimizer e scout falliscono

Contesto:
- Dopo `MS424`, l'optimizer compatto puo' tornare `NO_SYMBOLS` mentre `run_scalping_scout.py` riesce a persistere candidate REAL, come `NEARUSDC`.
- Il batch lanciava comunque `promote_current_scalping_probe.py`, consumando minuti su 720 simboli senza aggiungere valore.

Intervento:
- `run_real_candidate_refresh_batch.py` salta la promotion legacy anche quando lo scout persist termina con exit `0`.
- La promotion legacy resta fallback solo se optimizer e scout falliscono entrambi.

## 2026-06-04 - Ciclo REAL bounded senza fallback legacy lungo

Contesto:
- Dopo il fix HFT `MS426`, optimizer e scout possono correttamente tornare `NO_SYMBOLS`.
- In quel caso il batch lanciava comunque `promote_current_scalping_probe.py` su 720 simboli, restando CPU-bound oltre 13 minuti e superando la frequenza operativa da 10 minuti senza produrre candidati utili.

Intervento:
- `run_real_candidate_refresh_batch.py` non lancia piu' la promotion legacy quando optimizer e scout non trovano candidati REAL.
- Il ciclo resta bounded: se non ci sono candidati, logga il motivo e dorme fino al prossimo refresh.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` OK.

## 2026-06-04 - Grid optimizer bounded per ripristino segnali REAL

Contesto:
- La REAL RUN era attiva ma da piu' cicli non produceva BUY.
- HFT era operativo e Influx leggeva 720 simboli; Docbrown girava ogni ciclo e chiudeva correttamente, ma `scalping_scout_optimizer_run` tornava `NO_SYMBOLS` con `trades=0`.
- Diagnosi sui dati live/history: la maggioranza dei simboli veniva scartata da `momentum5_min=0.0015`; gli unici superstiti non arrivavano a `min_replay_trades=3` o fallivano il quote-volume minimo.

Intervento:
- Estesa la griglia bounded di `run_scalping_parameter_optimizer.py` includendo alternative DB-backed per:
  - `momentum5_min`: base, `0`, `0.0005`, `0.0015`.
  - `momentum15_min`: base, `-0.002`, `0`.
  - `volume_ratio_min`: base, `0.5`, `0.7`.
  - `runtime_entry_min_quote_volume_multiplier`: base, `2`.
  - `min_replay_trades`: base, `2`, `3`.
- `apply_runtime_config` persiste ora anche `momentum15_min`, `runtime_entry_min_quote_volume_multiplier` e `min_replay_trades`, evitando parametri impliciti non allineati al DB.
- La griglia resta bounded: dry-run reale con 432 configurazioni, circa 72 secondi.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` OK, 40 test.
- Dry-run optimizer REAL: `DEPLOYABLE`, best symbols `MMTUSDC`, `XTZUSDC`, `MEMEUSDC`, `ENAUSDC`, 15 replay trade, 13 win, 2 loss, win-rate `0.86666667`, net return `0.03276809496069114`.

## 2026-06-04 08:20 CEST - MS430 runtime entry ratio ottimizzati

Contesto:
- Dopo MS429 HFT vede correttamente tutti i candidati SCALPING_SCOUT, ma gli ingressi restano pochi.
- Analisi eventi live: WUSDC falliva `BUY_CHECK_ENTRY_TREND_REJECTED` per soglia runtime `trend_min = candidate_trend * 0.5`; XTZUSDC era al limite del quote-volume assoluto; questi rapporti erano DB-backed ma non ottimizzati dal tuner.

Intervento:
- `run_scalping_parameter_optimizer.py` include ora `runtime_entry_trend_ratio` nella griglia bounded (`0.25`, `0.5`, valore DB).
- Reintrodotto `runtime_entry_min_quote_volume_multiplier=1.0` nella griglia, oltre al valore DB e `2.0`.
- `apply_runtime_config` persiste a DB anche `runtime_entry_trend_ratio`.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` OK.
- Dry-run REAL: `DEPLOYABLE`, 96 config in circa 45s, best symbols `MMTUSDC`, `WUSDC`, `XTZUSDC`, 8 replay trade, 7 win, 1 loss, win-rate `0.875`, net return `0.029107244184157804`.
- Best config: `runtime_entry_trend_ratio=0.25`, `runtime_entry_min_quote_volume_multiplier=1.0`.

## 2026-06-04 08:35 CEST - MS431 attrito REAL nel tuner SCALPING_SCOUT

Contesto:
- La REAL RUN post-MS430 ha prodotto un'altra loss (`XTZUSDC`) da stop-loss stretto: il replay considerava parametri con `profit_target=0.0025` e `stop_loss=0.002`, ma il MARKET reale ha eseguito la SELL peggio del last price.
- Questo rendeva il replay troppo ottimistico rispetto alla microstruttura Binance.

Intervento:
- `scalping_scout_runtime_config` ora espone parametri DB-backed per attrito reale: `min_profit_target`, `min_stop_loss`, `min_stop_loss_grace_seconds`, `execution_slippage_rate`.
- `run_scalping_scout.py` carica questi valori dal DB e li usa nel replay, sottraendo anche `execution_slippage_rate` oltre alle fee round-trip.
- `run_scalping_parameter_optimizer.py` non puo' piu' proporre target/stop/grace sotto i floor DB e persiste `stop_loss_grace_seconds` quando applica una config deployable.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 08:45 CEST - MS432 validazione live finale optimizer REAL

Contesto:
- Dopo MS431 l'optimizer ha applicato `WUSDC` con parametri realistici, ma HFT ha visto subito feature non comprabili: `momentum5=0`, `momentum15=-0.00847458`, `trend=-0.00431373`, `volume_ratio=0.24288835`.
- Docbrown aveva promosso `WUSDC` dal bucket history con `candidate_momentum5=0.0078125` e volume forte: la candidate era stale rispetto al live bucket consumato da HFT.

Intervento:
- `run_scalping_parameter_optimizer.py` calcola sempre anche il risultato sul live bucket.
- Se il live bucket trova candidate, l'optimizer applica quelle.
- Se il bucket history trova candidate ma il live bucket non le conferma, la run usa il risultato live vuoto e non persiste parametri stale.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 08:55 CEST - MS434 deactivation candidate stale su NO_SYMBOLS REAL

Contesto:
- MS432 ha correttamente prodotto run `77` con `NO_SYMBOLS` quando il live bucket non confermava la candidate history.
- Tuttavia `WUSDC` della run `76` restava attiva fino a TTL, lasciando HFT libero di consumare una candidate stale.

Intervento:
- Quando `run_scalping_parameter_optimizer.py` gira con `--apply --execution-mode REAL` e la run non e' deployable, disattiva i vecchi `stan_strategy_parameters` attivi per `strategy_source=ML_WALK_FORWARD` e `model_type=SCALPING_SCOUT` se `deactivate_previous_scout` e' true.
- La deactivation resta REAL-only e DB-backed.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 09:00 CEST - MS435 momentum ratio runtime ottimizzato

Contesto:
- Run `78` ha promosso `HEIUSDC` live-confermato, ma HFT ha rigettato per `BUY_CHECK_ENTRY_MOMENTUM_REJECTED`.
- Il rigetto era marginale: `momentum5` live `0.03773585`, soglia persistita `0.03980687`; `momentum15`, trend e volume passavano.
- Il rapporto `runtime_entry_momentum_ratio=0.70` era DB-backed ma non scelto dal tuner.

Intervento:
- `run_scalping_parameter_optimizer.py` include `runtime_entry_momentum_ratio` nel grid bounded (`0.50`, `0.60`, `0.70`, valore DB).
- `apply_runtime_config` persiste a DB il rapporto scelto, allineando Docbrown e HFT anche sul momentum live.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 09:43 CEST - MS436 coerenza DB stop catastrofico e batch observable

Contesto:
- La REAL RUN ha chiuso in perdita `MEMEUSDC`, `XTZUSDC`, `AIGENSYNUSDC` e `HEIUSDC`.
- Dopo MS431 il tuner aveva introdotto floor realistici per target/stop, ma il floor `min_stop_loss_grace_seconds=600` ha reso troppo lenta l'uscita su `HEIUSDC`.
- L'optimizer produceva run replay deployable con win-rate apparente alto su campioni piccoli, ma la conferma live successiva non ha retto in REAL.

Intervento:
- `run_scalping_scout.py` aggiunge `catastrophic_stop_loss` a schema, alter difensivo, dataclass, config JSON e `params_for`.
- `min_stop_loss_grace_seconds` Docbrown passa a `120`, coerente con HFT V36.
- `ensure_runtime_config` normalizza sempre `stop_loss_grace_seconds <= 120`, `min_stop_loss_grace_seconds <= 120` e `catastrophic_stop_loss >= 0.012`.
- `run_real_candidate_refresh_batch.py` sincronizza gli stessi valori dal batch, usando solo DB/runtime e non env strategiche.
- `run_scalping_parameter_optimizer.py` persiste `catastrophic_stop_loss` quando applica una config.
- Rimossi/evitati toggle env di strategia: l'abilitazione passa da `real_trading_runtime_config.refresh_enabled` e `scalping_scout_runtime_config.enabled`.
- Il batch logga inizio optimizer/scout con timeout DB, evitando cicli apparentemente muti durante elaborazioni da 80-90 secondi.

Verifiche e stato operativo:
- Test: `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK, 24 test.
- Immagine `docbrown-real-refresh:latest` ricostruita e container ricreato.
- Primo ciclo post-V36: `SCALPING_SCOUT optimizer` e `scout` hanno prodotto `NO_SYMBOLS`; fallback legacy saltato correttamente.
- Simboli esclusi per recent REAL loss: `MEMEUSDC`, `XTZUSDC`, `AIGENSYNUSDC`, `HEIUSDC`.

Decisione:
- Non forzare candidati quando optimizer/scout tornano `NO_SYMBOLS`; il comportamento corretto e' `REAL_WAIT`/fail-closed.
- Per aumentare frequenza senza peggiorare il win-rate serve una successiva modifica di validazione out-of-sample/live shadow, non un semplice abbassamento soglie.

## Piano 12 - Cap Volume Ratio SCALPING_SCOUT

Contesto:
- L'analisi dei trade REAL HFT ha separato friction loss da market loss: la maggioranza delle loss e' di mercato, non da quota troppo bassa.
- La combinazione piu' promettente sui trade recenti e' momentum15 alto e volume ratio non spike.

Intervento:
- `run_scalping_scout.py` auto-allinea `runtime_entry_volume_ratio_max` su `hft.scalping_scout_runtime_config`.
- Il cap viene usato in selezione live, replay entry e params JSON promossi come `volume_ratio_max`.
- `run_scalping_parameter_optimizer.py` include `runtime_entry_volume_ratio_max` nella griglia e lo persiste se la configurazione e' deployable.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

### Rettifica Piano 12 - Floor DB non rilassabili

- La prima run post-deploy ha caricato una configurazione precedente a V38 e l'optimizer poteva ancora scegliere valori piu' permissivi nella griglia (`momentum15_min=-0.002`, `runtime_entry_volume_ratio_max=0`, quote multiplier `1.0`).
- Corretto `config_grid`: momentum15, cap volume ratio e quote-volume multiplier non possono essere piu' permissivi del valore DB caricato come base.
- `apply_runtime_config` resta DB-backed: non contiene soglie hardcoded, persiste la configurazione gia' vincolata dalla base DB.
- Verifica ripetuta: `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 17:45 CEST - Parametri esecuzione SCALPING_SCOUT in params JSON

Contesto:
- Il compromesso runtime ha promosso `WLDUSDC`, ma HFT ha rigettato la BUY per dust guard.
- `scalping_scout_runtime_config` conteneva `max_buy_dust_rate` e `max_buy_tick_rate`, ma i parametri promossi non li includevano in `stan_strategy_parameters.params_json`.

Intervento:
- `run_scalping_scout.py` include `max_buy_tick_rate` e `max_buy_dust_rate` in `params_for`, cosi' ogni candidate SCALPING_SCOUT porta con se' i limiti usati dalla pipeline.
- La candidate attiva `WLDUSDC` e' stata aggiornata a DB con i valori runtime correnti per evitare attesa del prossimo ciclo.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 17:55 CEST - NO_SYMBOLS non cancella candidate non scadute

Contesto:
- Dopo il restart, Docbrown ha promosso `ARKMUSDC` con run `135`, ma una run successiva `NO_SYMBOLS` ha disattivato subito tutte le candidate SCALPING_SCOUT.
- HFT ha quindi visto `activeStrategySymbols` passare da 1 a 0 prima di completare buffer e BUY.

Intervento:
- `deactivate_stale_scouts` ora disattiva solo candidate gia' scadute (`valid_until <= CURRENT_TIMESTAMP(6)`).
- Una singola run `NO_SYMBOLS` non cancella piu' candidate fresche ancora dentro TTL; il fail-closed resta garantito dalla scadenza TTL.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 18:05 CEST - Replay floor anti-slippage REAL

Contesto:
- `ENAUSDC` e' stata comprata in REAL, ma ha chiuso a `-0.353699 USDC`.
- Il replay aveva margine netto `+0.00481414`, ma il fill BUY reale ha mostrato uno scostamento di circa `0.5%` rispetto al prezzo decisionale, annullando l'edge.
- L'optimizer poteva ancora rilassare `min_replay_trades`, `min_replay_win_rate` e `min_replay_net_return` sotto il profilo DB corrente.

Intervento:
- DB runtime portato a `execution_slippage_rate=0.006`, `min_replay_net_return=0.008`, `min_replay_trades=3`, `min_replay_win_rate=0.70`.
- `config_grid` non puo' piu' scegliere valori sotto i floor DB per `min_replay_trades`, `min_replay_win_rate`, `min_replay_net_return`, `profit_target` e `stop_loss`.
- Aggiornato il test optimizer alla policy floor DB non rilassabile.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## 2026-06-04 18:40 CEST - Rimozione normalizzazioni hardcoded runtime

Contesto:
- `ensure_runtime_config` e il batch real-refresh applicavano normalizzazioni hardcoded su `scalping_scout_runtime_config` (`LEAST(...,120)`, `GREATEST(...,0.012)`).
- Anche se difensive, queste scritture violavano la policy DB-driven: i valori operativi devono arrivare dal DB/Flyway, non da runtime script.

Intervento:
- `CONFIG_NORMALIZE_SQL` non modifica piu' valori runtime.
- `sync_scalping_scout_runtime_config` non sovrascrive piu' stop/grace/catastrophic con costanti hardcoded.
- Resta solo la sincronizzazione DB-to-DB di dimensione ciclo (`max_symbols`, `probe_symbol_limit`, `ttl_minutes`) da `real_trading_runtime_config` a `scalping_scout_runtime_config`.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.

## Piano 13 - Fix Cursore MySQL E Recovery Segnali SCALPING_SCOUT

Data: 2026-06-04.

Obiettivo:
- Ripristinare un refresh Docbrown affidabile per SCALPING_SCOUT.
- Evitare che query dummy/normalizzazioni non necessarie lascino result set MySQL non consumati.
- Tornare a un profilo DB-driven piu' permissivo quando la pipeline REAL diventa sterile, senza introdurre soglie hardcoded nel codice.

Intervento:
- `run_scalping_scout.py`: rimossa la query dummy `SELECT 1` da `CONFIG_NORMALIZE_SQL`.
- `ensure_runtime_config`: esegue la normalizzazione solo se la stringa SQL e' non vuota.
- Il profilo runtime resta controllato dalla tabella `hft.scalping_scout_runtime_config`.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` -> 25 test OK.
- Container `docbrown-real-refresh` aggiornato con il fix e riavviato.

Note runtime:
- Root cause operativa osservata: errore `mysql.connector.errors.InternalError: Unread result found` durante il refresh SCALPING_SCOUT.
- Il bug impediva cicli Docbrown affidabili anche con HFT attivo.
- Successivamente, con Influxer riavviato, HFT ha rivisto `influxSymbols=720` e candidati attivi, confermando che il problema residuo era di soglie/segnali live e non di pipeline spenta.

## Piano 14 - Griglia Optimizer Bounded Per Refresh 300s

Data: 2026-06-04.

Obiettivo:
- Evitare che parametri DB permissivi generino una griglia cartesiana troppo ampia nel `SCALPING_SCOUT_TUNER`.
- Rendere il ciclo compatibile con `refresh_interval_seconds=300`.
- Conservare profili alternativi utili senza introdurre soglie operative hardcoded fuori dal DB.

Intervento:
- `run_scalping_parameter_optimizer.py`: `config_grid` non usa piu' prodotto cartesiano esteso.
- La griglia ora usa profili bounded deduplicati costruiti a partire dai valori DB e da poche varianti gia' testate.
- Restano applicati i floor DB per profit target, stop loss, grace, quote volume e replay.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` -> 25 test OK.
- Container `docbrown-real-refresh` aggiornato e riavviato con image commit `14d54077b0b9734157309feb3b3109b82b6d1bd5c6616df4ef14b4f75a98e72b`.

## Piano 15 - Fix Selezione Bucket Live Vuoto

Data: 2026-06-04.

Obiettivo:
- Evitare che un bucket live senza candidati sovrascriva candidati validi calcolati sul bucket storico.
- Consentire a HFT di fare il controllo live finale sui parametri promossi.

Intervento:
- `run_scalping_parameter_optimizer.py`: `should_use_live_result` ora usa il risultato live solo se contiene candidati.
- Aggiornato il test unitario che codificava il comportamento errato precedente.

Root cause:
- Con history contenente candidati e live senza candidati, la funzione restituiva `true` e rimpiazzava il risultato storico con il risultato live vuoto.
- Effetto osservato: optimizer run `152` e precedenti con `NO_SYMBOLS` anche quando la diagnostica filtro trovava candidati su `binance-history`.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` -> 25 test OK.
- Container `docbrown-real-refresh` aggiornato e riavviato con image commit `9ff4411f8d25f47b4607c4d50be635943ffe3fc0ac0553d766d2f3ebe91880a8`.
- Optimizer post-fix: run `154`, `DEPLOYABLE`, `CKBUSDC`, replay `2/2`, net `0.008342219417694714`, `applied=true`.

## Piano 16 - Coerenza Runtime Momentum15 Negativo

Data: 2026-06-04.

Obiettivo:
- Rendere coerenti le soglie runtime persistite con le soglie discovery DB-backed.
- Evitare che un candidato accettato con `momentum15` negativo venga poi rigettato sempre da HFT per `momentum15_min=0`.

Intervento:
- `run_scalping_scout.py`: `runtime_entry_thresholds` non clampa piu' `momentum15_min` a zero quando il candidato ha momentum15 negativo e il DB consente un floor negativo.
- Aggiunto test unitario dedicato su candidato tipo `CKBUSDC`.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` -> 26 test OK.
- Container `docbrown-real-refresh` aggiornato e riavviato con image commit `af7dd2379fa4eb08a5f1bbff2b699bc638c831080d696449da79fbd4da56a57c`.
- Optimizer post-fix: run `156`, `DEPLOYABLE`, `EPICUSDC`, replay `4` trade, win rate `0.75`, net `0.005472726814553887`, `applied=true`.

## Piano 17 - Default Bucket Operativo Unico

Data: 2026-06-04.

Contesto:
- La pipeline SCALPING_SCOUT deve usare la stessa sorgente dati su optimizer, refresh e runtime HFT.
- I default Docbrown puntavano ancora a `binance-history`, anche se il DB operativo ora deve puntare a `binance`.

Intervento:
- Aggiornati default in `docker-compose.yml`, `application.yml`, `python/config.example.yml`, `settings.py`, `run_real_candidate_refresh_batch.py` e DDL fallback di `run_scalping_scout.py`.
- Il default `INFLUX_BUCKET` ora e' `binance`.
- I test che costruiscono esplicitamente scenari live/history separati restano per compatibilita' e copertura fallback.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` OK, 55 test.

## Piano 18 - Test E Fixture Su Bucket Unico

Data: 2026-06-04.

Contesto:
- Dopo la rimozione del bucket Influx `binance-history`, anche test resource e fixture Python devono rappresentare il bucket operativo unico.

Intervento:
- `src/test/resources/application*.properties`: `influxdb.history-bucket=binance`.
- Fixture Python `ScoutConfig`: `history_bucket=binance`.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` OK, 55 test.

## Piano 19 - Lettura Influx Realtime-Only

Data: 2026-06-05.

Contesto:
- Dopo l'unificazione del bucket `binance`, la serie realtime e la serie storica/repair potevano coesistere con tag diversi.
- Docbrown deve calcolare scout/optimizer sui dati realtime correnti, non su serie storiche taggate eventualmente ferme.

Intervento:
- `InfluxTickRepository` filtra `not exists r.base` in `symbols`, `ticks` e `latest_timestamp`.
- Le query Docbrown ignorano serie storiche con tag `base/quote` e leggono la serie realtime non taggata.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer python.tests.test_promote_current_scalping_probe` OK, 55 test.
- Image `docbrown-real-refresh:latest` rebuildata e container ricreato.

## Piano 20 - Analisi E Persistenza CENTROID_STRATEGY

Data: 2026-06-07.

Contesto:
- `CENTROID_STRATEGY` deve determinare pattern buoni/tossici partendo dai BUY salvati e testare la finestra PAPER corrente senza avviare REAL.

Intervento:
- Aggiunto modulo `docbrown_ml.patterns.centroid_strategy` con fail score e sintesi cluster.
- Aggiunto script `python/analyze_centroid_strategy.py`.
- `run_scalping_scout.py` ora conosce le colonne centroid DB-backed della config runtime.
- Aggiunti test unitari `test_centroid_strategy` e fixture aggiornate.

Esiti storico REAL:
- Analizzati 58 BUY REAL con `params_json`.
- Cluster salvati in `hft.centroid_strategy_pattern_cluster`:
  - `CLEAN`: WATCH, 18 sample, win rate 0.4444, EV -0.04308160.
  - `FAIL_SCORE_1`: WATCH, 10 sample, win rate 0.10, EV -0.08034535.
  - `FAIL_SCORE_2`: BLOCKED, 8 sample, win rate 0.25, EV -0.10343395.
  - `FAIL_SCORE_3`: BLOCKED, 3 sample, win rate 0.3333, EV -0.03898433.
  - `FAIL_SCORE_4`: BLOCKED, 19 sample, win rate 0.1053, EV -0.04675201.
- Nessun cluster e' stato promosso `ACTIVE`: anche `CLEAN` e' negativo in-sample e resta solo WATCH.

Esiti PAPER/current:
- Dry-run ufficiale `run_scalping_scout.py` su finestra corrente: `PAPER_WAIT`, 0 candidati, nessuna persistenza.
- Influx ha serie realtime aggiornate, ma al momento del test il bucket contiene solo circa 10 tick/minuti densi per BTCUSDC nella finestra 24h; non ci sono almeno 35 tick per calcolare momentum/replay robusti sui raw current.
- Test 1 simbolo e 20 simboli sulla finestra current quindi non e' statisticamente eseguibile ora; va ripetuto quando Influx avra' almeno 35-90 minuti di profondita' realtime continua.

Verifiche:
- `PYTHONPATH=python python3 -m unittest python.tests.test_centroid_strategy python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK, 29 test.
- Report generato: `target/centroid-strategy-real-current.json`.

## 2026-06-07 13:05 CEST - CENTROID_STRATEGY gate Docbrown e PAPER refinement

- `run_scalping_scout.py` ora legge `centroid_require_active_cluster` e pesi ranking `score_*` da `hft.scalping_scout_runtime_config`.
- Il gate CENTROID viene applicato prima della selezione/persistenza dei candidati; il report include `centroidRejectedSymbols`.
- Fix critico: con `--persist` e zero candidati, se `deactivate_previous_scout=1`, Docbrown disattiva comunque i vecchi `SCALPING_SCOUT` attivi. Prima una run `PAPER_WAIT` poteva lasciare simboli stale tradabili da HFT.
- `analyze_centroid_strategy.py` accetta `--current-hours` e ha entrypoint diretto.
- PAPER/refinement: strict 3h `PAPER_WAIT evaluated=0`; strict 48h `PAPER_WAIT evaluated=0`; diagnostica 48h rilassata `PAPER_WAIT evaluated=0`.
- Diagnosi raw 48h: `EDENUSDC` replay 470 trade win rate 36.6% net `-2.02`; `HEIUSDC` 494 trade win rate 48.6% net `-2.18`; `OPNUSDC` 526 trade win rate 50.0% net `-2.56`. Nessun pattern corrente giustifica REAL.
- Build `docbrown-real-refresh:latest` aggiornato ma container lasciato fermo; `hft.real_trading_runtime_config.refresh_enabled=0`.

## 2026-06-07 13:27 CEST - CENTROID optimization extra PAPER x3

- Eseguita ricerca genetica/grid leggera su 58 trade REAL storici con split temporale train/test.
- Soglie migliori trovate: `centroid_trend_min=0.003`, `centroid_fast_quote_volume_min=350`, `centroid_replay_net_return_min=0.005`, `centroid_replay_trades_min=3`, `centroid_fail_score_block_threshold=1`.
- Risultato storico con queste soglie: `CLEAN ACTIVE` 7 trade, win rate 71.4%, EV `+0.05683282`, PF `1.6955`; `FAIL_SCORE_1..4` risultano `BLOCKED` e negativi.
- Caveat: test out-of-sample di questa ricerca ha solo 1 trade accettato; utile come hardening, non come autorizzazione REAL.
- Rigenerati cluster con `analyze_centroid_strategy.py --persist-clusters`.
- PAPER 1: primary default + CENTROID ottimizzato -> `PAPER_WAIT`, `evaluated=0`.
- PAPER 2: feature/replay 24h, `momentum5_min=-0.0005`, `min_replay_trades=3` -> `PAPER_WAIT`, `evaluated=0`.
- Diagnosi su `ZECUSDC`: su replay 24h e' negativo (`72` trade, win rate 44.4%, net `-0.3704`), quindi non va promosso con replay lungo.
- PAPER 3: feature 24h + replay 90m, `volume_ratio_min=0.5`, `momentum5_min=-0.0005` -> `PAPER_WAIT`, `evaluated=0`.
- Stato finale DB ripristinato conservativo sui primary guard (`lookback=180`, `replay_lookback=90`, `momentum5_min=0.0015`, `volume_ratio_min=0.7`) mantenendo CENTROID ottimizzato e fail-closed (`centroid_apply_to_real=0`, `active SCALPING_SCOUT=0`).

## 2026-06-07 14:05 CEST - Candidate Outcome Dataset e Threshold Learning

- Aggiunto script `python/run_scalping_scout_candidate_dataset.py` per costruire dataset SCALPING_SCOUT da Influx 48h e apprendere soglie centroid/fail-score.
- Lo script legge tutti i parametri operativi da `hft.scalping_scout_candidate_dataset_config`; lo schema e' Flyway-managed da HFT e Docbrown ora lo verifica soltanto.
- Fix anti-leakage: le feature replay sono calcolate solo su finestre precedenti all'entry; il primo threshold run basato su outcome futuro e' stato marcato `INVALID_LEAKAGE`.
- Dataset corretto `dataset_run_id=3`: 3.748 osservazioni BUY/SELL simulate su 281 simboli, win rate `29.30%`, net `-16.53588089`.
- Threshold run valido `id=3`: 26.244 combinazioni, status `WATCH_WIN_RATE`, accepted sample 490, win rate `30.2041%`, EV `-0.00472817`, PF `0.17429146`.
- Cluster persistiti: `CLEAN` resta `WATCH`; `FAIL_SCORE_1..4` restano `BLOCKED`.
- PAPER RUN finale: `PAPER_WAIT`, nessun simbolo selezionato, nessun parametro persistito.
- Verifiche: `PYTHONPATH=python python3 -m unittest python/tests/test_centroid_strategy.py` OK; `py_compile` OK.

## 2026-06-07 14:15 CEST - Cluster Activation Senza Hardcode

- Rimossi gli ultimi hardcode semantici dalla promozione cluster: `ACTIVE` usa ora `centroid_active_win_rate_min` e `centroid_active_expected_value_min` da DB/config.
- Aggiornati `run_scalping_scout.py`, `analyze_centroid_strategy.py` e `run_scalping_scout_candidate_dataset.py`.
- PAPER RUN finale V44: `WLDUSDC` valutato e bloccato dal centroid gate (`FAIL_SCORE_2`, `BLOCKED`, reason `replay_net_return,replay_trades`); nessun parametro persistito.
- Verifiche: suite Python `test_centroid_strategy`, `test_scalping_scout`, `test_scalping_parameter_optimizer` OK; image `docbrown-real-refresh:latest` rebuildata ma non avviata.

## 2026-06-07 14:25 CEST - PAPER optimization x5 target 70%

- Eseguite 5 iterazioni di optimization su dataset candidate senza REAL e senza apply runtime.
- Ottimizzato il trainer `run_scalping_scout_candidate_dataset.py`: train/test/all vengono calcolati in un solo pass per combinazione.
- Risultati:
  - Run 1 `threshold_run=4`: all WR `44.44%`, test `50.00%`, EV negativa.
  - Run 2 `threshold_run=5`: all WR `52.31%`, test `62.96%`, train `44.74%`, EV negativa.
  - Run 3 `threshold_run=6`: all WR `35.86%`, test `45.24%`, EV negativa.
  - Run 4 `threshold_run=7`: all WR `41.86%`, test `50.00%`, EV negativa.
  - Run 5 `threshold_run=8`: all WR `43.85%`, test `30.67%`, EV negativa.
- PAPER RUN finale: `HOMEUSDC` respinto dal centroid gate per `replay_trades`; nessun parametro persistito.
- Conclusione: il target 70% non emerge dai dati 48h correnti. Forzarlo significherebbe overfitting, non readiness REAL.
- Verifiche: `PYTHONPATH=python python3 -m unittest python.tests.test_centroid_strategy python.tests.test_scalping_scout python.tests.test_scalping_parameter_optimizer` OK.
