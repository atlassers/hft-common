# Session 58 - REM Dynamic Band Baseline PAPER Run

Data: 2026-06-17

## Vincoli

- Nessuna modifica algoritmica in questa sessione.
- Obiettivo: validare il baseline profittevole ottenuto con la RUN 53 usando una PAPER piu' lunga.
- La PAPER deve consumare solo advice `PAPER_ELIGIBLE` generate da DocBrown.
- ACDC resta runtime BUY/SELL; DocBrown resta miner ML e segnalatore.
- Nessuna REAL RUN autorizzata o avviata.

## Configurazione Baseline

- ACDC profile: `REM_CURRENT`.
- Execution type: `PAPER`.
- DocBrown promotion mode: `PURE_REVERSAL`.
- DocBrown signal enabled: `true`.
- Lookback: 12h.
- Max samples: 5000.
- Min validation profit rate: 0.58.
- Advice min safe net return: 0.0020.
- Advice min max net return: 0.0040.
- Advice min profit probability: 0.60.
- ACDC ENTRY runtime:
  - `entry_price_present`;
  - `entry_snapshot_fresh`;
  - `reversal_ml_rules_min`;
  - `entry_reversal_ml_score_positive`;
  - `entry_ml_advice_paper_eligible`.
- ACDC EXIT runtime:
  - ML advice take profit / positive duration exit;
  - ML advice loss cap;
  - ML advice timeout;
  - trailing/risk guards DB-driven secondo configurazione runtime.

## Batch DocBrown

- Generated at: `2026-06-17T16:17:19.308414257Z`.
- Band version: `REM-BAND-2026-06-17T161658.772295034-4428a90e`.
- Band status: `ACTIVE`.
- Coverage range: `0..1`.
- Distinct price points min: `1`.
- Max gap seconds: `60`.
- Validation samples: `3264`.
- Good samples: `2078`.
- Bad samples: `1186`.
- Validation precision good: `0.636642156862745098`.
- Validation recall good: `1`.
- Validation avg MFE: `0.002371471995381403`.
- Band score: `2.510431513263630496`.

## Mining Output

- Evaluated rules: `505`.
- Promoted rules: `159`.
- `PURE_REVERSAL_OBSERVED`: `114`.
- `PAPER_ELIGIBLE`: `45`.
- Prime advice `PAPER_ELIGIBLE` osservate:
  - `ENAUSDC`;
  - `COTIUSDC`;
  - `DYDXUSDC`;
  - `LAYER2`;
  - `ARBUSDC`;
  - `0GUSDC`.

## PAPER Execution 54

- Execution id: `54`.
- Started at: `2026-06-17 16:17:03`.
- Completed at: `2026-06-17 16:47:43`.
- Status: `COMPLETED`.
- Initial budget: `100`.
- Current budget: `100.298000935354684800`.
- Reserved budget: `0`.
- Realized profit quote: `0.298000935354684800`.
- Trades: `3`.
- Wins: `3`.
- Losses: `0`.
- Gross win: `0.298000935354684800`.
- Gross loss: `0`.
- Average net return: `0.00397335`.
- Best MFE: `0.00866169`.

## Trade Detail

| Symbol | Result | Buy Quote | Sell Quote | Net Profit | Net Return | Hold Seconds | Exit |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| `ENAUSDC` | WIN | `24.999999926800000000` | `25.266808890800000000` | `0.216542155182400000` | `0.008661686232657418` | `647` | ML take profit |
| `0GUSDC` | WIN | `24.999999968000000000` | `25.082236810000000000` | `0.032154605222000000` | `0.001286184210526316` | `666` | ML positive duration exit |
| `1000SATSUSDC` | WIN | `24.999999999993200000` | `25.099403578522000000` | `0.049304174950284800` | `0.001972166998011928` | `730` | ML positive duration exit |

## Exit Semantics Check

`GuardEvaluator.mlAdviceTakeProfit` ha due condizioni accettanti:

1. `net_return >= ml_advice_safe_net_return`;
2. a fine `ml_advice_duration_seconds`, se `max_net_return > 0` e `net_return > 0`.

Quindi `0GUSDC` e `1000SATSUSDC` non sono take-profit pieni rispetto a `safe_net`.
Sono uscite positive a fine durata prevista. Il reason operativo resta `EXIT_ML_ADVICE_TAKE_PROFIT`, ma semanticamente e' piu' corretto leggerle come positive duration exit.

Questa scelta ha migliorato la robustezza della RUN 54 perche' ha evitato di trasformare trade positivi ma sotto target in timeout/loss tardive.
Non e' stata modificata in questa sessione.

## Confronto Con RUN 53

- RUN 53: `+0.384220124584400000`, 4 trade, 3 win, 1 loss.
- RUN 54: `+0.298000935354684800`, 3 trade, 3 win, 0 loss.
- RUN 53 aveva una regola `FAMILY UNKNOWN / acceleration_reversal`.
- RUN 54 ha prodotto molte piu' advice, con segnali piu' orientati al simbolo e una selezione PAPER piu' ampia.
- Entrambe le RUN sono profittevoli.
- RUN 54 e' piu' pulita per valutare il baseline perche' non contiene loss e ha chiuso tutte le posizioni entro la finestra di test.

## Evidenze Scientifiche

- Il baseline attuale non va cambiato prima di avere un confronto A/B.
- La band dinamica resta molto larga (`0..1`), quindi il profitto sembra arrivare soprattutto da:
  - mining reversal;
  - ranking delle advice;
  - policy per trade specifico;
  - uscita positiva a durata.
- Penalizzare subito le band larghe potrebbe migliorare la precisione, ma potrebbe anche degradare questo risultato.
- Prima di introdurre penalita' su ampiezza/scope serve un replay comparativo contro RUN 53 e RUN 54.

## Prossimo Step Consigliato

Prima di cambiare la pipeline:

1. congelare questo baseline come riferimento;
2. eseguire replay/scoring offline di una variante STRICT o penalizzata;
3. confrontare almeno:
   - expected net after fees/slippage/dust;
   - win rate;
   - average net return;
   - worst loss;
   - missed winners;
   - durata media;
   - rapporto tra safe target e positive duration exit.

Solo se la variante migliora questi indicatori senza perdere i winner principali, promuoverla a PAPER.
