# Session 13 - Counterfactual PAPER Run

Data: 2026-06-14

## Obiettivo

Capire se i reject prodotti dalle soglie candidate-specific sono utili o se stanno bloccando trade profittevoli.

## Implementazione

- Aggiunta migration `V13__add_counterfactual_entry_evaluation.sql`.
- Aggiunta tabella `acdc_counterfactual_entry_evaluation`.
- Ogni decisione ENTRY PAPER salva:
  - simbolo;
  - accepted/rejected;
  - reason;
  - prezzo;
  - feature JSON;
  - execution;
  - stato `PENDING`.
- Aggiunto job counterfactual:
  - legge prezzi futuri da Influx;
  - calcola max/min/end return;
  - sottrae fee round-trip stimata;
  - marca would-hit-profit e would-hit-stop.
- Endpoint:
  - `POST /acdc/counterfactual/evaluate/{executionId}?horizonSeconds=600`;
  - `GET /acdc/counterfactual/report/{executionId}?horizonSeconds=600`.

## PAPER RUN

- Execution: `12`.
- Durata: 15 step, uno al minuto.
- Nessuna BUY aperta.
- Nessun USDT.
- Execution fermata a fine test.

## Job Al Minuto 10

- Richiesto ed eseguito allo step 10.
- Risultato:
  - `evaluated=0`;
  - `waiting=26`;
  - `noData=0`.
- Motivo: nessuna decisione aveva ancora 600 secondi completi di futuro disponibile.

## Job Finale

- Dopo stop execution:
  - `evaluated=10`;
  - `waiting=39`;
  - `noData=0`.

Aggregato sui reject maturi:

| Reason | Decisions | Would Profit | Would Stop | Avg Net Max | Avg Net End | Missed Net |
|---|---:|---:|---:|---:|---:|---:|
| `ENTRY_MOMENTUM5_OUT_OF_BAND` | 6 | 6 | 0 | 0.01345465 | 0.01168252 | 0.08072789 |
| `ENTRY_MOMENTUM10_OUT_OF_BAND` | 4 | 4 | 0 | 0.01825699 | 0.01419589 | 0.07302796 |

## Verdetto

Preliminare ma chiaro: nella finestra osservata, le soglie candidate-specific su `momentum5` e `momentum10` sono troppo restrittive.

Il campione maturo e' piccolo; le 39 righe ancora `PENDING` vanno rivalutate dopo maturazione dell'orizzonte completo.

## Verifiche

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- V13 applicata su MySQL.

## Note

- Nessuna REAL RUN avviata.
- Nessun ordine Binance inviato.
