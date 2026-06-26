# Session 99 - Live Revalidation Slope Delta Result

Data: 2026-06-20.

## Ipotesi

Rimuovere `reversal_slope_delta` dal live hard gate poteva recuperare opportunity bloccate senza aprire perdite.

Controfattuale pre-run su execution `4,6,7,9`:

- would pass: 1;
- GOOD_BLOCK: 0;
- BAD_BLOCK: 1;
- AMBIGUOUS: 0.

## Implementazione

DocBrown:

- rimosso temporaneamente `reversal_slope_delta` da `LIVE_REVALIDATION_FEATURES`;
- BUY, SELL, trailing, mining, ranking e round-robin invariati.

Commit temporaneo:

- `c7c4eaf` - remove slope delta from live revalidation ranges.

Deploy:

- DocBrown Docker redeployato.
- ACDC gia' allineato a 288 simboli.

## PAPER

Execution: `10`.

Artifact:

`/tmp/session99-slope-delta-refinement-run`

Run interrotta anticipatamente dopo evidenza sufficiente di peggioramento.

## Risultato

- Trade: 6.
- Wins: 3.
- Losses: 3.
- Net profit quote: `-0.04795612367618`.
- Avg net return: `-0.000319707491498765`.
- Loss-cap exits: 2.
- Timeout exits: 0.
- Target hits: 1.
- Trailing armed: 4.
- Avg capture ratio: `0.2745181022202529`.

Trade:

| Symbol | Net return | Max net return | Exit |
|---|---:|---:|---|
| JUPUSDC | 0.00783267716535433 | 0.014715551181102363 | EXIT_ML_ADVICE_DYNAMIC_TRAILING |
| YGGUSDC | 0.001695893451720311 | 0.002435072142064373 | EXIT_ML_ADVICE_DYNAMIC_TRAILING |
| SAGAUSDC | -0.004964391691394659 | 0 | EXIT_ML_ADVICE_LOSS_CAP |
| WALUSDC | -0.007660056657223796 | 0 | EXIT_ML_ADVICE_LOSS_CAP |
| METUSDC | -0.000019167217448777 | 0.001961665565102445 | EXIT_ML_ADVICE_DYNAMIC_TRAILING |
| RAYUSDC | 0.0011968 | 0.0027952 | EXIT_ML_ADVICE_DYNAMIC_TRAILING |

## Counterfactual Post-Run

- Blocked advice: 23.
- Blocked decisions: 185.
- Replayed: 23.
- GOOD_BLOCK: 10.
- BAD_BLOCK: 6.
- AMBIGUOUS_BLOCK: 7.

Feature failures:

- `reversal_quality`: 18 fail, 4 BAD_BLOCK, 8 GOOD_BLOCK, 6 AMBIGUOUS.
- `reversal_trough_age_seconds`: 7 fail, 3 BAD_BLOCK, 2 GOOD_BLOCK, 2 AMBIGUOUS.
- `raw_volume`: 6 fail, 2 BAD_BLOCK, 1 GOOD_BLOCK, 3 AMBIGUOUS.
- `reversal_slope_short`: 11 fail, 1 BAD_BLOCK, 4 GOOD_BLOCK, 6 AMBIGUOUS.
- `reversal_distance_from_trough`: 11 fail, 2 BAD_BLOCK, 6 GOOD_BLOCK, 3 AMBIGUOUS.

## Verdict

`FAIL_REVERTED`.

La rimozione di `reversal_slope_delta` aumenta le BUY, ma introduce:

- loss-cap zero-MFE;
- capitale bloccato su posizioni senza target;
- PnL netto negativo;
- maggiore esposizione senza miglioramento robusto.

Rollback eseguito:

- `reversal_slope_delta` ripristinato in `LIVE_REVALIDATION_FEATURES`;
- commit DocBrown `910a678`;
- Docker DocBrown redeployato.

Stato attivo dopo rollback:

- `reversal_pre_trough_drop` fuori dal live hard gate;
- `reversal_slope_delta` dentro il live hard gate.
