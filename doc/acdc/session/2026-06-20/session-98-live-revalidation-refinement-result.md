# Session 98 - Live Revalidation Refinement Result

Data: 2026-06-20.

## Modifica

DocBrown:

- rimosso `reversal_pre_trough_drop` da `LIVE_REVALIDATION_FEATURES`;
- BUY, SELL, trailing, scoring, mining e triage invariati.

Commit:

- ACDC docs: `063583b`.
- DocBrown code: `869a9f4`.

Deploy:

- DocBrown Docker ricreato su `docbrown:latest`.
- ACDC Docker ricreato con `ACDC_INFLUX_SHADOW_SYMBOL_LIMIT=288` per allineare universo ACDC e DocBrown.

## Run

Execution `8` e' stata abortita: DocBrown lavorava su 288 simboli, ACDC valutava 200. Nessuna posizione aperta, PnL zero.

Execution valida: `9`.

Artifact:

`/tmp/session98-live-revalidation-refinement-aligned-aligned-run`

## Risultato Operativo

- Trade: 1.
- Wins: 1.
- Losses: 0.
- Net profit quote: `0.3230705383788`.
- Net return: `0.012922821576763485`.
- Max net return: `0.021213278008298756`.
- Exit: `EXIT_ML_ADVICE_DYNAMIC_TRAILING`.
- Capture ratio: `0.6091855097410218`.

Trade:

| Symbol | Advice | Net return | Max net return | Exit | Hold seconds |
|---|---:|---:|---:|---|---:|
| HEIUSDC | 164 | 0.012922821576763485 | 0.021213278008298756 | EXIT_ML_ADVICE_DYNAMIC_TRAILING | 655 |

Timeline:

- Round-robin -> advice: `55s`.
- Advice valid-from -> BUY: `16s`.
- Open -> SELL: `658s`.
- Advice -> close: `674s`.

## Counterfactual Blocchi Live

Execution `9`:

- blocked advice: 11;
- blocked decisions: 111;
- replayed: 11;
- GOOD_BLOCK: 5;
- BAD_BLOCK: 6;
- AMBIGUOUS_BLOCK: 0.

Interpretazione:

- il refinement ha permesso un trade profittevole e trailing funzionante;
- il live gate residuo non e' ancora scientificamente validato come soluzione finale, perche' blocca ancora 6 opportunity profittevoli su 11 advice bloccati.

## Feature Failure

| Feature | Failures | BAD_BLOCK | GOOD_BLOCK |
|---|---:|---:|---:|
| reversal_quality | 5 | 3 | 2 |
| reversal_slope_delta | 7 | 4 | 3 |
| reversal_trough_age_seconds | 6 | 3 | 3 |
| reversal_distance_from_trough | 6 | 2 | 4 |
| reversal_slope_short | 5 | 3 | 2 |
| raw_volume | 1 | 1 | 0 |

## Verdict

`PASS_PROVISIONAL_REFINEMENT__LIVE_GATE_NOT_FINAL`.

Accettato:

- `reversal_pre_trough_drop` non deve tornare nel live hard gate senza nuova evidenza multi-execution.

Non accettato:

- rimozione ampia delle feature live;
- promozione del gate residuo come scientificamente completo;
- update charter come soluzione finale.

Prossimo step:

- controfattuale multi-execution su `reversal_slope_delta`, candidato perche' nella execution `9` la sua rimozione isolata avrebbe sbloccato 1 BAD_BLOCK e 0 GOOD_BLOCK.
