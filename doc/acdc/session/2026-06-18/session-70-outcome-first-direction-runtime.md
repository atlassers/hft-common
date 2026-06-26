# Session 70 - Outcome-first Direction Runtime

Date: 2026-06-18

## Context

SHADOW 61 proved that the market was not flat:

- decisions: 3600
- confirmed reversals: 126
- positive direction candidates: 693
- confirmed positive reversals: 126
- reject reason: `REVERSAL_ML_RULE_MISSING`

The failure was not missing positive reversals. The failure was DocBrown not promoting usable advice for ACDC.

## Change

DocBrown no longer uses `reversal_direction_score` as a preliminary TRAIN/VALIDATION sample-count gate.

The corrected flow is:

1. build the outcome signature from historical GOOD/BAD samples;
2. validate the signature on temporal split;
3. compute advice economics from the outcome distribution;
4. add `reversal_direction_score >= 0` only to the persisted runtime ranges consumed by ACDC.

This keeps the scientific validation outcome-driven while preventing ACDC from consuming downward reversals.

## HTTP Fix

Manual DocBrown endpoint:

- `POST /docbrown/rem/research/{profileKey}/run`

now returns `409 Conflict` when a research job is already running, instead of a generic `500`.

Example payload:

```json
{
  "profileKey": "REM_CURRENT",
  "status": "RUNNING",
  "message": "REM research job already running",
  "timestamp": "2026-06-18T11:46:54.200083356Z"
}
```

## Verification

Manual DocBrown batch with scheduler disabled for isolation:

- generated at: `2026-06-18T11:53:38Z`
- scanned points: 5000
- outcome GOOD: 1241
- outcome BAD: 3759
- runtime promoted rules after batch: 1

Promoted rule:

- symbol: `ATOMUSDC`
- rule: `signature_reversal_pre_trough_drop`
- validation samples: 21
- validation profit rate: `0.904761904761904762`
- validation average net return: `0.004837480842317480`
- promotion class: `SIGNATURE_PAPER_ELIGIBLE`
- runtime direction range: `reversal_direction_score` min `0`, max `999999`

## Operational Note

DocBrown signal mode stayed `MANUAL`, so no PAPER was started automatically during verification.
