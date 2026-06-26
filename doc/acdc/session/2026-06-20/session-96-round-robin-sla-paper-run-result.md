# Session 96 - Round-Robin SLA PAPER Run Result

Data: 2026-06-20.

## Implementazione

- Aggiunto endpoint ACDC `GET /diagnostics/acdc/paper/timeline?executionIds=...`.
- Reso `POST /acdc/paper/stop-buy/{profileKey}` idempotente quando la PAPER e' gia' terminale.
- Aggiunto a DocBrown il passaggio opzionale di `roundRobinStartedAt` e `roundRobinCompletedAt` nella promotion rolling, persistiti in `advice_json`.
- Deploy Docker completato:
  - ACDC commit `d77f2b4`;
  - DocBrown commit `dde109b`.

## RUN

- Execution: PAPER `5`.
- Start: 2026-06-20T11:56:54Z.
- Stop: 2026-06-20T12:58:12Z.
- Artifact: `/tmp/session96-rr-sla-timeline-run`.
- Protocollo:
  - `ROUND_ROBIN_SLA`;
  - `symbolLimit=288`;
  - `hotSymbolLimit=96`;
  - `warmCadenceCycles=2`;
  - `coldCadenceCycles=4`;
  - audit ogni 4 cicli;
  - `perSymbolLimit=2`.

## Metriche

- Cicli rolling: 25.
- Audit full scan: 6.
- `FAIL_SELECTION_BIAS`: 25/25.
- Candidate PASS: 0.
- Promotion: 0.
- Trade PAPER: 0.
- Net PAPER: 0.
- Rolling duration:
  - min 21s;
  - max 40s;
  - media 26.60s;
  - cicli ridotti media 23.32s;
  - audit media 37.00s.

## Diagnosi

La run non ha misurato BUY->SELL su nuovi trade perche' nessun advice e' stato promosso.

La causa non e' il round-robin: 6 audit full scan hanno fallito nello stesso modo.
La causa non e' ACDC: la PAPER e' partita, e gli endpoint timeline/scoring funzionano.

La causa e' una configurazione statistica impossibile:

- `perSymbolLimit=2`;
- `holdoutWindows=1`;
- `holdoutRows=2`;
- criterio: Wilson holdout lower 95% deve superare 50%.

Con `n=2`, anche `2/2` successi produce lower bound `0.34237195288961925`.
Il primo `n` che puo' superare 50% con successi perfetti e' `n=4`.

## Verdict

La run e' valida come diagnosi negativa.
Non dimostra che il mercato non avesse opportunita'.
Dimostra che il test era statisticamente non promuovibile.

## Prossimo Step

Ripetere la PAPER con `perSymbolLimit=4`, senza cambiare BUY, SELL, trailing o guardie.
Questo rende possibile passare Wilson 95% senza abbassare il criterio scientifico.

## Refinement Eseguito - `perSymbolLimit=4` + Autotick

### Perche'

Con `perSymbolLimit=2`, Wilson 95% era matematicamente impossibile da superare.
Il refinement ha portato `perSymbolLimit=4`, mantenendo invariato il criterio Wilson.
Inoltre e' stato aggiunto tick ACDC immediato dopo promotion nello script operativo, per rispettare il requisito: advice fresco -> BUY subito.

### Esito

- Execution: PAPER `7`.
- Start: 2026-06-20T13:08:11Z.
- Stop: 2026-06-20T13:40:45Z.
- Cicli: 12.
- Advice promossi: 6.
- ACDC BUY accettate: 1.
- Trade chiusi: 1.
- Net: `-0.16271154554398`.
- Win/Loss: 0/1.

### Timeline Trade

`AIGENSYNUSDC`

- Round-robin start: 2026-06-20T13:13:32Z.
- Round-robin end: 2026-06-20T13:14:02Z.
- Advice created: 2026-06-20T13:14:03Z.
- BUY: 2026-06-20T13:14:12Z.
- SELL: 2026-06-20T13:15:56Z.
- Round-robin -> advice: 31s.
- Advice -> BUY: 9s.
- BUY -> SELL: 104s.
- Advice -> SELL: 113s.
- Net return: `-0.006508461827754795`.
- MFE: `0`.
- Exit: `EXIT_ML_ADVICE_LOSS_CAP`.

### Diagnosi Refinement

- Il campione holdout corretto permette davvero promotion.
- Il tick immediato risolve la latenza advice -> BUY.
- I rifiuti ACDC residui sugli advice promossi falliscono `ml_advice_live_revalidation_pass=0`.
- L'unico trade accettato e' un BAD_ADVICE: zero MFE, perdita immediata, uscita loss-cap.

### Verdict

Il collo di bottiglia non e' piu' Docker, endpoint, round-robin o freschezza del tick.
Il problema scientifico ora e':

1. calibrazione dei `live_revalidation_ranges`;
2. capacita' del modello promosso di evitare zero-MFE/BAD_ADVICE;
3. verifica counterfactual degli advice promossi ma bloccati dal live gate.

Non aggiornare il charter come successo operativo: il refinement ha prodotto diagnosi, non modello profittevole.
