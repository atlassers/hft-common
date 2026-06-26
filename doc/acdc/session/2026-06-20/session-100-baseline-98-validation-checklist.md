# Session 100 - Baseline 98 Validation Checklist

Data: 2026-06-20.

## Obiettivo

Validare la baseline candidata derivata dalla sessione `98`, senza introdurre nuovi tuning.

Baseline attiva:

- `reversal_pre_trough_drop` fuori dal live hard gate;
- `reversal_slope_delta` dentro il live hard gate;
- BUY, SELL, trailing, ranking e round-robin invariati.

## Decisione Del Consiglio

Il saggio ascoltatore chiede di consolidare cio' che ha funzionato: la sessione `98` ha prodotto un flusso completo profittevole e va trattata come baseline candidata.

Lo scienziato severo vieta ulteriori rimozioni feature-by-feature: la sessione `99` ha dimostrato che allentare ancora il gate apre zero-MFE e loss-cap.

Il mediano chiede una validazione operativa misurabile: PAPER endpoint-driven, metriche complete, confronto con sessioni `98` e `99`, verdict scritto.

## Criteri Di Accettazione

`PASS_BASELINE` solo se:

- PnL netto positivo;
- almeno un flusso advice -> BUY -> SELL completo;
- zero-MFE/loss-cap rate non domina i trade;
- capture ratio non degrada rispetto alla sessione `98`;
- counterfactual blocked advice non mostra che il gate stia bloccando prevalentemente opportunity;
- reserved budget finale `0`.

`FAIL_BASELINE` se:

- PnL netto negativo con trade sufficienti;
- zero-MFE/loss-cap domina;
- BUY fresche continuano a non produrre MFE;
- SELL cattura poco nonostante MFE disponibile.

`INCONCLUSIVE` se:

- nessun trade;
- dati insufficienti;
- run interrotta da problema infrastrutturale.

## Checklist Livello 1

- [x] L1.1 Verificare codice attivo DocBrown: `pre_trough_drop` fuori, `slope_delta` dentro.
- [x] L1.2 Verificare container DocBrown e ACDC.
- [x] L1.3 Verificare ACDC universo 288.
- [x] L1.4 Eseguire PAPER baseline endpoint-driven.
- [x] L1.5 Stop-buy e drain ordinati.
- [x] L1.6 Raccogliere endpoint timeline/scoring/sell-capture/counterfactual.
- [x] L1.7 Classificare ogni trade causalmente.
- [x] L1.8 Confrontare sessioni `98`, `99`, baseline corrente.
- [x] L1.9 Decidere `PASS_BASELINE`, `FAIL_BASELINE` o `INCONCLUSIVE`.
- [x] L1.10 Aggiornare charter solo se `PASS_BASELINE`.
- [x] L1.11 Commit/push documentazione finale.

## Checklist Livello 2

### L2.1 Run

- [x] Run avviata da endpoint/script endpoint-driven.
- [x] Rolling validation DocBrown da endpoint.
- [x] Promotion DocBrown da endpoint.
- [x] ACDC PAPER tick/scheduler attivo.
- [x] Nessun cambio codice durante la run.

### L2.2 Metriche

- [x] Trade count.
- [x] Win/loss count.
- [x] PnL netto.
- [x] Avg/median net return.
- [x] Max net return per trade.
- [x] Safe target per trade.
- [x] Zero-MFE rate.
- [x] Loss-cap rate.
- [x] Capture ratio medio e per trade.
- [x] Advice age e entry drift.
- [x] Timeline round-robin -> advice -> BUY -> SELL.

### L2.3 Forensics

- [x] `GOOD_FLOW`.
- [x] `ZERO_MFE_BAD_ADVICE`.
- [x] `LOW_MFE_BAD_TARGET`.
- [x] `BAD_SELL`.
- [x] `LATE_BUY`.
- [x] `INCONCLUSIVE_DATA`.
- [x] Blocked advice GOOD/BAD/AMBIGUOUS.

## Stato Realtime

- Stato corrente: `INCONCLUSIVE_POSITIVE_NOT_PASS`.
- Ultimo aggiornamento: execution `11` completata con reserved `0`, net `+0.13874403358414`, 3 trade, 2 win, 1 loss. Non e' PASS per capture medio degradato rispetto alla sessione `98` e per un timeout zero-MFE.

## Risultato Execution 11

Artifact:

`/tmp/session100-baseline98-validation-run`

Sintesi:

- Trade: 3.
- Wins: 2.
- Losses: 1.
- Net profit quote: `+0.13874403358414`.
- Avg net return: `0.001849920451074229`.
- Loss-cap exits: 0.
- Timeout exits: 1.
- Target hits: 2.
- Trailing armed: 2.
- Avg capture ratio: `0.37968667854345717`.
- Reserved finale: `0`.

Trade:

- `VANRYUSDC`: `GOOD_FLOW_LOW_CAPTURE`, net `+0.003700815217391304`, max `0.009944565217391304`, exit `EXIT_ML_ADVICE_DYNAMIC_TRAILING`, capture `0.3721444966663023`.
- `BANKUSDC`: `ZERO_MFE_BAD_ADVICE`, net `-0.002`, max `0`, exit `EXIT_ML_ADVICE_TIMEOUT`.
- `SAPIENUSDC`: `GOOD_FLOW`, net `+0.003848946135831382`, max `0.005018735362997658`, exit `EXIT_ML_ADVICE_DYNAMIC_TRAILING`, capture `0.7669155389640692`.

Counterfactual blocked advice:

- Blocked advice: 2.
- GOOD_BLOCK: 1 (`XAIUSDC`).
- BAD_BLOCK: 1 (`AXSUSDC`).
- AMBIGUOUS_BLOCK: 0.

Confronto:

- Sessione `98`: 1 trade, 1 win, net `+0.3230705383788`, capture `0.6091855097410218`.
- Sessione `99`: 6 trade, 3 win, 3 loss, net `-0.04795612367618`, variante rigettata.
- Sessione `100`: baseline `98` conferma PnL positivo e trailing funzionante, ma non raggiunge qualita' capture della sessione `98` e contiene un timeout zero-MFE.

Verdict:

`INCONCLUSIVE_POSITIVE_NOT_PASS`.

Decisione:

- Non aggiornare charter come `PASS_BASELINE`.
- Non fare tuning feature-by-feature.
- Baseline `98` resta candidata, ma richiede almeno un'altra validazione comparabile.
