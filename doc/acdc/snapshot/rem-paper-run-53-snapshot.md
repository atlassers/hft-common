# REM PAPER Run 53 Snapshot

Data snapshot: 2026-06-17.

Obiettivo: congelare codice, parametri, configurazioni e soglie della prima PAPER profittevole con pipeline `Band Discovery -> Signal Mining -> ACDC Trading Runtime`, cosi' da poter tornare a questo stato se futuri esperimenti peggiorano il risultato.

## Git Tags

| Servizio | Tag | Commit |
| --- | --- | --- |
| ACDC | `rem-paper-run-53-acdc` | `a97bd9b4155a265e4e0ccde7d88e7a42b14aba90` |
| DocBrown | `rem-paper-run-53-docbrown` | `4cd6923e4a4d6d6392dc3add6fe282c32bc562c1` |
| HFT charter | `rem-paper-run-53-hft` | `cbca59337d2c5904f621fe3514f9359203d2ce9e` |

## Run

| Campo | Valore |
| --- | --- |
| execution_id | `53` |
| run_type | `PAPER` |
| status | `COMPLETED` |
| started_at | `2026-06-17 15:40:26` |
| completed_at | `2026-06-17 15:51:59` |
| buy_stopped_at | `2026-06-17 15:43:38` |
| initial_budget | `100.000000000000000000` |
| final_budget | `100.384220124584400000` |
| realized_profit_quote | `0.384220124584400000` |
| ROI | `0.3842201245844%` |

## Positions

| Symbol | Opened | Closed | Exit | Buy Quote | Sell Quote | Net Profit | Max Net Return |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: |
| `OPGUSDC` | `15:40:30` | `15:44:04` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `24.999999869700000000` | `25.343811262800000000` | `0.293467581967500000` | `0.011738703339882122` |
| `CATIUSDC` | `15:40:30` | `15:51:59` | `EXIT_ML_ADVICE_TIMEOUT` | `24.999999968000000000` | `24.999999968000000000` | `-0.049999999936000000` | `0.000000000000000000` |
| `FOGOUSDC` | `15:40:30` | `15:42:18` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `24.999999988740000000` | `25.115207361960000000` | `0.065092165869300000` | `0.002603686635944700` |
| `OPENUSDC` | `15:42:25` | `15:50:12` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `24.999999777000000000` | `25.125785939400000000` | `0.075660376683600000` | `0.003026415094339623` |

Aggregate:

- closed trades: `4`;
- wins/losses: `3/1`;
- gross wins: `0.434220124520400000`;
- gross loss: `0.049999999936000000`;
- profit factor: `8.684403`;
- average net return per trade: `0.00384220`;
- best MFE: `0.01173870`.

## Band Model

| Campo | Valore |
| --- | --- |
| model_version | `REM-BAND-2026-06-17T153958.141908433-e6fd6049` |
| status | `ACTIVE` |
| coverage_min | `0.000000000000000000` |
| coverage_max | `1.000000000000000000` |
| distinct_price_points_min | `1` |
| max_gap_seconds | `60` |
| high_volume_min | `1.500000000000000000` |
| min_match_ratio | `0.500000000000000000` |
| train_samples | `1164` |
| validation_samples | `3836` |
| validation_pass_samples | `3836` |
| validation_good_pass | `2369` |
| validation_bad_pass | `1467` |
| validation_precision_good | `0.617570385818561001` |
| validation_recall_good | `1.000000000000000000` |
| validation_avg_mfe | `0.002585363029157435` |
| score | `2.493677074552865502` |

Interpretazione: questa band ha massimizzato recall e precision sufficiente, ma e' molto larga. E' un buon baseline operativo, non ancora una band selettiva fine.

## Promoted Rule

| Campo | Valore |
| --- | --- |
| scope_type | `FAMILY` |
| scope_key | `UNKNOWN` |
| rule_key | `acceleration_reversal` |
| status | `PROMOTED` |
| promotion_mode | `PURE_REVERSAL` |
| promotion_class | `PAPER_ELIGIBLE` |
| score | `0.340953453453453420` |
| train_samples | `573` |
| validation_samples | `666` |
| train_profit_rate | `0.062827225130890052` |
| validation_profit_rate | `0.620120120120120120` |
| validation_avg_net_return | `0.002709706780505813` |
| band_model_id | `1` |

Rule ranges:

| Feature | Min | Max |
| --- | ---: | ---: |
| `reversal_slope_delta` | `-0.005772005772005772` | `0.005479452054794521` |
| `reversal_acceleration` | `-0.002612671456564337` | `0.00406916330687061` |
| `reversal_trough_age_seconds` | `15` | `230` |
| `reversal_data_coverage_ratio` | `0.0` | `1.0` |
| `reversal_distinct_price_points` | `1` | `999999` |
| `reversal_max_gap_seconds` | `0` | `60` |

Advice:

| Campo | Valore |
| --- | --- |
| advice_valid_from | `2026-06-17 15:40:23` |
| advice_valid_until | `2026-06-17 15:44:23` |
| entry_validity_seconds | `240` |
| validity_source | `delayed_entry_outcome_distribution` |
| duration_seconds | `680` |
| safe_net_return | `0.002208333333333333` |
| max_net_return | `0.007662177328843996` |
| loss_cap_net_return | `-0.003555658028220972` |

Economics gates:

| Config | Valore |
| --- | --- |
| `rem.ml.advice.min.safe_net_return` | `0.0020` |
| `rem.ml.advice.min.max_net_return` | `0.0040` |
| `rem.ml.advice.min.profit_probability` | `0.60` |
| `rem.ml.min.validation.profit_rate` | `0.58` |
| `rem.ml.min.validation.samples` | `12` |

## Runtime Configuration

Run mode budget:

| Run Type | Status | Default Budget | Budget Source |
| --- | --- | ---: | --- |
| `DRY` | `ACTIVE` | `100.000000000000000000` | `SIMULATED_DEFAULT` |
| `SHADOW` | `ACTIVE` | `100.000000000000000000` | `SIMULATED_DEFAULT` |
| `PAPER` | `ACTIVE` | `100.000000000000000000` | `SIMULATED_DEFAULT` |
| `REAL` | `DISABLED` | `0.000000000000000000` | `BINANCE` |

Universe:

| Config | Valore |
| --- | --- |
| allowed_quote_assets_csv | `USDC` |
| candidate_source | `INFLUX_USDC_MICROBAR` |
| max_candidates | `200` |

ML data settings:

| Config | Valore |
| --- | --- |
| `rem.ml.lookback.hours` | `12` |
| `rem.ml.horizon.seconds` | `900` |
| `rem.ml.sample.every.seconds` | `60` |
| `rem.ml.symbol.limit` | `288` |
| `rem.ml.max.samples` | `5000` |
| `rem.ml.validation.percent` | `30` |
| `rem.ml.promotion.mode` | `PURE_REVERSAL` |
| `rem.ml.live_audit.limit` | `200` |
| `rem.ml.live_audit.min.samples` | `1` |
| `rem.ml.live_audit.max.zero_mfe_loss_rate` | `0.34` |
| `rem.ml.live_audit.max.under_safe_non_profit_rate` | `0.34` |
| `rem.ml.live_audit.penalty.weight` | `1.0` |

## Active Entry Guards

| Guard | Feature | Operator | Min | Max |
| --- | --- | --- | ---: | ---: |
| `entry_price_present` | `price` | `PRESENT` | | |
| `entry_snapshot_fresh` | `snapshot_age_seconds` | `LTE` | | `15` |
| `reversal_ml_rules_min` | `reversal_ml_rules` | `GTE` | `1` | |
| `entry_reversal_ml_score_positive` | `reversal_ml_score` | `GTE` | `0` | |
| `entry_ml_advice_paper_eligible` | `ml_advice_paper_eligible` | `GTE` | `1` | |

Disabled ENTRY guards at this snapshot:

- `entry_reversal_data_coverage_min`;
- `entry_reversal_distinct_price_points_min`;
- `entry_reversal_microbar_gap_max`;
- `entry_reversal_volume_confirmation_min`;
- `reversal_outcome_quality_score_min`;
- `reversal_outcome_quality_probability_min`;
- `reversal_outcome_quality_samples_min`.

## Active Exit Guards

| Guard | Feature | Operator |
| --- | --- | --- |
| `exit_ml_advice_take_profit` | `net_return` | `ML_ADVICE_TAKE_PROFIT_EXIT` |
| `exit_ml_advice_loss_cap` | `net_return` | `ML_ADVICE_LOSS_CAP_EXIT` |
| `exit_ml_advice_timeout` | `hold_seconds` | `ML_ADVICE_TIMEOUT_EXIT` |

## Ranking

| Rank | Feature | Weight | Direction |
| --- | --- | ---: | --- |
| `rank_reversal_ml_score` | `reversal_ml_score` | `100` | `DESC` |
| `rank_ml_advice_safe_net_return` | `ml_advice_safe_net_return` | `2000` | `DESC` |
| `rank_ml_advice_max_net_return` | `ml_advice_max_net_return` | `1000` | `DESC` |
| `rank_reversal_ml_profit_probability` | `reversal_ml_profit_probability` | `20` | `DESC` |
| `rank_reversal_data_coverage` | `reversal_data_coverage_ratio` | `5` | `DESC` |
| `rank_reversal_volume_confirmation` | `reversal_volume_confirmation` | `0.5` | `DESC` |
| `rank_reversal_microbar_gap` | `reversal_max_gap_seconds` | `0.02` | `ASC` |

## BUY Timing In This Run

| Symbol | BUY Time | Seconds From `advice_valid_from` | Outcome |
| --- | --- | ---: | --- |
| `OPGUSDC` | `15:40:30` | `~7s` | take-profit, `+0.293467581967500000` |
| `CATIUSDC` | `15:40:30` | `~7s` | timeout, `-0.049999999936000000` |
| `FOGOUSDC` | `15:40:30` | `~7s` | take-profit, `+0.065092165869300000` |
| `OPENUSDC` | `15:42:25` | `~122s` | take-profit, `+0.075660376683600000` |

Observation: in this run the late BUY inside the validity window (`OPENUSDC`, roughly halfway through the 240s entry-validity window) still produced profit. This single run is not enough to conclude that late entry is always safe.

## Validity Semantics In Current Code

Current behavior:

- `advice_valid_from` is set to `LocalDateTime.now(UTC)` when DocBrown saves a promoted rule.
- It is not currently estimated as a future start time.
- `advice_valid_until = advice_valid_from + validity_seconds`.
- `validity_seconds` is:
  - `min(entry_validity_seconds, rem.ml.advice.validity.max.seconds)` when model-derived validity exists;
  - otherwise fallback `rem.ml.advice.validity.seconds`.
- For this rule:
  - model-derived `entry_validity_seconds = 240`;
  - max cap `900`;
  - final validity window `240s`.

Interpretation:

- The validity window is currently a BUY window.
- ACDC uses it to decide whether a rule is active and may be used for ENTRY.
- Once a BUY is opened, the trade lifecycle is governed by SELL advice:
  - take profit via `safe_net_return`;
  - loss cap via `loss_cap_net_return`;
  - timeout via `duration_seconds`.
- Therefore a trade may remain open after `advice_valid_until`; the validity expiration does not force SELL.

Risk:

- Buying near the end of the validity window is currently allowed.
- The model does not currently degrade score/size/eligibility based on remaining validity.
- If the opportunity decays over the 240s window, a late BUY can consume budget with lower expected value.

Scientific next step, if we continue:

- Evaluate delayed-entry buckets from historical samples:
  - `0-25%` of validity window;
  - `25-50%`;
  - `50-75%`;
  - `75-100%`.
- Measure profit rate, average net return, MFE, timeout rate and loss-cap rate per bucket.
- Only if late buckets degrade materially, add a model output such as:
  - `entry_validity_decay_curve`;
  - `latest_buy_offset_seconds`;
  - `late_entry_score_penalty`;
  - or `budget_fraction_by_remaining_validity`.

Do not implement a late-entry rule without this delayed-entry analysis.
