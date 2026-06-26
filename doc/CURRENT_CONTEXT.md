# Current Context

Ultimo aggiornamento: 2026-06-26 15:47 CEST.

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

Obiettivo in corso: rendere configurabile la severita' dei filtri di selezione ML e provare tre profili operativi
`0%`, `50%`, `100%`, poi ricerca binaria.

Implementato e verificato prima dell'interruzione:

- `hft-common`: aggiunte costanti `selectionStrictnessPercent`, `selectionFilterProfile`,
  `SELECTION_FILTERS_MIN`, `SELECTION_FILTERS_50`, `SELECTION_FILTERS_100` e action id
  `APPLY_SELECTION_FILTERS_MIN/50/100`.
- `docbrown`: rolling validation accetta `selectionStrictnessPercent`; default `100` preserva il comportamento storico.
  La severita' scala solo i filtri di selezione ML, non WATCH, BUY o SELL runtime.
- `kenshiro`: action management per applicare i tre profili e inoltro della severita' nei payload rolling validation.
- `hft-fe`: contract TypeScript aggiornati per le nuove action/costanti.
- `acdc`: fix runtime SHADOW per evitare query Influx dentro transazioni e ridurre il drain SHADOW in buy-stop ai soli
  simboli con posizione aperta.

Verifiche completate:

- Documentazione REM/HFT centralizzata sotto `/home/mbc/Documenti/ws/java/hft/hft-common/doc`.
- Skill `acdc-rem` aggiornata ai nuovi path e alla gerarchia `Recovery Plan -> Current Context -> Handoff`.
- `hft-common`: `mvn install -DskipTests` OK.
- `docbrown`: test OK.
- `kenshiro`: string scanner OK; fix successiva su query freshness ancora da ricompilare/deployare.
- `hft-fe`: `npm run check` e `npm run build` OK.
- `acdc`: string scanner OK, compile/package OK; test completo non concluso entro timeout per startup Quarkus
  Testcontainers/H2, quindi non conteggiato come validazione operativa.
- `acdc-vpn` redeployato e validato su MySQL/container.

Run effettuate:

- Profilo `SELECTION_FILTERS_MIN` (`selectionStrictnessPercent=0`) ha avviato Forward A/B 98.
- PAPER B execution `89`: `STOPPED`, 0 posizioni.
- SHADOW A execution `90`: dopo fix ACDC ha drenato 3 posizioni ed e' `COMPLETED`, PnL netto `-0.0336245338892`.
- Profilo `SELECTION_FILTERS_50` applicato; rolling validation batch `management-rolling-20260626T132119Z` ha prodotto
  `INCONCLUSIVE`, nessun simbolo selezionato, nessuna PAPER avviata.

Problema rilevato:

- Kenshiro `/management/state` puo' diventare lento su query freshness batch perche' alcune query su
  `acdc_rem_observation_candidate` filtrano solo `batch_id` invece di usare anche `profile_id`, nonostante l'indice
  disponibile sia `(profile_id, batch_id, ...)`.
- Patch locale gia' applicata in `kenshiro/ManagementService.java` per filtrare `signalFreshness` e
  `persistPromotionPreflight` anche per profilo. La patch e' da ricompilare, testare, deployare e verificare.

## Stato Repo

Worktree attesi sporchi per lavoro in corso:

- `hft-common`: contract selection + nuova documentazione centralizzata.
- `docbrown`: selection strictness.
- `kenshiro`: selection profile + fix query freshness.
- `hft-fe`: contract selection.
- `acdc`: fix transazione Influx/SHADOW drain.

Non committare finche' la documentazione non e' spostata e la fix Kenshiro non e' validata o dichiarata separatamente.

## Prossimo TODO

1. Ricompilare/testare/deployare Kenshiro con la fix freshness.
2. Riprendere dal FE `/management`:
   - se runtime pulito, applicare `SELECTION_FILTERS_100` e avviare `AUTO_FORWARD_AB_CYCLE_START`;
   - poi valutare ricerca binaria dalle evidenze raccolte.
