# Current Context

Ultimo aggiornamento: 2026-06-27 22:24 CEST.

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
  `rem.ml.live_advice.max_buy_age_seconds`; valore operativo corrente: `75`.
- `docbrown`: `ROLLING_PAPER` ora pubblica anche `ml_advice_no_mfe_timeout_seconds` e i blocchi completi
  `history_*`/`live_*`, inclusi `live_max_buy_age_seconds` e `live_no_mfe_timeout_seconds`.
- Validazione runtime: run PAPER `143` e SHADOW `144` sono partite dopo il fix di contract rolling. PAPER `143` ha
  comprato solo `SYRUPUSDC` da `ROLLING_PAPER`, ha chiuso `EXIT_ML_ADVICE_NO_MFE_DECAY`, contract diagnostics
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

- Le posizioni PAPER `99`-`105` avevano `policy_json.ml_advice_no_mfe_timeout_seconds = NULL`.
- Dopo il primo fix, DocBrown pubblicava la key nei `ruleJson` di mining/promotion, ma non nel path effettivo
  `LiveMlAdviceScoringService.liveAdvice(...)` usato dal ciclo management.
- Le RUN `107`/`108` e `109`/`110` del 2026-06-27 restano contaminate per no-MFE: la generation live non conteneva
  `ml_advice_no_mfe_timeout_seconds`; SHADOW `110` e' anche lifecycle-contaminata per stop/abandon.
- I reject `PAPER_BUDGET_OR_EXCHANGE_RULES_REJECTED` osservati non erano exchange filter: con budget PAPER 100,
  `min_trade_quote=25` e fee, dopo 3 posizioni restavano circa 24.925 quote, sotto il minimo per un quarto BUY.
- WATCH non e' bocciata: nelle run pulite successive ha aperto, confermato BUY e alimentato SELL no-MFE correttamente.

Diagnosi del Consiglio:

- Saggio ascoltatore: fermare ogni inferenza di performance sulle run contaminate e ripartire da un contratto pulito.
- Scienziato severo: nessun fallback su timeout no-MFE; se il campo manca, la guardia deve restare fail-closed.
- Mediano pragmatico: correggere anche il live-score producer e aggiungere un guardrail ACDC di entry/readiness per non
  avviare PAPER con advice prive del campo.
- Decisione unica: no-MFE runtime usa solo il timeout pubblicato dall'advice live (`live_no_mfe_timeout_seconds`,
  mappato in ACDC nel campo canonico `ml_advice_no_mfe_timeout_seconds` per le guardie). ACDC deve considerare non
  contract-active e respingere PAPER ENTRY se il campo manca.

Implementato e verificato:

- `hft-common`: centralizzata la chiave `ml_advice_no_mfe_timeout_seconds`; rimosse le costanti di fallback ratio/min-hold.
- `hft-common`: aggiunte le chiavi condivise `history_*`, `live_*`, `entry_*`, `exit_*` per separare contratto storico
  ML, contratto live-score, fotografia BUY e fotografia SELL.
- `docbrown`: `SignaturePaperAdvicePromotionService` e `ReversalMlRuleMiningService` pubblicano
  `ml_advice_no_mfe_timeout_seconds` nell'advice usando `entryValiditySeconds` candidate-specific.
- `docbrown`: source/validity source literal dei due `ruleJson` toccati sono stati portati in `OperationalString`.
- `docbrown`: `LiveMlAdviceScoringService.liveAdvice(...)` non modifica piu' semanticamente i campi ML originari
  dell'advice. Produce un blocco `history_*` copiato dal contratto storico e un blocco `live_*` speculare/valorizzato
  dal live-score. Il timeout no-MFE runtime ora e' `live_no_mfe_timeout_seconds`; per regole storiche viene derivato
  una tantum da `entry_validity_seconds` nel producer live, non in ACDC.
- `acdc`: `OutcomeQualityModelService` copia `history_*` e `live_*`, usa `live_*` per valorizzare i campi canonici
  `ml_advice_*` consumati dalle guardie runtime, e scrive `entry_*` al momento della decisione BUY.
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
  `ml_advice_no_mfe_timeout_seconds`; nessun fallback da durata, ratio, metadata DB o config runtime.

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
- MySQL operativo: guardia `exit_ml_advice_no_mfe_decay` ha `min_threshold=0`, `max_threshold=0`, metadata senza ratio/min-hold.
- MySQL operativo: generation `live-1782565383` e `live-1782565442` hanno 5/5 advice con
  `ml_advice_no_mfe_timeout_seconds`.
- RUN pulita `111`/`112`, group `ab98-20260627T130406Z`: PAPER `111` ha 3 posizioni chiuse con
  `EXIT_ML_ADVICE_NO_MFE_DECAY`; SHADOW `112` ha `ALGOUSDC` chiusa take-profit e `HBARUSDC` chiusa no-MFE.
- RUN successiva `113`/`114`, group `ab98-20260627T130751Z`: PAPER `113` ha 2 posizioni chiuse no-MFE e 1 dynamic
  trailing; SHADOW `114` e' stata stoppata/abbandonata dopo `AUTO_AB_STOP`, quindi non e' evidenza Forward A/B pulita.
- Nuovo ciclo management del 2026-06-27:
  - `REFRESH_DIAGNOSTICS` iniziale: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `paperRunning=false`,
    `openPositions=0`, nessuna advice attiva.
  - `AUTO_AB_START` da FE `/management` ha generato advice fresche contract-active con
    `ml_advice_no_mfe_timeout_seconds`.
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
  - Tutte le posizioni PAPER/SHADOW `115`-`122` hanno `ml_advice_no_mfe_timeout_seconds` valorizzato nel `policy_json`
    (`120` o `175`) e `ml_advice_pre_buy_watch_timeout_seconds=20`; PAPER `115`, `117`, `119`, `121` hanno generation
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
  `PAPER_ELIGIBLE` attive, `ML_RULES_MISSING` e `ML_PROMOTED_RULES_MISSING` non bloccano piu' `ML_READY`; restano
  warning diagnostici. Le guardie su advice attive, advice paper-eligible, contratto attivo e posizioni aperte restano
  bloccanti.
- Deploy runtime: container `acdc-vpn` rebuildato e riavviato, startup prod OK su MySQL 8.0.
- Verifica: `acdc mvn -q test` OK e `./mvnw -q -DskipTests package` OK.
- RUN runtime dopo fix:
  - `1`/`2`, group `ab98-20260627T203315Z`: PAPER B partita da generation
    `management-rolling-20260627T203248Z`; WATCH ha aperto `JTOUSDC`, sell
    `EXIT_ML_ADVICE_NO_MFE_DECAY`, net `-0.1047430562718`; una WATCH PAPER e una WATCH SHADOW sono scadute senza BUY.
  - `3`/`4`, group `ab98-20260627T203619Z`: PAPER B partita da generation
    `management-rolling-20260627T203556Z`; nessuna WATCH PAPER ha confermato il BUY, PAPER chiusa con 0 trade; SHADOW
    baseline completata con PnL runtime negativo.
- Stato finale post stop automazione: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `mlReady=false`,
  `paperRunning=false`, `openPositions=0`, nessuna advice attiva. Automazione fermata con `AUTO_AB_STOP`.
- Interpretazione: il blocco readiness che impediva lo start PAPER era un mismatch ACDC legacy vs rolling advice-driven ed
  e' corretto. WATCH observer e scadenza contratto sono operativi. Trading non promosso: evidenza PnL ancora negativa o
  assente, quindi serve nuova FORWARD_AB_98 pulita prima di qualsiasi promozione.

## Prossimo TODO

1. Committare e pushare l'allineamento endpoint diagnostici/management ai blocchi `history_*`/`live_*`/`entry_*`/`exit_*`.
2. Non usare `107`/`108`, `109`/`110` o `113`/`114` come evidenza baseline pulita; sono contaminati rispettivamente da
   contratto no-MFE mancante o stop/abandon SHADOW.
3. Usare `111`/`112` e `115`-`122` come evidenza tecnica positiva del contratto WATCH/no-MFE, ma non ancora come
   promozione baseline: serve forensics A/B completa e almeno un ciclo pristine prima di `PASS_BASELINE`.
4. Prossimo piano operativo: generare una nuova `FORWARD_AB_98` da FE `/management` solo dopo nuove advice
   contract-active, lasciando chiudere entrambi i bracci senza stop/abandon SHADOW se si vuole evidenza scientifica.
5. Prossimo piano scientifico: valutare WATCH/no-MFE su run pulita, mantenendo:
   - WATCH pre-BUY runtime;
   - take-profit prioritario;
   - no-MFE decay;
   - niente REAL;
   - PAPER solo `FORWARD_AB_98` e solo se `ML_READY=true`.
