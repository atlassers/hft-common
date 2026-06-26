# Session 19 - Rolling ranker PAPER deadline attempt

Date: 2026-06-13

## Goal

Implement as much as possible before the 16:30 deadline to move from static thresholds to a rolling shadow ranker and, if current evidence allows it, start a PAPER run with profitable SELL behavior.

## Implementation

Added:

- `python/docbrown_ml/scripts/run_microbar_rolling_ranker.py`

The script:

- loads `binance-microbar`;
- reuses the microbar shadow matrix and SELL policy simulation;
- trains a `HistGradientBoostingRegressor` on rolling train windows;
- scores the next rolling trade window;
- selects only top predicted rows by quantile;
- applies a window-level kill threshold;
- outputs JSON with aggregate metrics, window metrics, selected symbols and a current decision candidate.

The script is intentionally shadow/offline. It does not yet make HFT execute the ML model directly.

## Best deadline findings

### Fast 75m diagnostic

- range: `2026-06-13 12:29:40` to `2026-06-13 13:41:50`
- best:
  - policy `(0.01, 0.004, 0.0005, 12)`
  - train `30m`
  - window `5m`
  - top quantile `0.995`
  - trades `31`
  - quote net `+0.0213 USDC`
  - PF `1.064`
- verdict: not PAPER-ready.

### Fast 60m diagnostic

- range: `2026-06-13 12:51:05` to `2026-06-13 13:48:20`
- best:
  - policy `(0.006, 0.0025, 0.001, 12)`
  - train `30m`
  - window `5m`
  - top quantile `0.9975`
  - trades `43`
  - quote net `+0.4337 USDC`
  - PF `2.0338`
  - avg win `0.010342`
  - avg loss `-0.002724`
- verdict: PAPER-ready at that snapshot.

### Fast 60m with symbol export

- range: `2026-06-13 12:53:15` to `2026-06-13 13:50:20`
- best:
  - policy `(0.01, 0.004, 0.002, 12)`
  - train `30m`
  - window `5m`
  - top quantile `0.995`
  - trades `48`
  - quote net `+1.0072 USDC`
  - PF `2.7880`
  - avg win `0.012980`
  - avg loss `-0.003939`
- verdict: strong PAPER candidate.
- selected symbols were exported per window.

### Current decision reruns

The later current reruns degraded:

- range ending `13:54:55`:
  - best `29` trades, quote net `+0.5667 USDC`, PF `3.3645`;
  - good quality, but did not pass the initial minimum trade count.
- range ending `13:56:20`:
  - best `15` trades, quote net `+0.1789 USDC`, PF `2.4222`;
  - avg win below avg loss;
  - not PAPER-ready.

## Operational decision

No PAPER was started from this iteration.

Reason:

- the ranker produced a strong candidate for a short period;
- the current decision degraded before activation;
- forcing PAPER after the kill/degradation would violate the new shadow-promotion discipline.

## Conclusion

The rolling ranker is a better path than static thresholds:

- it found windows with PF above `2`;
- it naturally exposed regime degradation;
- it gives a mechanism to avoid starting PAPER when the signal has already cooled down.

Next step:

- wire a scheduled shadow ranker cycle that writes short-lived `back_test.shadow_trading_decision` rows only when `currentDecision.ready=true`;
- then start PAPER from FE and let HFT buy only symbols with fresh DB shadow eligibility;
- keep validity very short, around one trade window (`5m` to `10m`).

## 2026-06-13 - Exchange-aware 45-50m retune

Issue found after the user challenged the current live filters:

- the exchange-aware replay was still reporting selected symbols with friction metadata from a fixed preferred policy;
- `quoteNet` was also still using a fixed `budget_per_trade` multiplier even when Binance constraints forced a larger per-symbol budget;
- this could overstate a profile and could also reject/promote current symbols against the wrong policy.

Fix applied:

- `run_microbar_rolling_ranker.py` now stores exchange metadata per policy;
- current symbol reporting uses metadata from the exact winning policy;
- replay window `quoteNet` uses the selected row budget when exchange-aware metadata exists;
- current decisions expose rejected symbols with explicit reasons:
  - `NON_POSITIVE_PREDICTION`;
  - `NO_FLOW_QUALITY`;
  - `NOT_TRADABLE_WITH_BUDGETS`;
  - `ENTRY_FRICTION_TOO_HIGH`.

Retune evidence:

- `60m`, budget `5.5,10,15,25`, friction share `0.70`:
  - corrected best: `13` trades, `6` wins, `7` losses, quote net `-0.0211`, PF `0.830`;
  - verdict: the earlier positive 60m result was not reliable after real budget/friction correction.
- `45m`, budget `5.5,10,15,25`, friction share `0.70`:
  - best snapshot: `26` trades, `17` wins, `9` losses, quote net `+0.6158`, PF `4.9597`;
  - later snapshot: `17` trades, quote net `+0.1005`, PF `1.4586`;
  - verdict: useful but volatile.
- `50m`, budget `5.5,10,15,25`, friction share `0.70`:
  - best snapshot: `27` trades, `16` wins, `11` losses, quote net `+0.6218`, PF `4.3017`;
  - current decision rejected all selected rows because prediction was negative, not because of friction.
- Relaxing friction to `1.00` or restricting budgets to `5.5,10` did not improve deployability:
  - `45m small`: quote net `+0.0830`, PF `1.319`, not paper-ready;
  - `50m share100`: quote net `-0.0315`, PF `0.570`, not paper-ready;
  - `50m small`: quote net `-0.1036`, PF `0`, not paper-ready.

Operational rule:

- do not start PAPER when the best replay is positive but `currentDecision.ready=false`;
- if rejected symbols are mostly `NON_POSITIVE_PREDICTION`, wait for the next slice instead of loosening frictions;
- the currently useful regime is around `45m-50m` max window, `30m` train, `5m` trade window, policy around `(target=0.006, stop=0.0025, trail=0.001, hold=12)`;
- start PAPER only when this family is replay-positive and the latest current slice has at least one positive, tradable symbol.

## 2026-06-13 - Short-window transfer audit and PAPER control

Fixes:

- adaptive windows in `run_microbar_rolling_ranker.py`:
  - `30m`: train `15/20`, trade `3/5`;
  - `20m`: train `10/15`, trade `2/3/5`;
  - `10m`: train `5/7`, trade `1/2/3`;
  - `5m`: train `2/3`, trade `1/2`.
- adaptive PAPER readiness:
  - short windows can promote with fewer trades;
  - short windows require stronger PF and avg win at least `2x` avg loss.
- transfer audit added:
  - model gates;
  - exchange gates;
  - HFT tick/dust/loss-cap gates;
  - HFT recent PAPER loss cooldown gate.

Shadow sweep:

- `5m`: not enough usable entries, `profilesEvaluated=0`;
- `10m`: evaluated, best `1` trade, loss, not ready;
- `20m`: first interesting short-window edge:
  - before persist: `4` trades, `3` wins, `1` loss, quote net `+0.1931`, PF `7.66`;
  - persist run: `12` trades, `6` wins, `6` losses, quote net `+0.1270`, PF `3.00`;
  - profile: train `10m`, trade window `2m`, policy `(0.006, 0.004, 0.002, 12)`, top quantile `0.9925`;
  - current symbols: `NEIROUSDC`, `MEGAUSDC`;
  - shadow decisions persisted: ids `3006`, `3007`, valid until `2026-06-13 16:20:47 UTC`.
- `30m`: evaluated, negative;
- `45m`: had a transient current-ready `GPSUSDC`, but degraded before persistence;
- `50m`: replay stronger but current empty, not promoted.

PAPER control:

- HFT params promoted:
  - `NEIROUSDC`: `stan_strategy_parameters.id=2942`;
  - `MEGAUSDC`: `stan_strategy_parameters.id=2943`;
  - allowlist `NEIROUSDC,MEGAUSDC`;
  - quote budget floor `5.5`.
- PAPER started via FE at `2026-06-13 16:15:57`.
- Results:
  - `NEIROUSDC` BUY at `16:16:03`, SELL at `16:16:31`;
  - micro profit take triggered;
  - net profit `+0.00673356 USDC`;
  - `MEGAUSDC` blocked by `BUY_CHECK_RECENT_LOSS_COOLDOWN_REJECTED` because of earlier PAPER loss.
- PAPER stopped via FE after the control result.

Interpretation:

- The shadow -> DB -> FE PAPER -> BUY -> SELL chain can produce positive trades when the current slice is fresh.
- The infrastructure was not fully aligned: a real HFT cooldown gate was not represented in the shadow transfer audit and blocked `MEGAUSDC`.
- The fixed `trades>=15` readiness gate was too rigid for short-window models; adaptive readiness is now in place.
- Next required improvement is to make promotion exclude symbols already in HFT cooldown before writing active params.

## 2026-06-13 - Environment cleanup and post-cleanup shadow validation

Cleanup:

- MySQL:
  - deleted obsolete inactive ML `SCALPING_SCOUT` params older than 12h: `2531`;
  - deleted expired old PAPER shadow decisions: `2270`;
  - preserved all paper/real trade evidence, closed positions, paper runs, events, and replay material.
- Docker:
  - pruned stopped containers;
  - pruned old build cache;
  - pruned unused images;
  - verified active containers remained up after cleanup.
- Logs/artifacts:
  - truncated large generated runtime logs under `target`;
  - removed old generated shadow JSON artifacts while keeping recent diagnostics.

Post-cleanup state:

- open positions: `0`;
- active strategy params: `0`;
- active best-winner runtime processes: `0`.

Fresh shadow validation:

- `10m`: not promotable, one negative simulated trade and recent-loss cooldown in transfer audit;
- `20m`: not promotable, one negative simulated trade;
- `30m`: not promotable, `4` trades, quote net `-0.04984810`, PF `0.129`;
- `45m`: not promotable, `7` trades, quote net `-0.12213880`, PF `0.136`;
- `60m`: not promotable, `8` trades, quote net `-0.33242668`, PF `0`.

Operational decision:

- PAPER was not started.
- Reason: no fresh shadow run produced both a positive replay and a valid current decision.
- Starting PAPER with zero active symbols would validate only orchestration, not the trading strategy.
