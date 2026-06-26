# Session 96 - Round-Robin SLA PAPER Run Checklist

Data: 2026-06-20.

## Obiettivo

Eseguire una RUN PAPER esplorativa usando `ROUND_ROBIN_SLA` nel rolling ML e produrre una timeline per ogni trade:

- round-robin/rolling start;
- rolling end;
- promotion/advice;
- BUY;
- SELL;
- durata tra ogni step;
- forensics causale.

## Vincoli

- Endpoint-driven.
- Docker gia' deployato per DocBrown.
- Nessun filtro BUY nuovo.
- Nessun DROP definitivo di simboli.
- Audit ogni 4 cicli.
- Report finale obbligatorio.

## Checklist Livello 1

- [x] L1.1 Creare checklist.
- [x] L1.2 Verificare stato PAPER.
- [ ] L1.3 Avviare RUN PAPER endpoint-driven.
- [x] L1.4 Eseguire loop rolling `ROUND_ROBIN_SLA`.
- [ ] L1.5 Salvare timeline ciclo/advice/trade.
- [x] L1.6 Stop endpoint-driven.
- [x] L1.7 Raccogliere scoring/sell-capture/forensics.
- [x] L1.8 Costruire tabella tempi per trade.
- [x] L1.9 Report finale parziale run senza trade.
- [ ] L1.10 Commit/push docs.

## Checklist Livello 2

### L2.1 RUN

- [ ] Start PAPER.
- [x] Rolling cycle con `universeMode=ROUND_ROBIN_SLA`.
- [ ] Promotion dei soli symbol PASS.
- [x] Audit cycle ogni 4 cicli.
- [x] Stop-buy tentato; endpoint ha risposto 500 per assenza run attiva.
- [x] Stop.

### L2.2 Timeline

- [ ] Mappare batch/cycle -> rolling start/end.
- [ ] Mappare batch/cycle -> promotion time.
- [ ] Mappare advice -> batch/rule.
- [ ] Mappare trade -> adviceAt/buyAt/sellAt.
- [ ] Calcolare durate step-by-step.
- [x] Implementare endpoint diagnostico timeline PAPER.
- [x] Verificare endpoint timeline su execution storica.
- [x] Verificare run nuova con timeline artifact.

### L2.3 Verdict

- [ ] Tempo ciclo medio.
- [ ] Advice age medio.
- [ ] BAD_ADVICE / BAD_SELL / BAD_TARGET / GOOD_FLOW.
- [ ] Zero post-buy MFE.
- [ ] Net.
- [x] Decisione scientifica su run senza trade.

## Stato Realtime

- Stato corrente: `WIP_REFINEMENT_REQUIRED`.
- Ultimo aggiornamento: RUN PAPER valida `executionId=5` completata. 25 cicli rolling, 6 audit full scan, tutti `FAIL_SELECTION_BIAS`, 0 PASS, 0 promotion, 0 trade. Root cause scientifica: con `holdoutRows=2`, Wilson lower 95% non puo' superare 50% nemmeno con 2/2 win; la configurazione del test rende impossibile promuovere candidate.

## Esito Parziale Non Valido

- `paper/run`: HTTP 500, nessuna nuova esecuzione PAPER avviata.
- Rolling ML: 27 cicli, tutti `FAIL_SELECTION_BIAS`.
- Promote: 0.
- Trade: 0.
- Nota scientifica: questi dati misurano solo il comportamento del rolling scheduler, non una run PAPER end-to-end.

## Esito Run Valida 2026-06-20 11:56-12:58 UTC

- Execution: PAPER `5`, status `STOPPED`.
- Durata: 2026-06-20T11:56:54Z -> 2026-06-20T12:58:12Z.
- Cicli rolling: 25.
- Audit full scan: 6.
- Status rolling: 25/25 `FAIL_SELECTION_BIAS`.
- Persisted rows: 19 cicli a 1344 righe, 6 audit a 2304 righe.
- Durata rolling: min 21s, max 40s, media 26.60s.
- Durata cicli ridotti: media 23.32s.
- Durata audit: media 37.00s.
- PASS candidates: 0.
- Promotions: 0.
- Trade: 0.
- Net PAPER: 0.
- Timeline trade: endpoint disponibile, nessun trade nella nuova execution.

## Root Cause Scientifica

La run usava `perSymbolLimit=2` e `holdoutWindows=1`, quindi `holdoutRows=2`.
La regola di validazione richiede `holdoutWinRateLower95 > 0.50`.
Con Wilson 95%, anche il caso perfetto 2/2 ha lower bound `0.34237195288961925`.
La prima dimensione campionaria che puo' superare 0.50 con successi perfetti e' `n=4`.

Conclusione: questa RUN non dimostra assenza di opportunita' di mercato; dimostra che la configurazione statistica rende impossibile promuovere qualunque candidate.

## Refinement `perSymbolLimit=4`

- Primo tentativo manual tick: PAPER `6`, interrotto come non valido per BUY immediate perche' il tick ACDC era esterno al ciclo di promotion e arrivava troppo tardi rispetto a `maxBuyAgeSeconds=20`.
- Secondo tentativo autotick: PAPER `7`, valido per advice fresco -> ACDC tick immediato.

### RUN Autotick Valida

- Execution: PAPER `7`, status `COMPLETED`.
- Durata: 2026-06-20T13:08:11Z -> 2026-06-20T13:40:45Z.
- Cicli: 12.
- Audit full scan: 3.
- Cicli con candidate PASS: 5.
- Advice promossi: 6.
- ACDC accepted BUY: 1.
- Trade chiusi: 1.
- Net: `-0.16271154554398`.
- Win/Loss: 0/1.
- Rifiuti ACDC su advice promossi non comprati: 5/5 per `ml_advice_live_revalidation_pass=0`.

### Timeline Trade

Trade: `AIGENSYNUSDC`.

- Round-robin start: 2026-06-20T13:13:32Z.
- Round-robin end: 2026-06-20T13:14:02Z.
- Advice created/valid from: 2026-06-20T13:14:03Z.
- BUY decision/open: 2026-06-20T13:14:12Z.
- SELL decision/close: 2026-06-20T13:15:56Z.
- Round-robin -> advice: 31s.
- Advice -> BUY: 9s.
- BUY -> SELL: 104s.
- Advice -> SELL: 113s.
- Net return: `-0.006508461827754795`.
- MFE: `0`.
- SELL reason: `EXIT_ML_ADVICE_LOSS_CAP`.

### Verdict Refinement

- `perSymbolLimit=4` corregge l'impossibilita' matematica Wilson e consente promotion.
- Autotick corregge la latenza advice -> BUY e consente almeno una BUY con age 8s.
- Il problema successivo e' scientifico/contrattuale: il live revalidation gate blocca la maggioranza degli advice promossi e l'unico advice accettato e' BAD_ADVICE con zero MFE.
- Non modificare BUY/SELL/trailing.
- Prossimo lavoro: validare scientificamente se `live_revalidation_ranges` e' calibrato come conferma minima del reversal o se sta usando feature troppo volatili/non causali. Non abbassare la guardia senza counterfactual sui promoted bloccati.
