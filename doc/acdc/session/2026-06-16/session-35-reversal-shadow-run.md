# Session 35 - Reversal Outcome SHADOW Run

Data: 2026-06-16.

## Obiettivo

Eseguire una SHADOW esplorativa dopo il passaggio a runtime reversal/outcome-only e decidere se procedere a PAPER.

## Run

- Execution: `34`.
- Tipo: `SHADOW`.
- Sorgente: `INFLUX`.
- Runtime DB:
  - guardie outcome disabilitate per SHADOW;
  - ranking unico su `outcome_quality_score`;
  - fallback esplorativo basato su feature `reversal_*`.

## Risultato SHADOW

- Stato finale: `COMPLETED`.
- Initial budget: `100`.
- Current budget: `99.700000000549283640`.
- Reserved budget: `0`.
- Realized profit quote: `-0.299999999450716360`.
- Posizioni aperte: `6`.
- Posizioni chiuse: `6`.
- Net totale: `-0.30`.

BUY/SELL:

- `0GUSDC`: net `-0.049999999500000000`, max net return `0.001297029702970297`.
- `1000CATUSDC`: net `-0.049999999998000000`, max net return `0`.
- `1000CHEEMSUSDC`: net `-0.049999999999860000`, max net return `0`.
- `1000SATSUSDC`: net `-0.049999999999990960`, max net return `0`.
- `1INCHUSDC`: net `-0.049999999953000000`, max net return `0`.
- `1MBABYDOGEUSDC`: net `-0.049999999999865400`, max net return `0`.

Exit:

- `EXIT_FEE_RANGE_MAX_HOLD`: `6`.

Decisioni:

- `BUY ACCEPTED`: `6`.
- `SELL EXIT_FEE_RANGE_MAX_HOLD`: `6`.
- `EXIT_HOLD`: `231`.
- `SHADOW_BUY_STOPPED`: `394`.
- `SHADOW_BUDGET_OR_EXCHANGE_RULES_REJECTED`: `15363`.

## Lettura

- Tutti i BUY avevano `outcome_quality_score = -0.2623228662538199`.
- Tutti i BUY avevano `reversal_confirmed = 0`.
- Molti BUY avevano `reversal_quality = 0`.
- Solo `0GUSDC` ha avuto un MFE netto positivo minimo, ma la policy reale non ha catturato profitto e ha chiuso a costo/fee.

Conclusione:

- La SHADOW ha fatto correttamente esplorazione.
- Non ha prodotto evidenza per PAPER.
- PAPER ora avrebbe correttamente bloccato questi ingressi tramite `REVERSAL_OUTCOME_QUALITY_NEGATIVE`.

## Mining Outcome-First

Run:

```text
POST /diagnostics/acdc/outcome/REM_CURRENT/mine?lookbackHours=6&horizonSeconds=900&sampleEverySeconds=300&symbolLimit=80&maxSamples=400&validationPercent=30
```

Risultato:

- campioni: `400`;
- created: `256`;
- updated: `144`;
- GOOD: `91`;
- BAD: `309`;
- NEUTRAL: `0`;
- average max net return: `-0.001434494252125125`;
- average end net return: `-0.002725060329313110`;
- promoted signatures: `0`.

## Decisione

Non procedere a PAPER.

Motivo:

- nessuna firma reversal promossa;
- validation avg net return negativa su tutti gli scope;
- SHADOW ha chiuso tutte le posizioni in perdita da fee/max-hold;
- `reversal_confirmed` non si e' manifestato nei BUY scelti.

## Prossimo Passo Scientifico

La SHADOW va mantenuta esplorativa, ma il ranking non deve spendere budget simulato su forme senza reversal strutturale.

Proposta tecnica:

- mantenere disabilitati i gate `outcome_quality_*` in SHADOW;
- aggiungere una guardia SHADOW-specific o un ranking floor su `reversal_confirmed` / `reversal_quality`;
- continuare a vietare PAPER finche' il mining non promuove almeno una firma validata.

## Rettifica Tecnica Successiva

La diagnosi successiva ha trovato due cause che rendono questa run non conclusiva sulla validita' del reversal:

- `acdc_outcome_training_sample` conteneva campioni creati prima dell'introduzione delle feature `reversal_*`;
- il calcolo `reversal_slope_short/medium` usava finestre in secondi, ma il mining storico campionava spesso barre sparse, quindi molte slope diventavano `0`.

Correzione applicata nella sessione successiva:

- fallback bar-based per il calcolo delle slope quando la finestra temporale non trova due punti distinti;
- reset di `acdc_outcome_training_sample` e `acdc_outcome_signature` per `REM_CURRENT` tramite `V25`.

## Esito Mining Dopo Fix

Mining pulito dopo `V25`:

- campioni creati: `400`;
- GOOD: `94`;
- BAD: `306`;
- promoted signatures: `0`;
- `reversal_confirmed = 1`: `1` campione su `400`;
- l'unico campione `reversal_confirmed = 1` e' `BAD`.

Interpretazione:

- il problema iniziale era reale: le feature erano contaminate e le slope temporali erano troppo fragili;
- dopo il fix, il booleano `reversal_confirmed` resta troppo rigido e non rappresenta il reversal profittevole;
- per il charter, il reversal va identificato outcome-first: un punto T e' interessante se nei successivi N secondi produce MFE netto positivo, non se al tempo T e' gia' visivamente confermato;
- `reversal_confirmed` deve restare una feature candidata, non un gate operativo.
