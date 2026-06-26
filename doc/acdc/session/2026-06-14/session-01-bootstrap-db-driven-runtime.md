# Sessione 1 - Bootstrap DB-Driven Runtime

Data: 2026-06-14.

## Obiettivo

Creare ACDC come nuovo servizio Quarkus generico, DB-driven, pronto a eseguire SHADOW RUN e PAPER RUN senza dipendere dai guard legacy di HFT.

## Piano Sessione

1. Scaffold Quarkus Maven.
2. Modello DB profili/guardie/snapshot/run.
3. Engine generico guardie.
4. Configurazione seed `REM_CURRENT`.
5. Endpoint SHADOW/PAPER.
6. JUnit dedicati per configurazione e guardie.
7. Build/test.

## Vincoli

- HFT resta riferimento storico.
- ACDC non hardcoda REM nel motore.
- Ogni filtro operativo appartiene a una configurazione DB.
- Ogni configurazione seedata ha test dedicato.
- Nessuna REAL RUN.

## Implementazione Completata

- Creato servizio Quarkus Maven `acdc`.
- Aggiunte migrazioni Flyway:
  - `V1__create_acdc_runtime.sql`;
  - `V2__seed_rem_current_profile.sql`.
- Tabelle create:
  - `acdc_strategy_profile`;
  - `acdc_guard_definition`;
  - `acdc_market_snapshot`;
  - `acdc_shadow_run`;
  - `acdc_shadow_decision`;
  - `acdc_paper_run`;
  - `acdc_paper_position`.
- Motore generico:
  - `GuardEvaluator`;
  - `DecisionEngine`;
  - `FeatureSnapshot`;
  - `FeatureSnapshotMapper`.
- Operatori guardia:
  - `GTE`;
  - `LTE`;
  - `BETWEEN`;
  - `PRESENT`;
  - `FRESH_WITHIN_SECONDS`;
  - `ECONOMIC_FEASIBLE`;
  - `DYNAMIC_TRAILING_EXIT`;
  - `ABSOLUTE_LOSS_EXIT`;
  - `QUOTE_LOSS_CAP_EXIT`.
- Endpoint:
  - `GET /acdc/profiles`;
  - `GET /acdc/profiles/{profileKey}/guards`;
  - `POST /acdc/profiles/{profileKey}/snapshots`;
  - `POST /acdc/shadow/run/{profileKey}`;
  - `POST /acdc/paper/run/{profileKey}`.

## Configurazione REM_CURRENT

- Profilo seedato:
  - `REM_CURRENT`;
  - `status=ACTIVE`;
  - `default_execution_mode=PAPER`.
- Guardie ENTRY seedate:
  - prezzo presente;
  - freschezza snapshot;
  - bande `momentum5/10/15`;
  - banda `trend`;
  - minimo `volume_ratio`;
  - minimo `quote_volume_fast`;
  - banda `distance_from_low`;
  - banda `pullback_depth`;
  - cap `entry_friction_quote`.
- Guardie EXIT seedate:
  - trailing dinamico;
  - absolute loss;
  - quote loss cap.

## Verifiche

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- Test dedicati:
  - `RemCurrentConfigurationTest`;
  - `GuardEvaluatorTest`;
  - `AcdcRunServiceTest`.

## Refactor Package

Richiesta utente: usare il repository `https://github.com/atlassers/acdc.git`, ma prima rendere leggibile il codice creando package per gruppi di classi.

Layout applicato:

- `it.mbc.hft.acdc.dto`;
- `it.mbc.hft.acdc.entity`;
- `it.mbc.hft.acdc.model`;
- `it.mbc.hft.acdc.repository`;
- `it.mbc.hft.acdc.resource`;
- `it.mbc.hft.acdc.service`.

Layout test applicato:

- `it.mbc.hft.acdc.config`;
- `it.mbc.hft.acdc.service`.

Verifiche dopo il refactor:

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.

## Stato Operativo

- ACDC e' pronto per SHADOW RUN con:
  - `POST /acdc/shadow/run/REM_CURRENT`.
- ACDC e' pronto per PAPER RUN simulata con:
  - `POST /acdc/paper/run/REM_CURRENT`.
- Le run possono usare snapshot nel body oppure snapshot gia' persistiti in `acdc_market_snapshot`.
- Nessuna REAL RUN implementata o avviata.
