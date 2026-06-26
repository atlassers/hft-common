# Sessione 20 - Reversal Event Mining

Data: 2026-06-14.

## Obiettivo

Implementare il miner outcome-first dei reversal:

- cerca punti in cui una BUY avrebbe prodotto guadagno netto;
- misura MFE, MAE, drawdown e tempo di uscita;
- etichetta `GOOD_REVERSAL`, `BAD_REVERSAL`, `NEUTRAL_REVERSAL`;
- genera firme per simbolo/famiglia/globale;
- promuove a DB solo firme validate;
- imposta trailing dinamico e loss exit nei parametri HFT.

## Implementazione

- Aggiunto `python/docbrown_ml/reversal_event_mining.py`.
- Aggiunto runner `python/docbrown_ml/scripts/run_reversal_event_miner.py`.
- Aggiunti test `python/tests/test_reversal_event_mining.py`.
- Persistenza DB:
  - `hft.reversal_event_mining_event`;
  - `hft.reversal_event_mining_signature`;
  - promozione in `hft.stan_strategy_parameters`.

## Policy Promossa

Ogni firma REM promossa contiene:

- `entry_gate_profile=REVERSAL_EVENT_MINING`;
- `dynamic_profit_trailing=true`;
- `profit_trailing_enabled=true`;
- `profit_arm_threshold=0`;
- `profit_floor_after_arm=0`;
- `profit_trailing_drawdown=0.001`;
- `absolute_loss_stop=0.004`;
- `max_net_loss_quote=0.045`;
- `max_loss_cap_friction_quote=0.045`;
- `skip_model_gate=true`;
- `live_candidate_degradation_gate_enabled=false`;
- `ignore_recent_loss_cooldown=true`;
- `ignore_recent_close_cooldown=true`;
- `entry_microbar_seconds=5`.

## Shadow E Promozioni

- Finestra 15m:
  - `ALLOUSDC`;
  - `4/4` win;
  - net return `0.03185524377345873`;
  - avg return `0.007963810943364683`;
  - profit factor `999`;
  - promosso a DB.
- Finestra 10m:
  - `165` eventi;
  - tutti `BAD_REVERSAL`;
  - nessuna promozione.
- Finestra 5m:
  - zero eventi/tick sufficienti;
  - nessuna promozione.

## Fix Di Robustezza

- `visible_profit_target` ora copre fee round-trip e slippage.
- `volume_ratio` e `quote_volume_fast` sono min-only, senza upper cap.
- La soglia minima di liquidita' ha tolleranza microbar:
  - `volume_ratio_min = min_good * 0.75`;
  - `quote_volume_fast_min = min_good * 0.60`.
- Le condizioni supportano bande senza `max`.
- I cooldown legacy vengono bypassati per REM tramite parametri DB.

## Verifiche

- `PYTHONPATH=python python3 -m unittest python.tests.test_reversal_event_mining -v`
  - `3` test OK.

## Nota Operativa

La strategia e' profittevole solo se le firme vengono rinnovate quando il reversal e' ancora vivo. Se la promozione arriva tardi, HFT blocca correttamente su momentum/trend/volume e non compra.

## Containerizzazione Corretta

- Il runner REM deve girare in DocBrown, non in Kenshiro.
- Fix applicato a `run_reversal_event_miner.py`:
  - non sostituisce piu' `host.docker.internal` con `localhost`;
  - rispetta `INFLUXDB_URL`/`INFLUX_URL` passati dal container.
- Immagine `docbrown-real-refresh:latest` rebuildata.
- Shadow containerizzata validata:
  - comando via `docker run docbrown-real-refresh:latest ... run_reversal_event_miner.py --dry-run --minutes 15`;
  - `ALLOUSDC`, `4/4` win;
  - net return `0.032831712423874185`;
  - avg return `0.008207928105968546`;
  - profit factor `999`;
  - condizioni principali:
    - `momentum15` tra `0.0024604364064823247` e `0.004681231274725172`;
    - `momentum10` tra `-0.00036997885835095` e `0.00406976744186045`;
    - `momentum5` tra `-0.0026586359729183886` e `0.0008164618774117005`;
    - `trend` tra `0.0017271312217326696` e `0.0031873893220188522`;
    - `quote_volume_fast >= 25.004903999999996`.

## Fix Promozione Multi-Finestra E Simboli Non Tradabili

- Problema osservato in PAPER:
  - il loop lanciava finestre `10,15,30`;
  - ogni promozione REM disattivava tutte le firme REM attive;
  - quindi l'ultima finestra cancellava i simboli promossi dalle finestre precedenti.
- Fix:
  - `promote_signatures` ritira solo:
    - firme REM scadute (`valid_until <= CURRENT_TIMESTAMP(6)`);
    - firme attive dello stesso simbolo che viene ripromosso;
  - non esiste piu' un replace globale del set REM a ogni sotto-finestra.
- Aggiunto filtro simboli:
  - accetta solo pattern Binance-like `^[A-Z0-9]+USDC$`;
  - evita firme tipo `币安人生USDC` lette da Influx ma non tradabili da HFT.
- Verifica:
  - `PYTHONPATH=python python3 -m unittest python.tests.test_reversal_event_mining` -> `5` test OK.

## Filtro Economico Pre-Promozione

- Problema osservato dopo tuning PAPER:
  - diversi candidati REM passavano i gate live, ma HFT li respingeva con `BUY_CHECK_LOSS_CAP_FEASIBILITY_REJECTED`;
  - la frizione minima stimata da fee, arrotondamento quantita' e un tick contro era circa `0.026 USDC`, sopra il cap operativo stretto `0.018 USDC`;
  - allentare ancora i gate avrebbe aumentato il rischio senza migliorare la tradabilita' economica.
- Fix DocBrown:
  - le firme REM salvano `representative_entry_price`, mediana dei prezzi degli eventi `GOOD_REVERSAL`;
  - prima della promozione a `stan_strategy_parameters`, il runner legge `tickSize`, `stepSize`, `minQty`, `minNotional` da Binance `exchangeInfo`;
  - simula una round-trip piatta con fee, rounding e un tick avverso;
  - promuove solo firme con `lossCapFrictionQuote <= max_loss_cap_friction_quote`/`max_net_loss_quote`;
  - scrive `reversal_entry_friction_audit` e `loss_cap_friction_quote` nel `params_json` promosso.
- Vincolo charter:
  - il filtro non sostituisce le firme outcome-first;
  - agisce solo come filtro di fattibilita' economica prima della promozione DB.
- Verifiche:
  - `PYTHONPATH=python python3 -m unittest python.tests.test_reversal_event_mining -v` -> `6` test OK;
  - `python3 -m py_compile` sui file REM modificati -> OK;
  - full suite DocBrown `PYTHONPATH=python python3 -m unittest discover -s python/tests -v` -> non OK per failure/error legacy non REM: `test_best_winner_window_model` attende 1 firma ma ne riceve 3; `test_scalping_parameter_optimizer` manca `symbol_allowlist` nei fixture `ScoutConfig`.
