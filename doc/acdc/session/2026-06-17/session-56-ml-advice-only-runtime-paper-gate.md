# Session 56 - ML Advice Only Runtime Paper Gate

Data: 2026-06-17.

## Obiettivo

Rendere ACDC coerente con il charter REM outcome-first:

- DocBrown decide quali advice sono `PAPER_ELIGIBLE`;
- ACDC non deve bloccare un advice promosso con guardie ENTRY parallele non outcome-driven;
- PAPER deve partire solo se esiste almeno una advice `PAPER_ELIGIBLE` attiva.

## Intervento

- Aggiunta migration `V44__ml_advice_only_entry_runtime.sql`.
- Disattivate dal profilo `REM_CURRENT` le guardie ENTRY runtime legacy:
  - `entry_reversal_data_coverage_min`;
  - `entry_reversal_distinct_price_points_min`;
  - `entry_reversal_microbar_gap_max`;
  - `entry_reversal_volume_confirmation_min`;
  - `reversal_outcome_quality_score_min`;
  - `reversal_outcome_quality_probability_min`;
  - `reversal_outcome_quality_samples_min`.
- Restano attive per ENTRY:
  - `entry_price_present`;
  - `entry_snapshot_fresh`;
  - `reversal_ml_rules_min`;
  - `entry_reversal_ml_score_positive`;
  - `entry_ml_advice_paper_eligible`.
- Aggiornati i JUnit per vincolare il nuovo contratto runtime.

## Verifica

- `./mvnw -q -Dtest=RemCurrentConfigurationTest test`: OK.
- `./mvnw -q package`: OK.
- Container ACDC ricostruito con `docker compose -f docker/vpn/compose.yml up -d --build`.
- Flyway MySQL reale applicato fino a `V44`.

## Run Attempt

Mining DocBrown fresco:

- `generatedAt`: `2026-06-17T15:07:44.639698269Z`;
- punti scansionati: `5000`;
- regole valutate: `290`;
- regole promosse: `38`.

Classi attive prodotte:

- `PURE_REVERSAL_OBSERVED`: `12`;
- `PAPER_ELIGIBLE`: `0`.

Esito:

- PAPER non avviata.
- Il blocco non e' piu' nel runtime ACDC: le migliori regole sono state classificate `PURE_REVERSAL_OBSERVED` per `band_passes=false`, con `outcome_band_ratio` quasi sempre `0`.

## Prossimo Passo Scientifico

Analizzare il criterio outcome-driven di data quality in DocBrown:

- verificare se le fasce `coverage/distinct/high_volume` sono troppo restrittive rispetto ai GOOD ad alto MFE;
- confrontare GOOD/BAD con `band_passes=true` e `band_passes=false`;
- promuovere a `PAPER_ELIGIBLE` solo se il criterio separa davvero outcome profittevoli da rumore.

Non avviare PAPER finche' il miner non produce almeno una advice `PAPER_ELIGIBLE` attiva.
