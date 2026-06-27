# Current Context

Ultimo aggiornamento: 2026-06-27 09:58 CEST.

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

Obiettivo eseguito: RUN Forward A/B dopo fix SELL MS822/MS823, diagnosi zero-MFE e aggiunta guardia no-MFE decay.

RUN analizzata:

- PAPER B execution `95`: `STOPPED`, PnL netto `-0.164842310168994`.
  - `AIGENSYNUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.11765237014504`, `maxNetReturn=0`.
  - `KMNOUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.01316371680981`, `maxNetReturn=0`.
  - `PENGUUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.034026223214144`, `maxNetReturn=0`.
- SHADOW A execution `96`: `COMPLETED`.
- WATCH evidence presente: PAPER `BUY WATCH_CONFIRMED_BUY=3`; la WATCH ha autorizzato BUY runtime, ma tutti i BUY
  PAPER sono rimasti a zero MFE fino al timeout.
- Post-sell forensics: `INCONCLUSIVE_GRANULARITY` su tutti i simboli; KMNO e PENGU hanno recuperato dopo il SELL, ma
  con gap 25-60s, quindi non e' prova sufficiente per trattenere la posizione in runtime second-level.

Diagnosi del Consiglio:

- MS822 e' deployata ma non poteva attivarsi nella RUN 95: la protezione positive-MFE richiede MFE netto positivo, mentre
  tutti e tre i PAPER sono rimasti a `maxNetReturn=0`.
- Il problema osservato e' no-MFE/decay: trade BUY-confirmed che non partono mai devono poter uscire prima del timeout
  completo dell'advice.
- La correzione va nel SELL runtime come risk-control, non nella selection e non nei gate PAPER.
- La durata no-MFE deve essere agganciata al contratto ML/advice quando presente (`ml_advice_no_mfe_timeout_seconds`) e,
  in fallback, derivata da `ml_advice_duration_seconds` tramite metadata DB esplicito.

Implementato e verificato:

- `hft-common`: aggiunto `GuardOperator.ML_ADVICE_NO_MFE_DECAY_EXIT` e centralizzate le chiavi:
  `ml_advice_no_mfe_timeout_seconds`, `no_mfe_timeout_ratio`, `no_mfe_min_hold_seconds`,
  `no_mfe_max_net_return_ceiling`, `no_mfe_exit_net_return_ceiling`.
- `acdc`: `GuardEvaluator` valuta `ML_ADVICE_NO_MFE_DECAY_EXIT`.
- `acdc`: migration `V73__rem_no_mfe_decay_exit.sql` aggiunge guardia active `exit_ml_advice_no_mfe_decay`, priority 18,
  fra dynamic trailing/take-profit e loss-cap/timeout.
- `acdc`: la guardia vende solo se `hold_seconds` supera il timeout no-MFE, `max_net_return` resta sotto/uguale al ceiling
  e `net_return` resta sotto/uguale al ceiling di uscita.
- `acdc-vpn`: immagine rebuildata e container ricreato; Flyway MySQL ha applicato `V73`, schema operativo ora `v73`.

Verifiche completate:

- `hft-common`: `mvn -q test` OK.
- `hft-common`: `mvn -q install` OK.
- `acdc`: `mvn -q test` OK; Flyway test valida/applica 73 migration.
- `acdc`: `./mvnw -q -DskipTests package` OK.
- `acdc`: `docker compose --env-file docker/vpn/.env -f docker/vpn/compose.yml up -d --build --force-recreate acdc` OK.
- ACDC log prod: MySQL 8.0, schema da `72` a `73`, startup OK.
- MySQL operativo: guardie EXIT active in ordine: take-profit, dynamic trailing, no-MFE decay, loss-cap, timeout.
- FE proxy: `paperRunning=false`, `openPositions=0`, `activeAdvice=0`, `paperEligibleContractActiveAdvice=0`.

## Stato Repo

Repo modificati da committare: `hft-common`, `acdc`.

## Prossimo TODO

1. Committare e pushare `hft-common` e `acdc` con stesso MS.
2. Dal FE `/management`, ripartire con `AUTO_AB_START`; non usare `/pipelines`.
3. Avviare PAPER solo se `ML_READY=true` e solo `FORWARD_AB_98`.
4. Nella prossima RUN monitorare:
   - quante uscite avvengono per `EXIT_ML_ADVICE_NO_MFE_DECAY`;
   - se MS822 intercetta eventuali rientri sotto break-even dopo MFE positivo;
   - se WATCH conferma BUY e riduce le BUY non meritevoli senza bloccare segnali profittevoli.
