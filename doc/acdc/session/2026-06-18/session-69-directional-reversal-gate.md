# Session 69 - Directional Reversal Gate

Data: 2026-06-18

## Problema

`CHIPUSDC` e' stato identificato come reversal acquistabile anche in casi in cui il movimento live era un reversal verso il basso o comunque non un upward reversal confermato.

La causa e' che il modello distingueva il rebound dal trough tramite slope, distance e outcome storici, ma non applicava una feature direzionale esplicita e appresa a ogni ciclo ML.

## Soluzione

Aggiunta feature:

- `reversal_direction_score`

Formula:

- impulso upward: componenti positive di `reversal_slope_short`, `reversal_slope_delta`, `reversal_acceleration`;
- impulso downward: valore assoluto delle componenti negative degli stessi segnali;
- score: `upwardImpulse - downwardImpulse`.

La frontiera `0` separa direzione upward/downward; non e' una soglia di tuning.

## Uso nel ML

DocBrown ora:

- salva `reversal_direction_score` nei sample del bucket;
- lo include tra le signature outcome-first;
- lo usa come range dinamico nelle rule multivariate;
- lo aggiunge sempre al fallback `SIGNATURE_PAPER_ELIGIBLE`.

Per ogni ciclo, il range direzionale e' derivato dai campioni GOOD del bucket corrente. Il minimo viene clampato a `0` per impedire advice PAPER su reversal ribassisti.

## Uso nel runtime

ACDC calcola la stessa feature dai microbar live, quindi le rule generate da DocBrown possono matchare solo se il live snapshot e' compatibile con il range upward appreso.

## Vincolo operativo

DocBrown resta in `DOCBROWN_ACDC_SHADOW_SIGNAL_MODE=MANUAL`, quindi questa modifica aggiorna mining/advice senza aprire nuove PAPER automaticamente.

