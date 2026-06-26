# Sessione 16 - REM Threshold Tuning e PAPER RUN

Data: 2026-06-15.

## Obiettivo

Rendere ACDC autonomo dai file Docker/VPN di HFT, usare il counterfactual per trovare soglie PAPER piu' adatte a generare profitto, e avviare una PAPER RUN.

## Docker/VPN

- Copiati i materiali VPN dentro `acdc/docker/vpn`.
- Il compose ora punta a:
  - `./proton`;
  - `./wireguard/wg0.conf`.
- I file sensibili/generati restano ignorati da git.

## Tuning

La execution counterfactual `12` ha mostrato che le soglie candidate-specific stavano scartando segnali profittevoli.

Scenario scelto:

- 43 accepted simulati;
- 43 would-hit-profit;
- 5 would-hit-stop;
- gross net max potenziale `0.65659039`.

Migration:

- `V14__relax_rem_entry_thresholds_from_counterfactual.sql`.

Soglie:

| Guard | Soglia |
| --- | --- |
| `momentum5` | `[-0.006, 0.0155]` |
| `momentum10` | `[-0.013, 0.021]` |
| `momentum15` | `[-0.014, 0.036]` |
| `trend` | `[-0.014, 0.036]` |
| `volume_ratio` | `>= 0.04` |
| `quote_volume_fast` | `>= 60` |
| `distance_from_low` | `[0, 0.036]` |
| `pullback_depth` | `[-0.019, 0.001]` |

## Freshness

Il primo step PAPER post V14 ha prodotto 10/10 reject per `ENTRY_STALE_MARKET`.

Motivo:

- snapshot validi da Docker/Influx osservati con `snapshot_age_seconds` 7-11;
- guard operativa precedente: 3s.

Migration:

- `V15__allow_influx_snapshot_latency.sql`;
- `entry_snapshot_fresh.max_threshold=15s`.

## PAPER Execution 13

Risultato:

- BUY: 12;
- SELL: 12;
- posizioni aperte residue: 0;
- `realizedProfitQuote=-0.585188224983442000`.

Motivi SELL osservati:

- `EXIT_ABSOLUTE_LOSS`: 2;
- `EXIT_FEE_RANGE_MAX_HOLD`: 3;
- `EXIT_MICRO_PROFIT_TAKE`: 2;
- `EXIT_QUOTE_LOSS_CAP`: 5.

Conclusione:

- ACDC ora arriva a BUY/SELL;
- il prossimo problema non e' piu' solo l'ENTRY, ma la qualita' netta di entry timing + SELL;
- questa e' PAPER esplorativa, non validazione REM definitiva outcome-first.

Nessuna REAL RUN avviata e nessun ordine Binance inviato.
