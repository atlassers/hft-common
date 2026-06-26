# Session 102 - Baseline 98 45m Validation Checklist

Data: 2026-06-20.

## Obiettivo

Eseguire una nuova validazione comparabile della baseline `98` per almeno `45` minuti di ciclo attivo, come prossimo step dopo il forensics della sessione `101`.

Vincoli:

- baseline `98` invariata;
- nessun tuning;
- nessuna nuova soglia;
- nessuna rimozione feature-by-feature;
- nessuna modifica BUY, SELL, ranking, live gate o trailing;
- run PAPER endpoint-driven;
- test operativo su container e MySQL, non H2;
- raccolta obbligatoria di scoring, timeline, sell-capture e counterfactual;
- charter aggiornabile solo in caso di `PASS_BASELINE`.

Artifact previsti:

`/tmp/session102-baseline98-45m-validation-run`

## Checklist Livello 1

- [x] L1.1 Creare checklist sessione.
- [x] L1.2 Verificare container e assenza blocchi operativi.
- [x] L1.3 Preparare runner endpoint-driven sessione `102`.
- [x] L1.4 Avviare PAPER execution.
- [x] L1.5 Eseguire rolling/promotion/tick per almeno `45` minuti.
- [x] L1.6 Stop-buy.
- [x] L1.7 Drain fino a reserved finale `0` o timeout dichiarato.
- [x] L1.8 Raccogliere timeline/scoring/sell-capture/counterfactual.
- [x] L1.9 Forensics trade-by-trade.
- [x] L1.10 Decidere `PASS_BASELINE`, `FAIL_BASELINE` o `INCONCLUSIVE`.
- [x] L1.11 Aggiornare charter solo se `PASS_BASELINE`.
- [x] L1.12 Commit/push documentazione finale.

## Checklist Livello 2

### L2.1 Runtime

- [x] ACDC container `acdc-vpn` attivo.
- [x] DocBrown container `docbrown` attivo.
- [x] MySQL container attivo.
- [x] InfluxDB container attivo.
- [x] Universo ACDC a `288` simboli.
- [x] Endpoint ACDC raggiungibile.
- [x] Endpoint DocBrown raggiungibile.

### L2.2 Baseline

- [x] `reversal_pre_trough_drop` fuori dal live hard gate.
- [x] `reversal_slope_delta` dentro il live hard gate.
- [x] `ROUND_ROBIN_SLA` usato come ottimizzazione ML, non filtro trading.
- [x] `perSymbolLimit=4`.
- [x] `symbolLimit=288`.
- [x] `maxBuyAgeSeconds=20`.
- [x] `validitySeconds=120`.

### L2.3 Run Metrics

- [x] Durata ciclo attivo >= `2700s`.
- [x] Numero cicli registrato.
- [x] PASS candidate registrati.
- [x] Promotion registrate.
- [x] BUY registrate.
- [x] SELL registrate.
- [x] Stop-buy registrato.
- [x] Reserved finale registrato.
- [x] PnL netto registrato.

### L2.4 Diagnostics

- [x] Timeline round-robin -> advice -> BUY -> SELL.
- [x] Scoring execution.
- [x] Sell-capture.
- [x] Live revalidation counterfactual.
- [x] Zero-MFE rate.
- [x] Loss-cap rate.
- [x] Capture ratio medio e per trade.
- [x] Advice age e entry drift.

### L2.5 Forensics

- [x] Classificare ogni trade `GOOD_FLOW`, `GOOD_ADVICE_LOW_CAPTURE`, `ZERO_MFE_BAD_ADVICE`, `BAD_SELL`, `LATE_BUY` o `INCONCLUSIVE_DATA`.
- [x] Distinguere problema advice da problema freshness.
- [x] Distinguere problema gate da problema SELL.
- [x] Confrontare con sessioni `98`, `100`, `101`.

## Stato Realtime

- Stato corrente: `FAIL_BASELINE`.
- Baseline: `98`.
- Durata richiesta: almeno `45` minuti.
- Prossima azione: commit/push documentazione finale.

## Risultato Execution 12

Artifact:

`/tmp/session102-baseline98-45m-validation-run`

Sintesi:

- Execution: `12`.
- Durata attiva: almeno `2700s`.
- Cicli: `16`.
- Promotion: `26`.
- BUY: `3`.
- Trade chiusi: `3`.
- Wins/losses: `0/3`.
- Net profit quote: `-0.213068181444`.
- Avg net return: `-0.002840909090909091`.
- Loss-cap exits: `1`.
- Timeout exits: `1`.
- Dynamic trailing exits: `1`.
- Target hits: `0`.
- Trailing armed: `1`.
- Avg capture ratio: `-0.1324009324009324`.
- Reserved finale: `0`.

Trade:

- `RAREUSDC`: `BAD_SELL_AFTER_MFE`, net `-0.002`, max `0.005035211267605634`, safe `0.000797122302158274`, exit `EXIT_ML_ADVICE_DYNAMIC_TRAILING`, trailing armed `true`, capture `-0.39720279720279716`, advice age `6s`, entry drift `0`.
- `MASKUSDC`: `ZERO_MFE_BAD_ADVICE_LOSS_CAP`, net `-0.004522727272727273`, max `0`, safe `0.001016793893129771`, exit `EXIT_ML_ADVICE_LOSS_CAP`, advice age `7s`, entry drift `0`.
- `KITEUSDC`: `ZERO_MFE_BAD_ADVICE_TIMEOUT`, net `-0.002`, max `0`, safe `0.0005`, exit `EXIT_ML_ADVICE_TIMEOUT`, advice age `7s`, entry drift `0`.

Counterfactual blocked advice:

- Blocked advice: `22`.
- GOOD_BLOCK: `10`.
- BAD_BLOCK: `9`.
- AMBIGUOUS_BLOCK: `3`.

Verdict:

`FAIL_BASELINE`.

Motivo:

- PnL netto negativo;
- `0/3` win;
- due trade su tre sono zero-MFE con advice fresca e drift zero;
- `RAREUSDC` aveva MFE superiore al safe target ma la SELL dinamica ha catturato una perdita;
- counterfactual dei blocchi non autorizza rimozione feature-by-feature, perche' GOOD/BAD block sono quasi bilanciati.

Decisione:

- Non aggiornare il charter come successo.
- Non rimuovere feature dal live gate sulla base di questa run.
- Prossimo step scientifico: separare esplicitamente due problemi prima di nuovi tuning:
  1. falsi positivi post-BUY con freshness ok (`MASKUSDC`, `KITEUSDC`);
  2. cattura SELL negativa dopo MFE positivo (`RAREUSDC`).
