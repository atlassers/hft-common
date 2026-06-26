# Session 28 - SHADOW Clean Post Fix

Data: 2026-06-16

## Obiettivo

Chiudere eventuali execution di oggi e avviare una nuova SHADOW con il codice aggiornato e funzionante.

## Stato iniziale

- Nessuna execution `RUNNING`.
- Execution `28` e `29` gia' `COMPLETED`.

## SHADOW

- Execution: `30`.
- Run: `92`.
- Data source: `INFLUX`.
- BUY iniziali:
  - `BIGTIMEUSDC`;
  - `LAYERUSDC`;
  - `JTOUSDC`.
- Stop-buy applicato: `2026-06-16 08:22:00`.
- `PYTHUSDC` e' stata aperta alle `08:21:55`, quindi prima dello stop-buy effettivo.

## Drain

- Dopo `buy_stopped_at`, i tick scheduler hanno sempre `opened=0`.
- SELL:
  - `JTOUSDC`: perdita;
  - `LAYERUSDC`: `EXIT_MICRO_PROFIT_TAKE`, profitto;
  - `BIGTIMEUSDC`: `EXIT_FEE_RANGE_MAX_HOLD`, perdita fee;
  - `PYTHUSDC`: `EXIT_FEE_RANGE_MAX_HOLD`, perdita fee.

## Risultato

- Stato finale: `COMPLETED`.
- `reservedBudget=0`.
- `currentBudget=100.084757950322200000`.
- `realizedProfitQuote=+0.084757950322200000`.

## Nota

- La SHADOW post-fix e' positiva.
- Il processo stop-buy/drain si e' comportato correttamente.
- E' riapparso un log `Telegram notifier Notification unsent` sulla SELL di `LAYERUSDC`; Telegram resta da ricontrollare.
