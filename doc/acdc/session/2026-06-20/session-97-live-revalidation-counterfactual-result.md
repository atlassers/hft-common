# Session 97 - Live Revalidation Counterfactual Result

Data: 2026-06-20.

## Implementazione

Endpoint ACDC:

`GET /diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=7&horizonSeconds=900`

Commit:

- `03f4b52` - endpoint counterfactual.
- `9e966af` - deduplicazione per advice unico.
- `618c546` - simulazione shadow `ignoreFeatures`.

Deploy:

- ACDC Docker `acdc:latest` redeployato su container `acdc-vpn`.

## Metodo

Il report prende le decisioni PAPER rifiutate con:

- `reason = ML_ADVICE_NOT_PAPER_ELIGIBLE`;
- feature `ml_advice_id` presente;
- deduplica per advice;
- prezzo decisione come entry counterfactual;
- `createdAt` decisione come entry time;
- replay su tick futuri Influx;
- fee runtime PAPER incluse.

Classificazione:

- `GOOD_BLOCK`: il gate ha evitato zero-MFE/perdita.
- `BAD_BLOCK`: il gate ha bloccato un advice che avrebbe raggiunto il safe target.
- `AMBIGUOUS_BLOCK`: MFE positivo ma sotto safe target.

## Risultato PAPER 7

- Advice bloccati unici: 5.
- Decisioni bloccate totali: 72.
- Replayed: 5.
- GOOD_BLOCK: 1.
- BAD_BLOCK: 3.
- AMBIGUOUS_BLOCK: 1.
- NO_FUTURE_DATA: 0.

## Dettaglio

| Symbol | Advice | Verdict | Max net return | Safe target | End net return |
|---|---:|---|---:|---:|---:|
| ATUSDC | 142 | GOOD_BLOCK | -0.002000000000000000 | 0.002949495926620902 | -0.006842797783933518 |
| AIGENSYNUSDC | 144 | AMBIGUOUS_BLOCK | 0.000644856278366112 | 0.003000000000000000 | -0.000488653555219365 |
| BABYUSDC | 145 | BAD_BLOCK | 0.002179916317991632 | 0.001049952591778151 | -0.006876569037656904 |
| AIGENSYNUSDC | 146 | BAD_BLOCK | 0.004036253776435045 | 0.001642561532681941 | 0.004036253776435045 |
| HEIUSDC | 147 | BAD_BLOCK | 0.006294280442804428 | 0.003000000000000000 | 0.006294280442804428 |

## Feature Critiche

- `reversal_pre_trough_drop`: 3 fail, 2 BAD_BLOCK.
- `reversal_trough_age_seconds`: 3 fail, 2 BAD_BLOCK.
- `reversal_distance_from_trough`: 3 fail, misto.
- `reversal_slope_delta`: 2 fail, 1 BAD_BLOCK.
- `reversal_quality`: 1 fail, 1 GOOD_BLOCK.

## Verdict

Il gate `live_revalidation_ranges` non e' validato come filtro operativo.

Nella PAPER 7 ha bloccato piu' advice che avrebbero raggiunto il safe target rispetto agli advice effettivamente salvati.
Non va rimosso a mano, ma va ricalibrato con questo stesso counterfactual su piu' execution.

## Shadow Ignore Features

PAPER `7`:

- `reversal_pre_trough_drop`: would pass 1, GOOD 0, BAD 1, AMBIGUOUS 0.
- `reversal_trough_age_seconds`: would pass 0.
- `reversal_pre_trough_drop,reversal_trough_age_seconds`: would pass 1, GOOD 0, BAD 1.
- combinazione ampia `pre_trough_drop,trough_age,distance_from_trough,slope_delta`: would pass 4, GOOD 0, BAD 3, AMBIGUOUS 1.

Aggregato execution `4,6,7`:

- `reversal_pre_trough_drop`: would pass 1, GOOD 0, BAD 1.
- `reversal_pre_trough_drop,reversal_trough_age_seconds`: would pass 3, GOOD 0, BAD 3.
- combinazione ampia `pre_trough_drop,trough_age,distance_from_trough,slope_delta`: would pass 11, GOOD 2, BAD 8, AMBIGUOUS 1.

Conclusione:

- non esiste evidenza per allentare molte feature insieme;
- `reversal_pre_trough_drop` non dimostra utilita' come hard gate nel campione e puo' essere rimossa in modo conservativo dal set `live_revalidation_ranges`;
- questa rimozione resta un esperimento PAPER, non un aggiornamento charter.

## Raccomandazione Del Consiglio

Non aggiornare il charter come successo operativo.

Aggiornamento ammesso solo come vincolo metodologico:

- ogni modifica al live revalidation deve passare da counterfactual BAD_BLOCK/GOOD_BLOCK;
- non usare feature con prevalenza BAD_BLOCK come hard gate finche' non sono ricalibrate;
- prossimo esperimento: ricalibrare o disabilitare in shadow `reversal_pre_trough_drop` e `reversal_trough_age_seconds`, poi ripetere PAPER autotick.
