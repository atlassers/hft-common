# Session 04 - WireGuard V47 PAPER validation

Date: 2026-06-07 19:25 CEST

## Scope

Validate the DB-backed profit-factor cap after the WireGuard VPN switch and run a conservative 48h PAPER optimization without starting REAL trading.

## Runtime State

- HFT Flyway version: `47`
- `profit_factor_no_loss_cap`: `5.00000000`
- `real_buy_enabled`: `0`
- `centroid_filter_enabled`: `1`
- `centroid_filter_apply_to_real`: `0`
- VPN: `proton-vpn` healthy, HFT reachable through `hft-vpn`

## Command

```bash
PYTHONPATH=python python3 python/docbrown_ml/scripts/run_scalping_scout_candidate_dataset.py \
  --config target/runtime/current-binance.yml \
  --build-dataset \
  --build-sell-policy-outcomes \
  --train-thresholds \
  --train-sell-policy-thresholds \
  --output target/sell_policy_run_6_wireguard_v47.json
```

## Dataset Result

- Dataset run id: `15`
- Run UUID: `8e07eaa1-9d16-4e6e-a178-33ad875e5052`
- Symbols requested: `289`
- Symbols loaded: `281`
- Observations generated: `3,753`
- Simulated wins: `731`
- Simulated losses: `3,022`
- Base win rate: `19.4778%`
- Simulated net return: `-20.014987`

## BUY Threshold Result

- Threshold run id: `9`
- Status: `WATCH_SAMPLE`
- Params:
  - `trendMin=0.00601566`
  - `fastQuoteVolumeMin=2583.0322628`
  - `replayNetReturnMin=-0.01724638`
  - `replayTradesMin=0`
  - `failScoreBlockThreshold=1`
- Train: sample `9`, win-rate `55.56%`, EV `-0.00586255`, PF `0.158027`, net `-0.05276299`
- Test: sample `1`, win-rate `100%`, EV `0.00021286`, PF `5.0`, net `0.00021286`
- All: sample `10`, win-rate `60%`, EV `-0.00525501`, PF `0.161423`, net `-0.05255013`

## SELL Policy Result

- Policies evaluated: `300`
- Outcomes generated: `1,125,900`
- Outcome wins: `187,370`
- Outcome losses: `938,530`
- Outcome win rate: `16.6418%`
- Outcome net return: `-5903.993145`
- Ready count: `0`
- Status counts: `WATCH_SAMPLE=300`
- Best policy for dataset `15`: `pt=0.008|sl=0.004|hold=5|grace=1`
- Best policy all metrics: sample `8`, win-rate `50%`, EV `-0.00171637`, PF `0.533531`

## Conclusion

The V47 cap is working: no no-loss policy receives an artificial `99` profit factor. The run remains fail-closed. There is no READY policy and no robust profitable pattern; the best candidates are tiny-sample overfits with negative all-sample expected value.

No REAL RUN was started.
