# Sessione 2 - Pipeline Flow DB Contract

## Piano 1 - Endpoint Flow Step-By-Step

Data: 2026-06-11 14:25 CEST.

Obiettivo: esporre a FE un endpoint narrativo della pipeline che legga solo campi reali da DB e renda espliciti i dati non persistiti.

Azioni completate:

- Aggiunto `GET /backoffice/pipeline/flow?executionMode=PAPER|REAL&executionUuid=<optional>&limit=20`.
- Aggiunti DTO `PipelineFlow`, `PipelineFlowSummary`, `PipelineFlowStep`.
- Implementati 10 step stabili: market data, Docbrown analysis, candidate selection, HFT runtime intake, buy check, buy execution, position management, sell check, sell execution, execution summary.
- Ogni step espone `does`, `input`, `output`, `records`, `missingData`.
- Ogni record include `sourceTable` e colonne reali lette da DB.
- L'endpoint sceglie l'execution richiesta oppure l'ultima osservabile per scenario.
- Nessun dato mocked: dati assenti restano null/assenti e sono riportati in `missingData`.

Origini DB usate:

- `hft.real_trading_runtime_config`
- `hft.scalping_scout_runtime_config`
- `hft.scalping_scout_optimizer_run`
- `hft.stan_strategy_parameters`
- `hft.paper_trading_event`
- `hft.paper_trading_run`
- `hft.trade_position`

Verifiche:

- `./mvnw test` OK, 11 test.
- `./mvnw package -DskipTests` OK.
- `docker compose up -d --build` OK, container `kenshiro-local` up su `8085`.
- Endpoint reale validato: `GET http://localhost:8085/kenshiro/backoffice/pipeline/flow?executionMode=PAPER&limit=3` restituisce 10 step, righe DB con `sourceTable`, summary PAPER e record BUY reali da `paper_trading_event`.
