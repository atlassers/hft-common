# hft-common

Shared Java contracts for the HFT REM workspace.

## Artifact

```text
it.mbc.hft:hft-common:1.0.0-SNAPSHOT
```

## Scope

- Cross-module request/response records.
- REM and management enums.
- Protocol constants shared by ACDC, DocBrown and Kenshiro.

This library must stay Java-only. It must not depend on Quarkus, JPA, databases, schedulers, trading runtime logic or
strategy tuning.

## Local Build

Run this before building consumers from a clean workspace:

```bash
mvn install
```

Consumers:

- `/home/mbc/Documenti/ws/java/hft/acdc`
- `/home/mbc/Documenti/ws/java/hft/docbrown`
- `/home/mbc/Documenti/ws/java/hft/kenshiro`
