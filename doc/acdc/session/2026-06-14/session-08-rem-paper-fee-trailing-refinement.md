# Sessione 08 - REM PAPER Fee e Trailing Refinement

Data: 2026-06-14.

## Obiettivo

Massimizzare le REM il prima possibile correggendo i due falsi negativi emersi:

- fee PAPER contate in modo troppo conservativo;
- loss cap che vendeva anche a prezzo invariato per sola frizione.

## Modifiche

### V7

Migrazione: `V7__align_paper_fee_and_trailing.sql`.

- Trailing ora usa:
  - `min_threshold=0.0025` come profit arm;
  - `max_threshold=0.0020` come giveback.
- `PaperSizingService.sellableQuantity()` non sottrae piu' fee dalla quantita' quando la fee e' contabilizzata in quote.

### V8

Migrazione: `V8__use_price_loss_for_quote_loss_cap.sql`.

- `exit_quote_loss_cap` passa da `net_loss_quote` a `price_loss_quote`.
- `price_loss_quote` viene calcolato come perdita prezzo pura:
  - `max(0, buyQuote - sellQuote)`;
  - esclusi fee e frizione.

## Verifiche

Comandi:

```bash
./mvnw -q test
./mvnw -q package -DskipTests
```

Esito: OK.

## Run MySQL/Inﬂux

V7:

- execution `3`;
- perdita ridotta rispetto alla run precedente;
- ma le chiusure negative erano ancora `EXIT_QUOTE_LOSS_CAP` a prezzo invariato.

V8:

- execution `4`;
- step 1: `opened=8`, `closed=0`, `current=7.752285515221147`, `reserved=92.24771448477885`;
- step 2/3: nessuna chiusura fee-only;
- step 4: `closed=1`, `net=-0.052607253902775`;
- step 11: `closed=1`, `net=-0.13523890772`;
- step 12: `closed=1`, `net=-0.0297697779761744`;
- stop: `current=10.85774437234658`, `reserved=88.92463968805447`, `realized=-0.2176159395989494`.

## Diagnosi

La perdita residua non e' piu' dovuta a fee-only.

SELL negative:

- `1000CHEEMSUSDT`: `EXIT_QUOTE_LOSS_CAP`;
- `0GUSDT`: `EXIT_ABSOLUTE_LOSS`;
- `1MBABYDOGEUSDT`: `EXIT_ABSOLUTE_LOSS`.

Open position armate:

- `1000CATUSDT max_net_return=0.005802721088435374`;
- `1000SATSUSDC max_net_return=0.003264392324093817`;
- `DGBUSDT max_net_return=0.003000000000000000`.

Conclusione:

- REM candidate vengono identificate e alcune arrivano sopra profit arm.
- Nella finestra osservata non e' ancora arrivato retrace sufficiente per trailing SELL positiva.
- Il prossimo tuning deve agire su policy risk/exit DB, non su fee accounting.
