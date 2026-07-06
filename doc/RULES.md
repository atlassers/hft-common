# BOLLINGER_CONTEXT_V1 Rules

Questo documento e' la fonte vincolante delle regole permanenti del ciclo REM `BOLLINGER_CONTEXT_V1`.

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

## Perimetro Di Autonomia Operativa

L'agente e' autorizzato a operare autonomamente dentro il perimetro del progetto:

```text
/home/mbc/Documenti/ws/java/hft
```

e nei container Docker generati, configurati o usati da quel progetto. Dentro questo perimetro puo' leggere, scrivere,
eseguire build/test/script diagnostici, interrogare MySQL e usare i servizi containerizzati necessari al task senza
richiedere conferme operative intermedie all'utente.

Questa autonomia non riduce le Regole Sacre: REAL resta vietata; PAPER resta governata da `/management`; nessuna
mutation runtime, promozione o modifica di configurazioni operative e' ammessa quando il task o il piano vincolante
prevedono solo laboratorio offline, diagnostica o report.

## Setup Ammessi

Sono ammessi solo:

- `BB_REENTRY_MEAN_REVERSION_LONG`
- `BB_SQUEEZE_BREAKOUT_LONG`

Trigger ammessi:

- `BB_REENTRY_CONFIRMED`
- `BB_UPPER_BREAKOUT_CONFIRMED`

Regimi Context V1 ammessi:

- `REGIME_RANGE`
- `REGIME_SQUEEZE`
- `REGIME_EXPANSION`
- `REGIME_TREND_UP`
- `REGIME_TREND_DOWN`
- `REGIME_CHAOS`
