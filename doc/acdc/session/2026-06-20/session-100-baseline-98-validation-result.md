# Session 100 - Baseline 98 Validation Result

Data: 2026-06-20.

## Obiettivo

Validare la baseline candidata sessione `98` senza cambiare codice.

Baseline:

- `reversal_pre_trough_drop` fuori dal live hard gate;
- `reversal_slope_delta` dentro il live hard gate;
- BUY, SELL, trailing, ranking e round-robin invariati.

## Run

Execution: `11`.

Artifact:

`/tmp/session100-baseline98-validation-run`

La run e' stata endpoint-driven. Stop-buy e drain sono stati completati; reserved finale `0`.

## Risultato

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

| Symbol | Classificazione | Net return | Max net return | Exit |
|---|---|---:|---:|---|
| VANRYUSDC | GOOD_FLOW_LOW_CAPTURE | 0.003700815217391304 | 0.009944565217391304 | EXIT_ML_ADVICE_DYNAMIC_TRAILING |
| BANKUSDC | ZERO_MFE_BAD_ADVICE | -0.002000000000000000 | 0 | EXIT_ML_ADVICE_TIMEOUT |
| SAPIENUSDC | GOOD_FLOW | 0.003848946135831382 | 0.005018735362997658 | EXIT_ML_ADVICE_DYNAMIC_TRAILING |

## Counterfactual Blocked Advice

- Blocked advice: 2.
- GOOD_BLOCK: 1.
- BAD_BLOCK: 1.
- AMBIGUOUS_BLOCK: 0.

Dettaglio:

- `AXSUSDC`: BAD_BLOCK, max net return `0.012453617021276595`.
- `XAIUSDC`: GOOD_BLOCK, max net return `-0.000806451612903226`.

## Confronto

Sessione `98`:

- 1 trade, 1 win.
- Net `+0.3230705383788`.
- Capture ratio `0.6091855097410218`.

Sessione `99`:

- 6 trade, 3 win, 3 loss.
- Net `-0.04795612367618`.
- Variante `slope_delta` rimossa rigettata.

Sessione `100`:

- Net positivo e nessun loss-cap.
- Due dynamic trailing vincenti.
- Un timeout zero-MFE.
- Capture ratio medio sotto sessione `98`.

## Verdict

`INCONCLUSIVE_POSITIVE_NOT_PASS`.

La baseline `98` resta la migliore candidata attuale, ma questa run non autorizza `PASS_BASELINE`:

- PnL positivo;
- flussi BUY->SELL completi;
- ma capture ratio degradato rispetto a sessione `98`;
- e presenza di `ZERO_MFE_BAD_ADVICE`.

Decisione:

- non aggiornare charter come successo operativo;
- non fare nuovi tuning feature-by-feature;
- ripetere una validazione comparabile della baseline `98`.
