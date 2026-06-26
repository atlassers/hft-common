# Sessione 5 - REM Preservation

Data: 2026-06-14.

## Obiettivo

Garantire che le azioni backoffice/FE non cancellino i parametri `REVERSAL_EVENT_MINING` quando preparano una PAPER run.

## Implementazione

- `BackofficeDbService.prepareBestWinnerRun` ora preserva:
  - `SCALPING_SCOUT`;
  - `REVERSAL_EVENT_MINING`.
- Aggiunto test in `BackofficeResourceTest` per verificare che una riga REM attiva sopravviva alla preparazione run.

## Verifiche

- `./mvnw test -Dtest=BackofficeResourceTest`
  - `16` test OK.

## Nota Operativa

Il comando FE `Avvia PAPER RUN` puo' convivere con firme REM gia' promosse. Resta aperto il problema del loop microbar lanciato da Kenshiro: il container non ha `python3`, quindi il miner REM deve essere eseguito da DocBrown o il container Kenshiro va esteso.
