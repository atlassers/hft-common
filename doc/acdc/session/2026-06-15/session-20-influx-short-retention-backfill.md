# Sessione 20 - Influx Short-Retention Backfill

Data: 2026-06-15.

## Obiettivo

Evitare che dopo restart `binance-realtime` e `binance-microbar`, entrambi a retention 1h, partano vuoti e blocchino SHADOW/ML/diagnostics REM.

## Implementazione

- Modificato `influxer`.
- Aggiunto job startup `ShortRetentionBucketBackfillJob`.
- Il job:
  - legge il solo quote asset `USDC`;
  - scarica klines Binance `1m` sull'ultima ora;
  - scrive le candle reali su `binance-realtime`;
  - sintetizza microbar `5s` su `binance-microbar`;
  - non cancella bucket;
  - gira in background e non blocca websocket.

## Verifiche

- `influxer ./mvnw -q -Dtest=ShortRetentionBucketBackfillJobTest test`: OK.
- `influxer ./mvnw -q test`: OK.
- `influxer ./mvnw -q -DskipTests package`: OK.
- Container `influxer` riavviato usando il mount `target/quarkus-app`.

## Runtime

- Backfill startup completato:
  - prima versione: `symbols=724` su USDC+USDT;
  - versione corrente: `symbols=288` solo USDC;
  - `realtimeCandles=17280`;
  - `microbarCandles=207360`.
- Query bucket:
  - `binance-realtime` popolato;
  - `binance-microbar` popolato;
  - dopo restart non risultano nuove serie `USDT`;
  - molti simboli USDC hanno almeno `80` price point, quindi superano il minimo storico del miner.
- REM ML dry-run post backfill:
  - `symbols=287`;
  - `ticks=181005`;
  - `events=163785`;
  - `GOOD_REVERSAL=4`;
  - `BAD_REVERSAL=163775`;
  - `NEUTRAL_REVERSAL=6`;
  - `signatures=0`;
  - `promoted=0`.

## Stato

Il problema operativo dei bucket 1h vuoti dopo restart e' risolto.
La PAPER resta bloccata per `ML_NO_SIGNATURES`, non piu' per mancanza di dati nei bucket short-retention.
