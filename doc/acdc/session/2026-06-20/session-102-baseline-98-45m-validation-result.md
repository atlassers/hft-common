# Session 102 - Baseline 98 45m Validation Result

Data: 2026-06-20.

## Obiettivo

Ripetere la validazione della baseline `98` per almeno `45` minuti, senza tuning e senza cambiare BUY, SELL, ranking, trailing o live gate.

## Run

Execution: `12`.

Artifact:

`/tmp/session102-baseline98-45m-validation-run`

Run endpoint-driven:

- PAPER start;
- rolling validation DocBrown;
- rolling paper promotion DocBrown;
- tick ACDC immediato;
- stop-buy;
- drain fino a reserved finale `0`;
- raccolta endpoint timeline/scoring/sell-capture/counterfactual.

## Risultato

- Cicli: `16`.
- Promotion: `26`.
- BUY: `3`.
- Trade chiusi: `3`.
- Wins: `0`.
- Losses: `3`.
- Net profit quote: `-0.213068181444`.
- Avg net return: `-0.002840909090909091`.
- Loss-cap exits: `1`.
- Timeout exits: `1`.
- Dynamic trailing exits: `1`.
- Target hits: `0`.
- Trailing armed: `1`.
- Avg capture ratio: `-0.1324009324009324`.
- Reserved finale: `0`.

| Symbol | Classificazione | Net return | Max net return | Safe net return | Exit |
|---|---:|---:|---:|---:|---|
| RAREUSDC | BAD_SELL_AFTER_MFE | -0.002000000000000000 | 0.005035211267605634 | 0.000797122302158274 | EXIT_ML_ADVICE_DYNAMIC_TRAILING |
| MASKUSDC | ZERO_MFE_BAD_ADVICE_LOSS_CAP | -0.004522727272727273 | 0 | 0.001016793893129771 | EXIT_ML_ADVICE_LOSS_CAP |
| KITEUSDC | ZERO_MFE_BAD_ADVICE_TIMEOUT | -0.002000000000000000 | 0 | 0.000500000000000000 | EXIT_ML_ADVICE_TIMEOUT |

## Timing

| Symbol | Round robin -> advice | Valid from -> BUY | Open -> close | Advice -> close |
|---|---:|---:|---:|---:|
| RAREUSDC | 32s | 7s | 427s | 434s |
| MASKUSDC | 56s | 9s | 123s | 132s |
| KITEUSDC | 32s | 8s | 909s | 917s |

Tutti i BUY sono freschi:

- advice age `6s`, `7s`, `7s`;
- entry drift `0` per tutti;
- freshness contract pass `true` per tutti.

Quindi la causa primaria delle loss non e' BUY tardivo.

## Forensics

### RAREUSDC

`RAREUSDC` aveva un MFE netto `0.005035211267605634`, superiore al safe target `0.000797122302158274`, e trailing armed `true`.

La chiusura e' avvenuta con `EXIT_ML_ADVICE_DYNAMIC_TRAILING`, ma il net return finale e' `-0.002`.

Classificazione: `BAD_SELL_AFTER_MFE`.

Razionale:

questo non e' un falso positivo puro. Il trade aveva profitto catturabile, ma la SELL dinamica ha restituito tutto il MFE e ha chiuso in perdita da frizione. E' il punto piu' grave della run per il trailing/capture.

### MASKUSDC

`MASKUSDC` e' entrato con advice fresca, drift `0`, live failures `0`, ma max net return `0`.

Exit: `EXIT_ML_ADVICE_LOSS_CAP`.

Classificazione: `ZERO_MFE_BAD_ADVICE_LOSS_CAP`.

Razionale:

questo e' un falso positivo post-BUY. Non e' freshness, non e' drift, non e' SELL: non si e' mai creato MFE.

### KITEUSDC

`KITEUSDC` e' entrato con advice fresca, drift `0`, live failures `0`, ma max net return `0`.

Exit: `EXIT_ML_ADVICE_TIMEOUT`.

Classificazione: `ZERO_MFE_BAD_ADVICE_TIMEOUT`.

Razionale:

secondo falso positivo post-BUY. Anche qui il problema e' a monte della SELL.

## Counterfactual Blocked Advice

- Blocked advice: `22`.
- GOOD_BLOCK: `10`.
- BAD_BLOCK: `9`.
- AMBIGUOUS_BLOCK: `3`.

Interpretazione:

Il live gate non e' chiaramente troppo stretto o chiaramente troppo largo in modo isolabile: blocca quasi tanti segnali utili quanti segnali dannosi, con tre casi ambigui.

Nessuna feature puo' essere rimossa scientificamente da questa run.

## Verdict

`FAIL_BASELINE`.

La baseline `98` non passa questa validazione:

- PnL netto negativo;
- nessuna win;
- due zero-MFE con advice fresca;
- una SELL negativa dopo MFE positivo;
- counterfactual dei blocchi non indica una singola feature da togliere.

Decisione:

- non aggiornare il charter come baseline validata;
- non fare tuning feature-by-feature;
- separare il prossimo lavoro in due assi misurabili:
  1. riduzione falsi positivi post-BUY senza perdere ogni BUY;
  2. verifica/fix del trailing dinamico quando MFE supera safe target.
