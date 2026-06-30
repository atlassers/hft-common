# Strategic Bollinger Context V1 REM Charter

Data: 2026-06-30.

## Status

Questo documento e' il charter strategico vincolante del ciclo REM.

Il vincolo operativo primario e' ora:

```text
hft-common/doc/BOLLINGER_CONTEXT_V1_PLAN.md
```

`BOLLINGER_ONLY_PLAN.md` resta baseline storica consolidata. Non e' piu' il piano strategico primario.

## Gerarchia Documentale

1. `STRATEGIC_REM_RECOVERY_PLAN.md`: charter sintetico e gerarchia vincolante.
2. `BOLLINGER_CONTEXT_V1_PLAN.md`: piano tecnico-operativo vincolante.
3. `CURRENT_CONTEXT.md`: snapshot corrente cross-modulo.
4. `STRATEGIC_REM_HANDOFF.md`: manuale operativo stabile.
5. `BOLLINGER_ONLY_PLAN.md`: baseline storica e riferimento di regressione.

Se i documenti confliggono, prevale questo charter; poi `BOLLINGER_CONTEXT_V1_PLAN.md`.

## Processo Vincolante

La pipeline resta:

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

Cambiano le definizioni decisionali:

- ML produce candidate/advice Bollinger con contratto context completo.
- Live-score produce un set live speculare, senza modificare semanticamente il contratto storico.
- WATCH osserva il mercato e compra solo quando trigger Bollinger e gate Context V1 passano.
- BUY esegue solo PAPER.
- SELL resta Bollinger-only nella fase 1, per isolare l'effetto dei nuovi gate di ingresso.
- Forensics distingue setup, trigger, regime, context gate, ingresso e uscita.

## Regole Sacre

1. REAL e' vietata.
2. Il ciclo operativo e' PAPER-only da `/management`.
3. SHADOW non e' un ramo operativo della strategia.
4. Nessuna logica legacy puo' selezionare, ordinare, bloccare o confermare BUY.
5. I nuovi payload operativi non devono emettere `reversal_*`.
6. Bollinger resta il segnale centrale: ogni advice deve dichiarare `bb_setup` e `bb_trigger`.
7. Le decisioni operative Context V1 possono usare solo contratto Bollinger `bb_*` e feature context esplicite:
   `market_regime`, EMA, RSI, volume ratio, ATR/risk e relative soglie contrattuali.
8. WATCH deve fallire chiusa se setup, trigger, regime o soglie context richieste sono mancanti.
9. La finestra temporale WATCH autorizza solo l'osservazione; non e' una condizione BUY.
10. WATCH e BUY non possono essere limitate da cap numerici su posizioni o osservazioni concorrenti; l'unico limite
    ammesso all'acquisto e' la disponibilita' di budget/exchange sizing al momento della BUY.
11. Nessuna soglia DB `rem_*` legacy puo' diventare guardia di trading.
12. Stringhe operative, payload key, status, reason e action devono stare in enum/costanti.

## Setup Ammessi

Sono ammessi solo:

- `BB_REENTRY_MEAN_REVERSION_LONG`
- `BB_SQUEEZE_BREAKOUT_LONG`

Trigger ammessi:

- `BB_REENTRY_CONFIRMED`
- `BB_UPPER_BREAKOUT_CONFIRMED`

Regimi ammessi dal contratto Context V1:

- `REGIME_RANGE`
- `REGIME_SQUEEZE`
- `REGIME_EXPANSION`
- `REGIME_TREND_UP`
- `REGIME_TREND_DOWN`
- `REGIME_CHAOS`

## Consiglio Scientifico Permanente

Il Consiglio resta attivo con tre ruoli:

- Saggio ascoltatore: continuita' e lettura prudente dei dati.
- Scienziato severo: rigore, contratto completo, evidenza non contaminata.
- Mediano pragmatico: implementazione misurabile e run controllate.

Decisione unica del Consiglio:

`BOLLINGER_CONTEXT_V1` e' il nuovo vincolo strategico. Va implementato come estensione disciplinata del runtime
Bollinger-only: nessuna riscrittura della pipeline, nessun ramo operativo parallelo, nessun ritorno a nomi `ml_*` o
contratti legacy.

## Stato Strategico

`BOLLINGER_CONTEXT_V1_CHARTER_ACTIVE_IMPLEMENTATION_IN_PROGRESS`.

Exit criteria tecnici della fase corrente:

- costanti, enum e reason Context V1 condivise;
- DocBrown pubblica advice con setup, trigger, regime e context completo;
- ACDC applica ContextGateAudit dopo il trigger audit Bollinger e prima della BUY;
- `policyJson` congela i valori `entry_*` usati per il BUY;
- Kenshiro espone strategy family, setup/regime e diagnostica context su `/management`;
- hft-fe mostra family, regime, reason context e confronto diagnostico;
- build, deploy e PAPER validation completate sui moduli toccati.

La valutazione finanziaria resta PAPER-only e separata per setup/regime. Context V1 prosegue solo se migliora le
metriche indicate nel piano vincolante.
