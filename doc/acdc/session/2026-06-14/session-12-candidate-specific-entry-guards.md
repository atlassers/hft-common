# Session 12 - Candidate-Specific Entry Guards

Data: 2026-06-14

## Obiettivo

Rimuovere l'ultimo legame forte tra ENTRY REM e soglie statiche ACDC.

Le guardie devono poter confrontare una feature live contro soglie provenienti dal candidato HFT/DocBrown, senza codificare REM nel servizio.

## Implementazione

- Aggiunti operatori generici:
  - `FEATURE_GTE`;
  - `FEATURE_LTE`;
  - `FEATURE_BETWEEN`.
- Ogni guardia puo' indicare in `metadata_json`:
  - `min_feature_key`;
  - `max_feature_key`.
- Se le feature candidate mancano, il motore usa `min_threshold`/`max_threshold` come fallback.
- Aggiunta migration `V12__use_candidate_entry_thresholds.sql`.
- `REM_CURRENT` ora usa soglie candidate per:
  - `momentum5`;
  - `momentum10`;
  - `momentum15`;
  - `trend`;
  - `volume_ratio`;
  - `quote_volume_fast`;
  - `distance_from_low`;
  - `pullback_depth`.

## Validazione

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- V12 applicata su MySQL schema `hft`.

### PAPER execution 11

- Sorgente: candidati HFT/DocBrown + Influx.
- `evaluated=2`.
- `accepted=0`.
- `opened=0`.
- `usdt=0`.
- Rejection:
  - `BABYUSDC` -> `ENTRY_MOMENTUM10_OUT_OF_BAND`;
  - `BANANAS31USDC` -> `ENTRY_TREND_OUT_OF_BAND`.
- Esempio audit:
  - `BABYUSDC momentum10=0.019230769230769232`, candidate max `0.00349510626580084`;
  - `BANANAS31USDC trend=-0.002899420115976805`, candidate max `-0.002988361623888935`.
- Execution fermata dopo il test.

## Note

- Nessuna REAL RUN avviata.
- Nessun ordine Binance inviato.
- La PAPER non ha comprato: e' il comportamento atteso con le nuove soglie candidate, perche' i due candidati live non rispettavano piu' la curva validata.
