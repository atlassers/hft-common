# Session 21 - REM Research Scheduler e Shared Config

Data: 2026-06-16.

## Obiettivo

Spostare il mining REM multivariato da ACDC a DocBrown, lasciando ACDC come runtime consumer delle regole promosse.

## Implementazione

- Aggiunto modulo Java `it.mbc.hft.docbrown.rem`.
- Aggiunto endpoint:
  - `POST /docbrown/rem/research/{profileKey}/run`.
- Aggiunto scheduler Quarkus disabilitato di default:
  - `docbrown.rem.scheduler.enabled=false`;
  - `docbrown.rem.scheduler.interval=30m`;
  - `docbrown.rem.profile-key=REM_CURRENT`.
- Il job legge la tabella condivisa `acdc_shared_runtime_config` quando presente:
  - bucket Influx;
  - microbar seconds;
  - feature window;
  - lookback/horizon/campionamento ML;
  - soglie minime di validazione.
- Rimosso il vecchio miner Python REM superseded.

## Verifica

- `./mvnw -q test` completato.
- Run manuale su `REM_CURRENT`:
  - `scannedPoints=5000`;
  - `good=1863`;
  - `bad=3136`;
  - `trainSamples=3470`;
  - `validationSamples=1530`;
  - `evaluatedRules=310`;
  - `promotedRules=6`.

## Ruolo Architetturale

DocBrown diventa il producer scientifico:

- legge Influx;
- calcola feature reversal;
- etichetta outcome netto cost-aware;
- valida temporalmente;
- promuove regole/firme in DB.

ACDC resta consumer/runtime:

- legge regole promosse;
- ordina e filtra candidate;
- esegue BUY/SELL in SHADOW/PAPER/REAL;
- mantiene temporaneamente diagnostiche di confronto.
