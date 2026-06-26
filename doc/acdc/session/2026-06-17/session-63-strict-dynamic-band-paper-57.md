# Session 63 - Strict Dynamic Band PAPER 57

Data: 2026-06-17

## Obiettivo

Eseguire una PAPER solo dopo:

- promozione REM in modalita' `STRICT`;
- nuovo band scoring dinamico DocBrown;
- presenza di almeno una advice `PAPER_ELIGIBLE` ancora valida.

## Preparazione

ACDC:

- commit runtime config: `c8b2fb2` (`MS675: require strict REM promotion for paper`);
- migration reale applicata: `V46__rem_ml_strict_paper_promotion.sql`;
- config reale: `rem.ml.promotion.mode = STRICT`;
- ACDC container ricostruito e riavviato;
- health `/acdc/profiles`: OK.

DocBrown:

- commit band scoring: `90af674` (`MS673: score REM bands by outcome discrimination`);
- container ricostruito dopo `./mvnw -q package`;
- scheduler disabilitato;
- signal automatico disabilitato;
- batch manuale `POST /rem/research/REM_CURRENT/run`.

## Batch DocBrown

- generated at: `2026-06-17T20:10:10.890321871Z`;
- lookback: `12h`;
- horizon: `900s`;
- scanned points: `5000`;
- samples created/updated: `2576/2424`;
- GOOD/BAD: `2323/2677`;
- promoted ML rules: `4`.

Band dinamica attiva:

- model version: `REM-BAND-2026-06-17T201001.544750914-4fb726e4`;
- status: `ACTIVE`;
- coverage: `0.20 .. 0.364640883977900570`;
- distinct price points min: `8`;
- max gap seconds: `60`;
- validation pass samples: `204`;
- validation good/bad pass: `133/71`;
- validation precision good: `0.651960784313725490`;
- validation recall good: `0.219834710743801653`;
- validation avg MFE: `0.002572863897570207`;
- score: `1.415501366825712014`.

La band e' molto piu' selettiva della precedente RUN 56 (`coverage 0..0.8`, `distinct >= 1`).

## Advice Promosse

Tutte le promozioni erano `PAPER_ELIGIBLE` su `ATUSDC`:

- `acceleration_reversal`, score `1.068601986249044976`, validita' `20:10:06 -> 20:17:06`, safe net `0.008038961038961038`, durata `685s`;
- `curve_reversal`, score `0.846905537459283400`, validita' `20:10:06 -> 20:13:06`;
- `early_rebound`, score `0.811611419812224576`, validita' `20:10:06 -> 20:18:06`;
- `pullback_rebound_volume`, score `0.718484650555192700`, validita' `20:10:06 -> 20:12:06`.

La PAPER e' partita mentre le advice erano ancora valide.

## PAPER 57

- avvio via wrapper ACDC: `scripts/acdc-start-paper-run.sh`;
- execution id: `57`;
- started at: `2026-06-17 20:10:55`;
- stop-buy at: `2026-06-17 20:14:11`;
- completed at: `2026-06-17 20:22:35`;
- initial budget: `100`;
- current budget: `100.123876582199200000`;
- realized profit quote: `0.123876582199200000`.

Trade:

- symbol: `ATUSDC`;
- buy: `0.158000000000000000`;
- sell: `0.159100000000000000`;
- buy quote: `24.999999984000000000`;
- sell quote: `25.174050616800000000`;
- buy fee: `0.024999999984000000`;
- sell fee: `0.025174050616800000`;
- gross profit: `0.174050632800000000`;
- net profit: `0.123876582199200000`;
- net return: `0.004955063291139241`;
- max net return: `0.004955063291139241`;
- hold: about `690s`;
- exit: `EXIT_ML_ADVICE_POSITIVE_DURATION`.

Policy snapshot:

- `reversal_ml_rule_acceleration_reversal = 1`;
- `reversal_ml_score = 1.0686019862490448`;
- `reversal_ml_probability = 0.7647058823529411`;
- `reversal_ml_validation_samples = 17`;
- `ml_advice_safe_net_return = 0.008038961038961038`;
- `ml_advice_duration_seconds = 685`;
- `ml_advice_loss_cap_net_return = -0.012079118028534373`;
- `coverage = 0.24418604651162792`;
- `distinct_price_points = 19`;
- `max_gap_seconds = 60`;
- `volume_confirmation = 1.176953364206981`;
- `raw_volume = 1182.5`.

## Lettura

La run e' positiva e coerente col piano:

- PAPER non ha consumato `PURE_REVERSAL`;
- ha consumato solo `PAPER_ELIGIBLE`;
- la band dinamica ha escluso le curve spigolose piu' evidenti;
- l'ingresso e' avvenuto dentro la finestra di validita';
- stop-buy ha evitato ingressi successivi;
- SELL ha chiuso in profitto a fine durata, sotto safe net ma positivo.

Limite ancora aperto:

- ACDC non salva ancora nel `policy_json` `rule_id`, `research_batch_id`, `advice_valid_from/until`, `advice_age_seconds`, `band_model_version` e hash/versione del rule json.
- Questo va fatto prima di valutare una PAPER piu' lunga, per rendere ogni trade auditabile senza dipendere da regole aggiornate in-place.

