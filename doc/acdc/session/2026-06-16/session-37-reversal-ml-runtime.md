# Session 37 - Reversal ML Runtime

Date: 2026-06-16

## Goal

Model REM ML so each runtime execution can identify symbols that are near a negative-to-positive trend reversal, while keeping the strategy outcome-first and temporally validated.

## Implemented

- Added table `acdc_reversal_ml_rule`.
- Added Java runtime components:
  - `ReversalMlRule`;
  - `ReversalMlRuleRepository`;
  - `ReversalMlRuleReport`;
  - `ReversalMlRuleMiningService`.
- Added endpoint:
  - `POST /diagnostics/acdc/reversal-ml/{profileKey}/mine`.
- Replaced the old DocBrown-based `scripts/acdc-run-rem-ml.sh` with an ACDC HTTP pipeline:
  1. outcome mining from `binance-microbar`;
  2. reversal ML rule mining;
  3. combined report in `target/rem-ml/latest.json`.
- `OutcomeQualityModelService` now applies promoted ML rules at runtime and exposes:
  - `reversal_ml_score`;
  - `reversal_ml_profit_probability`;
  - `reversal_ml_rules`;
  - `reversal_ml_validation_samples`.
- REM parity diagnostics now include those features.

## Strategic Constraint

The ML miner does not promote entry-only thresholds. It builds interpretable multivariate rules from outcome training samples and promotes only rules that pass validation split checks:

- enough validation samples;
- validation profit rate above configured minimum;
- positive validation average net return after fee/slippage/dust-aware outcome labels.

## Verification

- `./mvnw -q test`: OK.
- Docker ACDC/MySQL/Influx:
  - V27 applicata su MySQL;
  - profilo 12h/5000 completato in circa 42s;
  - `scannedPoints=5000`;
  - `trainSamples=3488`;
  - `validationSamples=1512`;
  - `promotedRules=3`.
- Regole promosse:
  - `ALLOUSDC / early_rebound`;
  - `ALLOUSDC / acceleration_reversal`;
  - `BANKUSDC / early_rebound`.
- Parity live:
  - `dataSource=INFLUX`;
  - `snapshots=200`;
  - nessun BUY nei primi 20;
  - nessun match live delle regole ML promosse nei top rows.

## Fixes After First Run

- Outcome mining is now rolling-window:
  - samples not touched by the current run are marked `INACTIVE`;
  - old outcome signatures are reset before current promotion.
- Reversal ML mining resets old ML rule statuses before current promotion.
- TRAIN/VALIDATION split is recalculated from the actual observed sample range, not from the requested lookback window.
- Family taxonomy lookups are cached during mining to keep 5000-sample runs usable.
- HTTP script now uses `curl -f` so server errors cannot be persisted as JSON reports.

## Next Operational Step

Run the service in Docker, execute:

```bash
scripts/acdc-run-rem-ml.sh
```

Then verify:

```bash
curl -sS http://localhost:8091/diagnostics/acdc/rem/parity?limit=10
```

Only proceed to SHADOW/PAPER if promoted rules exist and live parity exposes positive `reversal_ml_*` / `outcome_quality_*` candidates.
