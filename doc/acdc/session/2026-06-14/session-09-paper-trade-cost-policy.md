# Sessione 09 - PAPER Trade Cost Policy

Data: 2026-06-14.

## Obiettivo

Impostare il costo della trade PAPER come massimo tra:

- `25 USDC`;
- `5%` del budget libero.

## Implementazione

Migrazione:

- `V9__set_paper_trade_cost_policy.sql`.

Nuove colonne:

- `acdc_paper_runtime_config.min_trade_quote`;
- `acdc_paper_runtime_config.trade_budget_percent`.

Regola:

```text
trade_quote = max(min_trade_quote, currentBudget * trade_budget_percent)
```

Se `trade_quote > currentBudget`, la posizione non viene aperta.

Config attuale:

- `min_trade_quote=25`;
- `trade_budget_percent=0.05`.

## Verifica

Comandi:

```bash
./mvnw -q test
./mvnw -q package -DskipTests
./mvnw -q package -DskipTests -Dquarkus.profile=local
```

Esito: OK.

## Run MySQL

V9 applicata su MySQL `hft`.

PAPER execution `5`:

- `opened=3`;
- `current=24.92500003314311`;
- `reserved=75.0749999668569`.

Buy quote:

| Symbol | buy_quote | buy_fee_quote | reserved |
| --- | ---: | ---: | ---: |
| 0GUSDT | 24.999999976000000000 | 0.024999999976000000 | 25.024999975976000000 |
| 1000CATUSDT | 24.999999998610000000 | 0.024999999998610000 | 25.024999998608610000 |
| 2ZUSDC | 24.999999992280000000 | 0.024999999992280000 | 25.024999992272280000 |

La quarta apertura non avviene perche' il budget libero e' sotto `25`.

Nessuna REAL RUN e nessun ordine Binance.
