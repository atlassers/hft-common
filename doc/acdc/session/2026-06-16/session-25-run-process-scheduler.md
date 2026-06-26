# Session 25 - Run Process Scheduler

Date: 2026-06-16

## Scope

- Prevent missing SELL evaluations when no one manually calls `start-run`.
- Model RUN lifecycle with a common Java interface.
- Apply the mechanism to all schedulable RUN types.

## Changes

- Added `RunProcess` interface.
- Implemented `RunProcess` in:
  - `DryRunService`;
  - `ShadowRunService`;
  - `PaperRunService`.
- Added `RunProcessScheduler`.
- Added `RunExecutionRepository.running(RunType)`.
- Added `quarkus-scheduler`.
- Added config:
  - `acdc.run.scheduler.enabled`;
  - `acdc.run.scheduler.interval`.
- Scheduler is disabled in `test` and `local`.

## Semantics

- `DRY` is one-shot and not scheduled.
- `SHADOW` is scheduled while execution status is `RUNNING`.
- `PAPER` is scheduled while execution status is `RUNNING`.
- The scheduler uses a lock to avoid overlapping ticks.
- If a RUN is in stop-buy/drain and `reservedBudget=0`, the scheduler stops it automatically.

## Verification

- `./mvnw -q test` passed.
- ACDC rebuilt and redeployed from Docker compose.
- Runtime feature list includes `scheduler`.
- SHADOW `executionId=27` was started and immediately put in stop-buy.
- No manual drain call was made after stop-buy.
- Scheduler results:
  - `07:47:32`: automatic tick closed `ENAUSDC` by `EXIT_ABSOLUTE_LOSS`;
  - `07:48:32`: automatic tick closed `EPICUSDC` by `EXIT_MICRO_PROFIT_TAKE`;
  - `07:49:32`: automatic tick kept `DASHUSDC` in `EXIT_HOLD`.

## Current State

- SHADOW `27` remains `RUNNING` in drain mode.
- `DASHUSDC` is the only open position.
- Scheduler will keep evaluating it every configured interval.
