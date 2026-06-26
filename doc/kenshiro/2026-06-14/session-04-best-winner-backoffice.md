# Sessione 4 - Best Winner Backoffice

## Piano 1 - Endpoint Best Winner DB-Only

Obiettivo: esporre al FE configurazione, firme e azioni operative best-winner usando Kenshiro come boundary backoffice.

Endpoint aggiunti:

- `GET /backoffice/best-winner/window-config`;
- `PUT /backoffice/best-winner/window-config`;
- `GET /backoffice/best-winner/signatures`;
- `POST /backoffice/best-winner/actions/{action}`.

Fonti:

- `hft.best_winner_window_config`;
- `hft.best_winner_signature`;
- script versionati in `hft/scripts` per start/stop.

Regole:

- gli endpoint config/signature leggono campi reali DB;
- `start-run` prepara il runtime PAPER direttamente in Java/JDBC prima di lanciare l'orchestrazione esterna;
- `stop-run` e' interamente Java/JDBC e non usa piu' shell;
- le azioni ritornano output reale di processo con `status`, `exitCode`, `logPath`, `outputTail`;
- `start-run` rimane PAPER-only tramite script HFT.

Verifiche:

- `./mvnw -q -DskipTests package` OK;
- container `kenshiro-local` rebuild/restart;
- endpoint reali verificati su `http://localhost:8085/kenshiro/backoffice/best-winner/window-config`;
- endpoint proxy FE verificato su `http://localhost:5173/backoffice/best-winner/window-config`.

## Piano 2 - Azioni Runtime Mode-Aware

Obiettivo: lo start/stop runtime deve arrivare dal FE con modalita' esplicita e non deve riscrivere soglie operative a ogni avvio.

Modifiche:

- `POST /backoffice/best-winner/actions/{action}` accetta `BestWinnerActionRequest`;
- request supportata: `executionMode` in `DRY/PAPER/REAL` e `allowRealRun`;
- REAL e' bloccata senza `allowRealRun=true` e flag server-side `KENSHIRO_ALLOW_REAL_BEST_WINNER_RUN=true`;
- `start-run` non sovrascrive piu':
  - bucket runtime;
  - soglie momentum/trend/volume;
  - min replay;
  - score gate;
  - loss cap.
- `start-run` modifica solo campi di orchestrazione o derivati dalla config DB:
  - `execution_mode`;
  - `real_buy_enabled`;
  - `refresh_enabled`;
  - `buy_max_price_received_age_seconds`;
  - `dataset_hours`;
  - `profit_feature_set_source`;
  - `live_candidate_max_age_minutes`.

Verifiche:

- `./mvnw -q -Dtest=BackofficeResourceTest test` OK;
- `./mvnw -q -DskipTests package` OK;
- container `kenshiro-local` rebuild/restart.

## Piano 3 - State Directory Unificata Per Loop REM

Problema:

- `start-run` avviava anche `microbar-paper-signal-loop.sh`;
- Kenshiro passava solo `BEST_WINNER_STATE_DIR`;
- il loop REM usava quindi la default `/tmp/hft-microbar-paper-signals`;
- stop/monitoring via FE e log runtime potevano non guardare la stessa directory.

Fix:

- `runWorkspaceScript` e `runWorkspaceScriptDetached` passano anche `MICROBAR_SIGNAL_STATE_DIR`;
- il valore e' lo stesso di `BEST_WINNER_STATE_DIR`: `hft/target/best-winner-runtime`;
- per i processi detached viene imposto `MICROBAR_SIGNAL_RUNTIME_SECONDS=0`, quindi il loop resta vivo fino a stop esplicito.

Verifiche:

- `./mvnw test` -> `16` test OK;
- `./mvnw package -DskipTests` OK;
- container `kenshiro-local` rebuild/restart;
- start PAPER via FE shortcut ha avviato runtime e signal loop con log in `target/best-winner-runtime`.
