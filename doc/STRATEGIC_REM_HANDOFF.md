# REM Diagnostic Endpoints Handoff

Data: 2026-06-21.

## Scopo

Documento compatto per la prossima chat: dove leggere lo stato operativo, quali endpoint/proxy/script usare, quali
payload sono approvati e in che ordine eseguire le diagnostiche.

Questo documento e' il manuale operativo.

Deve contenere:

- ordine diagnostico standard;
- endpoint ACDC e DocBrown;
- payload operativi approvati;
- script utilizzabili;
- query diagnostiche approvate;
- procedure di build/deploy/monitoraggio/raccolta forensics.

Non deve sostituire:

- `STRATEGIC_REM_RECOVERY_PLAN.md`, che resta il vincolo strategico;
- `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md`, che resta lo snapshot cross-modulo con stato corrente e prossimo TODO.

Non deve contenere:

- snapshot live di singole sessioni;
- cronologia degli avanzamenti dell'ultimo piano;
- conclusioni operative temporanee che appartengono al current context.

Gerarchia:

1. Piano strategico: `hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`.
2. Snapshot workspace: `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md`.
3. Handoff operativo: questo file.

Se una procedura qui descritta confligge con il piano strategico, va fermata e corretta.

## Fonti E Vincoli Operativi

- Charter: `hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`.
- Checklist operativa autonoma: `hft-common/doc/acdc/session/2026-06-21/session-148-autonomous-to-paper-checklist.md`.
- Checklist Telegram SELL chart: `hft-common/doc/acdc/session/2026-06-21/session-145-telegram-sell-chart-link-checklist.md`.
- Checklist Forward A/B readiness: `hft-common/doc/acdc/session/2026-06-21/session-146-forward-ab-run-readiness-checklist.md`.
- Stato strategico: `BASELINE_98_CANDIDATE_REQUIRES_FORWARD_AB`.
- Stato operativo live, avanzamenti dell'ultimo piano e prossimo TODO stanno in
  `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md`.
- Dal 2026-06-21 ogni nuova SHADOW/PAPER usata come evidenza baseline deve essere `FORWARD_AB_98`; checklist: `hft-common/doc/acdc/session/2026-06-21/session-144-forward-ab-98-checklist.md`.
- Diary e checklist storici sono centralizzati in `hft-common/doc/<module>/<YYYY-MM-DD>/...`; per ACDC/REM usare
  `hft-common/doc/acdc/session/<YYYY-MM-DD>/...`. I progetti applicativi non devono mantenere copie operative dei diary.
- Nessuna REAL.
- Nessun H2 per test operativi.
- MySQL/container obbligatori.
- Nessuna PAPER validativa se `ML_READY=false`.
- DocBrown espone sotto root path `/docbrown`.
- Commit/push: usare naming convention workspace `MS<n>: <message>`, con `n = max(MS nei log workspace coinvolti) + 1`; per interventi multi-modulo logicamente unici usare lo stesso `MS<n>` in tutti i repo.
- Contratti Java condivisi: ACDC, DocBrown e Kenshiro devono importare payload/enum/costanti/model/entity cross-modulo da
  `it.mbc.hft:hft-common:1.0.0-SNAPSHOT`, sorgente remoto
  `https://github.com/atlassers/hft-common.git`. Prima di buildare i consumer da una workspace pulita eseguire:
  `git clone https://github.com/atlassers/hft-common.git /home/mbc/Documenti/ws/java/hft/hft-common` se assente, poi
  `cd /home/mbc/Documenti/ws/java/hft/hft-common && mvn install`. La libreria contiene anche entity JPA/Panache REM
  identiche fra ACDC e DocBrown; non deve contenere scheduler, risorse REST, migration, logica trading o tuning.
  ACDC e DocBrown devono indicizzarla con:
  `quarkus.index-dependency.hft-common.group-id=it.mbc.hft` e
  `quarkus.index-dependency.hft-common.artifact-id=hft-common`, altrimenti Hibernate non include le entity condivise
  nella persistence unit.
- Stringhe operative REM: action id, step id, runtime status, promotion class, advice source, candidate status, watch
  status, reason/status persistiti, config key operative e payload/json key operative devono usare enum/registry
  tipizzati o costanti centralizzate nello stesso contesto. Label UI, messaggi descrittivi, SQL, nomi colonne DB e path
  endpoint possono restare literal quando non sono identificatori di protocollo.
- Contratti FE/script verso backend: ogni payload/request/response usato per chiamare ACDC, DocBrown o Kenshiro deve
  avere nome e campi 1:1 con il record/classe equivalente in `hft-common` quando esiste. Sono vietati alias locali per
  campi operativi (`profileKey`, `payload`, `executionMode`, ecc.) e per action/status/protocol id. FE TypeScript e
  script Python/shell devono importare o generare i propri contract da una sede centralizzata, mai riscrivere literal
  protocollo nel punto d'uso.
- Guardrail implementati: `hft-fe` mantiene i contract in `src/lib/contracts/*` e `npm run check` include
  `scripts/check-operational-contracts.mjs`; ACDC mantiene i contract shell in `scripts/lib/rem_contracts.sh` e verifica
  i wrapper con `python3 scripts/check-script-contracts.py`. Lo scanner comune `check_operational_strings.py` riconosce
  i file `*contract*`, `*contracts*`, `*constants*` anche in `.sh` e rileva anche key operative in `Map.of`/`Map.entry`
  quando rappresentano identificatori di protocollo.
- Contratto economico advice/WATCH: DocBrown deve scrivere nell'advice live i campi
  `min_economic_safe_net_return`, `safe_net_return`, `max_net_return`, `pre_buy_watch_required` e
  `pre_buy_watch_timeout_seconds`. ACDC deve consumare questi campi e non ricostruire soglie economiche fisse da
  config runtime globale. Se `min_economic_safe_net_return` manca, ACDC non inventa un floor; il gate economico minimo
  resta `safe_net_return > 0` piu' eventuale min advice-specific rispettato. La WATCH operativa si attiva tramite
  `pre_buy_watch_required=true` o timeout positivo nel payload advice.
- Contratto temporale advice/WATCH: `max_buy_age_seconds` misura solo la freschezza del live-score/advice. Non deve
  essere usato come durata della WATCH. La finestra `pre_buy_watch_timeout_seconds` deve derivare dalla durata storica
  della firma (`entry_validity_seconds`, `duration_seconds` o timeout no-MFE esplicito). La live revalidation puo'
  essere falsa al momento della promozione: in quel caso DocBrown deve comunque pubblicare l'advice WATCH-eligible,
  ACDC apre la WATCH e compra solo se il contratto BUY diventa vero prima della scadenza.
- Le nuove key JSON/protocollo Java condivise devono essere aggiunte in
  `it.mbc.hft.common.rem.constants.RemConstants`; eventuali registry locali (`ManagementString`, `OperationalString`)
  sono ammessi solo come shim verso `hft-common`.
- Contratto advice separato: DocBrown live-score non deve modificare semanticamente i campi ML storici dell'advice.
  Deve produrre un blocco `history_*` copiato dal contratto ML/promozione storico e un blocco `live_*` speculare,
  valorizzato con il live-score corrente. ACDC consuma `live_*` per valorizzare i campi canonici `ml_advice_*` usati
  dalle guardie runtime, ma conserva `history_*` per audit. Al BUY ACDC scrive `entry_*` nel `policy_json`; alla SELL
  PAPER/SHADOW aggiunge `exit_*` nel feature snapshot di uscita. La SELL deve propagare `history_*`, `live_*` ed
  `entry_*` dal `policy_json`, non ricalcolarli dai live feature di uscita.
- Diagnostica contract block: gli endpoint ACDC `/diagnostics/acdc/paper/sell-capture` e
  `/diagnostics/acdc/paper/post-sell-forensics` devono esporre per ogni riga un oggetto `contract` con mappe numeriche
  `history`, `live`, `entry`, `exit` e flag `complete`. Le run antecedenti alla separazione possono avere
  `contract.complete=false` e non vanno usate per validare completezza del nuovo contratto.
- Autonomia operativa: eseguire la checklist `hft-common/doc/acdc/session/2026-06-21/session-148-autonomous-to-paper-checklist.md` senza chiedere go per ogni microtest, fermandosi solo sulle stop condition dichiarate.
- Budget/cadenza orchestratore: non sostituisce il lifecycle SHADOW/PAPER ACDC. Ogni ciclo autonomo esterno fino alla PAPER ha budget massimo `25m`; dopo `FAIL_SELECTION_BIAS` attendere `10m` prima del ciclo successivo; dopo `6` cicli senza candidate promuovibili classificare `NO_PROMOTABLE_SIGNAL_WINDOW` e chiedere rivalutazione del Consiglio.
- Nuovo cockpit operativo: il prossimo ciclo REM deve partire da FE `/management`, che chiama Kenshiro
  `/backoffice/management/*`. Gli endpoint ACDC/DocBrown sotto restano approvati come runtime/diagnostica, ma non sono
  l'interfaccia primaria per avviare il ciclo utente.
- Kenshiro e' l'orchestratore di management: legge MySQL, calcola `ML_READY`, espone lifecycle/checklist/action e chiama
  endpoint ACDC/DocBrown esistenti solo quando una action FE lo richiede. Non aggiungere nuovi endpoint o carichi ad
  ACDC/DocBrown per la pagina management.

## Ordine Diagnostico Standard

### 0. Management Cockpit Kenshiro/FE

Per il prossimo ciclo modello/run, usare questa catena:

```text
FE /management -> Kenshiro /kenshiro/backoffice/management/* -> endpoint runtime ACDC/DocBrown esistenti
```

Standard FE vincolante:

- le proposte grafiche e i nuovi componenti FE per `/management` devono usare come riferimento primario la
  documentazione Flowbite Svelte Components: `https://flowbite-svelte.com/docs/components/*`;
- se Flowbite Svelte offre un componente applicabile, usarlo prima di introdurre widget custom;
- componenti gia' approvati per il cockpit: `Stepper`, `Progressbar`, `SpeedDial`, `Modal/Dialog` equivalente,
  `Tooltip`, badge/status, button e layout utility coerenti con Flowbite Svelte;
- per workflow graph espliciti della pagina `/management`, Svelte Flow (`@xyflow/svelte`) e' approvato come componente
  specialistico di visualizzazione: nodi = step/aree, edge = transizioni/condizioni, custom node per stato/tipo step,
  canvas read-only con pan/zoom; Flowbite resta la shell per tab, badge, layout e controlli;
- Svelte Flow non deve diventare workflow engine: la logica di esecuzione, gating, scheduling e avanzamento resta in
  Kenshiro/ACDC, mentre il FE disegna lo stato e le relazioni;
- eventuali componenti custom restano ammessi solo come glue/layout sottile quando Flowbite non copre il caso o quando
  serve mantenere vincoli REM gia' approvati.

Endpoint FE proxy approvati come interfaccia operativa primaria:

```bash
curl -sS 'http://localhost:5173/backoffice/management/state?profileKey=REM_CURRENT' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs?limit=20' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs/{executionId}' | jq '.'
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:5173/backoffice/management/actions/REFRESH_DIAGNOSTICS' \
  -d '{"profileKey":"REM_CURRENT","payload":{}}' | jq '.'
```

Endpoint Kenshiro diretti approvati solo per diagnostica tecnica/fallback, non come interfaccia operativa primaria:

```bash
curl -sS 'http://localhost:8085/kenshiro/backoffice/management/state?profileKey=REM_CURRENT' | jq '.'
curl -sS 'http://localhost:8085/kenshiro/backoffice/management/runs?limit=20' | jq '.'
curl -sS 'http://localhost:8085/kenshiro/backoffice/management/runs/{executionId}' | jq '.'
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8085/kenshiro/backoffice/management/actions/REFRESH_DIAGNOSTICS' \
  -d '{"profileKey":"REM_CURRENT","payload":{}}' | jq '.'
```

Action Kenshiro approvate:

```text
AUTO_AB_START
AUTO_AB_STOP
UNIVERSE_PREFILTER
RUN_RESEARCH
RESEARCH_STATUS
RUN_FULL_ML_AUDIT
RUN_ROUND_ROBIN_DEEP_30
APPLY_MICRO_ML_REFRESH
APPLY_ROUND_ROBIN_DEEP_30
APPLY_FAST_ML_REFRESH
APPLY_FULL_ML_AUDIT
APPLY_SELECTION_FILTERS_MIN
APPLY_SELECTION_FILTERS_50
APPLY_SELECTION_FILTERS_100
ENABLE_SIGNATURE_ADVICE
LIVE_SCORE
RESTORE_BASELINE_98_PROFILE
ENABLE_PAPER_CANDIDATES
ROLLING_VALIDATION
BASELINE_98_DIAGNOSTICS
ROLLING_NEAR_MISS_AUDIT
ROLLING_EV_AUDIT
ROLLING_SELECTION_ATTRIBUTION_AUDIT
ROLLING_LIFECYCLE_CAPTURE_AUDIT
LIFECYCLE_CAPTURE_MISMATCH_AUDIT
ROLLING_LATENCY_BOTTLENECK_AUDIT
INVERSE_COMPLEMENT_WALK_FORWARD_AUDIT
SHADOW_OPPORTUNITY_GUARD_TRACE
SHADOW_OPPORTUNITY_RULE_MISSING_BYPASS
LIVE_REVALIDATION_DRIFT_AUDIT
ENTRY_WINDOW_DECAY_AUDIT
LIVE_FALSE_CONTINUATION_AUDIT
ROLLING_PROMOTION
SHADOW_RUN
SHADOW_STOP_BUY
SHADOW_STOP
PAPER_FORWARD_AB_START
PAPER_STOP_BUY
PAPER_STOP
SAVE_MANAGEMENT_CONFIG
REFRESH_DIAGNOSTICS
REAL_RUN
```

Regole:

- `AUTO_AB_START` e' la action primaria del ciclo normale da FE `/management`: abilita in Kenshiro l'automazione
  persistente `FORWARD_AB_98`, che esegue prefiltro -> live score -> rolling validation -> live score immediato
  pre-promotion -> rolling promotion -> PAPER Forward A/B -> monitor, senza richiedere micro-click FE tra uno step e il
  successivo.
- Dal fix MS748 `AUTO_AB_START` e' anche action di bootstrap: se automazione e' spenta e il runtime e' pulito puo'
  partire da FE anche quando il current step consultivo e' su un passo manuale, per evitare mismatch FE abilitato /
  backend `required step auto-prefilter`. Le altre action primarie manuali restano gated sul current step.
- L'automazione persistente deve avere un tick interno Kenshiro, non dipendere dal refresh FE. Kenshiro usa scheduler
  interno ogni 30 secondi per valutare `rem.ml.management.automation.next_run_after` e riavviare il worker quando il
  cooldown e' scaduto, se non ci sono PAPER o posizioni aperte.
- Se un recreate/restart del container lascia in MySQL `rem.ml.management.automation.status=RUNNING_CYCLE` senza worker
  in memoria, Kenshiro deve trattarlo come ciclo stale dopo il timeout conservativo e riconciliare fail-closed in
  cooldown usando il terminale persistito del round-robin, quando disponibile (`PROMOTION_NO_ADVICE`,
  `NO_PROMOTABLE_CANDIDATE`, `FAIL_SELECTION_BIAS`, `LIFECYCLE_CAPTURE_REQUIRES_SHADOW_PREFLIGHT`).
- Dal fix MS748 le chiamate downstream Kenshiro hanno timeout fail-closed: `ROLLING_VALIDATION` 5 minuti,
  `RUN_RESEARCH` 30 minuti, live score/prefilter/promotion 90 secondi, start/stop runtime 45 secondi. Un timeout produce
  `DOWNSTREAM_TIMEOUT`/504 e l'autociclo deve andare in abort/cooldown, non restare `RUNNING_CYCLE` indefinito.
- I profili `APPLY_SELECTION_FILTERS_MIN`, `APPLY_SELECTION_FILTERS_50` e `APPLY_SELECTION_FILTERS_100` modificano solo
  `rem.ml.management.selection.strictness_percent` e il relativo payload DocBrown `selectionStrictnessPercent`.
  Scalano la severita' dei filtri di selezione ML per esperimenti controllati; non cambiano WATCH, BUY, SELL, runtime
  gate, live revalidation o divieti PAPER/REAL. Il default `100` preserva il comportamento storico.
- Quando l'automazione e' abilitata, le action manuali primarie sono bloccate server-side e visualmente disabilitate dal
  FE. Restano disponibili diagnostiche e STOP.
- Le diagnostiche ammesse durante automazione includono `REFRESH_DIAGNOSTICS`, `BASELINE_98_DIAGNOSTICS`,
  `ROLLING_NEAR_MISS_AUDIT`, `ROLLING_EV_AUDIT`, `LIVE_REVALIDATION_DRIFT_AUDIT` e
  `ENTRY_WINDOW_DECAY_AUDIT`: devono restare consultabili anche se le action manuali primarie sono bloccate.
- Quando `automation.enabled=true` e `automation.status=COOLDOWN`, Kenshiro deve mantenere il current step consultivo sul
  ramo auto (`auto-prefilter`) invece di saltare al ramo manuale `ml-round-robin`; il cooldown e' parte del ciclo
  automatico e deve essere rappresentato come tale.
- Dopo il live score post-validazione dell'autociclo, Kenshiro non deve saltare la rolling promotion: deve promuovere
  il batch rolling validato, scadere le advice PAPER precedenti e avviare `PAPER_FORWARD_AB_START` solo se la promozione
  rolling ha creato advice `PAPER_ELIGIBLE` WATCH-eligible. La BUY resta fail-closed finche' ACDC non vede vero il
  contratto live della stessa advice. Questo evita di consumare advice tecniche `PROMOTED_RULE` non attribuite al batch
  rolling appena validato e impedisce che una live revalidation istantanea negativa cancelli l'osservazione WATCH.
- `ROLLING_PROMOTION` deve usare `expireExisting=true` di default, non deve ricevere una short-list operativa di
  simboli e deve passare a DocBrown il `maxBuyAgeSeconds` runtime corrente. La selection rolling resta diagnostica/ranking:
  DocBrown promuove dal batch validato e scarta solo i simboli non promotable. La live revalidation istantanea viene
  riportata in diagnostica ma non delimita la promozione: e' la WATCH ACDC a rivalutare lo stesso contratto fino a BUY o
  scadenza.
- Un batch rolling con status promotable non deve mantenere il cockpit su `auto-promotion` se
  `latestRollingBatchAgeSeconds` supera `rem.ml.live_advice.max_buy_age_seconds`. In quel caso Kenshiro deve tornare a
  `auto-prefilter`: promuovere un batch stale genererebbe advice gia' fuori finestra e contaminerebbe il ciclo
  Forward A/B.
- `AUTO_AB_STOP`, `PAPER_STOP` e `SHADOW_STOP` disabilitano l'automazione (`rem.ml.management.automation.enabled=false`)
  e richiedono stop runtime ACDC. Lo STOP interrompe quindi ML/auto-cycle e runtime SHADOW/PAPER/RUN.
- Il runtime e' pulito solo se non esistono posizioni `OPEN` ne' in `acdc_paper_position` ne' in
  `acdc_shadow_position`. Kenshiro `/management/state` deve usare questo conteggio aggregato per `summary.openPositions`
  e per bloccare `AUTO_AB_START`/scheduler; una SHADOW Forward A/B aperta vale come blocker anche se non e' PAPER.
- Le query diagnostiche Kenshiro su `acdc_rem_observation_candidate` per batch management devono filtrare anche
  `profile_id` oltre a `batch_id`, per usare l'indice `(profile_id, batch_id, ...)` ed evitare scansioni MySQL che
  bloccano `/management/state` o il ritorno delle action.
- `PAPER_FORWARD_AB_START` e ogni fast-path automatico verso Forward A/B devono essere fail-closed se il runtime non e'
  pulito: PAPER gia' running, qualunque posizione PAPER/SHADOW aperta o qualunque SHADOW running. Questo evita gruppi
  A/B sovrapposti e finestre contaminate.
- In una `FORWARD_AB_98`, lo SHADOW A `A_BASELINE_98_CONTRACT` deve smettere di alimentare BUY quando il PAPER B
  `B_CURRENT_ROLLING_PIPELINE` dello stesso `forwardAbGroupId` ha chiuso il proprio lato BUY: `buy_stopped_at` presente,
  `completed_at` presente oppure status diverso da `RUNNING`. Lo SHADOW puo' drenare SELL/posizioni gia' aperte, ma non
  deve aprire nuovi BUY; eventuali decisioni ENTRY successive devono risultare `SHADOW_BUY_STOPPED`.
- `SHADOW_STOP` e' idempotente e deve riconciliare eventuali posizioni SHADOW simulate rimaste `OPEN` su execution
  terminali del profilo marcandole `ABANDONED`. Queste righe restano tracciate per audit, ma non contano come SELL
  chiuse, PnL, PASS/FAIL baseline o evidenza Forward A/B pulita. Se un gruppo A/B ha richiesto questa riconciliazione,
  classificarlo `INCONCLUSIVE_LIFECYCLE_CONTAMINATED`.
- Se la rolling validation arriva alla promotion ma non crea advice, Kenshiro deve persistere
  `rem.ml.management.round_robin.status=PROMOTION_NO_ADVICE` e i dettagli
  `rem.ml.management.round_robin.promotion_rows` / `promotion_statuses`. Non va mascherato come
  `FAIL_SELECTION_BIAS`, perche' la diagnosi e' diversa.
- DocBrown rolling validation deve usare solo outcome maturi: con `horizonSeconds=900` le finestre di classificazione
  devono escludere il trailing horizon non ancora osservabile e `evaluate()` non deve persistere righe candidate se
  l'ultimo tick futuro disponibile non raggiunge la fine dell'orizzonte, con tolleranza minima da candela 1m. Righe
  parziali con pochi tick non possono contribuire a `zero-MFE`, tail risk, `FAIL_SELECTION_BIAS`, `PASS_CANDIDATE` o
  promotion. Se questo riduce supporto/holdout, il ciclo deve restare fail-closed invece di classificare il futuro
  incompleto come perdita.
- Quando l'autociclo Kenshiro termina fail-closed prima di PAPER, deve eseguire e persistere automaticamente un summary
  diagnostico DB-only bounded in `rem.ml.management.auto.diagnostics.summary`, con reason e timestamp in
  `rem.ml.management.auto.diagnostics.reason` / `updated_at` e copia storica su
  `rem.ml.management.round_robin.audit.{batchId}.auto_diagnostics_summary`. Il pacchetto e' solo diagnostico: non crea
  advice, non promuove, non avvia SHADOW/PAPER/REAL, non cambia gate e non conta come Forward A/B evidence. Mapping
  approvato:
  - `PROMOTION_CREATED_NO_ADVICE` / `PROMOTION_NO_ADVICE`: `LIVE_REVALIDATION_DRIFT_AUDIT`,
    `ROLLING_SELECTION_ATTRIBUTION_AUDIT`, `ROLLING_LIFECYCLE_CAPTURE_AUDIT`,
    `LIFECYCLE_CAPTURE_MISMATCH_AUDIT`;
  - `NO_PROMOTABLE_CANDIDATE`: `BASELINE_98_DIAGNOSTICS`, `ROLLING_NEAR_MISS_AUDIT`, `ROLLING_EV_AUDIT`,
    `ROLLING_SELECTION_ATTRIBUTION_AUDIT`, `ROLLING_LIFECYCLE_CAPTURE_AUDIT`,
    `LIFECYCLE_CAPTURE_MISMATCH_AUDIT`;
  - `LIFECYCLE_CAPTURE_REQUIRES_SHADOW_PREFLIGHT`: `ROLLING_SELECTION_ATTRIBUTION_AUDIT`,
    `ROLLING_LIFECYCLE_CAPTURE_AUDIT`, `LIFECYCLE_CAPTURE_MISMATCH_AUDIT`.
  Lo status lifecycle-capture non va piu' mascherato come generico `NO_PROMOTABLE_CANDIDATE` nell'autociclo: deve
  terminare come `LIFECYCLE_CAPTURE_REQUIRES_SHADOW_PREFLIGHT` con diagnostica automatica, lasciando ogni eventuale
  SHADOW tecnico a una action/procedura esplicita gia' approvata.
- I marker diagnostici salvati in `acdc_shared_runtime_config` devono rispettare la lunghezza dello schema MySQL. Se
  `promotion_rows`, `rejectionReasons` o altri dettagli diagnostici superano il limite persistibile, Kenshiro deve
  salvarne una versione bounded/troncata e mantenere il lifecycle fail-closed leggibile; non deve abortire l'autociclo
  solo per eccesso di lunghezza del marker diagnostico.
- A ogni nuova `ROLLING_VALIDATION`, Kenshiro deve azzerare i marker promotion del ciclo precedente
  (`rem.ml.management.round_robin.promotion_rows` e `promotion_statuses`) prima di eventuale nuova promotion. I dettagli
  promotion devono riferirsi solo al batch corrente; su `FAIL_SELECTION_BIAS` / `NO_PROMOTABLE_CANDIDATE` non devono
  restare righe `PROMOTED` vecchie.
- Dal fix MS747 DocBrown include in ogni row di `rolling-paper-promotion` il campo `rejectionReasons`.
  Per `SKIPPED_LIVE_REVALIDATION_CONTRACT` deve indicare la feature live, il valore osservato e il range violato
  oppure il caso `no live revalidation features checked`. Kenshiro persiste queste righe senza trasformarle, quindi la
  prima diagnostica dopo `PROMOTION_NO_ADVICE` e' leggere `rem.ml.management.round_robin.promotion_rows`.
- Quando `PROMOTION_CREATED_NO_ADVICE` / `PROMOTION_NO_ADVICE` dipende da
  `SKIPPED_LIVE_REVALIDATION_CONTRACT`, Kenshiro puo' usare un cooldown adattivo breve e limitato per revalidare la
  stessa zona live prima che la finestra scappi. Questo e' solo orchestrazione/latenza: non allarga gate, non promuove
  batch falliti, non avvia PAPER se `ML_READY=false` e non sostituisce la Forward A/B 98. Marker DB:
  `rem.ml.management.automation.cooldown_kind`, `live_revalidation_retry_count`,
  `live_revalidation_retry_batch_id`; default runtime: retry breve `90s`, massimo `2` retry per batch, configurabili
  via `rem.ml.management.automation.live_revalidation_retry.seconds` e
  `rem.ml.management.automation.live_revalidation_retry.max` con tetti conservativi.
- Dopo `FAIL_SELECTION_BIAS`, `NO_PROMOTABLE_CANDIDATE`, timeout o errore tecnico non qualificato come live
  revalidation drift, il cooldown resta quello standard di `10m`; non accorciarlo per forzare PAPER.
- Dopo evidenza PAPER execution `36` (`XAIUSDC`) con MFE sopra safe (`maxNetReturn=0.007271`, safe `0.003`) ma SELL
  finale negativa, il guard `exit_ml_advice_take_profit` deve restare `ACTIVE` con priorita' precedente al dynamic
  trailing. Il dynamic trailing resta fallback; il safe target deve essere catturato appena osservato.
- Il dynamic trailing ML advice deve usare lo stesso calcolo in runtime e diagnostics. `trailingArmed` indica arming
  reale della policy, non semplice `maxNetReturn > 0`. Il default operativo e' coerente con V67: se il metadata non
  impone `require_safe_for_trailing=true`, l'arming usa `min_arm_net_return`/`safe_arm_ratio`; inoltre
  `protect_positive_mfe=true` permette di uscire quando un trade ha avuto MFE netto positivo e poi rientra sotto
  `break_even_floor`, senza aspettare il timeout.
- Dopo evidenza PAPER execution `95` con tre BUY confermati da WATCH ma `maxNetReturn=0` fino a timeout, il guard
  `exit_ml_advice_no_mfe_decay` deve restare `ACTIVE` tra dynamic trailing/take-profit e loss-cap/timeout. Operatore:
  `ML_ADVICE_NO_MFE_DECAY_EXIT`; reason: `EXIT_ML_ADVICE_NO_MFE_DECAY`. Il timeout no-MFE deve arrivare
  esclusivamente da `ml_advice_no_mfe_timeout_seconds` pubblicato dall'ML/advice. Se il campo manca, ACDC resta
  fail-closed e non ricostruisce fallback da durata, ratio, metadata DB o config runtime. Non usare questa guardia per
  allargare selection o PAPER.
- Le advice persistite con `status='ACTIVE'` ma `advice_valid_until < CURRENT_TIMESTAMP` sono residui operativi e devono
  essere marcate `EXPIRED` prima di una run pulita. Una PAPER Forward A/B non deve consumare advice vecchie o advice
  prodotte prima del contratto `ml_advice_no_mfe_timeout_seconds`.
- I diagnostics PAPER scoring e Forward A/B devono esporre `noMfeDecayExits`; senza questo campo, run come `103`
  sembrano prive di exit classificata anche se il SELL no-MFE ha funzionato.
- Se l'automazione non e' abilitata, gli step manuali restano eseguibili secondo current-step gating.
- REAL resta vietata dal piano strategico: il cockpit puo' mostrare readiness/eligibility, ma `REAL_RUN` deve restare
  bloccata finche' il vincolo strategico non viene modificato esplicitamente.
- Kenshiro deve esporre un solo step `current=true` per volta, con `order` esplicito.
- Le action primarie di avanzamento/start/stop devono essere abilitate solo sullo step corrente e bloccate server-side
  se chiamate fuori ordine.
- Le action diagnostiche possono restare disponibili anche sugli step non correnti.
- `REAL_RUN` deve restare bloccata.
- `PAPER_FORWARD_AB_START` deve restare fail-closed se `ML_READY=false`.
- Il runtime PAPER ACDC deve usare un hot path di orchestrazione per advice live `PAPER_ELIGIBLE`: quando esistono
  advice attive, il BUY path valuta i simboli advice da Influx prima dell'universo completo e mantiene comunque gli open
  positions per SELL. Scopo: ridurre la granularita' di rivalutazione dentro `max_buy_age_seconds` senza cambiare
  freshness, live revalidation, ranking scientifico o gate.
- Quando una PAPER Forward A/B e' legata a `expectedSourceGenerationId`, ACDC deve restare vincolata a quella
  generation: valuta prima le advice PAPER-eligible ancora attive della generation, poi eventuali WATCH aperte della
  stessa execution. Se non restano advice attive, WATCH o posizioni aperte, ferma la PAPER in modo ordinato invece di
  ricadere sull'universo completo. Questo evita rumore runtime e decisioni non attribuibili alla generation validata.
- Il lifecycle management espone tre step ML distinti prima della promozione:
  1. `ml-prefilter`: action `UNIVERSE_PREFILTER`, chiama DocBrown `universe-scheduler/shadow` con
     `ROUND_ROBIN_SLA` per ordinare HOT/WARM/COLD senza impatto diretto su BUY/SELL;
  2. `ml-heavy-research`: action `RUN_RESEARCH`, chiama DocBrown heavy research; resta fase preparatoria separata
     dalla finestra PAPER;
  3. `ml-round-robin`: action `LIVE_SCORE`, `ROLLING_VALIDATION`, `BASELINE_98_DIAGNOSTICS` e recovery DB-driven;
     qui il braccio B e' la pipeline rolling corrente e il braccio A e' il contratto `A_BASELINE_98_CONTRACT`.
- Dopo `FAIL_SELECTION_BIAS` o assenza batch, Kenshiro deve riportare lo step corrente a `ml-prefilter` per rendere
  visibile che il ciclo sta ripartendo; la progress bar FE non deve restare sul vecchio `rolling-validation`.
- `RUN_FULL_ML_AUDIT`, `RUN_ROUND_ROBIN_DEEP_30`, `APPLY_MICRO_ML_REFRESH`, `APPLY_ROUND_ROBIN_DEEP_30`,
  `APPLY_FAST_ML_REFRESH` e `APPLY_FULL_ML_AUDIT` sono action Kenshiro/FE DB-driven sullo step `ml-heavy-research`.
  `RUN_FULL_ML_AUDIT` applica prima il profilo `FULL_AUDIT` e poi invoca DocBrown research come fase preparatoria
  separata dalla finestra PAPER; non avvia SHADOW/PAPER/REAL e non cambia gate.
  `RUN_ROUND_ROBIN_DEEP_30` applica prima il profilo `ROUND_ROBIN_DEEP_30` e poi invoca DocBrown research come fase
  preparatoria rapida; resta compute scheduling only, non avvia SHADOW/PAPER/REAL, non cambia gate e non sostituisce
  full audit/Forward A/B per validazione scientifica.
  Le action `APPLY_*` aggiornano solo `acdc_shared_runtime_config`:
  - `MICRO_REFRESH_40`: `lookback.hours=2`, `sample.every.seconds=60`, `symbol.limit=40`, `max.samples=600`,
    `validation.percent=30`, `management.research.profile=MICRO_REFRESH_40`;
  - `ROUND_ROBIN_DEEP_30`: `lookback.hours=2`, `sample.every.seconds=60`, `symbol.limit=30`, `max.samples=5000`,
    `validation.percent=30`, `management.research.profile=ROUND_ROBIN_DEEP_30`;
  - `FAST_REFRESH`: `lookback.hours=4`, `sample.every.seconds=60`, `symbol.limit=96`, `max.samples=1200`,
    `validation.percent=30`, `management.research.profile=FAST_REFRESH`;
  - `FULL_AUDIT`: `lookback.hours=12`, `sample.every.seconds=60`, `symbol.limit=288`, `max.samples=5000`,
    `validation.percent=30`, `management.research.profile=FULL_AUDIT`.
  I profili micro/fast servono a ridurre latenza operativa e refresh delle regole, ma non sostituiscono full
  audit/forward A/B per validazione scientifica. `MICRO_REFRESH_40` e `ROUND_ROBIN_DEEP_30` sono scheduling
  computazionale: non escludono simboli in modo definitivo, non modificano BUY/SELL e non autorizzano PAPER se
  `ML_READY=false`.
- Kenshiro `/management/state` deve esporre metriche di freshness: `latestAdviceAgeSeconds`,
  `latestActiveAdviceAgeSeconds`, `latestAdviceRemainingSeconds`, `latestSignalToAdviceSeconds`,
  `latestRollingSignalLagSeconds`, `latestRoundRobinDurationSeconds`, simbolo/source/generation latest advice.
- `ENABLE_SIGNATURE_ADVICE` e `ENABLE_PAPER_CANDIDATES` sono action Kenshiro/FE di recovery REM: possono aggiornare
  solo `acdc_shared_runtime_config` per abilitare rispettivamente `rem.ml.signature_advice.enabled=true` e
  `rem.ml.paper_candidate.enabled=true`; non avviano trading, non chiamano ACDC/DocBrown e devono rispettare il current
  step `ml-round-robin`.
- `RESTORE_BASELINE_98_PROFILE` e' action Kenshiro/FE di recovery REM sullo step `ml-round-robin`: aggiorna solo
  `acdc_shared_runtime_config` per riportare il profilo vicino alla baseline 98 (`signature_advice=true`,
  `paper_candidate=true`, `paper_candidate.min.validation.profit_rate=0.60`,
  `paper_candidate.min.avg_net_return=0.0020`, `promotion.mode=PURE_REVERSAL`). Non crea advice, non promuove batch
  falliti, non avvia trading e non sostituisce la forward A/B.
- `ROLLING_VALIDATION` deve essere single-flight lato Kenshiro/FE: se una validazione e' gia' in corso, l'action deve
  risultare bloccata/disabilitata per evitare doppie richieste e deadlock sugli insert DocBrown.
- La `ROLLING_VALIDATION` manuale avviata da `/management` usa il payload management con
  `universeMode=ROUND_ROBIN_SLA`, universo base configurabile e audit periodico; lo scheduler resta solo computazionale e
  non e' un filtro BUY/SELL. L'autociclo `AUTO_AB_START` usa invece chunk piccoli per abbattere la latenza:
  `rem.ml.management.auto.round_robin.chunk_symbol_limit` default `30`,
  `rem.ml.management.auto.round_robin.hot_symbol_limit` default `10`,
  `rem.ml.management.auto.round_robin.per_symbol_limit` default `8`,
  `rem.ml.management.auto.round_robin.window_minutes` default `5`,
  `rem.ml.management.auto.round_robin.feature_window_minutes` default `10`. Le finestre automatiche piu' corte sono
  un intervento di freschezza/latency, non un allargamento dei gate: `horizonSeconds` e vincoli PAPER restano invariati.
  Un chunk senza candidato termina fail-closed e lascia al tick successivo l'analisi dello spezzone successivo; un chunk
  promuovibile passa subito a promotion/runtime, senza creare advice preliminari destinate a scadere durante una
  validazione lunga.
- DocBrown puo' esporre il ramo parallelo lifecycle-capture con:
  `candidate.lifecycleStatus=PASS_CAPTURE_CANDIDATE_REQUIRES_SHADOW` e
  `strategicStatus=LIFECYCLE_CAPTURE_REQUIRES_SHADOW_PREFLIGHT`. Questo status non e' `PASS_CANDIDATE`, non deve far
  partire `ROLLING_PROMOTION` e non autorizza PAPER. Kenshiro deve continuare a trattarlo come fail-closed per il path
  automatico PAPER, lasciando l'automazione in cooldown. Il vecchio preflight SHADOW dedicato al lifecycle-capture e'
  ritirato: non usare `LIFECYCLE_CAPTURE_SHADOW_PREFLIGHT`, non creare profili `SHADOW_LIFECYCLE_CAPTURE_PREFLIGHT_V1`
  e non salvare nuove config `rem.ml.shadow.lifecycle_capture.*`.
- `REM_PRE_BUY_WATCH_V1` e' il protocollo runtime corretto per la WATCH REM: non e' una action manuale e non e' un
  profilo SHADOW tecnico. Entra nel normale scheduler BUY dopo che un candidato e' gia' BUY-eligible. ACDC registra il
  candidato in `acdc_pre_buy_watch` con stato `WATCHING`, timeout derivato dall'ML/advice
  (`pre_buy_watch_timeout_seconds`, `watch_timeout_seconds`, `max_buy_age_seconds` o `duration_seconds`) e snapshot del
  contratto. Ai tick successivi il runtime rivaluta lo stesso contratto ENTRY/ML usato per la BUY: se passa entro timeout
  autorizza la BUY (`WATCH_CONFIRMED_BUY` -> `BUY_OPENED`), se scade chiude `WATCH_EXPIRED`, se il runtime rifiuta dopo
  conferma registra `BUY_REJECTED_RUNTIME`. `PAPER_STOP`, `PAPER_STOP_BUY`, `SHADOW_STOP` e `SHADOW_STOP_BUY` devono
  chiudere ogni WATCH ancora `WATCHING` della execution: `EXPIRED` se il timeout e' gia' superato, altrimenti
  `ABANDONED` con reason di stop. Il watch conta come BUY pending ai fini dei limiti operativi. Non introduce soglie
  parallele, non allarga gate/live/SELL, non promuove, non avvia PAPER se `ML_READY=false` e non avvia REAL.
- Una WATCH gia' aperta deve essere processata anche se il tick successivo non ripete il flag
  `pre_buy_watch_required`: il contratto da rivalutare e' quello salvato/aperto dalla WATCH. Se il BUY contract non e'
  ancora vero, la WATCH resta `WATCHING` con reason `WATCH_WAITING_BUY_CONTRACT`, senza invalidarsi immediatamente.
- `BASELINE_98_DIAGNOSTICS` e' action diagnostica Kenshiro/FE sullo step `ml-round-robin`: valuta il contratto
  `A_BASELINE_98_CONTRACT` sul batch `management-rolling-*` piu' recente o su `payload.batchId`, senza creare advice,
  senza promuovere, senza avviare trading. Serve a distinguere "baseline 98 senza opportunity" da "opportunity isolate
  ma non stabili".
- `ROLLING_NEAR_MISS_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation` e `ml-round-robin`:
  legge gli ultimi batch `management-rolling-*`, top symbol, simboli positivi ripetuti e near-miss per finestre rolling.
  Payload opzionale: `batchLimit` default `12`, `topLimit` default `8`, `economicSafeReturn` default `0.0030`.
  Non chiama ACDC/DocBrown, non crea advice, non promuove batch falliti e non autorizza allargamento gate. Se il verdict
  e' `POSITIVE_CLUSTERS_NOT_STABLE_ENOUGH`, la diagnosi corretta e' "segnali grezzi presenti ma non PAPER-promuovibili".
- `ROLLING_EV_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation` e `ml-round-robin`: legge gli
  ultimi batch `management-rolling-*` e calcola per simbolo EV proxy, win/loss ratio, safe-hit rate, zero-MFE rate,
  average MFE, worst/best end e classi `EV_CANDIDATE`, `POSITIVE_BUT_ZERO_MFE_RISK`, `MFE_PRESENT_BUT_EV_WEAK`,
  `NEGATIVE_EV`. Payload opzionale: `batchLimit` default `12`, `topLimit` default `12`, `economicSafeReturn` default
  `0.0030`. Serve a distinguere segnali con payoff atteso credibile da segnali con media positiva ma rischio zero-MFE
  e non crea advice, non promuove batch falliti, non allarga gate e non sostituisce Forward A/B 98.
- `ROLLING_SELECTION_ATTRIBUTION_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation` e
  `ml-round-robin`: legge il batch corrente o `payload.batchId`, confronta il simbolo selezionato da round-robin con i
  top simboli del batch e con la classifica EV recente, e riporta rank per `avg_end`, `avg_mfe`, `safe_hit_rate`,
  `zero_mfe_rate`, tail/worst-end, split stats e distribuzione `reversal_trough_age_seconds`. Classi principali:
  `UPSIDE_WITH_TAIL_RISK`, `SELECTION_RANKING_MISMATCH`, `EV_TAIL_RISK_BLOCK`, `POSITIVE_CLUSTER_UNSTABLE`,
  `WINDOW_NOT_USEFUL`, `PROMOTION_NOT_REACHED`. Payload opzionale: `batchId`, `batchLimit` default `12`, `topLimit`
  default `12`, `economicSafeReturn` default `0.0030`. Non crea advice, non promuove, non cambia ranking, non avvia
  SHADOW/PAPER/REAL e non conta come evidenza Forward A/B.
- `ROLLING_LIFECYCLE_CAPTURE_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation` e
  `ml-round-robin`: legge il batch corrente o `payload.batchId` e misura se candidati bocciati su end-return sarebbero
  stati lifecycle-captureable dal guard `EXIT_ML_ADVICE_TAKE_PROFIT`. Riporta per simbolo metriche aggregate e split
  selection/holdout: `avg_end`, `avg_mfe`, `safe_hit_rate`, `zero_mfe_rate`, `worst_end`,
  `post_safe_decay`, `holdout_avg_end`, `holdout_avg_mfe`, `holdout_safe_hit_rate`,
  `holdout_zero_mfe_rate`, `holdout_post_safe_decay` e `capture_score`. Classi principali:
  `CAPTURE_CANDIDATE_DECAYS_AFTER_SAFE`, `END_AND_SAFE_ALIGNED`, `NO_SAFE_OPPORTUNITY`, `INCONCLUSIVE`;
  verdict principali `SELECTED_CAPTURE_CANDIDATE_DECAYS_AFTER_SAFE`, `CAPTURE_CANDIDATES_PRESENT`,
  `NO_CAPTURE_OBJECTIVE_MISMATCH`. Payload opzionale: `batchId`, `symbol`, `batchLimit` default `12`, `topLimit`
  default `12`, `economicSafeReturn` default `0.0030`. Non crea advice, non promuove batch falliti, non cambia ranking,
  non allarga gate, non cambia SELL, non avvia SHADOW/PAPER/REAL e non conta come evidenza Forward A/B.
- `LIFECYCLE_CAPTURE_MISMATCH_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation`,
  `ml-round-robin` e `shadow`: legge il selected lifecycle-capture del batch corrente o `payload.batchId` e lo classifica
  senza avviare preflight SHADOW dedicati. Payload opzionale: `batchId`, `symbol`, `decisionLimit` default `20` massimo
  `100`, `economicSafeReturn` default `0.0030`. Classi principali:
  `SELECTED_CAPTURE_WITHOUT_LIVE_ML_CONTRACT`, `SELECTED_CAPTURE_WITHOUT_ML_RULES`,
  `SELECTED_CAPTURE_WITHOUT_PAPER_ELIGIBLE_ADVICE`, `LIFECYCLE_PREFLIGHT_REMOVED`. Serve a distinguere un candidato
  rolling captureable o gia' allineato su end/safe (`CAPTURE_CANDIDATE_DECAYS_AFTER_SAFE` o `END_AND_SAFE_ALIGNED`)
  senza contratto ML live (`reversal_ml_rules=0`, `ml_advice_paper_eligible=0`) da un problema di selection. Non crea
  advice, non promuove, non avvia SHADOW/PAPER/REAL, non allarga gate, non
  cambia SELL e non conta come evidenza Forward A/B.
- Dopo una `ROLLING_VALIDATION` con DocBrown score-breakdown abilitato, Kenshiro persiste diagnostica compatta e bounded
  nei marker:
  `rem.ml.management.round_robin.selected_score_breakdown` e
  `rem.ml.management.round_robin.selection_score_rows`; per lookup storico usa anche
  `rem.ml.management.round_robin.audit.{batchId}.selected_score_breakdown` e
  `rem.ml.management.round_robin.audit.{batchId}.selection_score_rows`. `ROLLING_SELECTION_ATTRIBUTION_AUDIT` restituisce
  questi marker come `docBrownSelectedScoreBreakdown` e `docBrownSelectionScoreRows`. Sono diagnostica DB-only:
  spiegano lo `stabilityScore` effettivo con chiavi sintetiche per i contributi `safe_hit`, `zero_mfe`, `early_trough`,
  `no_safe_opportunity`, `drawdown`, `instability` e per `lifecycleStatus` (`ls`); `selection_score_rows` contiene solo
  selected e prima alternativa per restare sotto il limite DB dei marker. Non cambiano gate, non creano advice e non
  sostituiscono Forward A/B 98.
- `ROLLING_LATENCY_BOTTLENECK_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation` e
  `ml-round-robin`: legge il batch corrente o `payload.batchId`, il simbolo selezionato, timing batch/round-robin e una
  classifica freshness/risk basata su `safe_hit_rate`, `zero_mfe_rate`, tail/worst-end e
  `reversal_trough_age_seconds`. Classi principali: `SELECTION_TOO_EARLY`, `SELECTION_TOO_LATE`,
  `TAIL_RISK_SELECTED`, `FRESHNESS_AWARE_SELECTION_WOULD_DIFFER`, `NO_LATENCY_BOTTLENECK`,
  `PROMOTION_NOT_REACHED`. Payload opzionale: `batchId`, `symbol`, `economicSafeReturn`. Non crea advice, non promuove,
  non cambia gate, non avvia SHADOW/PAPER/REAL e serve solo ad attribuire colli di bottiglia di timing/ranking.
- Rolling selection ranking DocBrown puo' usare pesi runtime cost-aware senza cambiare i gate PAPER:
  `rem.ml.rolling.selection.edge_weight`, `mfe_rate_weight`, `q10_mfe_weight`,
  `safe_hit_weight`, `zero_mfe_penalty_weight`, `early_trough_penalty_weight`, `instability_penalty_weight`,
  `drawdown_penalty_weight`, `lifecycle_capture_weight`, `decay_after_safe_relief_weight`. Questi pesi influenzano
  l'ordinamento diagnostico dei candidati, ma non producono una short-list operativa e non rimuovono i requisiti di
  promozione su holdout, worst-window, MFE eseguibile, q10 MFE
  positivo, safe/economic return e `ML_READY`. Default legacy: edge/mfe/q10 `0`, instability `1.50`, drawdown `1.25`;
  profilo maturity/tail-aware corrente: safe-hit `0.002`, zero-MFE penalty `0.006`, early-trough penalty `0.004`.
  Profilo cost-aware usato per refinement: edge `1`, mfe-rate `0.002`, q10 MFE `2`, instability `0.35`,
  drawdown `0.5`. Il refinement lifecycle-capture approvato dal Consiglio usa solo pesi DB-driven reversibili:
  `lifecycle_capture_weight` premia candidati con safe-hit/MFE robusti e zero-MFE basso; `decay_after_safe_relief_weight`
  alleggerisce parzialmente la penalita' drawdown solo quando esiste post-safe decay misurato. Entrambi default `0`,
  non allargano gate, non cambiano SELL e non sostituiscono la Forward A/B 98.
- La rolling validation management puo' usare `rem.ml.rolling.lookback.seconds` per ridurre o ampliare la finestra
  osservata dal payload Kenshiro verso DocBrown. Default legacy `3600`; bounds operativi `1200..7200`; profilo latency
  refinement corrente `1800`. Questo e' un parametro di orchestrazione/selection per ridurre window decay e latenza del
  batch, non un allargamento dei gate PAPER e non sostituisce holdout/worst-window/MFE/safe checks.
- `INVERSE_COMPLEMENT_WALK_FORWARD_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation` e
  `ml-round-robin`: legge batch `management-rolling-*`, seleziona i top simboli inverse solo sulla meta' train piu'
  vecchia e valuta la meta' test successiva senza usare runtime trading. Proxy inverse: `-end_net_return`; MFE favorevole
  inverse: `greatest(0, -min_net_return)`. Payload opzionale: `batchLimit` default `12`, `topLimit` default `20`,
  `economicSafeReturn`/`safeReturn` default `0.0030`. Output: selected train symbols, bucket test
  `SELECTED_TOP_INVERSE`/`COMPLEMENT`, fold test, contribution per simbolo, verdict conservativo e policy
  `DB_ONLY_TECHNICAL_ONLY`. Non crea advice, non promuove, non avvia SHADOW/PAPER/REAL, non conta come Forward A/B 98 e
  non autorizza inversione operativa. Un verdict `INVERSE_COMPLEMENT_RESEARCH_CANDIDATE` autorizza solo la
  pre-registrazione del prossimo protocollo scientifico; eventuale SHADOW inverse/complement deve essere progettato e
  approvato separatamente come `TECHNICAL_ONLY`.
- `SHADOW_OPPORTUNITY_GUARD_TRACE` e' action diagnostica Kenshiro/FE SHADOW-only per testare l'ipotesi "BUY gate troppo
  stretto / guardia di troppo" senza contaminare PAPER. Avvia una `SHADOW_EV` tecnica con
  `shadowRelaxationProfile=SHADOW_OPPORTUNITY_NO_BUY_GATE_AUDIT_V1`, `validationProtocol=TECHNICAL_ONLY`,
  `notForwardAbEvidence=true`, `notPaperEligible=true`, `doNotPromote=true`, `paperUntouched=true` e
  `bypassLevel=L0_NONE`. Non bypassa nessuna guardia: serve come guard-trace runtime sui top near-miss del batch
  corrente o sui simboli espliciti del payload (`symbols`, massimo 3). Richiede runtime pulito (`paperRunning=false`,
  `openPositions=0`), non crea advice, non promuove batch falliti, non avvia PAPER/REAL e non conta come Forward A/B 98.
  Se produce solo reject, leggere le decisioni SHADOW per attribuire guardia, feature e reason. Un eventuale bypass L1/L2
  va progettato separatamente dopo evidenza L0, una guardia alla volta, e resta `TECHNICAL_ONLY`.
- `SHADOW_OPPORTUNITY_RULE_MISSING_BYPASS` e' il livello L1 dello stesso audit, ammesso solo dopo evidenza L0 che i top
  near-miss sono bloccati da `REVERSAL_ML_RULE_MISSING`. Avvia `SHADOW_EV` tecnica con
  `shadowRelaxationProfile=SHADOW_EV_NEAR_MISS_RULELESS_PROBE_V1` sui simboli espliciti del payload o sui top near-miss
  del batch corrente, bypassando esclusivamente `REVERSAL_ML_RULE_MISSING`. Tutti gli altri gate, budget SHADOW, fee,
  SELL, loss-cap, max-hold e controlli runtime restano attivi. La run resta `TECHNICAL_ONLY`, non crea advice, non
  promuove batch falliti, non avvia PAPER/REAL e non conta come Forward A/B 98. Dopo l'avvio usare `SHADOW_STOP_BUY`
  appena raccolte le prime decisioni/BUY o al timeout breve, poi `SHADOW_STOP` dopo drain.
- `SHADOW_EV_NEAR_MISS_RULELESS_PROBE_V1` e' una procedura tecnica SHADOW-only approvata dal Consiglio per misurare
  outcome live di candidati EV/near-miss quando il runtime rifiuta solo per `REVERSAL_ML_RULE_MISSING`. Kenshiro puo'
  usarla solo dopo `NO_PROMOTABLE_CANDIDATE`, con runtime pulito (`paperRunning=false`, `openPositions=0`), massimo 1-3
  simboli selezionati da EV/near-miss, executionMode corto `SHADOW_EV` e metadata obbligatori:
  `validationProtocol=TECHNICAL_ONLY`, `counterfactual=RULE_MISSING_BYPASS_COUNTERFACTUAL`,
  `notForwardAbEvidence=true`, `notPaperEligible=true`, `doNotPromote=true`, `paperUntouched=true`,
  `sourceBatchId`, `experimentTrigger`. ACDC puo' bypassare esclusivamente il reject
  `REVERSAL_ML_RULE_MISSING`; tutti gli altri gate, budget SHADOW, fee, SELL, loss-cap, max-hold e controlli runtime
  restano attivi. La procedura non crea advice, non scrive PAPER eligibility, non modifica PAPER/REAL, non conta come
  Forward A/B 98 e non autorizza tuning da un singolo esito. Stop/valutazione: dopo primo BUY chiuso o breve finestra di
  probe, usare `SHADOW_STOP_BUY` e lasciare drain; se i probe producono `zero_mfe_rate` alto, safe-hit nullo o loss-cap
  ripetuti, classificare `FALSE_CONTINUATION_CONFIRMED` / `RULELESS_EV_SELECTOR_NOT_MONETIZABLE_AS_IS`.
- I probe automatici/tecnici `rem.ml.management.auto.shadow_probe.*` e `SHADOW_PAPER_LIKE_RELAXED_98_PROBE` sono
  ritirati. Non usarli come modo per osservare WATCH, non salvare nuove config `rem.ml.shadow.paper_like.*` e non
  bypassare `REVERSAL_ML_RULE_MISSING` per forzare una WATCH. La WATCH deve arrivare da advice ML/promossa tramite
  `REM_PRE_BUY_WATCH_V1`.
- `SAVE_MANAGEMENT_CONFIG` e' action Kenshiro/FE per salvare configurazioni `rem.ml.*` editabili in
  `acdc_shared_runtime_config`. E' bloccata se c'e' PAPER running o qualsiasi posizione aperta. Non avvia
  SHADOW/PAPER/REAL, non crea advice e non promuove batch.
- `LIVE_REVALIDATION_DRIFT_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation`,
  `auto-promotion`, `ml-round-robin` e `promotion`: legge i marker correnti `round_robin.batch_id`,
  eventuali `selected_symbols` legacy solo come contesto storico, `promotion_rows`, `promotion_statuses`,
  `promotion_attempt_at`, batch timing e marker retry
  `rem.ml.management.automation.*`. Serve a classificare `PROMOTION_NO_ADVICE` da
  `SKIPPED_LIVE_REVALIDATION_CONTRACT` come `PROMOTION_BOTTLENECK_LIVE_REVALIDATION_DRIFT`,
  `PROMOTION_EXPIRED_BEFORE_ATTEMPT`, `LIVE_REVALIDATION_TRUE_REJECT_OR_WEAK_VALIDATION` o
  `NO_LIVE_REVALIDATION_DRIFT_ROWS`, esponendo feature fallite, valore osservato, range contrattuale, distanza dal
  range, lag `signal->batch->promotion` e secondi residui a `validUntil` al momento dell'attempt. Non chiama
  DocBrown/ACDC, non crea advice, non promuove batch falliti, non avvia PAPER e non autorizza allargamento gate.
  Payload opzionale: `batchId`; se i marker per-batch sono stati persistiti da Kenshiro, l'audit legge lo storico del
  batch richiesto invece dei marker correnti.
- `ENTRY_WINDOW_DECAY_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `auto-validation`, `auto-promotion`,
  `auto-paper-start`, `auto-shadow-start`, `run-monitor`, `ml-round-robin`, `promotion`, `shadow` e `paper`: legge
  execution Forward A/B, advice live, decisioni PAPER/SHADOW e timeline entry per classificare
  `ENTRY_WINDOW_DECAY`, `A_B_RULE_ALIGNMENT_MISMATCH`, `LIVE_REVALIDATION_TRUE_REJECT`,
  `FRESHNESS_CONTRACT_EXPIRED` o `NO_ENTRY_WINDOW_DECAY`. Payload opzionale: `groupId`, `paperExecutionId`,
  `shadowExecutionId`; senza payload usa l'ultimo gruppo `FORWARD_AB_98`. Non chiama ACDC/DocBrown, non crea advice,
  non cambia `max_buy_age_seconds`, non allarga gate e non promuove run fallite.
- `LIVE_FALSE_CONTINUATION_AUDIT` e' action diagnostica Kenshiro/FE DB-only sugli step `run-monitor`, `shadow` e
  `paper`: legge PAPER Forward A/B, posizioni, policy JSON e post-sell forensics per classificare trade fresche con
  entry timing valido ma `max_net_return=0` come `LIVE_REVERSAL_FALSE_CONTINUATION`, distinguendole da
  `SELL_CAPTURE_CANDIDATE` quando `max_net_return >= safe_net_return` e la chiusura resta negativa. Payload opzionale:
  `executionId`; senza payload usa l'ultima PAPER `FORWARD_AB_98`. Non chiama ACDC/DocBrown, non cambia gate/ranking,
  non cambia SELL e non autorizza tuning da un singolo caso.
- `/pipelines` e sotto-pagine sono legacy per il prossimo ciclo REM.
- Curl diretti su ACDC/DocBrown sono ammessi come diagnostica tecnica o fallback esplicito, non come interfaccia utente
  primaria per avviare il ciclo.

### 0.1 Protocollo Forward A/B 98

Obbligatorio per ogni nuova SHADOW/PAPER che venga usata come evidenza scientifica sulla baseline `98`.

Checklist vincolante:

```text
hft-common/doc/acdc/session/2026-06-21/session-144-forward-ab-98-checklist.md
```

Regola:

- se la run non produce confronto `FORWARD_AB_98`, e' `TECHNICAL_ONLY` o `INCONCLUSIVE`;
- non puo' produrre `PASS_BASELINE`;
- non puo' produrre `FAIL_BASELINE`;
- non autorizza tuning feature-by-feature;
- non autorizza modifiche a BUY/SELL/ranking/trailing.

Bracci minimi:

```text
A_BASELINE_98_CONTRACT
B_CURRENT_ROLLING_PIPELINE
```

### 0.2 Checklist Diagnosi Strutturale REM

Scopo: capire se l'architettura corrente e' ottimale o se sta perdendo finestre utili per latenza, selezione, gate,
promotion, freshness o SELL. Questa checklist non autorizza REAL, non autorizza allargamento gate e non sostituisce
Forward A/B 98. Deve essere eseguita mentre l'autociclo normale resta acceso, salvo bug tecnico o richiesta di stop.

Decisione del Consiglio:

- l'automazione `AUTO_AB_START` deve restare attiva per aspettare la finestra utile;
- dopo batch fail-closed si raccolgono diagnostiche, ma non si spegne il ciclo solo per assenza temporanea di candidati;
- se molte finestre fresche restano non promuovibili, si apre diagnosi strutturale prima di cambiare soglie o strategia;
- ogni modifica proposta deve essere classificata come: osservabilita', orchestrazione, latenza, selection/ranking,
  gate scientifico, promotion, runtime BUY/SELL. Solo osservabilita'/orchestrazione possono essere applicate senza
  rivalutare il vincolo strategico.

Checklist operativa:

1. Stato e automazione:
   - verificare `/management/state`;
   - `automationEnabled` deve essere `true` durante osservazione normale;
   - `paperRunning=false` e `openPositions=0` prima di ogni nuovo ciclo;
   - current step in cooldown deve restare su ramo auto;
   - se action necessaria e' disabilitata nel FE ma ammessa dal piano, correggere gating FE/Kenshiro.

2. Frequenza e copertura finestre:
   - conteggiare gli ultimi `N>=12` batch `management-rolling-*`;
   - misurare `rows_count`, `symbols`, `min/max created_at`, durata validation e gap tra batch;
   - verificare che HOT/WARM/COLD `ROUND_ROBIN_SLA` non lasci simboli utili fuori SLA;
   - se il ciclo non copre abbastanza finestre fresche, il problema e' orchestrazione/scheduling, non ranking.

3. Latenza signal->advice->promotion->PAPER:
   - leggere metriche Kenshiro: `latestSignalToAdviceSeconds`, `latestRollingSignalLagSeconds`,
     `latestAdviceAgeSeconds`, `latestAdviceRemainingSeconds`;
   - classificare ogni opportunity persa come `signal_late`, `promotion_late`, `paper_start_late`,
     `advice_expired`, `live_revalidation_drift`;
   - se una advice promossa scade prima della PAPER, correggere latenza/orchestrazione prima di toccare i gate.

4. Baseline 98 vs B corrente:
   - eseguire `BASELINE_98_DIAGNOSTICS` sul batch fresco;
   - registrare `acceptedAvgEndNetReturn`, `acceptedPositiveEndRate`, top symbol e rejection reasons;
   - se A-98 e B sono entrambi negativi, la finestra non e' utile;
   - se A-98 e' positivo e B negativo, il problema e' regressione selection/gate/promotion;
   - se B e' positivo ma non promuove, il problema e' promotion/live revalidation/freshness.

5. Near-miss e stabilita':
   - eseguire `ROLLING_NEAR_MISS_AUDIT`;
   - distinguere `NO_POSITIVE_CLUSTERS` da `POSITIVE_CLUSTERS_NOT_STABLE_ENOUGH`;
   - per ogni simbolo near-miss leggere `avg_mean_end`, `avg_worst_end`, `avg_safe_windows`,
     `avg_positive_windows`, `avg_mean_mfe`;
   - un simbolo con media forte ma worst-window negativa non e' promuovibile, ma e' candidato per diagnosi ranking/gate.

6. Selection/ranking:
   - confrontare simbolo selezionato dalla rolling validation con top symbol A-98 e top symbol near-miss;
   - eseguire `ROLLING_EV_AUDIT` per separare candidati con EV proxy credibile da candidati positivi ma zero-MFE-risk;
   - se il selezionato non e' tra i migliori per edge/MFE/stabilita', classificare `SELECTION_RANKING_MISMATCH`;
   - non cambiare ranking senza prima produrre confronto su piu' batch freschi.

7. Promotion e rejection reasons:
   - dopo `PROMOTION_NO_ADVICE`, leggere `rem.ml.management.round_robin.promotion_rows` e `promotion_statuses`;
   - eseguire `LIVE_REVALIDATION_DRIFT_AUDIT` per misurare lag e distanza dal range live senza cambiare contratto;
   - classificare ogni riga: `SKIPPED_LIVE_REVALIDATION_CONTRACT`, `NO_LIVE_REVALIDATION_FEATURES`,
     `ADVICE_EXPIRED`, `SOURCE_GENERATION_MISMATCH`, altro;
   - se la promotion arriva ma non crea advice per drift live, correggere latenza o contratto di revalidation solo dopo
     evidenza ripetuta.

8. PAPER e SELL, solo se parte PAPER:
   - confermare `FORWARD_AB_98`;
   - verificare BUY B corrente e SHADOW A baseline nella stessa finestra;
   - dopo SELL leggere forensics: MFE, safe hit, capture ratio, exit guard, loss cap, hold time;
   - una PAPER con MFE sopra safe e SELL negativa richiede diagnosi SELL/capture, non tuning entry;
   - una PAPER fresca con entry drift nullo ma `maxNetReturn=0`, `targetHit=false` e loss-cap richiede diagnosi
     `ENTRY_WINDOW_DECAY` / `LIVE_REVERSAL_FALSE_CONTINUATION`: confrontare validation split, timeline advice->BUY,
     live revalidation e post-exit forensics prima di toccare SELL o gate entry.
   - se PAPER non compra ma SHADOW A compra, eseguire `ENTRY_WINDOW_DECAY_AUDIT` per separare live revalidation
     temporale, scadenza freshness, mismatch di source/rule e differenza contrattuale A/B.

9. Decisione:
   - `WINDOW_NOT_USEFUL`: A-98 e B negativi, nessuna azione strategica;
   - `REGRESSION_SUSPECTED`: A-98 positivo e B negativo su piu' batch freschi;
   - `LATENCY_BOTTLENECK`: opportunity valida ma advice/PAPER arriva tardi;
   - `PROMOTION_BOTTLENECK`: validation positiva ma niente advice;
   - `SELL_CAPTURE_BOTTLENECK`: PAPER entra bene ma non cattura safe;
   - `ENTRY_WINDOW_DECAY`: validation/promozione positive, advice fresca, ma dopo BUY non compare MFE utile e il trade
     chiude in loss-cap;
   - `PAPER_READY_CANDIDATE`: PAPER Forward A/B pulita, trade sufficienti, PnL attivo e forensics coerenti.

Stop condition:

- fermare l'automazione solo per bug tecnico, PAPER/posizione da gestire, richiesta utente, oppure evidenza strutturale
  che il ciclo stia producendo carico inutile senza copertura nuova. La sola assenza di candidati in pochi batch non e'
  motivo sufficiente per spegnere l'autociclo normale.

Il braccio A rappresenta il contratto della sessione `98`: `reversal_pre_trough_drop` fuori dal live hard gate, `reversal_slope_delta` dentro, nessun ulteriore tuning. Se non e' ancora disponibile come runtime separato, deve essere calcolato almeno come shadow/counterfactual sullo stesso snapshot set della finestra realtime.

Il braccio B rappresenta la pipeline corrente: `ROLLING_PAPER`, source generation binding, live revalidation, economic safe, `ML_READY=true` per PAPER.

Payload minimo braccio A:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/shadow/run/REM_CURRENT' \
  -d '{
    "executionMode":"SHADOW",
    "validationProtocol":"FORWARD_AB_98",
    "forwardAbGroupId":"ab98-YYYYMMDDTHHMMSSZ",
    "forwardAbArm":"A_BASELINE_98_CONTRACT",
    "baselineReferenceSession":"98",
    "comparisonArm":"B_CURRENT_ROLLING_PIPELINE"
  }' | jq '.'
```

Payload minimo braccio B:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/paper/run/REM_CURRENT' \
  -d '{
    "executionMode":"PAPER",
    "validationProtocol":"FORWARD_AB_98",
    "forwardAbGroupId":"ab98-YYYYMMDDTHHMMSSZ",
    "forwardAbArm":"B_CURRENT_ROLLING_PIPELINE",
    "baselineReferenceSession":"98",
    "comparisonArm":"A_BASELINE_98_CONTRACT"
  }' | jq '.'
```

Diagnostica strutturale A/B:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/forward-ab/98?groupId=ab98-YYYYMMDDTHHMMSSZ' | jq '.'
```

Il report restituisce:

- `ready=true` solo se il gruppo contiene entrambi i bracci;
- riepilogo execution per braccio;
- metriche aggregate minime da decisioni e posizioni: decisioni, BUY, trade chiusi, win/loss, PnL, MFE medio, zero-MFE, loss-cap, timeout e dynamic trailing.

Nota runtime:

- le run senza `validationProtocol` vengono marcate `TECHNICAL_ONLY` in `acdc_run_execution.metadata_json`;
- un `FORWARD_AB_98` senza `forwardAbGroupId` o senza `forwardAbArm` valido viene rifiutato con `409`;
- gli arm validi sono `A_BASELINE_98_CONTRACT` e `B_CURRENT_ROLLING_PIPELINE`.

Artifact consigliato:

```bash
OUT=/tmp/session-next-forward-ab-98-$(date -u +%Y%m%dT%H%M%SZ)
mkdir -p "$OUT"
```

Pre-run minimo:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/ml-readiness?profileKey=REM_CURRENT' | tee "$OUT/ml_readiness_pre.json" | jq '.'
curl -sS 'http://localhost:8091/diagnostics/acdc/live-advice/REM_CURRENT' | tee "$OUT/live_advice_pre.json" | jq '.'
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/readiness?profileKey=REM_CURRENT' | tee "$OUT/rem_readiness_pre.json" | jq '.'
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/session-guard/REM_CURRENT' | tee "$OUT/session_guard_pre.json" | jq '.'
```

Post-run minimo per ogni execution prodotta:

```bash
EXEC={executionId}
curl -sS "http://localhost:8091/diagnostics/acdc/paper/scoring?executionIds=$EXEC" > "$OUT/scoring_$EXEC.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/timeline?executionIds=$EXEC" > "$OUT/timeline_$EXEC.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/sell-capture?executionIds=$EXEC" > "$OUT/sell_capture_$EXEC.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/post-sell-forensics?executionIds=$EXEC" > "$OUT/post_sell_forensics_$EXEC.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=$EXEC&horizonSeconds=900" > "$OUT/counterfactual_$EXEC.json"
```

Metriche minime da confrontare fra A e B:

- simboli valutati;
- advice totali e `PAPER_ELIGIBLE`;
- BUY e SELL;
- win/loss;
- PnL netto;
- MFE medio/massimo;
- safe hit e target hit;
- capture ratio;
- zero-MFE;
- loss-cap;
- timeout;
- dynamic trailing;
- `GOOD_BLOCK`, `BAD_BLOCK`, `AMBIGUOUS_BLOCK`;
- verdict post-SELL.

### 1. Stato Git E Runtime

```bash
git status --short
git log --oneline -5
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

Scopo:

- verificare se ci sono modifiche non committate;
- determinare il prossimo `MS<n>` se il gruppo deve essere committato;
- verificare container `acdc-vpn`, `docbrown`, `mysql_container`, `influxer`, `influxdb`, `grafana`;
- non partire con diagnosi trading se i container base non sono up.
- dopo restart o refill Influxer, verificare il completamento dai log container prima di giudicare incompleto il bucket:
  cercare `Short-retention Influx startup backfill completed. symbols=288`, `Ingesting symbol 288/288` e
  `Historical candles startup ingestion completed`. Non serve scorrere manualmente tutte le 48 ore se questi marker sono
  presenti; eventuali repair successivi `288/288` confermano la continuita' dell'ingest.

### 2. Stato DB Minimo

```bash
docker exec mysql_container sh -lc 'mysql -u${MYSQL_USER:-hft_user} -p"$MYSQL_PASSWORD" hft -e "
SELECT id, profile_key, status, execution_mode, current_budget, reserved_budget, realized_profit_quote, started_at, completed_at
FROM acdc_run_execution
ORDER BY id DESC
LIMIT 10;

SELECT COUNT(*) open_positions
FROM acdc_paper_position
WHERE status = \"OPEN\";

SELECT COUNT(*) rules, COALESCE(SUM(status=\"PROMOTED\"),0) promoted, MAX(created_at) max_created
FROM acdc_reversal_ml_rule;

SELECT status, COUNT(*) c, MAX(created_at) max_created, MAX(advice_valid_until) max_until
FROM acdc_live_ml_advice
GROUP BY status;
"'
```

Scopo:

- capire se ci sono PAPER running;
- verificare `reserved_budget`;
- verificare posizioni aperte;
- verificare se ML pesante ha regole;
- verificare se advice live sono fresche o residue.

### 3. Log Container

```bash
docker logs --tail 160 acdc-vpn
docker logs --tail 160 docbrown
docker logs --since 30m docbrown | tail -200
```

Scopo:

- cercare errori Flyway/startup;
- cercare timeout/rollback DocBrown;
- cercare scheduler ACDC che avvia execution non previste;
- cercare `Transaction Reaper`, `RollbackException`, `Connection is closed`.

### 4. Endpoint ACDC Operativi

Base URL:

```text
http://localhost:8091
```

Start/stop PAPER:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/paper/run/REM_CURRENT' \
  -d '{"executionMode":"PAPER","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/paper/stop-buy/REM_CURRENT' \
  -d '{"executionMode":"PAPER","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/paper/stop/REM_CURRENT' \
  -d '{"executionMode":"PAPER","symbols":[]}'
```

Uso:

- `paper/run`: solo dopo preflight `ML_READY=true`;
- `paper/stop-buy`: blocca nuove BUY e lascia drenare SELL;
- `paper/stop`: chiude execution se `reserved_budget=0`, altrimenti comportarsi come drain/stop-buy.

Start/stop SHADOW:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/shadow/run/REM_CURRENT' \
  -d '{"executionMode":"SHADOW","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/shadow/stop-buy/REM_CURRENT' \
  -d '{"executionMode":"SHADOW","symbols":[]}'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8091/acdc/shadow/stop/REM_CURRENT' \
  -d '{"executionMode":"SHADOW","symbols":[]}'
```

### 5. Endpoint ACDC Diagnostici

Base:

```text
http://localhost:8091/diagnostics/acdc
```

Live advice viste da ACDC:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/live-advice/REM_CURRENT' | jq '.'
```

REM readiness esistente:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/readiness?profileKey=REM_CURRENT' | jq '.'
```

ML readiness fast/fail-closed:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/ml-readiness?profileKey=REM_CURRENT' | jq '.'
```

Nota:

- per PAPER validativa deve restituire `ready=true`;
- advice `PURE_REVERSAL_OBSERVED` non bastano per readiness validativa;
- `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING` blocca PAPER;
- `sourceGenerationId` e `adviceSource` sono colonne queryable su `acdc_live_ml_advice`;
- una PAPER nuova viene legata a `expectedSourceGenerationId` e i BUY incoerenti vengono rifiutati;
- l'endpoint e' intenzionalmente fast e non include il replay completo REM, che resta su `/diagnostics/acdc/rem/readiness`.

REM parity:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/parity?profileKey=REM_CURRENT' | jq '.'
```

REM lifecycle replay:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/lifecycle-replay?profileKey=REM_CURRENT' | jq '.'
```

REM capturable replay:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/rem/capturable-replay?limit=100' | jq '.'
```

Paper scoring:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/scoring?executionIds=17' \
  | tee /tmp/acdc_paper_scoring.json \
  | jq '.'
```

Paper timeline:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/timeline?executionIds=17' \
  | tee /tmp/acdc_paper_timeline.json \
  | jq '.'
```

Paper sell-capture:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/sell-capture?executionIds=17' \
  | tee /tmp/acdc_paper_sell_capture.json \
  | jq '.'
```

Paper post-SELL forensics:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/post-sell-forensics?executionIds=17' \
  | tee /tmp/acdc_paper_post_sell_forensics.json \
  | jq '.'
```

Paper live revalidation counterfactual:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=17&horizonSeconds=900' \
  | tee /tmp/acdc_paper_counterfactual.json \
  | jq '.'
```

Paper session guard:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/paper/session-guard/REM_CURRENT' | jq '.'
```

Shadow replay:

```bash
curl -sS 'http://localhost:8091/diagnostics/acdc/shadow/{executionId}/replay?horizonSeconds=900' | jq '.'
```

### 6. Endpoint DocBrown

Base URL:

```text
http://localhost:8083/docbrown
```

Health Quarkus standard non e' affidabile in questo container: `/q/health` puo' rispondere `404`. Verificare gli endpoint reali sotto `/docbrown`.

Live advice scoring leggero:

```bash
curl -sS --max-time 180 -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/live-advice/REM_CURRENT/score' \
  | tee /tmp/docbrown_live_score.json \
  | jq '.'
```

Heavy research/mining:

```bash
curl -sS --max-time 1800 -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/research/REM_CURRENT/run' \
  | tee /tmp/docbrown_research_report.json \
  | jq '.'
```

Research status osservabile:

```bash
curl -sS 'http://localhost:8083/docbrown/rem/research/REM_CURRENT/status' | jq '.'
```

Nota:

- se risponde `{"status":"RUNNING"}`, un job e' gia' attivo;
- se dura troppo, controllare `docker logs --since 30m docbrown`;
- sessione 105 ha mostrato timeout/rollback del mining pesante.
- dal 2026-06-21 DocBrown persiste lo stato in `docbrown_rem_research_run`.

Rolling blank candidates:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/generate' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/rolling-validation' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/rolling-paper-promotion' \
  -d '{...}' | jq '.'
```

Universe triage/scheduler shadow:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/universe-triage/shadow' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/blank-candidates/REM_CURRENT/universe-scheduler/shadow' \
  -d '{...}' | jq '.'
```

DocBrown counterfactual:

```bash
curl -sS 'http://localhost:8083/docbrown/rem/live-advice/REM_CURRENT/counterfactual?from=2026-06-21T00:00:00Z&to=2026-06-21T01:00:00Z&horizonSeconds=900&limit=5000' | jq '.'
```

DocBrown paper execution forensics:

```bash
curl -sS 'http://localhost:8083/docbrown/rem/live-advice/paper-executions/forensics?executionIds=17' | jq '.'
```

Scientific preflight/acceptance:

```bash
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/live-advice/scientific/preflight' \
  -d '{...}' | jq '.'

curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:8083/docbrown/rem/live-advice/scientific/acceptance' \
  -d '{...}' | jq '.'
```

### 7. Script Esistenti

Directory:

```text
scripts/
```

Script disponibili:

```text
scripts/acdc-start-dry-run.sh
scripts/acdc-start-shadow-run.sh
scripts/acdc-start-paper-run.sh
scripts/acdc-start-real-run.sh
scripts/acdc-stop-run.sh
scripts/acdc-run-rem-ml.sh
scripts/acdc-stop-containers.sh
scripts/acdc-best-winner-action.sh
```

Uso consigliato:

- preferire endpoint espliciti per diagnosi scientifiche;
- usare script solo se accelerano azioni gia' comprese;
- non usare `acdc-start-real-run.sh`;
- non usare script PAPER se `ML_READY=false`.

### 8. Build E Deploy Container

Gate stringhe operative approvato:

```bash
cd /home/mbc/Documenti/ws/java/hft/kenshiro
python3 scripts/check_operational_strings.py --fail src/main/java src/test/java scripts

cd /home/mbc/Documenti/ws/java/hft/acdc
python3 /home/mbc/Documenti/ws/java/hft/kenshiro/scripts/check_operational_strings.py --fail src/main/java src/test/java
python3 /home/mbc/Documenti/ws/java/hft/kenshiro/scripts/check_operational_strings.py --fail scripts

cd /home/mbc/Documenti/ws/java/hft/docbrown
python3 /home/mbc/Documenti/ws/java/hft/kenshiro/scripts/check_operational_strings.py --fail src/main/java src/test/java
python3 /home/mbc/Documenti/ws/java/hft/kenshiro/scripts/check_operational_strings.py --fail scripts

cd /home/mbc/Documenti/ws/java/hft/hft-fe
python3 /home/mbc/Documenti/ws/java/hft/kenshiro/scripts/check_operational_strings.py --fail src scripts
```

Regole gate:

- un finding deve essere risolto spostando la stringa in enum/registry/costante del contesto, oppure classificato come
  literal accettabile aggiornando lo scanner in modo esplicito;
- non usare eccezioni locali o duplicati manuali per action/status/reason/config key/payload key;
- il gate e' statico e complementare a build/test: non sostituisce test Quarkus/Svelte, deploy container o smoke
  operativo da FE `/management`;
- se lo scanner segnala classi CSS, `data-testid`, label UI o altri attributi visuali, correggere lo scanner e non
  trasformarli in protocol constants.

Docker infrastructure AS-IS:

- Il vecchio container `vault` e' stato creato dal compose storico
  `/home/mbc/Documenti/ws/java/hft/hft/docker/docker-compose.yml` e monta ancora bind path sotto
  `/home/mbc/Documenti/ws/java/hft/hft/docker/vault/*`.
- La sorgente Git storica completa del tree Docker HFT e' disponibile in
  `/home/mbc/.local/share/Trash/files/hft/docker` al commit `3b6eaae`; usare questa sorgente solo come riferimento
  AS-IS/migrazione, senza copiare segreti o dati runtime in nuovi repo.
- In ACDC il mirror di struttura directory e' sotto `/home/mbc/Documenti/ws/java/hft/acdc/docker`; il file non segreto
  `docker/vault/config/config.hcl` puo' essere copiato dalla sorgente storica per permettere a Vault di partire. Il
  mirror non implica invece che dati Vault, certificati o file VPN siano stati copiati. Se un container monta directory
  vuote, Vault puo' fallire con `stat /vault/config/config.hcl: no such file or directory`.
- Per diagnosi Vault leggere prima `docker inspect vault` e `docker logs vault`; se il container e' running ma sealed,
  usare `/home/mbc/Documenti/ws/java/hft/unseal.sh`. Se invece manca `config.hcl`, non e' un problema di unseal ma di
  bind mount/config assente.

ACDC:

```bash
cd /home/mbc/Documenti/ws/java/hft/acdc
./mvnw -DskipTests package
docker build -f docker/Dockerfile.jvm -t acdc:latest .
docker compose --env-file docker/vpn/.env -f docker/vpn/compose.yml up -d --build --force-recreate acdc
docker logs --tail 120 acdc-vpn
```

DocBrown:

```bash
cd /home/mbc/Documenti/ws/java/hft/docbrown
./mvnw -DskipTests package
docker compose --env-file /home/mbc/Documenti/ws/java/hft/acdc/docker/vpn/.env -f docker-compose.yml up -d --build --force-recreate docbrown
docker logs --tail 120 docbrown
```

Se `docbrown` container name conflict:

```bash
docker rm -f docbrown
docker compose --env-file /home/mbc/Documenti/ws/java/hft/acdc/docker/vpn/.env -f docker-compose.yml up -d docbrown
```

### 9. Preflight ML_READY Manuale Finche' L'Endpoint Non Esiste

Eseguire questi controlli prima di qualunque PAPER:

```bash
docker exec mysql_container sh -lc 'mysql -u${MYSQL_USER:-hft_user} -p"$MYSQL_PASSWORD" hft -e "
SELECT COUNT(*) open_paper
FROM acdc_run_execution
WHERE execution_mode=\"PAPER\" AND status=\"RUNNING\";

SELECT COUNT(*) open_positions
FROM acdc_paper_position
WHERE status=\"OPEN\";

SELECT COUNT(*) rules, COALESCE(SUM(status=\"PROMOTED\"),0) promoted
FROM acdc_reversal_ml_rule;

SELECT status, COUNT(*) c, MAX(advice_valid_until) max_until
FROM acdc_live_ml_advice
GROUP BY status;

SELECT COUNT(*) post_sell_forensics_rows
FROM acdc_paper_post_sell_forensics;
"'
```

Fail-closed se:

- PAPER running inattesa;
- posizioni aperte inattese;
- `rules=0` senza stato `NO_SIGNATURES` esplicito;
- DocBrown research `RUNNING`;
- log DocBrown contiene rollback/timeout recente;
- advice active sono residue o incoerenti con la run.
- `GET /diagnostics/acdc/ml-readiness` restituisce `ready=false`.
- non ci sono advice active `PAPER_ELIGIBLE`.
- le advice active non hanno `source_generation_id`.
- la generation della run PAPER non coincide con quella dell'advice.

### 10. Monitor Run PAPER

Usare solo dopo `ML_READY=true`.

```bash
OUT=/tmp/session-next-paper-run
mkdir -p "$OUT"
EXEC={executionId}

while true; do
  TS=$(date -Iseconds)
  docker exec mysql_container sh -lc 'mysql -N -u${MYSQL_USER:-hft_user} -p"$MYSQL_PASSWORD" hft -e "
  SELECT CONCAT(\"run_status=\",status,\" current=\",current_budget,\" reserved=\",reserved_budget,\" realized=\",realized_profit_quote)
  FROM acdc_run_execution WHERE id='"$EXEC"';
  SELECT CONCAT(\"positions total=\",COUNT(*),\" open=\",COALESCE(SUM(status=\"OPEN\"),0),\" closed=\",COALESCE(SUM(status=\"CLOSED\"),0),\" net=\",COALESCE(SUM(net_profit_quote),0))
  FROM acdc_paper_position WHERE execution_id='"$EXEC"';
  SELECT CONCAT(\"forensics total=\",COUNT(*),\" pending=\",COALESCE(SUM(status=\"PENDING_LOCAL\"),0),\" completed=\",COALESCE(SUM(status=\"COMPLETED\"),0))
  FROM acdc_paper_post_sell_forensics WHERE execution_id='"$EXEC"';
  "' 2>/dev/null | sed "s/^/[$TS] /" | tee -a "$OUT/monitor.log"
  sleep 60
done
```

### 11. Raccolta Finale Post-Run

```bash
EXEC={executionId}
OUT=/tmp/session-next-paper-run

curl -sS "http://localhost:8091/diagnostics/acdc/paper/scoring?executionIds=$EXEC" > "$OUT/scoring.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/timeline?executionIds=$EXEC" > "$OUT/timeline.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/sell-capture?executionIds=$EXEC" > "$OUT/sell_capture.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/post-sell-forensics?executionIds=$EXEC" > "$OUT/post_sell_forensics.json"
curl -sS "http://localhost:8091/diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=$EXEC&horizonSeconds=900" > "$OUT/counterfactual.json"
```

Sintesi rapida:

```bash
jq '{totalTrades,totalWins,totalLosses,totalNetProfitQuote,lossCapExits,timeoutExits}' "$OUT/scoring.json"
jq '{rows,missedReversals,noReversalConfirmed,inconclusiveGranularity,inconclusiveNoTicks}' "$OUT/post_sell_forensics.json"
jq '{totalBlockedAdvice,goodBlocks,badBlocks,ambiguousBlocks}' "$OUT/counterfactual.json"
```

## Interpretazione Minima Del Consiglio

- `FAILED_PREREQ`: ML readiness non passa, mining rollback/timeout, scheduler contaminato, advice residue.
- `INCONCLUSIVE`: nessun trade, dati granulari assenti, infrastruttura parziale.
- `FAIL_BASELINE`: run pulita, trade sufficienti, PnL negativo o zero-MFE/loss-cap dominanti.
- `PASS_BASELINE`: run pulita, PnL positivo, flusso advice->BUY->SELL completo, capture accettabile, forensics non smentisce SELL, counterfactual non mostra opportunity bloccate sistematicamente.
