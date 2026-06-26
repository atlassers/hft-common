# REM Super Plan Status Checklist

Data: 2026-06-19.

Obiettivo: stato consolidato DONE/WIP/TODO del superpiano REM, distinguendo infrastruttura implementata, verifica scientifica parziale e lavoro ancora necessario prima di promuovere conclusioni strategiche.

## Sintesi

- Stato complessivo: `WIP_FAILING_VALIDATION`.
- Infrastruttura scientifica/ingegneristica: `DONE`.
- Acceptance tecnica endpoint: `DONE`.
- Validazione strategica profittevole fuori campione: `TODO`.
- Blocco principale: la RUN nuova con diagnostics SELL V63 e forensics non inconclusiva e' stata eseguita, ma ha fallito scientificamente: modello sotto baseline e cause miste `BAD_ADVICE`/`BAD_SELL`/`BAD_TARGET`.

## DONE - Fondamenta Del Superpiano

- [x] Vincolo strategico aggiornato con superpiano in due fasi.
- [x] Precedenza del superpiano chiarita rispetto a scorciatoie storiche.
- [x] Regola permanente rispettata: modifiche codice buildate, pushate e rilasciate su Docker.
- [x] Flusso operativo endpoint-driven disponibile.
- [x] Nessuna REAL RUN avviata senza autorizzazione.

## DONE - Fase 1 Scientifica

- [x] Preflight scientifico via endpoint.
- [x] Pre-registrazione endpoint di:
  - ipotesi causale;
  - metrica primaria;
  - metrica secondaria;
  - `DIAGNOSIS_SET`;
  - `SELECTION_SET`;
  - `HOLDOUT_SET`;
  - baseline;
  - criterio di accettazione;
  - criterio di rigetto.
- [x] Separazione temporale delle finestre validata dal preflight.
- [x] Report controfattuale multi-finestra.
- [x] Report dichiara finestre, orizzonte, righe valutate e dati mancanti.
- [x] Baseline automatiche implementate:
  - `NO_TRADE`;
  - `RANDOM_TOP_VOLUME`;
  - `NAIVE_REVERSAL`.
- [x] Confronto baseline vs modello corrente nel report multi-finestra.
- [x] Cost model dichiarato nel preflight.
- [x] Execution model dichiarato nel preflight.
- [x] Incertezza multi-finestra calcolata e richiesta dall'acceptance.
- [x] Metriche robuste aggiunte al report controfattuale:
  - mediana;
  - percentile 10/25/75/90;
  - drawdown massimo cumulato su end-return;
  - Wilson interval 95% sul win rate.
- [x] Forensics con classificazione causale disponibile:
  - `BAD_ADVICE`;
  - `LATE_BUY`;
  - `BAD_SELL`;
  - `BAD_TARGET`;
  - `BAD_EXECUTION`;
  - `GOOD_FLOW`;
  - `INCONCLUSIVE_DATA`.

## WIP - Fase 1 Scientifica

- [ ] `CURRENT_MODEL` e' dichiarato nel preflight, ma non e' ancora una baseline automatica separata nel multi-window come curva storica indipendente.
  - Stato attuale: il modello valutato rappresenta il current model.
  - Rischio: confronto con precedente versione/modello non ancora esplicito.
- [ ] Baseline `SCORE_ONLY` non ancora automatica.
  - Necessaria se testiamo se ranking ML aggiunge valore oltre lo score grezzo.
- [ ] Baseline `SYMBOL_ONLY` e `GLOBAL_ALLOWED` non ancora automatica.
  - Necessaria se testiamo lo scope rule.
- [ ] Baseline `FRESH_ONLY` non ancora automatica.
  - Necessaria se testiamo il freshness contract.
- [ ] Metriche statistiche complete ancora mancanti nel report controfattuale:
  - target-hit rate;
  - timeout rate;
  - loss-cap rate;
  - capture ratio medio/mediano.
- [x] Il confidence interval e' presente per expectancy media e win rate.
- [ ] Rischio multiple comparisons non ancora quantificato automaticamente.
- [ ] Distinzione `theoretical_opportunity`, `executable_opportunity`, `captured_opportunity` presente come modello concettuale e diagnostica parziale, ma non ancora consolidata in un singolo report finale post-RUN.

## DONE - Fase 2 Ingegneristica

- [x] DocBrown scoring live via endpoint.
- [x] Advice top-N pubblicate su DB.
- [x] Advice salvano target realistico, loss cap, entry reference, scoring time e validita'.
- [x] ACDC freshness contract implementato:
  - advice non usata;
  - advice temporalmente valida;
  - age sotto soglia;
  - drift prezzo BUY entro soglia;
  - nessun gate statico extra aggiunto per reazione a RUN.
- [x] Diagnostica live advice disponibile via endpoint ACDC.
- [x] SELL capture diagnostics persistita in ACDC.
- [x] SELL diagnostics V63 include:
  - max net reached;
  - max net reached at seconds;
  - dynamic loss cap;
  - trailing retention;
  - trailing fallout;
  - target hit;
  - reason finale;
  - capture ratio;
  - time BUY -> MFE;
  - time MFE -> SELL.
- [x] Acceptance endpoint unico DocBrown.
- [x] `strategicStatus` aggiunto all'acceptance:
  - `PASS_TECHNICAL`;
  - `PASS_STRATEGIC`;
  - `FAIL`;
  - `INCONCLUSIVE`.
- [x] Checklist scientifica e ingegneristica verificate automaticamente dall'acceptance.
- [x] Docker deploy completato per DocBrown e ACDC.
- [x] Commit e push completati.

## WIP - Fase 2 Ingegneristica

- [x] Prova conclusiva con diagnostics SELL V63 eseguita su RUN nuova `82`.
- [ ] La classificazione forense su execution storiche puo' restare `INCONCLUSIVE_DATA` per retention microbar.
- [x] Report post-RUN eseguito subito dopo RUN `82`, prima del decadimento microbar.
- [ ] Congelamento config/model version della RUN e' dichiarato nel preflight, ma non ancora persistito come snapshot immutabile dedicato.

## DONE - Verifica Finale Tecnica

- [x] Scoring live endpoint verificato su `REM_CURRENT`.
- [x] Multi-window recente valutabile eseguito:
  - finestre: 2;
  - advice valutate: 10;
  - missing ticks: 0 sulle finestre recenti.
- [x] Acceptance endpoint finale eseguito.
- [x] Esito endpoint: `PASS`.
- [x] Failed requirements: nessuno.
- [x] Baseline battute sul report finale:
  - `NO_TRADE = 0`;
  - `RANDOM_TOP_VOLUME = -0.004036198422161987`;
  - `NAIVE_REVERSAL = -0.005023060744421715`;
  - modello `weightedAvgEnd15mNetReturn = 0.000045991451420329`.
- [x] Incertezza riportata:
  - lower 95% `-0.012766009412718455`;
  - upper 95% `0.012857992315559112`.

## WIP - Interpretazione Della Verifica Finale

- [x] Il precedente PASS era tecnico, non strategico.
- [x] RUN `82` ha prodotto forensics non inconclusiva.
- [x] RUN `82` ha fallito acceptance:
  - `status=FAIL`;
  - `strategicStatus=FAIL`;
  - `weightedAvgEnd15mNetReturn=-0.00221575308935471`;
  - `weightedZeroMfeRate=0.50`;
  - `NO_TRADE`, `RANDOM_TOP_VOLUME`, `NAIVE_REVERSAL` battono il modello;
  - uncertainty multi-window `INCONCLUSIVE` per una sola finestra valutabile.
- [x] Cause forensi RUN `82`:
  - 3 trade;
  - 1 `BAD_ADVICE`;
  - 1 `BAD_SELL`;
  - 1 `BAD_TARGET`;
  - 0 `INCONCLUSIVE_DATA`.
- [x] La verifica dimostra che la pipeline funziona e che il modello corrente non e' promuovibile.

## TODO - Prossimo Step Obbligatorio

- [x] Avviare una RUN nuova `PAPER` o `VALIDATION` non REAL, con configurazione congelata a livello preflight.
- [ ] Prima della RUN:
  - [x] scoring live via endpoint;
  - [x] advice top-N generate;
  - [x] freshness contract controllato via runtime;
  - [x] target realistico controllato via runtime;
  - [x] baseline recente calcolata;
  - [x] run type dichiarato.
- [ ] Durante/subito dopo la RUN:
  - [x] acquisire executionId `82`;
  - [x] chiamare diagnostics SELL ACDC;
  - [x] chiamare forensics DocBrown prima che i microbar decadano;
  - [x] chiamare acceptance con executionId della RUN nuova.
- [ ] Dopo la RUN:
  - [x] classificare ogni trade;
  - [x] separare cause: `BAD_ADVICE`, `BAD_SELL`, `BAD_TARGET`;
  - [x] confrontare risultato con baseline;
  - [ ] aggiornare charter/context con conclusione supportata: modello corrente non promuovibile, prossima ipotesi deve spiegare cause miste.

## TODO - Miglioramenti Scientifici Non Bloccanti Ma Necessari

- [ ] Implementare baseline automatiche aggiuntive:
  - `SCORE_ONLY`;
  - `SYMBOL_ONLY`;
  - `GLOBAL_ALLOWED`;
  - `FRESH_ONLY`.
- [ ] Estendere multi-window con metriche statistiche complete:
  - mediana;
  - percentile 10/25/75/90;
  - drawdown massimo;
  - target-hit rate;
  - timeout rate;
  - loss-cap rate;
  - capture ratio medio/mediano;
  - confidence/wilson interval sul win rate.
- [x] Aggiungere campo esplicito `strategicStatus` nell'acceptance.
- [x] Rendere l'acceptance piu' severa: forensics tutta `INCONCLUSIVE_DATA` produce `INCONCLUSIVE`, non `PASS_STRATEGIC`.

## Decisione Attuale

Non promuovere il modello corrente.

La situazione corretta e':

- `DONE`: infrastruttura superpiano, endpoint, baseline minime, robust metrics, strategic status, Docker, commit/push, RUN `82`, forensics post-RUN.
- `WIP`: costruzione della prossima ipotesi scientifica a partire da cause miste.
- `TODO`: validare una modifica candidata su almeno due finestre valutabili e con baseline complete prima di qualunque promozione.
