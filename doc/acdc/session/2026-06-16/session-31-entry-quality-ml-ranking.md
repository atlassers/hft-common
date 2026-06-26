# Session 31 - Entry Quality ML Ranking

Data: 2026-06-16.

## Obiettivo

Evitare configurazioni di ingresso perdenti senza tornare a un sistema di soglie manuali.
ACDC deve restare DB-driven: il modello produce feature e ranking, non guardie hardcoded.

## Implementazione

- Aggiunta tabella `acdc_entry_quality_sample`.
- Aggiunta feature ranking DB `entry_quality_score`.
- Aggiunto training da SHADOW replay:
  - endpoint `POST /diagnostics/acdc/shadow/{executionId}/entry-quality/evaluate`;
  - label cost-aware con fee, dust e slippage gia' inclusi nel replay;
  - associa ogni posizione shadow alla decisione BUY originaria.
- Aggiunto scoring runtime:
  - kNN semplice su feature di ingresso;
  - output `entry_quality_score`, `entry_quality_profit_probability`, `entry_quality_samples`;
  - integrazione nel ranking tramite `acdc_ranking_feature`.
- Aggiunto endpoint diagnostico:
  - `GET /diagnostics/acdc/entry-quality/{profileKey}`;
  - migliori/peggiori campioni e medie modello.
- Aggiornato `/diagnostics/acdc/rem/parity` per esporre le feature ML.

## Verifiche

- `./mvnw -q test -Dtest=SnapshotRankingServiceTest,RemCurrentConfigurationTest,GuardEvaluatorTest` OK.
- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- Docker build ACDC OK.
- Container ACDC riavviato su MySQL.

## Training Iniziale

Execution shadow: `32`.

- Campioni creati: `13`.
- Campioni profittevoli: `4`.
- Ritorno medio netto: `-0.000929105364421902`.
- Profitto medio netto quote: `-0.023227629666677696`.

Migliori label:

- `FETUSDC`: `+0.005895397489539749`.
- `JUPUSDC`: `+0.002416011787819253`.
- `MITOUSDC`: `+0.002330924855491329`.
- `BABYUSDC`: `+0.000806179775280899`.

Peggiori label:

- `BABYUSDC`: `-0.008689732142857143`, `EXIT_ABSOLUTE_LOSS`.
- `JUPUSDC`: `-0.002489946051986268`, `EXIT_FEE_RANGE_MAX_HOLD`.
- `NOMUSDC`: `-0.002000000000000000`, `EXIT_FEE_RANGE_MAX_HOLD`.

## Decisione Strategica

Il modello non blocca BUY.
Il modello ordina gli ingressi usando evidenza economica reale da SHADOW replay.
La promozione verso PAPER richiede altri campioni SHADOW e confronto positivo tra:

- ranking senza `entry_quality_score`;
- ranking con `entry_quality_score`;
- replay cost-aware delle uscite.
