# Session 106 - ML Readiness Before PAPER Checklist

Data: 2026-06-21.

## Mandato Del Consiglio

Costruire un percorso operativo che impedisca nuove PAPER non scientifiche. La baseline `98` resta candidata, ma non va piu' validata finche' la pipeline ML non e' pronta, osservabile e fail-closed.

## Razionale Dei Tre Saggi

### Saggio Ascoltatore

La sessione `17` va ascoltata ma non usata come verdetto sulla baseline:

- ha comprato advice rolling residue mentre `acdc_reversal_ml_rule=0`;
- il mining DocBrown era in timeout/rollback;
- i due trade zero-MFE sono un sintomo di orchestrazione non governata;
- il post-SELL forensics ha funzionato come persistenza, ma il dato era `INCONCLUSIVE_GRANULARITY`.

Decisione richiesta:

- prima si rende ripetibile il ciclo ML;
- poi si ripete la PAPER.

### Scienziato Severo

Nessuna PAPER deve partire se `ML_READY=false`.

Condizioni bloccanti:

- `acdc_reversal_ml_rule=0` senza stato esplicito `NO_SIGNATURES`;
- DocBrown research/mining `RUNNING`;
- DocBrown research/mining terminato in errore, rollback o timeout;
- advice con `sourceGenerationId` di sessioni precedenti non dichiarate valide;
- scheduler PAPER attivo fuori dalla finestra controllata;
- endpoint DocBrown/ACDC non raggiungibili o path non verificato;
- forensics non persistente o tabella non migrata.

Decisione richiesta:

- fail-closed prima di ogni BUY;
- fail-closed prima di ogni start PAPER.

### Mediano

Separare tre fasi:

1. preparazione ML pesante;
2. scoring live leggero;
3. PAPER runtime.

La finestra da `60m` deve misurare solo la fase 3. Se la fase 1 richiede 30 minuti o va in rollback, quello e' un test infrastrutturale fallito, non una run trading.

Decisione richiesta:

- batch/checkpoint nel mining;
- readiness report unico;
- run PAPER solo dopo preflight verde.

## Piano Livello 1

- [ ] L1.1 Congelare lo stato attuale e documentare il failure `SESSION_105`.
- [x] L1.2 Implementare diagnostica `ML_READY` endpoint-driven.
- [x] L1.3 Rendere DocBrown heavy mining osservabile.
- [x] L1.4 Spezzare il mining pesante in checkpoint/batch persistenti.
- [x] L1.5 Bloccare ACDC PAPER start quando `ML_READY=false`.
- [x] L1.6 Bloccare BUY PAPER quando advice source non e' coerente con la run validativa.
- [ ] L1.7 Disabilitare o governare lo scheduler PAPER durante finestre controllate.
- [ ] L1.8 Eseguire preflight ML completo.
- [ ] L1.9 Solo se preflight passa, lanciare PAPER pulita `60m`.
- [ ] L1.10 Stop-buy/drain e forensics post-run.
- [ ] L1.11 Verdict del Consiglio: `PASS_BASELINE`, `FAIL_BASELINE`, `INCONCLUSIVE`, o `FAILED_PREREQ`.

## Checklist Livello 2

### L2.1 Freeze E Diagnosi

- [ ] Registrare `acdc_reversal_ml_rule=0`.
- [ ] Registrare DocBrown timeout/rollback da log.
- [ ] Registrare execution `17` come non pulita.
- [ ] Salvare artifact sessione `105`.
- [ ] Confermare nessuna posizione aperta.
- [ ] Confermare REAL bloccata.

### L2.2 Endpoint ML_READY

- [x] Endpoint ACDC diagnostics `GET /diagnostics/acdc/ml-readiness`.
- [x] Stato sintetico `ready=true/false`.
- [x] Motivi bloccanti strutturati.
- [x] Conteggio regole totali/promosse.
- [x] Stato advice live: active/expired/used/residue.
- [x] Stato advice live dentro contratto operativo `max_buy_age_seconds`.
- [x] Ultimo DocBrown research status.
- [x] Ultimo scoring live status.
- [x] Stato scheduler PAPER.
- [x] Stato execution PAPER corrente.
- [x] Verifica post-SELL forensics table/worker.

### L2.3 DocBrown Mining Osservabile

- [x] Tabella o stato persistente `docbrown_rem_research_run`.
- [x] Campi: run id, profile, status, startedAt, completedAt, failedAt.
- [x] Campi: phase corrente (`OUTCOME_MINING`, `BAND_DISCOVERY`, `RULE_MINING`, `SIGNATURE_PROMOTION`).
- [x] Campi: elapsed seconds.
- [x] Campi: symbols planned/processed.
- [x] Campi: samples planned/processed.
- [x] Campi: last checkpoint.
- [x] Campi: error class/message.
- [x] Endpoint status DocBrown.
- [ ] Log progresso periodico.

### L2.4 Mining A Checkpoint

- [x] Rimuovere transazione unica lunga dal mining completo.
- [x] Persistenza per simbolo o micro-batch.
- [x] Commit periodico dei training sample.
- [ ] `rejectAll` solo in fase finale atomica o con generazione/versione.
- [ ] Nessuna cancellazione di regole valide finche' la nuova generazione non e' completa.
- [ ] Promozione regole per `generationId`.
- [ ] Rollback parziale non deve lasciare modello vuoto.
- [ ] Timeout per batch, non per intero ciclo.

### L2.5 Advice Source Governance

- [x] Ogni advice deve avere `sourceGenerationId`.
- [x] Ogni run PAPER deve dichiarare `expectedSourceGenerationId`.
- [x] ACDC rifiuta advice non coerenti con la run validativa.
- [x] ACDC distingue `PROMOTED_RULE`, `ROLLING_PAPER`, `LIVE_SCORE_ONLY`, `RESIDUE`.
- [x] `ML_READY` considera solo `PAPER_ELIGIBLE` ancora entro `max_buy_age_seconds` per dichiarare readiness e legare la generation.
- [ ] Advice `used_at` non deve restare `ACTIVE` semanticamente riusabile.
- [ ] Advice scadute o usate non alimentano nuove PAPER pulite.

### L2.6 Scheduler Governance

- [ ] Prima di run controllata: verificare nessuna PAPER `RUNNING`.
- [ ] Stop/disable scheduler automatico se richiesto dalla finestra controllata.
- [ ] Avvio PAPER solo da endpoint/script controllato.
- [x] Nessuna nuova execution mentre `ML_READY=false`.
- [ ] Dopo stop-buy, drain fino a reserved `0`.
- [ ] Dopo drain, execution terminale e scheduler coerente.

### L2.7 Preflight Scientifico

- [ ] DocBrown endpoint path verificato (`/docbrown/...`).
- [ ] DocBrown research completato senza rollback.
- [ ] Regole promosse > 0 oppure `NO_SIGNATURES` dichiarato.
- [ ] Scoring live salva advice fresche.
- [ ] Advice active coerenti con generation attesa.
- [ ] ACDC vede le advice.
- [ ] ACDC readiness `ready=true`.
- [ ] Post-SELL forensics worker attivo.
- [ ] Influx microbar/realtime disponibile.
- [ ] MySQL migration applicate.

### L2.8 PAPER 60m Pulita

- [ ] Creare artifact directory.
- [ ] Avviare execution da endpoint.
- [ ] Registrare execution id.
- [ ] Monitorare budget/current/reserved.
- [ ] Monitorare BUY/SELL.
- [ ] Monitorare post-SELL forensics pending/completed.
- [ ] Nessun tuning durante finestra.
- [ ] Stop-buy a `60m`.
- [ ] Drain fino a reserved `0`.
- [ ] Stop terminale.

### L2.9 Analisi Finale

- [ ] Scoring.
- [ ] Timeline.
- [ ] Sell-capture.
- [ ] Post-SELL forensics.
- [ ] Live revalidation counterfactual.
- [ ] PnL netto.
- [ ] Win/loss.
- [ ] Zero-MFE rate.
- [ ] Loss-cap rate.
- [ ] Capture ratio.
- [ ] Advice age.
- [ ] Entry drift.
- [ ] Verdict `PASS_BASELINE`/`FAIL_BASELINE`/`INCONCLUSIVE`/`FAILED_PREREQ`.

## Stato Realtime

- Stato corrente: `ML_ORCHESTRATION_PARTIAL_PASS_NOT_PAPER_READY`.
- `2026-06-21T03:49+02:00`: implementato endpoint ACDC `GET /diagnostics/acdc/ml-readiness`, fail-closed su `POST /acdc/paper/run/{profileKey}` quando `ML_READY=false`, endpoint DocBrown `GET /docbrown/rem/research/{profileKey}/status`, tabella `docbrown_rem_research_run`.
- `2026-06-21T03:52+02:00`: prima run DocBrown osservabile fallita in fase finale firme per `Connection is closed`; failure persistito come run `1`.
- `2026-06-21T03:57+02:00`: seconda run DocBrown completata senza rollback: run `2`, 288 simboli, 17280 sample, 136 regole promosse.
- `2026-06-21T03:58+02:00`: scoring live leggero completato, 5 advice salvate, tutte `PURE_REVERSAL_OBSERVED`.
- `2026-06-21T03:59+02:00`: `ML_READY=false` per `LIVE_ADVICE_ACTIVE_MISSING` e `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`; start PAPER verificato `409`; nessuna nuova execution creata.
- `2026-06-21T04:20+02:00`: aggiunta governance advice con colonne `source_generation_id` e `advice_source`; DocBrown valorizza `LIVE_SCORE_ONLY` per scoring live e `ROLLING_PAPER` per rolling paper promotion; ACDC espone source/generation in diagnostics, lega una nuova PAPER a `expectedSourceGenerationId` e rifiuta BUY incoerenti con `PAPER_ADVICE_SOURCE_GENERATION_MISMATCH`.
- `2026-06-21T04:20+02:00`: scoring live leggero ha salvato 5 advice `PURE_REVERSAL_OBSERVED` con generation `live-1782008415`; `ML_READY=false` per `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`; start PAPER verificato `409`.
- `2026-06-21T04:26+02:00`: rolling validation batch `session110-rolling-paper-20260621T0225Z` ha persistito 1280 righe ma verdict globale `FAIL_SELECTION_BIAS`/`INCONCLUSIVE`; promozione per-symbol ha creato 15 advice `PAPER_ELIGIBLE` `ROLLING_PAPER`, utile come preflight tecnico ma non come verdict scientifico.
- `2026-06-21T04:28+02:00`: `ml-readiness` ha mostrato verde transitorio mentre le advice erano formalmente attive; `rem/readiness` e' rimasto `NOT_READY` per `ML_REPORT_MISSING`, `ML_NO_SIGNATURES`, `PARITY_NO_BUY_READY`, `LIFECYCLE_NO_ENTRY_ACCEPTED`, `LIFECYCLE_NO_PROFITABLE_CLOSES`; nessuna PAPER avviata.
- `2026-06-21T04:30+02:00`: corretta e ridistribuita `ml-readiness`: `PAPER_ELIGIBLE` con `max_buy_age_seconds` scaduto non conta piu' come readiness; verifica post-deploy `ML_READY=false`, `ONLY_EXPIRED_ADVICE_PRESENT`.
- Execution di riferimento negativa: `17`, non pulita.
- Blocco principale residuo: riallineare full REM readiness e entry lifecycle con il modello/promozioni DocBrown; la sola presenza temporanea di advice `PAPER_ELIGIBLE` non abilita PAPER se parity/lifecycle restano `NOT_READY`.
