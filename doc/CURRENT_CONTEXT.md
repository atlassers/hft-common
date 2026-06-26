# Current Context

Ultimo aggiornamento: 2026-06-26 23:31 CEST.

Snapshot operativo corrente del workspace `/home/mbc/Documenti/ws/java/hft`.

## Gerarchia Documentale

1. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`
2. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md`
3. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/STRATEGIC_REM_HANDOFF.md`

Se i documenti confliggono, prevale il piano strategico. Il current context contiene solo lo stato corrente e il prossimo
TODO; procedure, endpoint, payload e diagnostica stabile stanno nell'handoff.

## Vincoli Hard Correnti

- REAL vietata.
- PAPER solo `FORWARD_AB_98` e solo se `ML_READY=true`.
- Il ciclo operativo parte da FE `/management` -> Kenshiro `/backoffice/management/*`.
- `/pipelines` non e' path operativo primario.
- Non allargare gate/live/SELL per forzare PAPER.
- SHADOW technical, WATCH tecnico e UI evidence non sono Forward A/B evidence.
- MySQL e container deployati sono obbligatori per validazione operativa; H2 non vale come validazione operativa.
- Il Consiglio elabora piani e monitora avanzamenti; Codex implementa secondo le indicazioni e produce report finale.
- Stringhe operative, action id, status, config key e payload key devono essere centralizzati in `hft-common` o registry
  locali usati come shim.
- FE e script devono mantenere mapping 1:1 con i contract/payload comuni quando esistono.

## Stato Ultima Attivita'

Obiettivo eseguito: analisi puntuale della RUN `FORWARD_AB_98` execution PAPER `93` / SHADOW `94` e fix runtime SELL
per il decadimento post-ingresso.

RUN analizzata:

- PAPER B execution `93`: `STOPPED`, PnL netto `-0.0920963975765`.
  - `HEIUSDC`: chiusa in profitto con `EXIT_ML_ADVICE_TAKE_PROFIT`, hold circa 154s, max net return
    `0.008193877551020408`.
  - `PENDLEUSDC`: entrata in piccolo MFE netto positivo `0.000352433281004710`, poi decaduta e chiusa solo a timeout
    con net return circa `-0.006704866562009419`.
  - `ICPUSDC`: mai positiva nello stream decisionale ACDC, chiusa a timeout con net return circa
    `-0.005172867513611615`.
- SHADOW A execution `94`: `COMPLETED`, PnL netto `-0.3971208329067`.
- WATCH evidence presente: PAPER `BUY_OPENED=3`, `BUY_REJECTED_RUNTIME=8`, `EXPIRED=4`; SHADOW `BUY_OPENED=2`,
  `EXPIRED=3`.

Diagnosi del Consiglio:

- Il problema osservato non e' l'ingresso WATCH: la WATCH e' entrata nel runtime e ha autorizzato BUY reali.
- Il problema principale e' SELL/hold decay: i simboli che non chiudono vicino all'ingresso tendono a degradare.
- `PENDLEUSDC` mostra il pattern critico: piccolo MFE netto positivo, rientro sotto break-even, nessuna uscita dinamica
  prima del timeout.
- `ICPUSDC` e' caso diverso: nessun MFE positivo; servira' una regola separata no-MFE/decay se il pattern si ripete.
- Il diagnostic `trailingArmed` era fuorviante: indicava `maxNetReturn > 0`, non l'arming reale del guard di trailing.
- Il guard V67 a DB dichiarava arming su `min_arm_net_return`, ma il codice defaultava a `require_safe_for_trailing=true`
  quando il metadata era assente, quindi poteva pretendere il `safeNetReturn` pieno invece del minimo eseguibile.

Implementato e verificato:

- `hft-common`: centralizzate le chiavi metadata del trailing ML advice:
  `min_arm_net_return`, `safe_arm_ratio`, `retention_ratio`, `break_even_floor`,
  `require_safe_for_trailing`, `protect_positive_mfe`.
- `acdc`: estratta `MlAdviceTrailingPolicy`, usata sia dal runtime SELL (`GuardEvaluator`) sia dai diagnostics
  (`PaperRunService`).
- `acdc`: il trailing ML advice ora defaulta a `require_safe_for_trailing=false`, coerente con metadata V67
  `min_arm_net_return=0.0005` e `safe_arm_ratio=0.00`.
- `acdc`: aggiunta protezione risk-control `protect_positive_mfe=true` di default: se un trade ha avuto MFE netto
  positivo e poi rientra sotto `break_even_floor`, puo' uscire senza aspettare il timeout.
- `acdc`: `trailingArmed` nei nuovi diagnostics riflette l'arming reale della policy, non il semplice `maxNetReturn > 0`.
- `acdc`: link Telegram chart reso anchor HTML sulla URL visibile; `TelegramNotifier` usava gia' `ParseMode.HTML`.
- `acdc-vpn`: immagine `acdc:latest` rebuildata e container ricreato su MySQL/prod.

Verifiche completate:

- `hft-common`: `mvn -q install -DskipTests` OK.
- `acdc`: `mvn -q test` OK, incluso nuovo test `MlAdviceTrailingPolicyTest`.
- `acdc`: `./mvnw -q -DskipTests package` OK.
- `acdc`: `docker build -f docker/Dockerfile.jvm -t acdc:latest .` OK.
- `acdc`: `docker compose --env-file docker/vpn/.env -f docker/vpn/compose.yml up -d --build --force-recreate acdc` OK.
- `acdc`: startup container su MySQL 8.0 OK; `paper_running=0`, `paper_open=0`, `shadow_open=0`.
- `acdc`: smoke HTTP `GET /diagnostics/acdc/paper/scoring?executionIds=93` OK.
- Durante i test ACDC, Vault Testcontainers `hashicorp/vault:1.15.2` e' partito correttamente; eventuali problemi Vault
  operativi vanno cercati in compose/config mount, non nella compatibilita' immagine base.

## Stato Repo

Repo modificati da committare: `hft-common`, `acdc`.

## Prossimo TODO

1. Committare e pushare `hft-common` e `acdc` con stesso MS.
2. Avviare da FE `/management` una nuova PAPER solo se `ML_READY=true` e solo `FORWARD_AB_98`.
3. Nella prossima RUN monitorare:
   - quante uscite avvengono per `EXIT_ML_ADVICE_DYNAMIC_TRAILING`;
   - se casi tipo `PENDLEUSDC` escono al rientro sotto break-even;
   - se casi tipo `ICPUSDC` senza MFE richiedono un guard no-MFE/decay advice-specific.
