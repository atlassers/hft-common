# Current Context

Ultimo aggiornamento: 2026-06-27 10:57 CEST.

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

Obiettivo eseguito: correzione contratto end-to-end SELL no-MFE dopo diagnosi di fallback non autorizzato.

Diagnosi corretta:

- Le posizioni PAPER `99`-`105` avevano `policy_json.ml_advice_no_mfe_timeout_seconds = NULL`.
- ACDC usava un fallback non autorizzato `ml_advice_duration_seconds * 0.25` per il timeout no-MFE.
- DocBrown non pubblicava ancora `ml_advice_no_mfe_timeout_seconds` nell'advice.
- ACDC non propagava il campo nella whitelist `MlAdviceFeatures.ADVICE_KEYS`.
- Le RUN `99`-`105` restano storiche ma sono contaminate per valutare no-MFE/WATCH: non misurano un timeout deciso
  dall'ML/advice.
- Il concetto WATCH non e' bocciato da queste run; il contratto pipeline era incompleto.

Diagnosi del Consiglio:

- Saggio ascoltatore: fermare ogni inferenza di performance sulle run contaminate e ripartire da un contratto pulito.
- Scienziato severo: nessun fallback su timeout no-MFE; se il campo manca, la guardia deve restare fail-closed.
- Mediano pragmatico: correggere DocBrown, propagazione ACDC, DB operativo e advice residue prima di riprendere il ciclo
  da FE `/management`.
- Decisione unica: no-MFE usa solo `ml_advice_no_mfe_timeout_seconds` pubblicato dall'ML/advice. Nessuna PAPER valida
  puo' partire con advice vecchie o senza quel campo.

Implementato e verificato:

- `hft-common`: centralizzata la chiave `ml_advice_no_mfe_timeout_seconds`; rimosse le costanti di fallback ratio/min-hold.
- `docbrown`: `SignaturePaperAdvicePromotionService` e `ReversalMlRuleMiningService` pubblicano
  `ml_advice_no_mfe_timeout_seconds` nell'advice usando `entryValiditySeconds` candidate-specific.
- `docbrown`: source/validity source literal dei due `ruleJson` toccati sono stati portati in `OperationalString`.
- `acdc`: `MlAdviceFeatures.ADVICE_KEYS` include `ml_advice_no_mfe_timeout_seconds`.
- `acdc`: `OutcomeQualityModelService` copia il timeout no-MFE da advice/rule_json alle feature SELL.
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
- `docbrown`: container `docbrown` rebuildato/ricreato, startup prod OK su MySQL 8.0.
- `acdc`: `mvn -q test` OK; Flyway test valida/applica 75 migration.
- `acdc`: `./mvnw -q -DskipTests package` OK.
- `acdc`: container `acdc-vpn` rebuildato/ricreato.
- ACDC log prod: MySQL 8.0, schema operativo da `74` a `75`, startup OK.
- MySQL operativo: `acdc_flyway_schema_history` ultimo `version=75`, `success=1`.
- MySQL operativo: guardia `exit_ml_advice_no_mfe_decay` ha `min_threshold=0`, `max_threshold=0`, metadata senza ratio/min-hold.
- MySQL operativo: `acdc_live_ml_advice` ha solo righe `EXPIRED`; `ACTIVE=0`.
- FE/Kenshiro finale: `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`, `paperRunning=false`, `openPositions=0`,
  `activeAdvice=0`, `paperEligibleActiveAdvice=0`.

## Stato Repo

Repo modificati da committare: `hft-common`, `acdc`, `docbrown`.

## Prossimo TODO

1. Committare e pushare `hft-common`, `acdc`, `docbrown` con stesso MS826.
2. Non avviare PAPER su advice vecchie: serve live score/promotion nuova da FE `/management`, con advice contenenti
   `ml_advice_no_mfe_timeout_seconds`.
3. Prossimo piano operativo: riprendere da FE `/management`, generare advice fresche e solo se `ML_READY=true` portare
   a `PAPER_FORWARD_AB_98`.
4. Prossimo piano scientifico: valutare WATCH/no-MFE su run pulita, mantenendo:
   - WATCH pre-BUY runtime;
   - take-profit prioritario;
   - no-MFE decay;
   - niente REAL;
   - PAPER solo `FORWARD_AB_98` e solo se `ML_READY=true`.
