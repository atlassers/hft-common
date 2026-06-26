# Session 39 - Purge Legacy Non-Compliant Runtime

Data: 2026-06-16.

## Obiettivo

Rimuovere da ACDC ogni residuo operativo non conforme alla pipeline attuale:

DocBrown -> DB shared config/regole promosse -> ACDC runtime.

Sono mantenute solo le posizioni/trade storiche come riferimento.

## Pulizia Codice

- Eliminato `HftCandidateService`.
- Eliminato modello `HftCandidate`.
- `CandidateSnapshotService` non legge piu' HFT/Stan.
- SHADOW/PAPER arricchiscono snapshot solo con:
  - filtro quote USDC;
  - feature Influx/microbar;
  - outcome quality;
  - regole REM ML promosse.
- Diagnostiche parity/lifecycle non espongono piu':
  - `strategyParameterId`;
  - `strategySource`;
  - `pipelineStage`;
  - `candidateThresholds`.

## Pulizia DB

Migrazione: `V29__purge_legacy_non_compliant_runtime.sql`.

Azioni:

- `candidate_source` aggiornato a `INFLUX_USDC_MICROBAR`.
- Eliminate guardie/ranking candidate/entry-quality legacy se presenti.
- Eliminate outcome signatures non `PROMOTED`.
- Eliminate reversal ML rules non `PROMOTED`.
- Eliminati outcome samples non `ACTIVE`.
- Eliminate decisioni SHADOW/PAPER precedenti alla run DocBrown-compliant `35`.
- Eliminati run summary non collegati a posizioni/trade.
- Conservate posizioni SHADOW/PAPER come riferimento storico.

## Stato DB Reale

- Flyway a `v29`.
- Universo:
  - `REM_CURRENT`;
  - `USDC`;
  - `INFLUX_USDC_MICROBAR`;
  - `maxCandidates=200`.
- Guardie ENTRY:
  - `entry_price_present`;
  - `entry_snapshot_fresh`;
  - `reversal_outcome_quality_score_min`;
  - `reversal_outcome_quality_probability_min`;
  - `reversal_outcome_quality_samples_min`.
- Ranking:
  - solo `rank_reversal_outcome_quality_score`.
- Regole/dati REM:
  - `99` outcome signatures `PROMOTED`;
  - `6` reversal ML rules `PROMOTED`;
  - `5000` outcome training samples `ACTIVE`.
- Decisioni:
  - SHADOW residue solo execution `35`;
  - PAPER residue `0`.
- Posizioni mantenute:
  - `68` shadow;
  - `65` paper.

## Verifica

- `./mvnw -q test` completato.
- `./mvnw -q package -DskipTests` completato.
- Container ACDC rebuildato e riavviato.
- `/diagnostics/acdc/rem/parity?limit=2` non contiene piu' campi HFT/candidate legacy.
