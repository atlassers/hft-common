# Current Context

Ultimo aggiornamento: 2026-06-26 21:20 CEST.

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

Obiettivo in corso: verificare selezione ML con severita' configurabile, arrivare a una PAPER `FORWARD_AB_98` pulita e
capire il comportamento della WATCH/runtime prima di nuovo tuning.

Implementato e verificato:

- `hft-common`: aggiunte costanti `selectionStrictnessPercent`, `selectionFilterProfile`,
  `SELECTION_FILTERS_MIN`, `SELECTION_FILTERS_50`, `SELECTION_FILTERS_100` e action id
  `APPLY_SELECTION_FILTERS_MIN/50/100`.
- `docbrown`: rolling validation accetta `selectionStrictnessPercent`; default `100` preserva il comportamento storico.
  La severita' scala solo i filtri di selezione ML, non WATCH, BUY o SELL runtime.
- `kenshiro`: action management per applicare i tre profili, inoltro della severita' nei payload rolling validation e
  fix query freshness/promotion preflight su `profile_id + batch_id`.
- `hft-fe`: contract TypeScript aggiornati per le nuove action/costanti.
- `acdc`: fix runtime SHADOW per evitare query Influx dentro transazioni e ridurre il drain SHADOW in buy-stop ai soli
  simboli con posizione aperta.

Verifiche completate:

- Documentazione REM/HFT centralizzata sotto `/home/mbc/Documenti/ws/java/hft/hft-common/doc`.
- Skill `acdc-rem` aggiornata ai nuovi path e alla gerarchia `Recovery Plan -> Current Context -> Handoff`.
- `hft-common`: `mvn install -DskipTests` OK.
- `docbrown`: test OK.
- `kenshiro`: string scanner OK; package OK; redeploy Docker OK; `/management/state` tornato veloce dopo fix freshness.
- `hft-fe`: `npm run check` e `npm run build` OK.
- `acdc`: string scanner OK, compile/package OK; test completo non concluso entro timeout per startup Quarkus
  Testcontainers/H2, quindi non conteggiato come validazione operativa.
- `acdc-vpn` e `kenshiro-local` redeployati e validati su MySQL/container.

Run effettuate:

- Profilo `SELECTION_FILTERS_MIN` (`selectionStrictnessPercent=0`) ha avviato Forward A/B 98.
- PAPER B execution `89`: `STOPPED`, 0 posizioni.
- SHADOW A execution `90`: dopo fix ACDC ha drenato 3 posizioni ed e' `COMPLETED`, PnL netto `-0.0336245338892`.
- Profilo `SELECTION_FILTERS_50` applicato; rolling validation batch `management-rolling-20260626T132119Z` ha prodotto
  `INCONCLUSIVE`, nessun simbolo selezionato, nessuna PAPER avviata.
- Profilo `SELECTION_FILTERS_100` applicato dopo redeploy Kenshiro; batch `management-rolling-20260626T183915Z` terminato
  `NO_PROMOTABLE_CANDIDATE`, nessuna PAPER.
- Profilo `SELECTION_FILTERS_MIN` riapplicato:
  - batch `management-rolling-20260626T185119Z`: selezionato `XPLUSDC`, ma terminato fail-closed
    `LIFECYCLE_CAPTURE_REQUIRES_SHADOW_PREFLIGHT`; non e' stato bypassato perche' il piano strategico non autorizza
    PAPER da lifecycle-capture.
  - batch successivo ha avviato Forward A/B 98 group `ab98-20260626T190239Z`.
  - PAPER B execution `91`: `STOPPED`, 0 posizioni, PnL `0`.
  - SHADOW A execution `92`: `COMPLETED`, 4 posizioni, PnL netto `-0.1236832028357`.
    Trade SHADOW: `KMNOUSDC +0.0355308219028`, `REUSDC +0.0684080952972`, `HEIUSDC -0.267173912616`,
    `LDOUSDC +0.0395517925803`.

Stato live dopo monitor:

- `globalStatus=BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`.
- `mlReady=false`.
- `paperRunning=false`.
- `openPositions=0`.
- Automazione fermata via FE proxy `AUTO_AB_STOP` dopo completamento drain per evitare cicli non supervisionati:
  `automationEnabled=false`, `automationStatus=STOPPED`, `automationLastStopReason=USER_STOP`.
- Config persistita: `rem.ml.management.selection.strictness_percent=0`.

Problemi rilevati:

- La card/step FE/Kenshiro continua a esporre `selectionStrictnessPercent=100` in alcuni dati di step anche quando la
  config MySQL effettiva e' `0`. La RUN ha usato la config minima, ma il dato UI e' fuorviante e va corretto.
- PAPER B `91` ha ricevuto advice fresche ma non ha aperto posizioni; SHADOW A `92` ha aperto e perso nel complesso.
  Serve diagnosticare la divergenza B-vs-A: freshness contract, Pre-BUY Watch, live revalidation e differenza fra
  baseline A e current pipeline B.
- Il ramo `LIFECYCLE_CAPTURE_REQUIRES_SHADOW_PREFLIGHT` e' ancora attivo come fail-closed strategico. Non va confuso con
  il vecchio `REVERSAL_WATCH_SHADOW_PREFLIGHT` rimosso/ritirato: non autorizza PAPER e non va bypassato senza modifica
  esplicita del piano strategico.

## Stato Repo

Repo attesi puliti salvo questo aggiornamento di `CURRENT_CONTEXT.md`.

## Prossimo TODO

1. Diagnosticare perche' PAPER B execution `91` non ha aperto posizioni mentre SHADOW A execution `92` ha aperto 4 trade.
2. Correggere l'esposizione UI/Kenshiro della severita' selection quando la config effettiva e' diversa dal dato stale.
3. Prima di ulteriori tuning o ricerca binaria, produrre report su:
   - advice generation `live-1782500554`;
   - motivi di rifiuto PAPER B / WATCH;
   - trade SHADOW A e cause della loss `HEIUSDC`;
   - se il profilo minimo e' utile solo come esplorazione o se deve restare bloccato finche' WATCH non dimostra di
     filtrare le loss.
