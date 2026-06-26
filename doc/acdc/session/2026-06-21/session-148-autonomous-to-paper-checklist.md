# Session 148 - Autonomous Path To PAPER Checklist

Data: 2026-06-21.

## Scopo

Definire il percorso operativo che puo' essere eseguito senza chiedere un go per ogni microtest, fino a una PAPER validativa `FORWARD_AB_98` o fino a una stop condition vincolante.

## Autorizzazione Operativa

Sono autorizzati senza ulteriore conferma utente:

- preflight runtime e git;
- check MySQL/container;
- endpoint diagnostici ACDC;
- endpoint diagnostici DocBrown;
- scoring live leggero DocBrown;
- rolling validation DocBrown;
- rolling paper promotion solo se il verdict scientifico e' promuovibile;
- retry brevi di `ML_READY`;
- avvio di SHADOW/PAPER validativa solo se `ML_READY=true` e protocollo `FORWARD_AB_98` completo;
- stop-buy, drain e stop ordinato della run PAPER;
- raccolta forensics post-run;
- aggiornamento checklist/handoff;
- commit/push con naming `MS<n>: <message>`.

## Budget Di Orchestrazione E Cadenza

Questo non sostituisce e non modifica il ciclo di vita SHADOW/PAPER.

SHADOW/PAPER restano gestiti dal runtime ACDC: start endpoint-driven, BUY/SELL DB-driven, stop-buy, drain, stop
ordinato, max-hold, trailing, loss-cap e forensics secondo handoff.

Il budget qui sotto riguarda solo l'orchestratore esterno della checklist autonoma: quanto aspettare tra tentativi
DocBrown/ACDC e quando smettere di ripetere microtest sulla stessa evidenza.

- ogni ciclo orchestratore ha budget massimo `25m`;
- un endpoint singolo non deve restare appeso oltre il timeout gia' dichiarato nell'handoff;
- dopo `FAIL_SELECTION_BIAS`, attendere `10m` prima del ciclo successivo;
- dopo errore tecnico transitorio, attendere `2m` e riprovare una volta;
- dopo due errori tecnici consecutivi sullo stesso endpoint, fermarsi e documentare `TECHNICAL_BLOCKED`;
- non eseguire piu' di `3` rolling validation consecutive sulla stessa ora di mercato senza almeno `30m` di nuova finestra realtime;
- se compare `ML_READY=true`, interrompere il ciclo di attesa e passare subito a Forward A/B;
- se dopo `6` cicli autonomi consecutivi non compare alcuna candidate promuovibile, classificare `NO_PROMOTABLE_SIGNAL_WINDOW` e chiedere valutazione del Consiglio prima di continuare.

## Stop Condition Obbligatorie

Fermarsi e chiedere conferma utente solo se serve:

- modificare `STRATEGIC_REM_RECOVERY_PLAN.md`;
- avviare REAL;
- cambiare famiglia strategica REM outcome-first;
- modificare BUY/SELL/ranking/trailing per far passare una run;
- introdurre tuning manuale feature-by-feature;
- promuovere una candidate con `FAIL_SELECTION_BIAS`;
- usare H2 come prova operativa;
- bypassare `ML_READY=false`;
- ignorare advice residue, scadute o source generation incoerente;
- procedere quando git contiene modifiche locali non correlate che impediscono un commit sicuro.

## Checklist Autonoma

### 1. Preflight

- [ ] Leggere `CURRENT_CONTEXT.md`, piano strategico e handoff.
- [ ] Verificare git status dei repo coinvolti.
- [ ] Determinare prossimo `MS<n>`.
- [ ] Verificare container base.
- [ ] Verificare DB: nessuna PAPER running, nessuna posizione `OPEN`.
- [ ] Verificare DocBrown research status.
- [ ] Verificare `ML_READY`.

### 2. Preparazione Segnali

- [ ] Se `ML_READY=false`, eseguire scoring live leggero DocBrown.
- [ ] Ricontrollare `ML_READY`.
- [ ] Se manca `PAPER_ELIGIBLE`, eseguire rolling validation su nuova finestra realtime.
- [ ] Se `strategicStatus=FAIL_SELECTION_BIAS`, non promuovere e ripetere su una nuova finestra.
- [ ] Applicare budget/cadenza orchestratore: wait `10m` dopo `FAIL_SELECTION_BIAS`, massimo `6` cicli autonomi consecutivi.
- [ ] Se il candidato e' promuovibile, eseguire rolling paper promotion.
- [ ] Ricontrollare `ML_READY` con retry brevi.

### 3. Forward A/B

- [ ] Se `ML_READY=true`, creare `forwardAbGroupId` unico.
- [ ] Avviare braccio A `A_BASELINE_98_CONTRACT` secondo handoff.
- [ ] Avviare braccio B `B_CURRENT_ROLLING_PIPELINE` secondo handoff.
- [ ] Se uno dei bracci manca o viene rifiutato, classificare `INCONCLUSIVE` o `TECHNICAL_ONLY`.
- [ ] Monitorare senza REAL.
- [ ] Applicare stop-buy e drain ordinato.
- [ ] Fermare la run quando budget riservato e posizioni aperte sono a zero.

### 4. Forensics

- [ ] Raccogliere diagnostica A/B.
- [ ] Raccogliere scoring.
- [ ] Raccogliere timeline.
- [ ] Raccogliere sell-capture.
- [ ] Raccogliere post-sell forensics.
- [ ] Raccogliere live revalidation counterfactual.
- [ ] Salvare output in artifact temporaneo e sintetizzare nel documento sessione.

### 5. Valutazione Del Consiglio

- [ ] `PASS_BASELINE` solo con A/B completo, non contaminato, forensics coerente e risultato economicamente difendibile.
- [ ] `FAIL_BASELINE` solo con A/B completo, non contaminato e difetto riproducibile.
- [ ] `INCONCLUSIVE` se manca evidenza sufficiente.
- [ ] `TECHNICAL_ONLY` se la run non soddisfa il protocollo scientifico.

### 6. Chiusura

- [ ] Aggiornare handoff se emergono nuovi check/procedure vincolanti.
- [ ] Aggiornare current context se cambia lo stato operativo.
- [ ] Build/deploy solo se ci sono modifiche codice.
- [ ] Commit/push dei documenti/codice coerenti con `MS<n>: <message>`.

## Stato Corrente

Stato iniziale della checklist:

`BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`.

Ultimo tentativo noto:

- scoring live: solo `PURE_REVERSAL_OBSERVED`;
- rolling validation: `FAIL_SELECTION_BIAS`;
- nessuna promotion;
- nessuna PAPER;
- nessuna `FORWARD_AB_98`.

## Evidenza Ciclo 1

Timestamp operativo: 2026-06-21 10:50 CEST.

Preflight:

- repo ACDC pulito;
- repo DocBrown pulito;
- prossimo commit previsto al preflight: `MS708`;
- container base up;
- PAPER running: `false`;
- open positions: `0`;
- DocBrown research run `2`: `COMPLETED`, 288 simboli, 17280 sample;
- `ML_READY=false`;
- blocker iniziali: `LIVE_ADVICE_ACTIVE_MISSING`, `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`.

Scoring live leggero:

- output: `/tmp/session148_docbrown_live_score_1.json`;
- rules `136`;
- snapshots `222`;
- matchedSymbols `5`;
- savedAdvice `5`;
- tutte le advice sono `PURE_REVERSAL_OBSERVED`;
- nessuna advice `PAPER_ELIGIBLE`.

Readiness dopo scoring:

- output: `/tmp/session148_ml_readiness_after_score_1.json`;
- `ML_READY=false`;
- blocker: `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- active advice `4`;
- observed active `4`;
- `PAPER_ELIGIBLE` contract-active `0`;
- latest advice source `LIVE_SCORE_ONLY`.

Rolling validation:

- output: `/tmp/session148_rolling_validation_1.json`;
- batch `session148-rolling-20260621T085027Z`;
- finestra `60m`, window `10m`, holdout `1`;
- persistedRows `5760`;
- selectionWindows `5`;
- holdoutRows `960`;
- strategicStatus `FAIL_SELECTION_BIAS`;
- selectedCandidate `symbol=BLURUSDC`;
- selectionAvgEndNetReturn `0.000263514112522687`;
- selectionWorstWindowAvgEndNetReturn `-0.001925407974119423`;
- selectionPositiveWindowRate `0.4`;
- holdoutAvgEndNetReturn `0.000749947671058535`;
- holdoutPositiveEndRate `0.875`;
- holdoutWinRateLower95 `0.5291051942301386`;
- reason: `almeno una finestra selection negativa`.

Decisione del Consiglio:

- Saggio ascoltatore: il holdout positivo non basta; il ciclo fail-closed sta funzionando.
- Scienziato severo: promotion vietata per `FAIL_SELECTION_BIAS`; promuovere sarebbe contaminazione.
- Mediano pragmatico: attendere nuova finestra realtime e ripetere scoring/rolling, senza avviare PAPER.

Stato dopo ciclo 1:

- output: `/tmp/session148_ml_readiness_final_1.json`;
- `ML_READY=false`;
- blocker: `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- active advice `2`;
- observed active `2`;
- `PAPER_ELIGIBLE` contract-active `0`;
- PAPER running `false`;
- open positions `0`;
- nessuna rolling paper promotion;
- nessuna PAPER;
- nessuna `FORWARD_AB_98`.

## Evidenza Ciclo 2

Timestamp operativo: 2026-06-21 11:02 CEST.

Motivo del ciclo:

- attesa una nuova porzione di finestra realtime prima di ripetere rolling validation;
- nessun go utente richiesto, come da checklist autonoma.

Preflight:

- repo ACDC pulito;
- prossimo commit previsto al preflight: `MS709`;
- `ML_READY=false`;
- blocker iniziali: `LIVE_ADVICE_ACTIVE_MISSING`, `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- PAPER running `false`;
- open positions `0`;
- DocBrown research run `2`: `COMPLETED`.

Scoring live leggero:

- output: `/tmp/session148_docbrown_live_score_2.json`;
- rules `136`;
- snapshots `222`;
- matchedSymbols `5`;
- savedAdvice `5`;
- tutte le advice sono `PURE_REVERSAL_OBSERVED`;
- nessuna advice `PAPER_ELIGIBLE`.

Readiness dopo scoring:

- output: `/tmp/session148_ml_readiness_after_score_2.json`;
- `ML_READY=false`;
- blocker: `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- active advice `4`;
- observed active `4`;
- `PAPER_ELIGIBLE` contract-active `0`;
- latest advice source `LIVE_SCORE_ONLY`.

Rolling validation:

- output: `/tmp/session148_rolling_validation_2.json`;
- batch `session148-rolling2-20260621T090251Z`;
- finestra `60m`, window `10m`, holdout `1`;
- persistedRows `5760`;
- selectionWindows `5`;
- holdoutRows `960`;
- strategicStatus `FAIL_SELECTION_BIAS`;
- selectedCandidate `symbol=1000CATUSDC`;
- selectionAvgEndNetReturn `-0.0028`;
- selectionWorstWindowAvgEndNetReturn `-0.0028`;
- selectionPositiveWindowRate `0`;
- holdoutAvgEndNetReturn `-0.0028`;
- holdoutPositiveEndRate `0`;
- holdoutWinRateLower95 `0`;
- reasons:
  - `expectancy selection non positiva`;
  - `almeno una finestra selection negativa`;
  - `expectancy holdout non positiva`;
  - `Wilson holdout non supera 50%`.

Decisione del Consiglio:

- Saggio ascoltatore: il secondo ciclo conferma che il blocco non e' infrastrutturale.
- Scienziato severo: promotion vietata; la candidate fallisce selection e holdout.
- Mediano pragmatico: continuare solo su nuova finestra realtime, senza modificare parametri trading o gating.

Stato dopo ciclo 2:

- output: `/tmp/session148_ml_readiness_final_2.json`;
- `ML_READY=false`;
- blocker: `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`;
- active advice `3`;
- observed active `3`;
- `PAPER_ELIGIBLE` contract-active `0`;
- PAPER running `false`;
- open positions `0`;
- nessuna rolling paper promotion;
- nessuna PAPER;
- nessuna `FORWARD_AB_98`.

## Diagnosi Segnali

Domanda: e' solo sfortuna o e' cambiato qualcosa?

Risposta del Consiglio:

- Non e' corretto dire che non intercettiamo piu' segnali: lo scoring live continua a intercettare reversal observed.
- Il segnale mancante e' `PAPER_ELIGIBLE` contract-active.
- Il mining pesante DocBrown corrente ha prodotto 136 regole `PROMOTED`, ma tutte con `promotion_class=PURE_REVERSAL_OBSERVED`.
- La configurazione DB corrente e' `rem.ml.promotion.mode=PURE_REVERSAL`, descritta come baseline ripristinata dalla run PAPER 53.
- Quindi lo scoring live leggero non e' oggi la fonte primaria di `PAPER_ELIGIBLE`; produce observed e puo' solo alimentare diagnostica/freshness.
- La fonte ammessa per `PAPER_ELIGIBLE` e' la rolling paper promotion, ma i cicli recenti hanno restituito `FAIL_SELECTION_BIAS`.
- Le ultime rolling window mostrano expectancy aggregata negativa:
  - `session148-rolling-20260621T085027Z`: avg end circa `-0.00341`, avg max circa `-0.00093`;
  - `session148-rolling2-20260621T090251Z`: avg end circa `-0.00323`, avg max circa `-0.00074`.

Conclusione:

- c'e' una componente di mercato/finestra sfavorevole;
- c'e' anche una scelta architetturale/configurativa: con `PURE_REVERSAL`, il passaggio a PAPER dipende dalla rolling promotion, non dallo scoring live pesante;
- non risulta evidenza sufficiente per chiamarla regressione applicativa, ma il comportamento va monitorato con timeout e cicli tracciati.
