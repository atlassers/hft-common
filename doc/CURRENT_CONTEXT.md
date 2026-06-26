# Current Context

Ultimo aggiornamento: 2026-06-26 23:13 CEST.

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

Obiettivo eseguito: eliminare il residuo di soglia economica fissa nel contratto live advice, attivare la
`REM_PRE_BUY_WATCH_V1` da advice ML e verificare una nuova RUN `FORWARD_AB_98` da FE `/management`.

Implementato e deployato:

- `hft-common`: centralizzate costanti operative per advice payload e WATCH:
  `safe_net_return`, `max_net_return`, `loss_cap_net_return`, `min_economic_safe_net_return`,
  `pre_buy_watch_required`, `pre_buy_watch_timeout_seconds`, `watch_required`, `watch_timeout_seconds` e relativi
  campi `ml_advice_*`.
- `acdc`: `OutcomeQualityModelService` non calcola piu' `ml_advice_min_economic_safe_net_return` da
  `entry_friction + buffer`. ACDC consuma il valore scritto dall'advice; se assente non inventa un floor globale.
  `ml_advice_economic_safe_pass` richiede solo `safe_net_return > 0`, eventuale min advice-specific rispettato e
  `max_net_return >= safe_net_return`.
- `acdc`: `PreBuyWatchService` usa costanti centralizzate per advice id, rule id, generation e timeout fallback.
- `docbrown`: `LiveMlAdviceScoringService` scrive sempre nel live advice il contratto
  `pre_buy_watch_required=true`, `pre_buy_watch_timeout_seconds=max_buy_age_seconds` e
  `min_economic_safe_net_return` advice-specific. Rimosso il filtro live fisso
  `rem.ml.live_advice.min_expected_net_return` come gate preliminare.
- `docbrown`: rolling validation/promotion non usa piu' il floor economico fisso `friction + buffer = 0.003`.
  Il minimo economico e' candidate/advice-specific: `min(q10PositiveMaxNetReturn, worstWindowAvgEndReturn)` quando
  entrambi sono positivi, altrimenti `0`.

Verifiche completate:

- `hft-common`: `mvn -q install -DskipTests` OK.
- `acdc`: `mvn -q test` OK come regressione, ma usa H2/Testcontainers e non vale come validazione operativa.
- `docbrown`: `mvn -q test` OK.
- `acdc-vpn` redeployato da immagine `acdc:latest`; log startup su MySQL 8.0 OK.
- `docbrown` redeployato da immagine `docbrown:latest`; log startup su MySQL 8.0 OK.
- Validazione operativa eseguita su MySQL/container tramite FE proxy `/backoffice/management/actions/AUTO_FORWARD_AB_CYCLE_START`.

RUN verificata:

- Action FE/Kenshiro: `AUTO_FORWARD_AB_CYCLE_START`.
- Validation batch: `management-rolling-20260626T205700Z`, `strategicStatus=PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`.
- Live advice generation: `live-1782507433`, 5 advice salvate:
  `ICPUSDC`, `BCHUSDC`, `FETUSDC`, `HEIUSDC`, `PENDLEUSDC`.
- DB advice check: tutte le 5 advice hanno `pre_buy_watch_required=true`, `pre_buy_watch_timeout_seconds=20`,
  `min_economic_safe_net_return=0`, safe positivo.
- Forward A/B group: `ab98-20260626T205718Z`.
- PAPER B execution `93`: `STOPPED`, PnL netto `-0.0920963975765`.
  - `ICPUSDC`: `-0.129321679812`, max net return `0`.
  - `HEIUSDC`: `+0.2048469385215`, max net return `0.008193877551020408`.
  - `PENDLEUSDC`: `-0.167621656286`, max net return `0.000352433281004710`.
- SHADOW A execution `94`: `COMPLETED`, PnL netto `-0.3971208329067`.
  - `FETUSDC`: `-0.2081606207067`, max net return `0.000300518134715026`.
  - `BCHUSDC`: `-0.1889602122`, max net return `0`.
- WATCH evidence:
  - PAPER: `BUY_OPENED=3`, `BUY_REJECTED_RUNTIME=8`, `EXPIRED=4`.
  - SHADOW: `BUY_OPENED=2`, `EXPIRED=3`.

Interpretazione del Consiglio:

- Fix confermata: il residuo `economic safe return=0.003` non deve piu' essere generato come floor globale runtime/live.
- WATCH e' entrata nel giro runtime reale e ha filtrato segnali tramite scadenza, ma non ha ancora dimostrato di
  rendere profittevole la configurazione.
- La perdita PAPER non e' dovuta a mancata attivazione WATCH: 3 BUY sono state aperte per contratto confermato; 2 sono
  poi degradate durante SELL/hold. Il prossimo problema scientifico e' SELL/hold decay, non accesso alla PAPER.

Stato live dopo monitor:

- PAPER execution `93` chiusa.
- SHADOW execution `94` chiusa.
- Nessuna REAL avviata.
- Lo stato aggregato FE continua a mostrare `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE` e `mlReady=false`
  anche mentre una RUN e' partita/completata: e' un problema di esposizione stato/Kenshiro da correggere, non di DB run.

## Stato Repo

Repo modificati da committare: `hft-common`, `acdc`, `docbrown`.

## Prossimo TODO

1. Correggere stato aggregato FE/Kenshiro: `mlReady`, `paperRunning`, `shadowRunning`, `openPositions` non devono restare
   null/false quando `acdc_run_execution` contiene run attive o appena concluse.
2. Diagnosticare SELL/hold decay su execution `93`:
   - perche' `ICPUSDC` e `PENDLEUSDC` sono rimaste aperte fino a loss;
   - se serve trailing/timeout advice-specific piu' corto;
   - se WATCH deve richiedere anche conferma di momentum post-open o solo pre-BUY.
3. Eseguire forensics post-run su `93` con endpoint diagnostici ACDC e aggiornare il piano di tuning prima della prossima
   ricerca binaria delle soglie di selezione.
