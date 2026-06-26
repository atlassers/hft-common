# Session 36 - Microbar Reversal Source

Data: 2026-06-16.

## Domanda

Confrontare due scenari per REM:

- training su bucket storico `binance`, con fallback bar-based;
- training su `binance-microbar`, coerente con SHADOW/PAPER runtime.

## Decisione

Usare `binance-microbar` come sorgente outcome-first per REM.

Motivo:

- REM vive sul cambio di curva a breve;
- SHADOW/PAPER costruiscono BUY/SELL live da `binance-realtime` e `binance-microbar`;
- addestrare su `binance` introduce una geometria diversa dal runtime;
- il fallback bar-based resta solo una protezione matematica quando due punti temporali non sono disponibili, non una scelta strategica.

Tradeoff accettato:

- `binance-microbar` puo' avere meno profondita' storica;
- il training e' pero' allineato alla superficie decisionale live.

## Implementazione

- `InfluxSnapshotService.historicalBucketName()` ora ritorna `microbarBucket`.
- `historicalUsdcSymbols()` legge da `binance-microbar`.
- `historicalTicks()` legge da `binance-microbar`.
- Aggiunta migrazione `V26__reset_outcome_samples_for_microbar_source.sql`:
  - elimina campioni outcome precedenti;
  - elimina firme outcome precedenti.

## Vincolo

Da ora le firme REM valide devono derivare da microbar, non dal bucket storico `binance`.
