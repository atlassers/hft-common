# Sessione 1 - Backoffice DB-Only Runtime

## Piano 1 - Runtime Check Pipeline

User story: `KEN-S01-P01-US-001`.

Decisioni:

- Aggiunto runtime check Pipeline DB-only per esporre stato REAL/PAPER dedotto dallo schema HFT.
- Le execution event-only sono visibili anche quando non esistono ancora posizioni.
- Il dettaglio usa gli eventi runtime per evitare stato `UNKNOWN` su run vive.

Verifiche:

- `./mvnw test`: 7 test OK.

Esito: completato.

## Piano 8 - Expose Scalping Optimizer Runs

User story: `KEN-S01-P08-US-008`.

Decisioni:

- `GET /backoffice/pipeline/runtime-check` include anche i run `hft.scalping_scout_optimizer_run`.
- Gli optimizer run vengono fusi con i batch Docbrown esistenti e ordinati per `startedAt`.
- Gli stati esposti sono:
  - `SCOUT_CONFIG_APPLIED`
  - `SCOUT_DEPLOYABLE`
  - reason tecnico (`NO_SYMBOLS`, `INSUFFICIENT_TRADES`, ecc.).
- Fixata la gestione di liste immutabili nel merge dei batch runtime.

Verifiche:

```text
./mvnw test
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

## Piano 2 - Dockerizzazione Kenshiro

User story: `KEN-S01-P02-US-002`.

Decisioni:

- Aggiunto `src/main/docker/Dockerfile.jvm` per runtime Quarkus JVM packaged.
- Aggiunto `docker-compose.yml` con porta `8085`, root path `/kenshiro` e datasource configurabili via env.
- In Docker i datasource puntano di default a `host.docker.internal` per raggiungere MySQL locale esposto dall'host.

Verifiche:

- `./mvnw -DskipTests package`: build OK.
- `docker compose build`: immagine `kenshiro:local` buildata.
- `docker compose config`: configurazione valida.
- `./mvnw test`: 7 test OK.

Esito: completato.

## Piano 3 - Porta 8085 E Runtime Docker

User story: `KEN-S01-P03-US-003`.

Decisioni:

- Porta runtime Kenshiro spostata da `8084` a `8085`.
- Aggiornati `application.yml`, `Dockerfile.jvm`, `docker-compose.yml` e README.
- Default password MySQL Docker allineata al MySQL locale (`gessin.123`).
- Avviato container `kenshiro-local` su `8085`.

Verifiche:

- `./mvnw -DskipTests package`: OK.
- `docker compose build`: OK.
- `docker compose up -d --force-recreate kenshiro`: OK.
- `GET /kenshiro/backoffice/pipeline/runtime-check?executionMode=REAL`: HTTP 200.

Esito: completato.

## Piano 4 - Dashboard Scoped Per Execution Mode

User story: `KEN-S01-P04-US-004`.

Decisioni:

- `GET /backoffice/dashboard?executionMode=PAPER|REAL` ora calcola totali e trend solo per il mode richiesto.
- In `PAPER`, i campi `real*` sono zero e il trend contiene solo eventi PAPER.
- In `REAL`, i campi `paper*` sono zero e il trend contiene solo posizioni REAL chiuse.
- Aggiunto test specifico per assicurare lo scope PAPER/REAL.

Verifiche:

```text
./mvnw test
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

PAPER totals: realBuys=0, realSells=0
REAL totals: paperBuys=0, paperSells=0
```

Esito: completato.

## Piano 5 - Facciata Docbrown DB-Only

User story: `KEN-S01-P05-US-005`.

Decisioni:

- Aggiunti endpoint Kenshiro per la pagina `/docbrown` sotto `/backoffice/docbrown/*`.
- Gli endpoint leggono direttamente lo schema Docbrown `back_test` tramite datasource `docbrown` e non chiamano Docbrown via HTTP.
- Esposti config supervised/legacy, policy market-regime deployable, riepiloghi run per walk-forward/HFT/supervised e reverse oracle DB-only.
- Le POST restituiscono summary dell'ultimo `backtest_run` persistito con metadata `BACKOFFICE_DB_ONLY`: non avviano l'engine Docbrown.
- Query rese tolleranti a tabelle opzionali non ancora presenti, con default dichiarati solo per config non persistite.

Verifiche:

```text
./mvnw test
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

./mvnw -DskipTests package
BUILD SUCCESS

docker compose build && docker compose up -d --force-recreate
kenshiro-local ricreato e avviato

GET /kenshiro/backoffice/docbrown/supervised-signals/config -> HTTP 200
POST /kenshiro/backoffice/docbrown/walk-forward -> HTTP 200, trainingMetadata.modelType BACKOFFICE_DB_ONLY
```

## Piano 7 - Vault DB Credentials

User story: `KEN-S01-P07-US-007`.

Problema:

- Il compose locale esponeva fallback DB hardcoded per i datasource HFT e Docbrown.
- Kenshiro deve restare il boundary FE/backoffice, ma non deve ricevere credenziali DB via Docker.

Decisioni:

- Aggiunta estensione `quarkus-vault`.
- I datasource `hft` e `docbrown` mantengono URL JDBC separati, ma leggono username/password da Vault:
  - `datasource-username`
  - `datasource-password`
- Il compose passa solo configurazione Vault:
  - `QUARKUS_VAULT_URL`
  - `QUARKUS_VAULT_AUTHENTICATION_CLIENT_TOKEN`
  - `QUARKUS_VAULT_SECRET_CONFIG_KV_PATH`
- Rimossi fallback username/password hardcoded dal compose.

Vincolo:

- Il DB user risolto da Vault deve avere permessi su entrambi gli schemi `hft` e `back_test`.

Esito: completato.

## Piano 6 - Runtime REAL Batch E Config DB

User story: `KEN-S01-P06-US-006`.

Decisioni:

- Esteso `GET /backoffice/pipeline/runtime-check` con dati DB-only per allineare il FE alle configurazioni REAL introdotte stamattina.
- Aggiunto `realRuntimeConfig`, letto da `hft.real_trading_runtime_config`, con `confidenceMode`, refresh, universe, soglie BALANCED e parametri runtime REAL.
- Aggiunto `docbrownBatchRuns`, letto da `back_test.backtest_ml_optimization_run` e `back_test.backtest_ml_validation_run`, per mostrare quando il batch ha girato e quali metriche/parametri JSON ha prodotto.
- Aggiunto `generatedParameters`, letto da `hft.stan_strategy_parameters`, per esporre i parametri attivi generati nello scenario selezionato.
- `GET /backoffice/config/hft/db` include ora le entry `realRuntime.*`, cosi' la pagina config vede le configurazioni portate a DB.
- Le query restano tolleranti a schemi parziali nei test: se le tabelle batch/runtime non esistono, Kenshiro risponde comunque con array vuoti o config nulla.

Verifiche:

```text
./mvnw test
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

./mvnw -DskipTests package
BUILD SUCCESS

docker compose up -d --build
kenshiro-local ricreato e avviato

GET /kenshiro/backoffice/pipeline/runtime-check?executionMode=REAL -> HTTP 200
status OPERATIVE, Flyway V18, confidenceMode BALANCED, batchCount 5, latestBatch REAL_WAIT, generatedParameters 0

GET /kenshiro/backoffice/config/hft/db -> HTTP 200
realRuntime entries 29
```

Esito: completato.

## Piano 8 - Generated Parameters Stop-Loss Grace

User story: `KEN-S01-P08-US-008`.

Decisioni:

- `PipelineGeneratedParameter` espone ora `stopLossGraceSeconds` e `minQuoteVolume`.
- I valori vengono estratti da `hft.stan_strategy_parameters.params_json`, preservando compatibilita' con schemi parziali/test privi di `params_json`.
- Kenshiro continua a leggere direttamente il DB senza contattare HFT/Docbrown.

Verifiche:

```text
./mvnw test
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

Esito: completato.

## Piano 9 - Pipeline Step Chain DB-Only

User story: `KEN-S01-P09-US-009`.

Decisioni:
- `GET /backoffice/pipeline/runtime-check` espone ora `pipelineSteps`, una sequenza DB-only che rappresenta la chain operativa:
  - `docbrown-optimizer`.
  - `legacy-refresh-script`.
  - `stan-guard`.
  - `stan-buy`.
  - `stan-sell`.
- Ogni step contiene component, status, timestamp, summary, soglie/guard, outcome, dati BUY, dati SELL e config completa consultabile dal FE.
- `PipelineGeneratedParameter` espone anche `catastrophicStopLoss` da `stan_strategy_parameters.params_json`.
- `GET /backoffice/config/hft/db` include la curva completa di `position_sizing_config`: `lowBalanceMaxQuote`, `lowBalanceStepQuote`, `lowBalanceStartPercent`, `highBalanceTradePercent`.
- La lettura resta diretta su schemi HFT/Docbrown; nessuna chiamata HTTP verso HFT o Docbrown.

Verifiche:

```text
./mvnw -q test
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0

./mvnw -q -DskipTests package
OK
```

Esito: completato.

Deploy locale MS437:
- Immagine `kenshiro:local` ricostruita e `kenshiro-local` ricreato.
- Verifica `GET /kenshiro/backoffice/pipeline/runtime-check?executionMode=REAL`: HTTP 200, `pipelineSteps=5`.

## Piano 10 - Trade Chain BUY SELL

User story: `KEN-S01-P10-US-010`.

Decisioni:
- `GET /backoffice/pipeline/runtime-check` espone ora `tradeChains`.
- Ogni chain rappresenta una posizione da `hft.trade_position` e contiene:
  - ultimi check BUY Stan dello stesso `execution_uuid + symbol` prima del BUY.
  - nodo BUY effettivo da `trade_position`.
  - nodo SELL effettivo da `trade_position` quando la posizione e' chiusa.
- La SELL viene letta da `trade_position` perche' HFT non persiste eventi SELL in `paper_trading_event` per REAL.
- Nessuna chiamata HTTP a HFT/Docbrown: lettura diretta DB-only.

Verifiche:
- `./mvnw -q test` OK.
- `./mvnw -q -DskipTests package` OK.

Esito: completato.

## Piano 7 - Runtime Pipeline DB Config Completa

Data: 2026-06-08 17:25 CEST.

Obiettivo: esporre dal backoffice le configurazioni DB realmente associate agli step della chain corrente, incluse le tabelle introdotte dalla normalizzazione centroidi.

Interventi:

- `GET /backoffice/pipeline/runtime-check` arricchisce `pipelineSteps.config` con `dbRuntimeConfig`.
- `dbRuntimeConfig` include `technical_runtime_constant`, `crypto_ranking_runtime_config` e `achievement_runtime_config`.
- Lo step Stan guard e lo step Stan buy espongono `buyCentroidConfig` da `normalized_centroid_model_config`, `normalized_centroid_feature_scaler` e `normalized_centroid_cluster` lato BUY.
- Lo step Stan sell espone `sellCentroidConfig` lato SELL, predisposto anche se il modello SELL resta disabilitato/non operativo.
- I cluster centroidi vengono limitati ai primi 20 per evitare payload ingestibili in UI.

Verifiche:

- `./mvnw -q -DskipTests package` -> OK.
- `./mvnw -q test` -> OK.

Nota operativa: endpoint solo osservabilità/config; nessuna RUN avviata.

### Fix - Config Payload Nel Campo Corretto

Data: 2026-06-08 17:30 CEST.

Correzione post-rebuild: le configurazioni runtime degli step Stan erano esposte nel campo `thresholds`, quindi il FE execution-scoped non le vedeva come `Configurazione associata`.

Intervento:

- Spostate le configurazioni Stan guard, Stan buy e Stan sell nel campo `PipelineStep.config`.
- Lasciati input/outcome nei campi dedicati (`thresholds`, `outcome`, `buyData`, `sellData`).
- Verificato endpoint locale PAPER: `hasDbRuntimeConfig=true`, `steps=5`, `trades=14`.
