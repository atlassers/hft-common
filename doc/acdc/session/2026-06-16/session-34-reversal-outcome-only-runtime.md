# Session 34 - Reversal Outcome-Only Runtime

Data: 2026-06-16.

## Obiettivo

Rendere ACDC mirato al reversal e al suo `outcome_quality`, rimuovendo dal runtime le logiche entry/trend legacy.

## Implementazione

- Aggiunta migrazione `V23__reversal_outcome_only_runtime.sql`.
- Cancellate dal DB runtime `REM_CURRENT` le guardie ENTRY legacy:
  - momentum;
  - trend;
  - volume;
  - quote volume;
  - distanza dal minimo generica;
  - pullback generico;
  - friction entry.
- Cancellati dal DB runtime i ranking ENTRY legacy:
  - candidate score HFT;
  - candidate net profit;
  - candidate win rate;
  - candidate trades;
  - expected return;
  - entry quality.
- Ranking runtime rimasto:
  - `rank_reversal_outcome_quality_score` su `outcome_quality_score`.
- Guardie operative BUY rimaste:
  - prezzo presente;
  - snapshot fresco;
  - `reversal_outcome_quality_score_min`;
  - `reversal_outcome_quality_probability_min`;
  - `reversal_outcome_quality_samples_min`.
- SHADOW disabilita i gate outcome per esplorazione.
- PAPER/REAL applicano i gate outcome.

## Feature Reversal

`InfluxSnapshotService` ora calcola:

- `reversal_slope_short`;
- `reversal_slope_medium`;
- `reversal_slope_delta`;
- `reversal_acceleration`;
- `reversal_trough_age_seconds`;
- `reversal_distance_from_trough`;
- `reversal_pre_trough_drop`;
- `reversal_volume_confirmation`;
- `reversal_confirmed`;
- `reversal_quality`.

## Outcome Model

- `HistoricalOutcomeMiningService` promuove firme solo su feature `reversal_*`.
- `OutcomeQualityModelService` misura similarita' solo su feature `reversal_*`.
- Il fallback esplorativo SHADOW di `SnapshotRankingService` ordina per forma reversal, non per trend positivo generico.

## Pulizia Componenti

Rimosso dal runtime:

- `CounterfactualEntryService`;
- `CounterfactualDiagnosticsService`;
- `CounterfactualEntryEvaluation`;
- `CounterfactualEntryEvaluationRepository`;
- DTO counterfactual;
- endpoint `/acdc/counterfactual/*`;
- endpoint `/diagnostics/acdc/counterfactual/*`;
- registrazione counterfactual da `PaperRunService`.

Nota:

- Le migrazioni storiche V13 e V20 restano per compatibilita' Flyway.
- Nessun codice runtime usa piu' dataset entry/counterfactual.
- Aggiunta migrazione `V24__drop_entry_based_training_tables.sql` per rimuovere dal DB runtime:
  - `acdc_entry_quality_sample`;
  - `acdc_counterfactual_entry_evaluation`.

## Verifiche

- `./mvnw -q test` OK.

## Vincolo Operativo

Da ora:

- SHADOW puo' esplorare reversal abbassando i gate outcome.
- PAPER/REAL non devono comprare se `outcome_quality_*` non passa.
- Il tuning deve avvenire via mining outcome-first su dati reali Influx e validazione temporale, non tramite soglie entry manuali.
