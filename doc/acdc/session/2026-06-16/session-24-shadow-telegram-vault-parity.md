# Session 24 - Shadow Telegram Vault Parity

Date: 2026-06-16

## Scope

- Make SHADOW runs emit Telegram messages like PAPER/HFT.
- Align ACDC runtime configuration with HFT compose/Vault behavior.
- Relaunch a clean SHADOW run and verify BUY/SELL lifecycle.

## Findings

- `ShadowRunService` did not call Telegram at all.
- ACDC had the Telegram client code but lacked the `quarkus-vault` extension used by HFT.
- Vault contains the expected keys:
  - `hft.telegram.bot-token`;
  - `hft.telegram.chat-id`.
- The ACDC compose was close to HFT but not fully aligned:
  - missing some runtime envs carried by HFT;
  - VPN volumes used indirection instead of local ACDC files.

## Changes

- Added `ShadowTelegramNotificationService`.
- Wired SHADOW notifications for:
  - execution start;
  - BUY;
  - SELL;
  - stop;
  - completion when no open positions remain.
- Added `ShadowPositionRepository.closedByExecution`.
- Added `quarkus-vault` dependency to ACDC.
- Aligned `docker/vpn/compose.yml` with HFT structure while keeping ACDC names/port/image.
- Telegram notifier now dispatches in a background thread, so Telegram delays do not block run endpoints.

## Verification

- `./mvnw -q test` passed.
- `./mvnw -q package -DskipTests` passed.
- ACDC container rebuilt and restarted from compose.
- Runtime feature list includes `vault`.
- `api.telegram.org` is reachable from `acdc-vpn`.
- No `Telegram notifier Notification unsent` logs after the final redeploy and run.

## Shadow Run

- Stopped previous non-clean SHADOW `executionId=25`.
- Started new SHADOW `executionId=26`.
- Opened:
  - `ORCAUSDC`;
  - `ALLOUSDC`;
  - `PYTHUSDC`.
- Stop-buy applied immediately.
- First drain cycle:
  - `ORCAUSDC` closed by `EXIT_MICRO_PROFIT_TAKE`;
  - net profit `+0.088970583801 USDC`;
  - `ALLOUSDC` and `PYTHUSDC` remain open in `EXIT_HOLD`.

## Current State

- SHADOW `26` is still `RUNNING` in drain mode.
- No new BUY can be opened because `buy_stopped_at` is set.
- Reserved budget remains about `50.0499998815817 USDC` for the two open positions.
