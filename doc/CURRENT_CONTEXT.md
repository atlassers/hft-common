# Current Context

Ultimo aggiornamento: 2026-06-27 15:13 CEST.

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

Obiettivo eseguito: correzione contratto end-to-end SELL no-MFE e validazione operativa WATCH/no-MFE dopo refactor
stringhe/contract.

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
- Decisione unica: no-MFE usa solo `ml_advice_no_mfe_timeout_seconds` pubblicato dall'ML/advice. ACDC deve considerare
  non contract-active e respingere PAPER ENTRY se il campo manca.

Implementato e verificato:

- `hft-common`: centralizzata la chiave `ml_advice_no_mfe_timeout_seconds`; rimosse le costanti di fallback ratio/min-hold.
- `docbrown`: `SignaturePaperAdvicePromotionService` e `ReversalMlRuleMiningService` pubblicano
  `ml_advice_no_mfe_timeout_seconds` nell'advice usando `entryValiditySeconds` candidate-specific.
- `docbrown`: source/validity source literal dei due `ruleJson` toccati sono stati portati in `OperationalString`.
- `docbrown`: `LiveMlAdviceScoringService.liveAdvice(...)` garantisce `ml_advice_no_mfe_timeout_seconds`; per regole
  storiche lo deriva una tantum da `entry_validity_seconds` nel producer live, non in ACDC.
- `acdc`: `MlAdviceFeatures.ADVICE_KEYS` include `ml_advice_no_mfe_timeout_seconds`.
- `acdc`: `OutcomeQualityModelService` copia il timeout no-MFE da advice/rule_json alle feature SELL.
- `acdc`: `MlReadinessDiagnosticsService` conta `paperEligibleContractActiveAdvice` solo se
  `ml_advice_no_mfe_timeout_seconds > 0` e il contratto temporale BUY e' ancora valido.
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
- `docbrown`: container `docbrown` rebuildato/ricreato dopo il fix live-score, startup prod OK su MySQL 8.0.
- `acdc`: `mvn -q test` OK; Flyway test valida/applica 75 migration.
- `acdc`: `./mvnw -q -DskipTests package` OK.
- `acdc`: container `acdc-vpn` rebuildato/ricreato dopo il guardrail no-MFE.
- ACDC log prod: MySQL 8.0, schema operativo da `74` a `75`, startup OK.
- MySQL operativo: `acdc_flyway_schema_history` ultimo `version=75`, `success=1`.
- MySQL operativo: guardia `exit_ml_advice_no_mfe_decay` ha `min_threshold=0`, `max_threshold=0`, metadata senza ratio/min-hold.
- MySQL operativo: generation `live-1782565383` e `live-1782565442` hanno 5/5 advice con
  `ml_advice_no_mfe_timeout_seconds`.
- RUN pulita `111`/`112`, group `ab98-20260627T130406Z`: PAPER `111` ha 3 posizioni chiuse con
  `EXIT_ML_ADVICE_NO_MFE_DECAY`; SHADOW `112` ha `ALGOUSDC` chiusa take-profit e `HBARUSDC` chiusa no-MFE.
- RUN successiva `113`/`114`, group `ab98-20260627T130751Z`: PAPER `113` ha 2 posizioni chiuse no-MFE e 1 dynamic
  trailing; SHADOW `114` e' stata stoppata/abbandonata dopo `AUTO_AB_STOP`, quindi non e' evidenza Forward A/B pulita.
- FE/Kenshiro finale: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `paperRunning=false`, `openPositions=0`,
  `activeAdvice=0`, `paperEligibleActiveAdvice=0`, automazione `STOPPED`.

## Stato Repo

Repo modificati da committare: `hft-common`, `acdc`, `docbrown`.

## Prossimo TODO

1. Committare e pushare i repo modificati con stesso MS successivo.
2. Non usare `107`/`108`, `109`/`110` o `113`/`114` come evidenza baseline pulita; sono contaminati rispettivamente da
   contratto no-MFE mancante o stop/abandon SHADOW.
3. Usare `111`/`112` come evidenza tecnica positiva del contratto WATCH/no-MFE, ma non ancora come promozione baseline:
   serve forensics A/B completa prima di `PASS_BASELINE`.
4. Prossimo piano operativo: generare una nuova `FORWARD_AB_98` da FE `/management` solo dopo nuove advice
   contract-active, lasciando chiudere entrambi i bracci senza stop/abandon SHADOW se si vuole evidenza scientifica.
5. Prossimo piano scientifico: valutare WATCH/no-MFE su run pulita, mantenendo:
   - WATCH pre-BUY runtime;
   - take-profit prioritario;
   - no-MFE decay;
   - niente REAL;
   - PAPER solo `FORWARD_AB_98` e solo se `ML_READY=true`.
