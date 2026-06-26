# Session 103 - Post SELL Reversal Forensics Checklist

Data: 2026-06-21.

## Obiettivo

Verificare, prima di una nuova run, se le SELL della execution `12` hanno mancato reversal immediatamente successivi all'uscita.

Metodo approvato dal Consiglio:

- finestra locale: `50%` della durata trade prima del BUY e dopo la SELL;
- minimo operativo: `120s`;
- finestra estesa: `100%` della durata trade dopo la SELL;
- il grafico puo' restare ampio, ma il verdetto primario usa la finestra locale;
- dati da Influx/MySQL reali, nessun H2;
- la granularita' del dato e' parte del verdetto: candle `1m` non basta per diagnosticare reversal mancati di pochi secondi;
- se realtime/microbar non sono piu' disponibili, usare Binance aggTrades storici; se non disponibili, classificare come `INCONCLUSIVE_GRANULARITY`;
- nessun tuning durante il forensics.

Artifact sorgente:

`/tmp/session102-baseline98-45m-validation-run`

## Checklist Livello 1

- [x] L1.1 Creare checklist.
- [x] L1.2 Estrarre timeline e sell-capture execution `12`.
- [x] L1.3 Rifiutare verdict storico su granularita' diversa.
- [x] L1.4 Implementare endpoint post-SELL forensics per nuove run.
- [x] L1.5 Verificare endpoint su container/MySQL.
- [x] L1.6 Decidere se lanciare nuova sessione da 1h.
- [x] L1.7 Spostare il forensics nel flusso post-SELL applicativo.
- [x] L1.8 Salvare record persistenti per posizione venduta.
- [x] L1.9 Usare worker leggero solo per completare record pendenti maturi.
- [x] L1.10 Build, deploy container e verifica MySQL.
- [x] L1.11 Commit/push documentazione finale.

## Checklist Livello 2

### L2.1 Metriche Per Trade

- [x] Hold seconds.
- [x] Local window seconds.
- [x] Extended window seconds.
- [x] In-trade max net return.
- [x] Post-exit local max net return.
- [x] Post-exit extended max net return.
- [x] Time to post-exit max.
- [x] Would hit safe target after exit.
- [x] Would recover loss after exit.
- [ ] Pre-entry context.
- [x] Data source granularity.

### L2.2 Classificazioni

- [x] `SELL_TOO_EARLY_REVERSAL_AFTER_EXIT`.
- [ ] `BAD_TRAILING_AFTER_MFE`.
- [ ] `TRUE_ZERO_MFE_BAD_ADVICE`.
- [x] `NO_REVERSAL_CONFIRMED`.
- [ ] `INCONCLUSIVE_WINDOW`.
- [x] `INCONCLUSIVE_GRANULARITY`.

## Stato Realtime

- Stato corrente: `POST_SELL_FORENSICS_EVENT_DRIVEN_DEPLOYED`.
- Execution: `12`.
- Prossima azione: attendere nuova RUN con SELL reali per produrre record persistenti.

## Decisione Del Consiglio

Il Consiglio approva lo step, con una condizione forte: il forensics post-SELL produce verdict solo se la finestra usa dati omogenei e sufficientemente granulari. Se il dato disponibile e' solo `binance` a gap `60s`, il report deve restare `INCONCLUSIVE_GRANULARITY`.

Razionale:

- il dato storico a 1 minuto puo' essere guardato come contesto visuale;
- non puo' decidere se una SELL e' avvenuta pochi secondi prima del reversal;
- il valore scientifico nasce se il report viene raccolto subito dopo la run, entro retention `binance-microbar`/`binance-realtime`.

## Implementazione

Endpoint aggiunto:

`GET /diagnostics/acdc/paper/post-sell-forensics?executionIds=...`

Decisione successiva del Consiglio:

- il forensics non deve essere raccolto da uno script periodico come meccanismo primario;
- alla SELL viene creato un record `PENDING_LOCAL` per il solo simbolo/posizione venduta;
- un worker leggero completa solo i record pendenti quando la finestra locale e' maturata;
- l'endpoint continua a mostrare fallback live per execution storiche senza record persistenti;
- nessuno scanner globale su tutti i simboli.

Per ogni trade chiuso calcola:

- hold seconds;
- finestra locale `max(120s, 50% hold)`;
- finestra estesa `max(120s, 100% hold)`;
- source bucket usato;
- tick count;
- max gap seconds;
- post-exit local/extended max net return;
- post-exit local/extended end net return;
- time to post-exit max;
- would hit safe target;
- would recover loss;
- verdict conservativo.

Soglia verdict operativo:

- `maxGapSeconds <= 15`.

Se la soglia non passa:

`INCONCLUSIVE_GRANULARITY`.

## Verifica Execution 12

Risultato endpoint su execution `12`:

- `RAREUSDC`: source `binance`, max gap `60s`, verdict `INCONCLUSIVE_GRANULARITY`.
- `MASKUSDC`: source `binance`, max gap `60s`, verdict `INCONCLUSIVE_GRANULARITY`.
- `KITEUSDC`: source `binance`, max gap `60s`, verdict `INCONCLUSIVE_GRANULARITY`.

Decisione:

- nessun giudizio retroattivo sui reversal mancati della run storica;
- nuova run da 1h scientificamente utile solo se il report viene raccolto subito dopo stop/drain.

## Verifica Deploy Event-Driven

- `./mvnw -DskipTests package`: OK.
- `docker build -f docker/Dockerfile.jvm -t acdc:latest .`: OK.
- `docker compose --env-file docker/vpn/.env -f docker/vpn/compose.yml up -d --build --force-recreate acdc`: OK.
- Flyway MySQL: migrazione `69 - paper post sell forensics` applicata con successo.
- Endpoint execution `12`: ancora disponibile in `LIVE_FALLBACK`, con tre verdict `INCONCLUSIVE_GRANULARITY`.
- Execution `14` nata dallo scheduler dopo redeploy: fermata; zero posizioni, zero forensics, budget invariato.
