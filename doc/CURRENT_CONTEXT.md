# Current Context

Ultimo aggiornamento: 2026-06-28 20:18 CEST.

Snapshot operativo corrente del workspace `/home/mbc/Documenti/ws/java/hft`.

## Gerarchia Documentale

1. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`
2. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/BOLLINGER_ONLY_PLAN.md`
3. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md`
4. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/STRATEGIC_REM_HANDOFF.md`

Se i documenti confliggono, prevale il piano strategico. Il current context contiene solo lo stato corrente e il prossimo
TODO; procedure, endpoint, payload e diagnostica stabile stanno nell'handoff.

## Vincoli Hard Correnti

- REAL vietata.
- PAPER solo da ciclo `/management` e solo se `BB_READY=true`.
- Il ciclo operativo parte da FE `/management` -> Kenshiro `/backoffice/management/*`.
- `/pipelines` non e' path operativo primario.
- Non allargare gate/live/SELL per forzare PAPER.
- SHADOW e Forward A/B 98 sono legacy operativi ritirati dal ciclo management.
- MySQL e container deployati sono obbligatori per validazione operativa; H2 non vale come validazione operativa.
- Il Consiglio elabora piani e monitora avanzamenti; Codex implementa secondo le indicazioni e produce report finale.
- Stringhe operative, action id, status, config key e payload key devono essere centralizzati in `hft-common` o registry
  locali usati come shim.
- FE e script devono mantenere mapping 1:1 con i contract/payload comuni quando esistono.
- `BOLLINGER_ONLY_PLAN.md` e' il piano strategico vincolante: sono ammessi solo setup
  `BB_REENTRY_MEAN_REVERSION_LONG` e `BB_SQUEEZE_BREAKOUT_LONG`, solo trigger Bollinger e nessuna famiglia decisionale
  esterna.

## Stato Ultima Attivita'

Aggiornamento operativo MS904 completato:

- `BOLLINGER_ONLY_PLAN.md` e' il piano strategico vincolante e sostituisce il precedente piano REM outcome-first.
- `hft-common` introduce enum/costanti per setup, trigger, protocollo `BOLLINGER_ONLY_V2` e contract `bb_*`.
- `docbrown` promuove advice setup-specifiche:
  - `BB_REENTRY_MEAN_REVERSION_LONG`;
  - `BB_SQUEEZE_BREAKOUT_LONG`.
- `docbrown` non usa piu' pesi DB `bb.rolling.selection.*` per ordinare le candidate.
- `acdc` compra da WATCH solo con dispatch setup-specifico e fail-closed se setup/trigger/soglie Bollinger mancano.
- `acdc` migration `V84__bollinger_only_v2_runtime_config_purge.sql` rimuove dal DB live:
  - `bb.signature_advice.*`;
  - `bb.paper_candidate.*`;
  - `bb.rolling.selection.*`;
  - audit round-robin storici persistiti sotto `bb.management.round_robin.audit.*`.
- `kenshiro` espone solo azioni management Bollinger/PAPER: niente SHADOW, niente Forward A/B, niente enable
  signature/paper-candidate.
- `hft-fe` `/management` e' allineato al ciclo PAPER Bollinger-only e non espone azioni legacy.

Verifiche completate:

- `hft-common mvn -q install`;
- `docbrown mvn -q -Dtest=BlankRemCandidateServiceTest test`;
- `docbrown ./mvnw -q -DskipTests package`;
- `acdc mvn -q -Dtest=AcdcRunServiceTest,it.mbc.hft.acdc.config.RemCurrentConfigurationTest test`;
- `acdc ./mvnw -q -DskipTests package`;
- `kenshiro mvn -q -DskipTests compile`;
- `kenshiro mvn -q -DskipTests package`;
- `hft-fe npm run check`;
- `hft-fe npm run build`.

Deploy completato:

- `docbrown` up su 8083;
- `acdc-vpn` up su 8091, schema `hft` migrato a `acdc_flyway_schema_history` versione `84`;
- `kenshiro-local` up su 8085;
- `hft-fe-local` up su 5173.

Validazione runtime post-V84:

- `PAPER 41`, generation `management-rolling-20260628T181353Z`, protocollo `BOLLINGER_ONLY_V2`:
  - started `2026-06-28T18:14:30Z`, completed `2026-06-28T18:16:40Z`;
  - posizioni `0`;
  - BUY accettate `0`;
  - WATCH scadute `19`;
  - decisioni `WATCH_WAITING_BUY_CONTRACT` `684`;
  - interpretazione: WATCH non compra sul solo vincolo temporale.
- `PAPER 42`, generation `management-rolling-20260628T181650Z`, protocollo `BOLLINGER_ONLY_V2`:
  - started `2026-06-28T18:17:23Z`, stopped `2026-06-28T18:18:01Z`;
  - posizioni `0`;
  - BUY accettate `0`;
  - avviata perche' l'automazione aveva gia' superato la promotion quando e' stato inviato lo stop; fermata
    manualmente per lasciare runtime controllato.
- Stato live finale dopo redeploy da `/management`:
  - `paperRunning=false`;
  - `openPositions=0`;
  - `activeAdvice=0`;
  - `paperEligibleContractActiveAdvice=0`;
  - `BB_READY=false`;
  - `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`;
  - automazione `STOPPED`.

Nota tecnica:

- Le ultime PAPER `39` e `40` pre-V84 erano gia' `BOLLINGER_ONLY_V2`; `40` ha chiuso con due posizioni e somma
  posizioni positiva, ma e' stata osservata una possibile incoerenza contabile tra `acdc_run_execution` e
  `acdc_paper_run.net_profit_quote`. Da verificare separatamente; non modifica il vincolo strategico.

## Stato Storico Precedente

Aggiornamento strategico MS in corso:

- MS900: rimozione ponte HFT/scalping legacy e mining outcome-first storico. Le tabelle
  `stan_strategy_parameters`, `scalping_scout_candidate_dataset_config`, `scalping_scout_candidate_dataset_run`,
  `scalping_scout_runtime_config`, `acdc_outcome_signature`, `acdc_outcome_training_sample` e
  `acdc_rem_data_quality_band_model` sono legacy nel processo Bollinger-only e vengono droppate da nuove migration.
  Kenshiro `/management` legge le candidate operative da `acdc_live_bb_advice`, non da parametri HFT legacy.

- MS899: pulizia residuale DB/codice/script post Bollinger-only. Le tabelle `best_winner_signature` e
  `best_winner_window_config` sono legacy: ACDC `V81` e DocBrown `V31` le droppano, e sono stati rimossi endpoint,
  pagina FE, contratti comuni, script ACDC e modello Python DocBrown collegati.

- Il processo REM runtime e' stato riallineato a Bollinger-only: `ML -> live-score -> WATCH -> BUY -> SELL -> forensics`
  resta invariato, ma i criteri decisionali sono solo `bb_*`.
- Il ciclo management e' PAPER-only: il ramo SHADOW non viene piu' avviato, fermato o mostrato come braccio operativo
  parallelo da `/management`.
- I nuovi payload runtime filtrano i campi `reversal_*`; Bollinger e' l'unica famiglia di feature emessa nei nuovi
  snapshot/advice/policy operativi.
- `hft-common`: aggiunte le costanti condivise Bollinger per feature, contract block, baseline label e filtri candidato.
- `docbrown`: candidate/rolling promotion non generano piu' regole `symbol=...`; live-score rank e target cap non usano
  piu' slope/trough/reversal; live revalidation e' limitata a feature Bollinger.
- `acdc`: WATCH conferma il BUY solo su `bb_buy_contract_pass`; `BB_ADVICE_PAPER_ELIGIBLE` richiede contratto Bollinger
  completo/fresco ma non blocca piu' per drift/live-revalidation/reversal.
- `acdc`: migration `V76__bollinger_only_watch_entry.sql` disattiva le guardie ENTRY legacy di mercato,
  `V77__bollinger_only_entry_ranking.sql` lascia attivo un solo ranking ENTRY (`bb_buy_contract_pass`) e
  `V78__expire_non_bollinger_active_advice.sql` scade advice attive legacy prive di contratto `bb_*`. Restano attive
  solo guardie tecniche minime (`entry_price_present`, `entry_snapshot_fresh`).
- Profilo sperimentale Bollinger corrente: rolling validation `2h`, aggregazione feature `1m`
  (`market.microbar.seconds=60`) e `featureWindowMinutes=20`, cosi' BB20 usa circa 20 minuti coerenti tra ML,
  live-score e WATCH; il polling WATCH puo' restare piu' frequente, ma il trigger BUY resta `bb_buy_contract_pass=1`.
- Validazione runtime immediata post-cambio:
  - PAPER `37`, group `ab98-20260628T163823Z`, generation `management-rolling-20260628T163751Z`: `SUSDC`, closed
    `EXIT_BB_ADVICE_LOSS_CAP`, hold `23s`, max MFE netto `0`, net `-0.009032604668395701`,
    realized quote `-0.22581511658452`. Contract completo; entry drift non passato (`0.0085949177877429 > 0.0015`),
    quindi evidenza contaminata per analisi economica.
  - PAPER `38`, group `ab98-20260628T164131Z`, generation `management-rolling-20260628T164107Z`: `SUSDC`, closed
    `EXIT_BB_ADVICE_LOSS_CAP`, hold `101s`, max MFE netto `0`, net `-0.008278373382624769`,
    realized quote `-0.20695933439881`. Contract completo, `entry_drift=0`, `entry_drift_pass=1`.
  - Post-sell forensics `37`/`38`: nessun recupero/safe hit osservato dopo exit, ma verdict
    `INCONCLUSIVE_GRANULARITY` per dati post-exit a gap 56-60s.
  - Runtime finale: `paperRunning=false`, `openPositions=0`, `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`.
- Verifiche locali completate: `hft-common mvn -q install`, `docbrown mvn -q -Dtest=BlankRemCandidateServiceTest test`,
  `acdc mvn -q -Dtest=AcdcRunServiceTest test`,
  `acdc mvn -q -Dtest=it.mbc.hft.acdc.config.RemCurrentConfigurationTest test`,
  `docbrown ./mvnw -q -DskipTests package`, `acdc ./mvnw -q -DskipTests package`.

Aggiornamento operativo in corso:

- `docbrown`: separato il significato temporale di `max_buy_age_seconds` e `pre_buy_watch_timeout_seconds`. Il primo
  resta freschezza del live-score/advice; il secondo deriva dalla durata storica della firma (`entry_validity_seconds`,
  `duration_seconds` o timeout no-MFE).
- `docbrown`: la rolling promotion non scarta piu' advice solo per live revalidation istantanea negativa. Se il batch e'
  promotable, pubblica advice `PAPER_ELIGIBLE` WATCH-eligible e riporta eventuali rejection live come diagnostica della
  riga. ACDC resta responsabile di comprare solo quando il contratto BUY diventa vero durante la WATCH.
- `hft-fe`: `/management` mostra la WATCH come osservatore con conteggio attivo e finestra, e la mappa runtime distingue
  freschezza live, contratto BUY e finestra storica WATCH.
- Verifiche locali completate: `docbrown mvn -q test`, `docbrown ./mvnw -q -DskipTests package`,
  `hft-fe npm run check`, `hft-fe npm run build`.

Aggiornamento operativo MS884:

- `kenshiro`: rimosso il fast-path dell'autociclo che, dopo il live-score post-validazione, poteva avviare
  `PAPER_FORWARD_AB_START` direttamente da advice `PROMOTED_RULE`. L'autociclo deve passare sempre da
  `ROLLING_PROMOTION`, cosi' PAPER testa solo il candidato rolling e non un universo tecnico diverso.
- `kenshiro`: `ROLLING_PROMOTION` usa `expireExisting=true` di default e salva fino a 3 simboli rolling ordinati
  (`selectedCandidate` + top candidates non duplicati). DocBrown promuove comunque solo il subset che passa
  promotability e live revalidation.
- `kenshiro`: il payload di `ROLLING_PROMOTION` passa `maxBuyAgeSeconds` dal runtime
  `bb.live_advice.max_buy_age_seconds`; valore operativo corrente: `75`.
- `docbrown`: `ROLLING_PAPER` ora pubblica anche `bb_advice_no_mfe_timeout_seconds` e i blocchi completi
  `history_*`/`live_*`, inclusi `live_max_buy_age_seconds` e `live_no_mfe_timeout_seconds`.
- Validazione runtime: run PAPER `143` e SHADOW `144` sono partite dopo il fix di contract rolling. PAPER `143` ha
  comprato solo `SYRUPUSDC` da `ROLLING_PAPER`, ha chiuso `EXIT_BB_ADVICE_NO_MFE_DECAY`, contract diagnostics
  `complete=true`, hold `125s`, net return `-0.003277493606138107`. SHADOW `144` e' partita senza posizioni.
- Retry successivi: short-list rolling `RESOLVUSDC,SNXUSDC,REUSDC` e poi `RESOLVUSDC,REUSDC,SNXUSDC` non hanno prodotto
  advice PAPER per live revalidation drift / non-promotability. Il sistema e' tornato fail-closed a
  `BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, con `paperRunning=false`, `openPositions=0`, `activeAdvice=0`.
- Interpretazione del Consiglio: obiettivo operativo PAPER_ELIGIBLE raggiunto e contratto rolling corretto; obiettivo
  economico non raggiunto. Non allargare gate/SELL per forzare run: i candidati rolling correnti decadono in live
  revalidation oppure producono zero-MFE loss.

Obiettivo eseguito: correzione contratto end-to-end SELL no-MFE, validazione operativa WATCH/no-MFE dopo refactor
stringhe/contract e separazione esplicita dei blocchi `history_*`, `live_*`, `entry_*`, `exit_*`.

Diagnosi corretta:

- Le posizioni PAPER `99`-`105` avevano `policy_json.bb_advice_no_mfe_timeout_seconds = NULL`.
- Dopo il primo fix, DocBrown pubblicava la key nei `ruleJson` di mining/promotion, ma non nel path effettivo
  `LiveBbAdviceScoringService.liveAdvice(...)` usato dal ciclo management.
- Le RUN `107`/`108` e `109`/`110` del 2026-06-27 restano contaminate per no-MFE: la generation live non conteneva
  `bb_advice_no_mfe_timeout_seconds`; SHADOW `110` e' anche lifecycle-contaminata per stop/abandon.
- I reject `PAPER_BUDGET_OR_EXCHANGE_RULES_REJECTED` osservati non erano exchange filter: con budget PAPER 100,
  `min_trade_quote=25` e fee, dopo 3 posizioni restavano circa 24.925 quote, sotto il minimo per un quarto BUY.
- WATCH non e' bocciata: nelle run pulite successive ha aperto, confermato BUY e alimentato SELL no-MFE correttamente.

Diagnosi del Consiglio:

- Saggio ascoltatore: fermare ogni inferenza di performance sulle run contaminate e ripartire da un contratto pulito.
- Scienziato severo: nessun fallback su timeout no-MFE; se il campo manca, la guardia deve restare fail-closed.
- Mediano pragmatico: correggere anche il live-score producer e aggiungere un guardrail ACDC di entry/readiness per non
  avviare PAPER con advice prive del campo.
- Decisione unica: no-MFE runtime usa solo il timeout pubblicato dall'advice live (`live_no_mfe_timeout_seconds`,
  mappato in ACDC nel campo canonico `bb_advice_no_mfe_timeout_seconds` per le guardie). ACDC deve considerare non
  contract-active e respingere PAPER ENTRY se il campo manca.

Implementato e verificato:

- `hft-common`: centralizzata la chiave `bb_advice_no_mfe_timeout_seconds`; rimosse le costanti di fallback ratio/min-hold.
- `hft-common`: aggiunte le chiavi condivise `history_*`, `live_*`, `entry_*`, `exit_*` per separare contratto storico
  ML, contratto live-score, fotografia BUY e fotografia SELL.
- `docbrown`: il producer live pubblica `bb_advice_no_mfe_timeout_seconds` nell'advice usando la durata
  candidate-specific del contratto generato.
- `docbrown`: source/validity source literal dei due `ruleJson` toccati sono stati portati in `OperationalString`.
- `docbrown`: `LiveBbAdviceScoringService.liveAdvice(...)` non modifica piu' semanticamente i campi ML originari
  dell'advice. Produce un blocco `history_*` copiato dal contratto storico e un blocco `live_*` speculare/valorizzato
  dal live-score. Il timeout no-MFE runtime ora e' `live_no_mfe_timeout_seconds`; per regole storiche viene derivato
  una tantum da `entry_validity_seconds` nel producer live, non in ACDC.
- `acdc`: `OutcomeQualityModelService` copia `history_*` e `live_*`, usa `live_*` per valorizzare i campi canonici
  `bb_advice_*` consumati dalle guardie runtime, e scrive `entry_*` al momento della decisione BUY.
- `acdc`: `MlAdviceFeatures.exitFeatures(...)` propaga da `policy_json` i blocchi `history_*`, `live_*` ed `entry_*`
  verso la SELL, evitando che i live feature ricalcolati in uscita sovrascrivano la fotografia di ingresso.
- `acdc`: `PaperRunService` e `ShadowRunService` aggiungono `exit_*` nei feature snapshot di uscita mantenendo i campi
  legacy usati dalle guardie.
- `acdc`: `MlReadinessDiagnosticsService` considera contract-active le advice PAPER solo se il blocco live contiene
  `live_no_mfe_timeout_seconds > 0` e la freschezza live e' ancora valida.
- `acdc`: `PaperRunService` respinge PAPER ENTRY con `PAPER_ADVICE_NO_MFE_TIMEOUT_MISSING` se la feature no-MFE manca
  o non e' positiva.
- `acdc`: `GuardEvaluator` non calcola piu' fallback da durata/ratio; senza timeout esplicito la guardia no-MFE resta
  `EXIT_HOLD`.
- `acdc`: `V74__remove_no_mfe_timeout_fallback_metadata.sql` rimuove ratio/min-hold dal metadata operativo della guardia
  e usa `min_threshold=0`, `max_threshold=0` come confine DB-driven esplicito per zero-MFE/break-even.
- `acdc`: `V75__expire_stale_live_advice.sql` marca `EXPIRED` le advice `ACTIVE` gia' scadute, eliminando residui senza
  nuovo contratto no-MFE.
- `hft-common/doc`: piano strategico e handoff aggiornati: il timeout no-MFE deve arrivare solo da
  `bb_advice_no_mfe_timeout_seconds`; nessun fallback da durata, ratio, metadata DB o config runtime.

Verifiche completate:

- `hft-common`: `mvn -q install` OK.
- `docbrown`: `mvn -q test` OK.
- `docbrown`: `./mvnw -q -DskipTests package` OK.
- `docbrown`: container `docbrown` rebuildato/ricreato dopo la separazione `history_*`/`live_*`, startup prod OK su MySQL
  8.0.
- `acdc`: `mvn -q test` OK; Flyway test valida/applica 75 migration.
- `acdc`: `mvn -q -Dtest=MlAdviceFeaturesTest test` OK con copertura specifica della propagazione
  `history_*`/`live_*`/`entry_*` verso SELL.
- `acdc`: `./mvnw -q -DskipTests package` OK.
- `acdc`: container `acdc-vpn` rebuildato/ricreato dopo la separazione contract block, startup prod OK su MySQL 8.0.
- ACDC log prod: MySQL 8.0, schema operativo up to date, startup OK.
- MySQL operativo: `acdc_flyway_schema_history` ultimo `version=75`, `success=1`.
- MySQL operativo: guardia `exit_bb_advice_no_mfe_decay` ha `min_threshold=0`, `max_threshold=0`, metadata senza ratio/min-hold.
- MySQL operativo: generation `live-1782565383` e `live-1782565442` hanno 5/5 advice con
  `bb_advice_no_mfe_timeout_seconds`.
- RUN pulita `111`/`112`, group `ab98-20260627T130406Z`: PAPER `111` ha 3 posizioni chiuse con
  `EXIT_BB_ADVICE_NO_MFE_DECAY`; SHADOW `112` ha `ALGOUSDC` chiusa take-profit e `HBARUSDC` chiusa no-MFE.
- RUN successiva `113`/`114`, group `ab98-20260627T130751Z`: PAPER `113` ha 2 posizioni chiuse no-MFE e 1 dynamic
  trailing; SHADOW `114` e' stata stoppata/abbandonata dopo `AUTO_AB_STOP`, quindi non e' evidenza Forward A/B pulita.
- Nuovo ciclo management del 2026-06-27:
  - `REFRESH_DIAGNOSTICS` iniziale: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `paperRunning=false`,
    `openPositions=0`, nessuna advice attiva.
  - `AUTO_AB_START` da FE `/management` ha generato advice fresche contract-active con
    `bb_advice_no_mfe_timeout_seconds`.
  - `115`/`116`, group `ab98-20260627T131935Z`: PAPER `115` `STOPPED`, 3 posizioni chiuse, net
    `-0.243031196346800000`; SHADOW `116` `COMPLETED`, 2 posizioni chiuse, net `-0.083058606990000000`.
  - `117`/`118`, group `ab98-20260627T132306Z`: PAPER `117` `STOPPED`, 3 posizioni chiuse, net
    `-0.134157024976800000`; SHADOW `118` `COMPLETED`, 2 posizioni chiuse, net `-0.086254814085000000`.
  - `119`/`120`, group `ab98-20260627T132638Z`: PAPER `119` `STOPPED`, 3 posizioni chiuse, net
    `+0.028302675459600000`; SHADOW `120` `COMPLETED`, 2 posizioni chiuse, net `-0.229538972310000000`.
  - `AUTO_AB_STOP` e' stato inviato in finestra pulita dopo `119` chiusa e `120` completata, ma un worker aveva gia'
    iniziato il ciclo successivo.
  - Race finale `121`/`122`, group `ab98-20260627T133008Z`: PAPER `121` `STOPPED`, 3 posizioni chiuse, net
    `+0.008618152636200000`; SHADOW `122` `COMPLETED`, 2 posizioni chiuse, net `-0.088206647087000000`.
  - Tutte le posizioni PAPER/SHADOW `115`-`122` hanno `bb_advice_no_mfe_timeout_seconds` valorizzato nel `policy_json`
    (`120` o `175`) e `bb_advice_pre_buy_watch_timeout_seconds=20`; PAPER `115`, `117`, `119`, `121` hanno generation
    source rispettivamente `live-1782566372`, `live-1782566582`, `live-1782566795`, `live-1782567005`.
- FE/Kenshiro finale post-deploy: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `mlReady=false`,
  `paperRunning=false`, `openPositions=0`, `activeAdvice=0`, `paperEligibleActiveAdvice=0`,
  `paperEligibleContractActiveAdvice=0`.
- Automazione finale: `automationEnabled=false`, `automationStopRequested=true`, last stop reason `USER_STOP`.
- Interpretazione: WATCH/no-MFE e contract runtime sono operativi; performance non promossa. Le PAPER `115`, `117`, `119`,
  `121` sono `STOPPED` e quindi non sono evidenza scientifica pristine per `PASS_BASELINE`, anche se tutte le posizioni
  risultano chiuse e senza reject budget/exchange rilevanti.

## Stato Repo

Nuovo avanzamento post MS881:

- `acdc`: endpoint `/diagnostics/acdc/paper/sell-capture` e `/diagnostics/acdc/paper/post-sell-forensics` espongono
  ora un blocco `contract` con mappe `history`, `live`, `entry`, `exit` e flag `complete`.
- `kenshiro`: le query management diagnostiche leggono i dettagli da `live_*`/`entry_*` dove il contratto separato e'
  disponibile; `paperEligibleContractActiveAdvice` richiede `live_no_mfe_timeout_seconds > 0` e freshness
  `live_max_buy_age_seconds`.
- `kenshiro`: corretto un bug latente in `signalFreshness(...)`: le stringhe MySQL `str_to_date` con percentuali SQL
  sono state escapate per l'uso con `String.formatted`.
- Verifica endpoint: `/management/state` torna correttamente `BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`; gli endpoint ACDC
  mostrano `contract` presente. Le run storiche precedenti alla separazione mostrano `contract.complete=false`, quindi
  non vanno usate per validare la completezza dei nuovi blocchi.

Repo con modifiche coerenti da committare/pushare con lo stesso MS: `hft-common`, `acdc`, `kenshiro`.

Aggiornamento MS890 del 2026-06-27:

- `acdc`: `MlReadinessDiagnosticsService` e' stato allineato al path rolling advice-driven. Se esistono advice
  `PAPER_ELIGIBLE` attive, `ML_RULES_MISSING` e `ML_PROMOTED_RULES_MISSING` non bloccano piu' `BB_READY`; restano
  warning diagnostici. Le guardie su advice attive, advice paper-eligible, contratto attivo e posizioni aperte restano
  bloccanti.
- Deploy runtime: container `acdc-vpn` rebuildato e riavviato, startup prod OK su MySQL 8.0.
- Verifica: `acdc mvn -q test` OK e `./mvnw -q -DskipTests package` OK.
- RUN runtime dopo fix:
  - `1`/`2`, group `ab98-20260627T203315Z`: PAPER B partita da generation
    `management-rolling-20260627T203248Z`; WATCH ha aperto `JTOUSDC`, sell
    `EXIT_BB_ADVICE_NO_MFE_DECAY`, net `-0.1047430562718`; una WATCH PAPER e una WATCH SHADOW sono scadute senza BUY.
  - `3`/`4`, group `ab98-20260627T203619Z`: PAPER B partita da generation
    `management-rolling-20260627T203556Z`; nessuna WATCH PAPER ha confermato il BUY, PAPER chiusa con 0 trade; SHADOW
    baseline completata con PnL runtime negativo.
- Stato finale post stop automazione: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `mlReady=false`,
  `paperRunning=false`, `openPositions=0`, nessuna advice attiva. Automazione fermata con `AUTO_AB_STOP`.
- Interpretazione: il blocco readiness che impediva lo start PAPER era un mismatch ACDC legacy vs rolling advice-driven ed
  e' corretto. WATCH observer e scadenza contratto sono operativi. Trading non promosso: evidenza PnL ancora negativa o
  assente, quindi serve nuova FORWARD_AB_98 pulita prima di qualsiasi promozione.

Aggiornamento finale MS891 del 2026-06-27:

- Durante commit/push una race del worker ha avviato `5`/`6`, group `ab98-20260627T203918Z`, nonostante
  `automationStopRequested=true`.
- Sono stati inviati `PAPER_STOP_BUY` e `SHADOW_STOP_BUY`; la run e' drenata senza posizioni aperte finali.
- `5` PAPER: `STOPPED`, 0 posizioni, 0 trade, budget invariato.
- `6` SHADOW: `COMPLETED`, 1 posizione runtime chiusa, realized `+0.0239148305291`; le diagnostiche paper non la
  contano come trade PAPER B.
- `AUTO_AB_STOP` reinviato a stato drenato per riallineare la diagnostica: finale
  `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `mlReady=false`, `paperRunning=false`, `openPositions=0`,
  `activeAdvice=0`, `automationEnabled=false`, `automationStatus=STOPPED`.

Aggiornamento MS892 del 2026-06-27:

- Verifica WATCH sul BUY `JTOUSDC` execution `1`: all'apertura WATCH il contratto non era paper-eligible
  (`bb_advice_paper_eligible=0`, `bb_advice_live_revalidation_pass=0`, failure su `reversal_trough_age_seconds`);
  al BUY, 12 secondi dopo, `bb_advice_paper_eligible=1`, `bb_advice_live_revalidation_pass=1` e failure `0`.
- Interpretazione corretta: WATCH non ha comprato solo per vincolo temporale; ha rispettato il trigger configurato.
  Il contratto resta pero' debole dal punto di vista trading, perche' il BUY non ha prodotto MFE (`maxNetReturn=0`) ed
  e' uscito con `EXIT_BB_ADVICE_NO_MFE_DECAY`.
- `acdc`: aggiunti contatori diagnostici nei feature della decisione WATCH:
  `pre_buy_watch_trigger_checked`, `pre_buy_watch_trigger_failed`, `pre_buy_watch_trigger_passed`.
- `acdc`: corretto `source_generation_id` su `acdc_pre_buy_watch`, risolvendolo dall'advice sorgente invece di tentare di
  leggerlo dalla feature map numerica.
- `acdc`: rimosso l'uso di literal `WATCHING` dalle query `PreBuyWatchRepository`, usando enum/costante.
- Verifiche: `hft-common mvn -q install` OK, `acdc mvn -q -Dtest=AcdcRunServiceTest test` OK, `acdc mvn -q test` OK,
  `acdc ./mvnw -q -DskipTests package` OK. Container `acdc-vpn` rebuildato e riavviato, startup prod OK su MySQL 8.0.
- Stato finale runtime post deploy: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `mlReady=false`,
  `paperRunning=false`, `openPositions=0`, `activeAdvice=0`, automazione `STOPPED`.

Aggiornamento MS893 del 2026-06-27:

- RUN audit `7`/`8`, group `ab98-20260627T212331Z`, source generation `management-rolling-20260627T212315Z`:
  - PAPER `7`: WATCH `OPNUSDC` e' rimasta in attesa con `pre_buy_watch_trigger_passed=0` e failure `5/6`, quindi non
    ha comprato. Questo e' il comportamento corretto.
  - SHADOW `8`: ha confermato BUY su `OPNUSDC` pur avendo `pre_buy_watch_trigger_passed=0` e failure `5`. Causa:
    path SHADOW/exploration poteva rendere `entryDecision.accepted=true` prima della WATCH.
  - Run fermata con `PAPER_STOP_BUY`, `SHADOW_STOP_BUY`, `AUTO_AB_STOP`; finale senza posizioni aperte.
- Fix ACDC: `PreBuyWatchService` ora considera `entryDecision.accepted()` necessario ma non sufficiente. Se il trigger
  audit non passa, la WATCH resta `WATCH_WAITING_BUY_CONTRACT` e non conferma BUY. Questo vale anche per SHADOW.
- Fix diagnostico ACDC: `markBuyOpened`/`markBuyRejected` non sovrascrivono piu' `last_feature_json` con lo snapshot
  nudo, preservando i contatori `pre_buy_watch_trigger_*`.
- Verifiche: `acdc mvn -q -Dtest=AcdcRunServiceTest test` OK, `acdc mvn -q test` OK,
  `acdc ./mvnw -q -DskipTests package` OK. Container `acdc-vpn` rebuildato e riavviato, startup prod OK.
- Stato finale runtime post deploy: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `mlReady=false`,
  `paperRunning=false`, `openPositions=0`, `activeAdvice=0`, automazione `STOPPED`.

Aggiornamento MS898 del 2026-06-28:

- Pulizia Bollinger-only completata su DB, documentazione, script e moduli operativi.
- `acdc`: aggiunta migration `V80__bollinger_only_residual_config_cleanup.sql` per rimuovere config vive residue
  `observed`, `paper.session_guard.*`, `live_audit.*`, `rolling.lifecycle.*` e retry live-revalidation, mantenendo
  `bb.promotion.mode=BOLLINGER_ONLY`.
- `acdc`: rimosse costanti cooldown zero-MFE non piu' referenziate. La metrica zero-MFE resta ammessa solo come forensics
  post-trade, non come selezione o guardia extra.
- `docbrown`: rimosso `LiveRuleAuditService` e le costanti di live-audit/rolling-lifecycle non piu' operative.
- `kenshiro`: il cockpit `/management` non espone piu' action diagnostiche legacy
  near-miss/EV/lifecycle/live-revalidation/entry-decay/false-continuation/inverse. L'autociclo usa solo cooldown standard
  dopo terminali fail-closed; il retry breve da live-revalidation drift e' ritirato.
- `hft-common`: rimosse dal contract comune le suffix key del retry live-revalidation e aggiornati piano/handoff al path
  Bollinger-only.
- Verifiche locali: `hft-common mvn -q install` OK, `docbrown mvn -q -DskipTests compile` OK, `acdc mvn -q -DskipTests
  compile` OK, `kenshiro mvn -q -DskipTests compile` OK, `kenshiro mvn -q -Dtest=ManagementServiceCooldownTest test`
  OK, `acdc mvn -q -Dtest=it.mbc.hft.acdc.config.RemCurrentConfigurationTest test` OK con 80 migration validate.
- Deploy runtime: `acdc-vpn`, `docbrown` e `kenshiro-local` rebuildati/riavviati; startup prod OK. ACDC prod ha
  applicato `V80`, schema `hft` ora a versione 80.
- MySQL operativo post-deploy: config residue `observed`/`revalidation`/`zero_mfe`/`session_guard`/`lifecycle` = 0,
  tabella legacy `acdc_reversal_ml_rule` assente, posizioni PAPER/SHADOW aperte = 0.
- Kenshiro `/management/state`: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `mlReady=false`,
  `promotionMode=BOLLINGER_ONLY`, `paperRunning=false`, `openPositions=0`, `activeAdvice=0`,
  `paperEligibleActiveAdvice=0`, `paperEligibleContractActiveAdvice=0`.

## Prossimo TODO

1. Committare e pushare MS898 sui repo modificati.
2. Da FE `/management`, nuova RUN solo dopo nuove advice Bollinger `PAPER_ELIGIBLE` fresche e `BB_READY=true`.
