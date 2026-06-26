# Session 107 - Diagnostic Endpoints Handoff

Data: 2026-06-21.

## Scopo

Documento compatto per la prossima chat: quali step diagnostici eseguire, quali endpoint/script usare, e in che ordine, senza dover rileggere tutta la conversazione.

## Stato Da Assumere

- Charter: `doc/STRATEGIC_REM_RECOVERY_PLAN.md`.
- Checklist corrente: `doc/session/session-106-ml-readiness-before-paper-checklist.md`.
- Stato strategico: `BASELINE_98_CANDIDATE_BLOCKED_BY_ML_ORCHESTRATION`.
- Nessuna REAL.
- Nessun H2 per test operativi.
- MySQL/container obbligatori.
- Nessuna PAPER validativa se `ML_READY=false`.
- DocBrown espone sotto root path `/docbrown`.

## Ordine Diagnostico Standard

### 1. Stato Git E Runtime

```bash
git status --short
git log --oneline -5
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

Scopo:

- verificare se ci sono modifiche non committate;
- verificare container `acdc-vpn`, `docbrown`, `mysql_container`, `influxer`, `influxdb`, `grafana`;
- non partire con diagnosi trading se i container base non sono up.

### 2. Stato DB Minimo

```bash
docker exec mysql_container sh -lc 'mysql -u${MYSQL_USER:-hft_user} -p"$MYSQL_PASSWORD" hft -e "
SELECT id, profile_key, status, execution_mode, current_budget, reserved_budget, realized_profit_quote, started_at, completed_at
FROM acdc_run_execution
ORDER BY id DESC
LIMIT 10;

SELECT COUNT(*) open_positions
FROM acdc_paper_position
WHERE status = \"OPEN\";

SELECT COUNT(*) rules, COALESCE(SUM(status=\"PROMOTED\"),0) promoted, MAX(created_at) max_created
FROM acdc_reversal_ml_rule;

SELECT status, COUNT(*) c, MAX(created_at) max_created, MAX(advice_valid_until) max_until
FROM acdc_live_ml_advice
GROUP BY status;
"'
```

Scopo:

- capire se ci sono PAPER running;
- verificare `reserved_budget`;
- verificare posizioni aperte;
- verificare se ML pesante ha regole;
- verificare se advice live sono fresche o residue.

### 3. Log Container

```bash
docker logs --tail 160 acdc-vpn
docker logs --tail 160 docbrown
docker logs --since 30m docbrown | tail -200
```

Scopo:

- cercare errori Flyway/startup;
- cercare timeout/rollback DocBrown;
- cercare scheduler ACDC che avvia execution non previste;
- cercare `Transaction Reaper`, `RollbackException`, `Connection is closed`.

### 4. Endpoint ACDC Operativi

Base URL:

```text
http://localhost:8091
```

Start/stop PAPER:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/paper/run/REM_CURRENT' \
  -d '{"executionMode":"PAPER","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/paper/stop-buy/REM_CURRENT' \
  -d '{"executionMode":"PAPER","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/paper/stop/REM_CURRENT' \
  -d '{"executionMode":"PAPER","symbols":[]}'
```

Uso:

- `paper/run`: solo dopo preflight `ML_READY=true`;
- `paper/stop-buy`: blocca nuove BUY e lascia drenare SELL;
- `paper/stop`: chiude execution se `reserved_budget=0`, altrimenti comportarsi come drain/stop-buy.

Start/stop SHADOW:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/shadow/run/REM_CURRENT' \
  -d '{"executionMode":"SHADOW","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/shadow/stop-buy/REM_CURRENT' \
  -d '{"executionMode":"SHADOW","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/shadow/stop/REM_CURRENT' \
  -d '{"executionMode":"SHADOW","symbols":[]}'
```

### 5. Endpoint ACDC Diagnostici

Base:

```text
http://localhost:8091/diagnostics/acdc
```

Live advice viste da ACDC:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/live-advice/REM_CURRENT' | jq '.'
```

REM readiness esistente:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/readiness?profileKey=REM_CURRENT' | jq '.'
```

REM parity:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/parity?profileKey=REM_CURRENT' | jq '.'
```

REM lifecycle replay:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/lifecycle-replay?profileKey=REM_CURRENT' | jq '.'
```

REM capturable replay:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/capturable-replay?limit=100' | jq '.'
```

Paper scoring:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/scoring?executionIds=17' \
  | tee /tmp/acdc_paper_scoring.json \
  | jq '.'
```

Paper timeline:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/timeline?executionIds=17' \
  | tee /tmp/acdc_paper_timeline.json \
  | jq '.'
```

Paper sell-capture:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/sell-capture?executionIds=17' \
  | tee /tmp/acdc_paper_sell_capture.json \
  | jq '.'
```

Paper post-SELL forensics:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/post-sell-forensics?executionIds=17' \
  | tee /tmp/acdc_paper_post_sell_forensics.json \
  | jq '.'
```

Paper live revalidation counterfactual:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=17&horizonSeconds=900' \
  | tee /tmp/acdc_paper_counterfactual.json \
  | jq '.'
```

Paper session guard:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/session-guard/REM_CURRENT' | jq '.'
```

Shadow replay:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/shadow/{executionId}/replay?horizonSeconds=900' | jq '.'
```

### 6. Endpoint DocBrown

Base URL:

```text
http://localhost:8083/docbrown
```

Health Quarkus standard non e' affidabile in questo container: `/q/health` puo' rispondere `404`. Verificare gli endpoint reali sotto `/docbrown`.

Live advice scoring leggero:

```bash
curl -sS --max-time 180 -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/live-advice/REM_CURRENT/score' \
  | tee /tmp/docbrown_live_score.json \
  | jq '.'
```

Heavy research/mining:

```bash
curl -sS --max-time 1800 -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/research/REM_CURRENT/run' \
  | tee /tmp/docbrown_research_report.json \
  | jq '.'
```

Nota:

- se risponde `{"status":"RUNNING"}`, un job e' gia' attivo;
- se dura troppo, controllare `docker logs --since 30m docbrown`;
- sessione 105 ha mostrato timeout/rollback del mining pesante.

Rolling blank candidates:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/generate' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/rolling-validation' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/rolling-paper-promotion' \
  -d '{...}' | jq '.'
```

Universe triage/scheduler shadow:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/universe-triage/shadow' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/universe-scheduler/shadow' \
  -d '{...}' | jq '.'
```

DocBrown counterfactual:

```bash
curl -sS 'http://localhost:8083/docbrown/rem/live-advice/REM_CURRENT/counterfactual?from=2026-06-21T00:00:00Z&to=2026-06-21T01:00:00Z&horizonSeconds=900&limit=5000' | jq '.'
```

DocBrown paper execution forensics:

```bash
curl -sS 'http://localhost:8083/docbrown/rem/live-advice/paper-executions/forensics?executionIds=17' | jq '.'
```

Scientific preflight/acceptance:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/live-advice/scientific/preflight' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/live-advice/scientific/acceptance' \
  -d '{...}' | jq '.'
```

### 7. Script Esistenti

Directory:

```text
scripts/
```

Script disponibili:

```text
scripts/acdc-start-dry-run.sh
scripts/acdc-start-shadow-run.sh
scripts/acdc-start-paper-run.sh
scripts/acdc-start-real-run.sh
scripts/acdc-stop-run.sh
scripts/acdc-run-rem-ml.sh
scripts/acdc-stop-containers.sh
scripts/acdc-best-winner-action.sh
```

Uso consigliato:

- preferire endpoint espliciti per diagnosi scientifiche;
- usare script solo se accelerano azioni gia' comprese;
- non usare `acdc-start-real-run.sh`;
- non usare script PAPER se `ML_READY=false`.

### 8. Build E Deploy Container

ACDC:

```bash
cd /home/mbc/Documenti/ws/java/hft/acdc
./mvnw -DskipTests package
docker build -f docker/Dockerfile.jvm -t acdc:latest .
docker compose --env-file docker/vpn/.env -f docker/vpn/compose.yml up -d --build --force-recreate acdc
docker logs --tail 120 acdc-vpn
```

DocBrown:

```bash
cd /home/mbc/Documenti/ws/java/hft/docbrown
./mvnw -DskipTests package
docker compose --env-file /home/mbc/Documenti/ws/java/hft/acdc/docker/vpn/.env -f docker-compose.yml up -d --build --force-recreate docbrown
docker logs --tail 120 docbrown
```

Se `docbrown` container name conflict:

```bash
docker rm -f docbrown
docker compose --env-file /home/mbc/Documenti/ws/java/hft/acdc/docker/vpn/.env -f docker-compose.yml up -d docbrown
```

### 9. Preflight ML_READY Manuale Finche' L'Endpoint Non Esiste

Eseguire questi controlli prima di qualunque PAPER:

```bash
docker exec mysql_container sh -lc 'mysql -u${MYSQL_USER:-hft_user} -p"$MYSQL_PASSWORD" hft -e "
SELECT COUNT(*) open_paper
FROM acdc_run_execution
WHERE execution_mode=\"PAPER\" AND status=\"RUNNING\";

SELECT COUNT(*) open_positions
FROM acdc_paper_position
WHERE status=\"OPEN\";

SELECT COUNT(*) rules, COALESCE(SUM(status=\"PROMOTED\"),0) promoted
FROM acdc_reversal_ml_rule;

SELECT status, COUNT(*) c, MAX(advice_valid_until) max_until
FROM acdc_live_ml_advice
GROUP BY status;

SELECT COUNT(*) post_sell_forensics_rows
FROM acdc_paper_post_sell_forensics;
"'
```

Fail-closed se:

- PAPER running inattesa;
- posizioni aperte inattese;
- `rules=0` senza stato `NO_SIGNATURES` esplicito;
- DocBrown research `RUNNING`;
- log DocBrown contiene rollback/timeout recente;
- advice active sono residue o incoerenti con la run.

### 10. Monitor Run PAPER

Usare solo dopo `ML_READY=true`.

```bash
OUT=/tmp/session-next-paper-run
mkdir -p "$OUT"
EXEC={executionId}

while true; do
  TS=$(date -Iseconds)
  docker exec mysql_container sh -lc 'mysql -N -u${MYSQL_USER:-hft_user} -p"$MYSQL_PASSWORD" hft -e "
  SELECT CONCAT(\"run_status=\",status,\" current=\",current_budget,\" reserved=\",reserved_budget,\" realized=\",realized_profit_quote)
  FROM acdc_run_execution WHERE id='"$EXEC"';
  SELECT CONCAT(\"positions total=\",COUNT(*),\" open=\",COALESCE(SUM(status=\"OPEN\"),0),\" closed=\",COALESCE(SUM(status=\"CLOSED\"),0),\" net=\",COALESCE(SUM(net_profit_quote),0))
  FROM acdc_paper_position WHERE execution_id='"$EXEC"';
  SELECT CONCAT(\"forensics total=\",COUNT(*),\" pending=\",COALESCE(SUM(status=\"PENDING_LOCAL\"),0),\" completed=\",COALESCE(SUM(status=\"COMPLETED\"),0))
  FROM acdc_paper_post_sell_forensics WHERE execution_id='"$EXEC"';
  "' 2>/dev/null | sed "s/^/[$TS] /" | tee -a "$OUT/monitor.log"
  sleep 60
done
```

### 11. Raccolta Finale Post-Run

```bash
EXEC={executionId}
OUT=/tmp/session-next-paper-run

curl -sS "http://localhost:8091/diagnostics/acdc/paper/scoring?executionIds=$EXEC" > "$OUT/scoring.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/timeline?executionIds=$EXEC" > "$OUT/timeline.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/sell-capture?executionIds=$EXEC" > "$OUT/sell_capture.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/post-sell-forensics?executionIds=$EXEC" > "$OUT/post_sell_forensics.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=$EXEC&horizonSeconds=900" > "$OUT/counterfactual.json"
```

Sintesi rapida:

```bash
jq '{totalTrades,totalWins,totalLosses,totalNetProfitQuote,lossCapExits,timeoutExits}' "$OUT/scoring.json"
jq '{rows,missedReversals,noReversalConfirmed,inconclusiveGranularity,inconclusiveNoTicks}' "$OUT/post_sell_forensics.json"
jq '{totalBlockedAdvice,goodBlocks,badBlocks,ambiguousBlocks}' "$OUT/counterfactual.json"
```

## Interpretazione Minima Del Consiglio

- `FAILED_PREREQ`: ML readiness non passa, mining rollback/timeout, scheduler contaminato, advice residue.
- `INCONCLUSIVE`: nessun trade, dati granulari assenti, infrastruttura parziale.
- `FAIL_BASELINE`: run pulita, trade sufficienti, PnL negativo o zero-MFE/loss-cap dominanti.
- `PASS_BASELINE`: run pulita, PnL positivo, flusso advice->BUY->SELL completo, capture accettabile, forensics non smentisce SELL, counterfactual non mostra opportunity bloccate sistematicamente.

## Ultima Evidenza Nota

Sessione `105`:

- clean 60m non ottenuta;
- DocBrown heavy research timeout/rollback;
- `acdc_reversal_ml_rule=0`;
- execution `17` non pulita: 2 trade, 0 win, 2 loss, net `-0.451812168661600000`, max MFE `0`, 2 loss-cap;
- post-SELL forensics persistente ha funzionato, ma verdict `INCONCLUSIVE_GRANULARITY`.
