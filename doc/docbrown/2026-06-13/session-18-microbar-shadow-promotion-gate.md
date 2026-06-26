# Session 18 - Microbar shadow promotion gate

Date: 2026-06-13

## Goal

Validate whether the current microbar/scalping chain can produce a strategy worth promoting to PAPER/REAL using high-volume shadow runs instead of low-volume replay artifacts.

## Promotion rule

Every new strategy must pass a high-volume shadow run before PAPER or REAL promotion:

- real current data;
- preferably `binance-microbar` 5s for runtime entry/exit decisions;
- fee, stop, trailing, max hold and loss cap simulated;
- temporal split into train/validation/test/recent;
- no promotion if the profile only works on isolated trades or on one recent slice.

## Static threshold optimizer

Reusable script added:

- `python/docbrown_ml/scripts/run_microbar_shadow_optimizer.py`

The script loads `binance-microbar`, builds candidate entries for all USDC symbols, simulates 72 SELL policies and runs Optuna on ablation sets:

- `only_validation`
- `only_test`
- `only_recent`
- `validation_test`
- `validation_recent`
- `test_recent`
- `all_three`

The optimizer was extended to support:

- min/max feature bands, not only minimum thresholds;
- optional guard disablement per feature (`m1`, `m3`, `m6`, `m12`, `trend`, `dl`, `vr`, `fq`);
- explicit promotable profile only when validation, test and recent pass together.

## Results

### Snapshot 1

- range: `2026-06-13 12:01:20` to `2026-06-13 12:58:15`
- symbols: `288`
- raw points: `34,748`
- base entries: `25,820`
- shadow outcomes: `1,859,040`
- optimizer rows: `299,952`
- promotable profiles: `0`
- ablation profile counts: all `0`

### Snapshot 2

- range: `2026-06-13 12:06:05` to `2026-06-13 13:03:05`
- symbols: `288`
- raw points: `34,920`
- base entries: `25,992`
- shadow outcomes: `1,871,424`
- optimizer rows: `299,952`
- promotable profiles: `0`
- ablation profile counts: all `0`

### Bands + optional gate ablation

- range: `2026-06-13 12:10:55` to `2026-06-13 13:08:15`
- symbols: `288`
- raw points: `34,940`
- base entries: `26,012`
- shadow outcomes: `1,872,864`
- optimizer rows: `299,952`
- promotable profiles: `0`
- ablation profile counts: all `0`

## Diagnostics

No-filter SELL-policy baseline is strongly negative:

- best all-entry policy around `target=0.0025`, `stop=0.001`, `trail=0.002`, `hold=6`;
- `26,023` entries;
- all quote net about `-279 USDC`;
- recent quote net about `-83 USDC`;
- PF around `0.03`.

Oracle check shows opportunities exist if entry time is known in hindsight:

- top actual 2% by policy can produce about `+2.0` to `+2.9 USDC` per split;
- therefore the market contains profitable micro-moves;
- current static/runtime features do not identify them reliably out-of-sample.

ML diagnostics:

- Gradient boosting trained on early data can produce positive test/recent slices, but validation remains negative.
- Best non-walk-forward ML slice:
  - policy `(0.0025, 0.0025, 0.001, 12)`;
  - top quantile `0.995`;
  - validation `-0.2555 USDC`;
  - test `+0.1794 USDC`;
  - recent `+0.1388 USDC`.

Walk-forward diagnostic:

- best broad walk-forward:
  - policy `(0.006, 0.0025, 0.001, 12)`;
  - train `30m`, trade windows `5m`;
  - `96` trades;
  - quote net `+0.067 USDC`;
  - PF `1.0707`;
  - first window positive, later windows degrade.

Targeted walk-forward with regime kill:

- best targeted result:
  - policy `(0.006, 0.004, 0.002, 12)`;
  - train `30m`;
  - trade window `5m`;
  - top quantile `0.9975`;
  - `51` trades;
  - `24` wins, `27` losses;
  - quote net `+0.0581 USDC`;
  - PF `1.1501`;
  - average win `0.0034`;
  - average loss `-0.0026`;
  - window quote sequence `[-0.0045, +0.0615, +0.1176, -0.1166]`.

## Verdict

The current threshold strategy is not PAPER/REAL promotable.

The current ML/walk-forward evidence is directionally useful but too weak for REAL:

- it proves profitable opportunities exist;
- it proves static thresholds do not identify them;
- it suggests a short rolling model plus 5-minute regime kill can reduce damage;
- the observed edge is still too small and unstable for promotion.

Next technical path, if the project continues:

- implement a dedicated shadow-only rolling ranker pipeline;
- persist every shadow prediction and realized outcome;
- promote to PAPER only after repeated windows show PF materially above `1.5` and net profit positive after fees;
- use the FE only to start/stop PAPER/DRY/REAL runs.
