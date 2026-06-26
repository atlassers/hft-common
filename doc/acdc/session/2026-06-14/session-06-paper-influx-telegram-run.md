# Sessione 06 - PAPER Influx con Telegram Hook

Data: 2026-06-14.

## Obiettivo

Eseguire una PAPER RUN operativa ACDC con dati reali da Influx e notifiche Telegram agganciate come HFT.

## Setup

- Profilo Quarkus: `local`.
- DB ledger: H2 isolato.
- Influx: reale, container `influxdb`.
- Org: `dgsoft`.
- Bucket:
  - `binance`;
  - `binance-realtime`;
  - `binance-microbar`.
- REAL RUN: non avviata.
- Binance orders: nessuno.

## Esito Iniziale

Il primo tentativo ha prodotto `401 unauthorized` su Influx perche' il token era stato estratto con parsing shell che troncava il valore al primo `=`.

Correzione:

- token letto con preservazione dell'intero valore;
- ACDC riavviato;
- query Influx validate dalla PAPER RUN.

## PAPER RUN Valida

Endpoint:

```bash
POST /acdc/paper/run/REM_CURRENT
```

Body:

```json
{}
```

Risultati:

| Step | executionId | source | evaluated | accepted | rejected | opened | closed | net | budget |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 1 | INFLUX | 292 | 101 | 191 | 9 | 0 | 0 | 2.7472855650570933 |
| 2 | 1 | INFLUX | 289 | 94 | 195 | 3 | 2 | -0.13118755261462395 | 19.556945304585284 |
| 3 | 1 | INFLUX | 293 | 93 | 200 | 0 | 0 | 0 | 19.556945304585284 |
| 4 | 1 | INFLUX | 283 | 83 | 200 | 0 | 0 | 0 | 19.556945304585284 |
| 5 | 1 | INFLUX | 282 | 86 | 196 | 3 | 1 | -0.0617333834832 | 13.861290585939964 |
| 6 | 1 | INFLUX | 282 | 82 | 200 | 0 | 0 | 0 | 13.861290585939964 |

Stop:

- endpoint: `POST /acdc/paper/stop/REM_CURRENT`;
- `executionId=1`;
- `currentBudget=13.861290585939964`;
- `net=-0.19292093609782393`.

## Telegram

Il notifier e' stato invocato sugli eventi PAPER effettivi.

Nel runtime locale non erano presenti:

- `HFT_TELEGRAM_BOT_TOKEN`;
- `HFT_TELEGRAM_CHAT_ID`.

Il path Vault storico non ha restituito:

- `hft.telegram.bot-token`;
- `hft.telegram.chat-id`.

Log atteso:

```text
[Telegram notifier] Disabled: missing hft.telegram.bot-token/HFT_TELEGRAM_BOT_TOKEN or hft.telegram.chat-id/HFT_TELEGRAM_CHAT_ID
```

La PAPER RUN non e' stata bloccata.

## Stato

ACDC ha raggiunto BUY e SELL su dati reali Influx usando ledger PAPER e guardie/config DB-driven.

Per ricevere fisicamente i messaggi Telegram serve valorizzare le stesse chiavi gia' supportate da HFT/ACDC nel runtime ACDC.
