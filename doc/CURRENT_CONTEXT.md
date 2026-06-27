# Current Context

Ultimo aggiornamento: 2026-06-27 10:16 CEST.

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

Obiettivo eseguito: RUN Forward A/B dopo fix SELL no-MFE, diagnostica aggregata e stop automazione.

RUN analizzata:

- PAPER `99`: `STOPPED`, PnL `-0.29739923458501`, 3 trade, 0 win, 3 loss, 3 `EXIT_ML_ADVICE_NO_MFE_DECAY`.
- PAPER `101`: `STOPPED`, PnL `-0.1877739242187`, 3 trade, 0 win, 3 loss, 3 `EXIT_ML_ADVICE_NO_MFE_DECAY`.
- PAPER `103`: `STOPPED`, PnL `+0.01173248822932`, 3 trade, 1 win, 2 loss.
  - `HEIUSDC`: `EXIT_ML_ADVICE_TAKE_PROFIT`, net `+0.1180686402786`, hold 49s, `maxNetReturn=0.004722745625841184`.
  - `2ZUSDC` e `ALGOUSDC`: `EXIT_ML_ADVICE_NO_MFE_DECAY`, `maxNetReturn=0`.
- PAPER `105`: `COMPLETED` dopo `AUTO_AB_STOP`/drain, PnL `-0.17104095233731`, 3 trade, 0 win, 3 loss,
  3 `EXIT_ML_ADVICE_NO_MFE_DECAY`.
- Aggregato PAPER `101,103,105`: 9 trade, 1 win, 8 loss, PnL `-0.34708238832669`, 1 take-profit,
  8 no-MFE decay, 0 timeout, 0 loss-cap.
- Forward A/B gruppo migliore `ab98-20260627T080801Z`: B PAPER `103` positivo `+0.01173248822932`, A SHADOW `104`
  negativo `-0.1326908504614`; diagnostics `FORWARD_AB_98_READY` senza blockers.

Diagnosi del Consiglio:

- WATCH funziona come pre-BUY runtime: conferma BUY reali e non e' piu' shadow tecnico.
- SELL risk-control funziona: i casi zero-MFE non arrivano piu' al timeout lungo; escono con
  `EXIT_ML_ADVICE_NO_MFE_DECAY`.
- Il take-profit cattura profitto quando il segnale parte (`HEIUSDC` nella PAPER 103).
- Il collo di bottiglia ora non e' il SELL ma la selection/upside: troppi BUY confermati da WATCH restano zero-MFE.
- Prossima correzione consigliata: migliorare ranking/selection verso candidati con upside reale senza abbassare SELL o
  PAPER gate e senza introdurre soglie statiche globali.

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
- `acdc`: diagnostics PAPER scoring e Forward A/B espongono `noMfeDecayExits`.
- Automazione `AUTO_AB_STOP` eseguita da FE `/management`; stato finale `automationEnabled=false`, `automationStatus=STOPPED`,
  `paperRunning=false`, `openPositions=0`, `activeAdvice=0`.

Verifiche completate:

- `hft-common`: `mvn -q test` OK.
- `hft-common`: `mvn -q install` OK.
- `acdc`: `mvn -q test` OK; Flyway test valida/applica 73 migration.
- `acdc`: `./mvnw -q -DskipTests package` OK.
- `acdc`: `docker compose --env-file docker/vpn/.env -f docker/vpn/compose.yml up -d --build --force-recreate acdc` OK.
- ACDC log prod: MySQL 8.0, schema da `72` a `73`, startup OK.
- MySQL operativo: guardie EXIT active in ordine: take-profit, dynamic trailing, no-MFE decay, loss-cap, timeout.
- FE proxy finale: `paperRunning=false`, `openPositions=0`, `activeAdvice=0`, `paperEligibleContractActiveAdvice=0`.
- `GET /diagnostics/acdc/paper/scoring?executionIds=103`: `noMfeDecayExits=2`, `takeProfitExits=1`, PnL positivo.
- `GET /diagnostics/acdc/forward-ab/98?groupId=ab98-20260627T080801Z`: `FORWARD_AB_98_READY`, B PAPER positivo,
  A SHADOW negativo.

## Stato Repo

Repo modificati da committare: `hft-common`, `acdc`.

## Prossimo TODO

1. Committare e pushare `hft-common` e `acdc` con stesso MS per diagnostics/doc run.
2. Non riavviare automazione prima del prossimo piano del Consiglio.
3. Prossimo piano: correggere selection/ranking/upside per ridurre candidati zero-MFE, mantenendo:
   - WATCH pre-BUY runtime;
   - take-profit prioritario;
   - no-MFE decay;
   - niente REAL;
   - PAPER solo `FORWARD_AB_98` e solo se `ML_READY=true`.
