# Strategic Bollinger-Only REM Charter

Data: 2026-06-28.

## Status

Questo documento sostituisce il precedente charter REM outcome-first.

Il vincolo strategico operativo e' ora:

```text
hft-common/doc/BOLLINGER_ONLY_PLAN.md
```

Il piano `BOLLINGER_ONLY_PLAN.md` e' il riferimento primario per implementazione, diagnostica e run PAPER.

## Gerarchia Documentale

1. `STRATEGIC_REM_RECOVERY_PLAN.md`: charter sintetico e gerarchia vincolante.
2. `BOLLINGER_ONLY_PLAN.md`: piano strategico tecnico vincolante.
3. `CURRENT_CONTEXT.md`: snapshot corrente cross-modulo.
4. `STRATEGIC_REM_HANDOFF.md`: manuale operativo stabile.

Se i documenti confliggono, prevale questo charter; poi `BOLLINGER_ONLY_PLAN.md`.

## Processo Vincolante

La pipeline resta:

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

Cambiano solo le definizioni decisionali:

- ML produce solo candidate/advice Bollinger.
- Live-score produce un set live speculare, senza modificare semanticamente il contratto storico.
- WATCH osserva il mercato e compra solo quando il trigger Bollinger setup-specifico e' vero.
- BUY esegue solo PAPER.
- SELL usa il contratto Bollinger pubblicato nell'advice.
- Forensics distingue setup, trigger, blocco storico, blocco live, ingresso e uscita.

## Regole Sacre

1. REAL e' vietata.
2. Il ciclo operativo e' PAPER-only da `/management`.
3. SHADOW non e' un ramo operativo della strategia.
4. Nessuna logica legacy puo' selezionare, ordinare, bloccare o confermare BUY.
5. I nuovi payload operativi non devono emettere `reversal_*`.
6. Le decisioni operative possono usare solo feature e contratto `bb_*`.
7. Ogni advice deve dichiarare setup e trigger Bollinger.
8. Ogni WATCH deve fallire chiusa se setup, trigger o soglie Bollinger sono mancanti.
9. La finestra temporale WATCH autorizza solo l'osservazione; non e' una condizione BUY.
10. Nessuna soglia DB `rem_*` legacy puo' diventare guardia di trading.
11. Stringhe operative, payload key, status, reason e action devono stare in enum/costanti.

## Setup Ammessi

Sono ammessi solo:

- `BB_REENTRY_MEAN_REVERSION_LONG`
- `BB_SQUEEZE_BREAKOUT_LONG`

Trigger ammessi:

- `BB_REENTRY_CONFIRMED`
- `BB_UPPER_BREAKOUT_CONFIRMED`

## Consiglio Scientifico Permanente

Il Consiglio resta attivo con tre ruoli:

- Saggio ascoltatore: continuita' e lettura prudente dei dati.
- Scienziato severo: rigore, contratto completo, evidenza non contaminata.
- Mediano pragmatico: implementazione misurabile e run controllate.

Decisione unica del Consiglio:

prima implementare integralmente Bollinger-only setup-specifico, poi misurare PAPER. Non introdurre EMA, RSI, volume,
ATR, regime, momentum o altre famiglie finche' l'utente non approva un nuovo charter.

## Stato Strategico

`BOLLINGER_ONLY_V2_IMPLEMENTED_PAPER_VALIDATION_ACTIVE`.

Exit criteria della fase implementativa:

- build e deploy dei moduli toccati: completati;
- payload operativi nuovi senza `reversal_*`: completato per il ciclo management;
- advice con `bb_setup` e `bb_trigger`: completato;
- WATCH setup-specifica e fail-closed: completato;
- runtime finale PAPER-only: completato;
- almeno una PAPER da `/management` con forensics leggibile: completato.

La fase corrente e' misurazione PAPER Bollinger-only. Non introdurre nuove famiglie di segnali finche' il Consiglio non
chiude l'analisi del campione PAPER per setup.
