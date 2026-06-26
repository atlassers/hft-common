# Sessione 04 - PAPER Cross-Cycle con Influx Reale

Data: 2026-06-14.

## Obiettivo

Completare ACDC per essere pronto a una PAPER RUN che simula BUY/SELL senza ordini Binance, leggendo dati reali da Influx quando non vengono passati snapshot nel body.

## Riferimenti HFT Usati

- `PaperTradingService`: ledger PAPER, apertura e chiusura posizione simulate, separazione da REAL.
- `PaperTradingRunRepository`: ricerca delle run aperte per execution e simbolo.
- `V5__create_paper_trading_observability.sql`: osservabilita' PAPER e persistenza decisioni.
- `PositionSizingConfigService`: sizing progressivo configurabile.
- `BuyAction`: fee, min notional, step size e tick size come vincoli di esecuzione simulata.

## Implementazione

- Aggiunta migrazione `V4__add_paper_execution_runtime.sql`.
- Aggiunta `execution_uuid` a `acdc_run_execution`.
- Collegata `acdc_paper_position` alla execution persistente.
- Aggiunti fee BUY/SELL e gross profit alla posizione PAPER.
- Aggiunta tabella `acdc_paper_decision` per audit delle decisioni PAPER.
- Aggiunta tabella `acdc_paper_runtime_config` per configurare a DB:
  - fee rate;
  - min notional;
  - step size;
  - tick size;
  - max open positions;
  - curva sizing.
- Aggiunto servizio `PaperSizingService`.
- PAPER ora:
  - avvia o riusa una execution running;
  - legge Influx se non riceve snapshot nel body;
  - apre posizioni simulate solo dopo guardie, sizing, budget e regole exchange;
  - chiude posizioni in step successivi sulla stessa execution;
  - salva ogni decisione a DB.
- Aggiunto endpoint:
  - `POST /acdc/paper/stop/{profileKey}`.

## Validazione

Comandi verificati:

```bash
./mvnw -q test
./mvnw -q package -DskipTests
./mvnw -q package -DskipTests -Dquarkus.profile=local
```

Esito: OK.

## PAPER RUN con Influx Reale

Runtime locale avviato con:

- profilo Quarkus `local`;
- ledger H2 locale;
- Influx abilitato;
- bucket realtime/storico/microbar configurati;
- nessuna chiamata Binance per ordini;
- nessuna REAL RUN.

Step 1:

- endpoint: `POST /acdc/paper/run/REM_CURRENT`;
- body: `{}`;
- `dataSource=INFLUX`;
- `executionId=1`;
- `initialBudget=100`;
- `currentBudget=12.7572885792150800000000000000000`;
- `evaluated=242`;
- `accepted=49`;
- `rejected=193`;
- `opened=7`;
- `closed=0`;
- `netProfitQuote=0`.

Step 2:

- endpoint: `POST /acdc/paper/run/REM_CURRENT`;
- body: `{}`;
- `dataSource=INFLUX`;
- stessa `executionId=1`;
- `initialBudget=100`;
- `currentBudget=17.8258279846088300000000000000000`;
- `evaluated=262`;
- `accepted=68`;
- `rejected=194`;
- `opened=4`;
- `closed=2`;
- `netProfitQuote=-0.131189221949500000000000000000`.

## Stato

ACDC backend e' pronto a eseguire PAPER RUN da REST leggendo dati reali da Influx e simulando BUY/SELL su ledger locale ACDC.

Rimane fuori da questa sessione:

- notifiche Telegram;
- avvio operativo tramite FE;
- autorizzazione e implementazione di una REAL RUN.
