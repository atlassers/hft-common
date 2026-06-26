# Sessione 1 - Bucket Operativo Unico E Repair

## Piano 1 - Retention 48h E Repair Binance

Data: 2026-06-04.

Contesto:
- La REAL RUN ha operato con dati Influx in due bucket (`binance` live e `binance-history` storico), con buchi possibili dopo `Pong timeout` e incoerenza tra fonte live e fonte optimizer.
- L'utente ha richiesto di fermare la REAL RUN dopo la chiusura posizioni e mantenere solo dati realmente utili/coerenti.

Intervento:
- `HistoricalTicksConfig`: storico abilitato di default, `lookback-days=2`, bucket default `binance`, repair window `360` minuti.
- `HistoricalTicksIngestionJob`: startup backfill su finestra completa e repair schedulato ogni `10m`; guard `AtomicBoolean` per evitare overlap.
- `application-prod.yml`: `clear-bucket-on-startup=false`, storico attivo su bucket `binance`.
- Runtime InfluxDB: retention `48h` e shard `1h` su `binance` e `binance-history`.
- Runtime Influxer ricreato con DNS espliciti `1.1.1.1` e `8.8.8.8`, storico attivo e bucket `binance`.

Verifiche:
- `./mvnw -q test` OK.
- `./mvnw -q -DskipTests package` OK.
- Runtime: stream Binance con 720 kline attivo; startup backfill su 290 simboli USDC, 2881 candele per simbolo su finestra 48h.

Note operative:
- La REAL RUN era gia' bloccata prima del restart Influxer: `real_buy_enabled=0`, `refresh_enabled=0`, `hft-vpn` e `docbrown-real-refresh` spenti.
- Il bucket `binance-history` resta con retention 48h come compatibilita' temporanea, ma le config operative puntano a `binance`.

## Piano 2 - Runtime Realtime-Only

Data: 2026-06-05.

Contesto:
- In Grafana `BTCUSDC` risultava fermo a `00:03 CEST` perche' veniva letta la serie storica/repair con tag `base=BTC`, `quote=USDC`.
- La serie realtime senza tag `base/quote` avanzava correttamente (`BTCUSDC` aggiornato a `00:13 CEST` e poi oltre).
- L'utente ha confermato che e' sufficiente usare sempre la serie piu' aggiornata e che lo storico puo' essere rimosso.

Intervento:
- Influxer runtime ricreato con `HFT_HISTORICAL_TICKS_ENABLED=false`.
- Cancellate dal bucket `binance` le serie storiche `tick` con tag `quote=USDC`.
- `application.yml` e `application-prod.yml`: storico disabilitato di default.
- README aggiornato: lo storico e' solo per backfill/repair espliciti; runtime usa realtime.

Verifiche:
- Log Influxer: `Historical candles ingestion is disabled`.
- Stream Binance: 720 kline `1m` sottoscritte.
- Influx query `BTCUSDC`: unica serie senza tag `base/quote`, aggiornata.
- `./mvnw -q test` OK.
- `./mvnw -q -DskipTests package` OK.

## Piano 3 - Watchdog Realtime Binance

Data: 2026-06-11.

Contesto:

- Durante l'audit delle run Docbrown `952+`, i dati Influx mostravano finestre dominate dal repair storico (`289 simboli * minuti`) mentre il realtime utile era insufficiente per lo scout.
- La separazione applicativa ora e': storico taggato `base`/`quote`, realtime non taggato.
- Serve comunque un guard Influxer per non lasciare silenzioso un WebSocket formalmente aperto ma senza candele chiuse.

Intervento:

- `StreamingConfig`: aggiunte config `watchdog-enabled`, `watchdog-interval-seconds`, `watchdog-stale-seconds`.
- `BinanceWebSocketClient`: contatori `subscribedStreams`, `closedCandleWrites`, `lastClosedCandleAtMillis`, `connectionStartedAtMillis`.
- Il watchdog schedulato controlla assenza di candele chiuse oltre la soglia e forza un reconnect con retry esponenziale gia' esistente.
- Le scritture realtime aggiornano il timestamp dell'ultima candela chiusa solo dopo write Influx riuscita.

Verifiche:

- `./mvnw -q -Dtest=BinanceWebSocketClientE2ETest,BinanceKlineParserTest test` OK.
- `./mvnw -q package` OK.
- Restart container `influxer` OK.

Evidenze runtime:

- Startup post-fix: `Subscribed to 719 Binance 1m kline streams`.
- Watchdog attivo: `interval=60s staleAfter=180s`.
- Influx ultimi 5 minuti post-restart: `719` simboli realtime non taggati e repair storico taggato separato.

## Piano 4 - Microbar BUY Bucket 5s

Data: 2026-06-12.

Contesto:

- DocBrown deve continuare a lavorare su candele chiuse `1m` nel bucket `binance`.
- HFT deve validare la BUY su una vista piu' fresca ma meno rumorosa del tick puro.
- La regola operativa diventa: analisi DocBrown su `1m`, BUY HFT su microbarre `5s`, SELL/trailing su realtime.

Intervento:

- Aggiunto `influxdb.microbar-bucket`, default `binance-microbar`.
- Aggiunto `influxdb.microbar-retention-minutes`, default `60`.
- `InfluxDBService` crea automaticamente il bucket microbar con retention corta.
- `BinanceWebSocketClient` aggrega gli update Binance realtime in microbarre da `5s`:
  - `open` = primo close osservato nel bucket da 5s;
  - `high/low` = estremi dei close osservati;
  - `close` = ultimo close osservato;
  - volume, quote volume, trade e taker buy sono delta dei cumulativi Binance nella candela `1m`.
- La prima microbar dopo restart usa delta volume zero per evitare spike artificiali.

Verifiche:

- `./mvnw -q -Dtest=BinanceWebSocketClientE2ETest test` OK.
- `./mvnw -q -DskipTests package` OK.
- Container `influxer` riavviato.
- Bucket runtime:
  - `binance-realtime`: retention `1h0m0s`;
  - `binance-microbar`: retention `1h0m0s`.
- Query runtime su `binance-microbar` conferma punti `price` negli ultimi 5 minuti.

Nota operativa:

- Il bucket microbar nasce ora e ha retention `1h`: non puo' essere usato per validare retroattivamente trade storici precedenti alla sua creazione.
