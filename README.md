# hft-common

Shared Java contracts for the HFT REM workspace.

## Strategic Docs

For the current REM cycle, read the binding documents in this order:

1. `doc/RULES.md`
2. `doc/STRATEGIC_REM_RECOVERY_PLAN.md`
3. `doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`
4. `doc/archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`
5. `doc/CURRENT_CONTEXT.md`
6. `doc/STRATEGIC_REM_HANDOFF.md`

Current binding constraint:

```text
REAL forbidden.
PAPER only through /management.
Decision indicators, contract, WATCH, BUY and strategic SELL use closed 1m candles from binance.
5s microbar is replay/diagnostics/timing/gap/execution observation only.
No new PAPER RUN before 1m_alignment_ready=true.
```

## Artifact

```text
it.mbc.hft:hft-common:1.0.0-SNAPSHOT
```

## Scope

- Cross-module request/response records.
- REM and management enums.
- Shared REM value models.
- Shared REM JPA/Panache entities that are identical in ACDC and DocBrown.
- Protocol constants shared by ACDC, DocBrown and Kenshiro.

This library must not contain schedulers, trading runtime logic, strategy tuning, resources or database migrations.
It may depend on Quarkus/Panache only for shared JPA entity declarations. Consumers that use those entities must index
the dependency with:

```properties
quarkus.index-dependency.hft-common.group-id=it.mbc.hft
quarkus.index-dependency.hft-common.artifact-id=hft-common
```

## Package Layout

- `it.mbc.hft.common.rem.constants`
- `it.mbc.hft.common.rem.enums`
- `it.mbc.hft.common.rem.request`
- `it.mbc.hft.common.rem.response`
- `it.mbc.hft.common.rem.model`
- `it.mbc.hft.common.rem.entity`
- `it.mbc.hft.common.management.enums`
- `it.mbc.hft.common.management.request`
- `it.mbc.hft.common.management.response`

## Local Build

Run this before building consumers from a clean workspace:

```bash
mvn install
```

Consumers:

- `/home/mbc/Documenti/ws/java/hft/acdc`
- `/home/mbc/Documenti/ws/java/hft/docbrown`
- `/home/mbc/Documenti/ws/java/hft/kenshiro`
