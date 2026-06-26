# Session 30 - Cost-Aware SELL Replay

Data: 2026-06-16

## Obiettivo

- Ridurre SELL sotto soglia.
- Rendere loss cap e absolute loss consapevoli di fee/slippage/grace.
- Rigiocare la SHADOW `executionId=32` per verificare se i nuovi parametri avrebbero prodotto profitto.

## Modifiche

- Aggiunto operatore `COST_AWARE_QUOTE_LOSS_CAP_EXIT`.
- `exit_quote_loss_cap` ora usa:
  - `market_price_loss_quote = 0.075`;
  - `loss_grace_seconds = 90`;
  - `cost_multiplier = 1.25`;
  - `slippage_rate = 0.0005`;
  - `catastrophic_quote_loss = 0.300`.
- `exit_absolute_loss` ora usa:
  - `loss_grace_seconds = 90`;
  - `absolute_loss_net_return = -0.006`;
  - `catastrophic_net_return = -0.012`.
- Aggiunte feature EXIT:
  - `buy_quote`;
  - `buy_fee_quote`;
  - `sell_fee_quote`;
  - `round_trip_fee_quote`.
- Aggiunto endpoint:
  - `GET /diagnostics/acdc/shadow/{executionId}/replay?horizonSeconds=900`.

## Replay

- Run originale `executionId=32`:
  - net `-0.360614300558090000`.
- Replay con nuovi parametri, horizon `900s`:
  - net `-0.301959185666810044`.
- Replay con nuovi parametri, horizon `1800s`:
  - net `-0.301959185666810044`.
- Test temporaneo senza `exit_fee_range_max_hold`, horizon `1800s`:
  - mark-to-market circa `-0.7746847854782`;
  - variante scartata;
  - `exit_fee_range_max_hold` ripristinato `ACTIVE`.

## Esito

- La SELL e' meno prematura e recupera circa `0.058655 USDC` sulla run.
- Non basta a invertire il trend.
- Le perdite residue sono soprattutto BUY che non sviluppano edge netto sufficiente; diversi `replayMaxNetReturn` restano `0`.
- Prossima direzione: tuning scientifico di ENTRY/ranking, escludendo candidati con profilo simile ai loss replay o pesando il ranking su probabilita' di maxNetReturn sopra fee.
