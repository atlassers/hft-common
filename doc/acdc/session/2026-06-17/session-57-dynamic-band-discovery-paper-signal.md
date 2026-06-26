# Session 57 - Dynamic Band Discovery Paper Signal

Data: 2026-06-17.

## Obiettivo

Implementare la pipeline REM a tre step:

1. DocBrown scopre dinamicamente le fasce data-quality dalle ultime finestre Influx.
2. DocBrown usa la band versionata per cercare segnali reversal profittevoli e promuovere `PAPER_ELIGIBLE`.
3. ACDC riceve il primo signal e avvia una PAPER RUN.

## Modifiche

- HFT charter aggiornato con il vincolo `Band Discovery -> Signal Mining -> Trading Runtime`.
- DocBrown:
  - nuova tabella `acdc_rem_data_quality_band_model`;
  - nuova migration `V26__add_rem_data_quality_band_model.sql`;
  - nuovo servizio `RemDataQualityBandDiscoveryService`;
  - il job REM esegue outcome mining, band discovery, poi reversal ML;
  - il miner scrive `band_model_version` nel `rule_json`;
  - le band vengono inserite nei `ranges`, quindi ACDC le applica genericamente senza codice strategico aggiuntivo;
  - il signal viene inviato solo sul primo `PAPER_ELIGIBLE`;
  - timeout signal ACDC aumentato a 30 secondi per evitare falsi negativi.
- ACDC:
  - nuovo endpoint `POST /acdc/profiles/{profileKey}/research-batches/{researchBatchId}/paper-signal`;
  - l'endpoint avvia una PAPER RUN leggendo da Influx.

## Verifiche

- DocBrown build: `./mvnw -q package` OK.
- ACDC build: `./mvnw -q package` OK.
- ACDC container ricostruito e riavviato.
- DocBrown Flyway reale applicato a `V26`.

## Batch Scientifico

DocBrown batch:

- `generatedAt`: `2026-06-17T15:40:28.376667742Z`;
- punti scansionati: `5000`;
- regole valutate: `1430`;
- regole promosse: `1`.

Band model:

- version: `REM-BAND-2026-06-17T153958.141908433-e6fd6049`;
- status: `ACTIVE`;
- coverage: `[0, 1]`;
- distinct price points min: `1`;
- max gap seconds: `60`;
- validation samples: `3836`;
- pass samples: `3836`;
- good pass: `2369`;
- bad pass: `1467`;
- precision good: `0.617570385818561`;
- recall good: `1`;
- avg MFE: `0.002585363029157435`;
- score: `2.4936770745528656`.

Regola promossa:

- scope: `FAMILY UNKNOWN`;
- rule: `acceleration_reversal`;
- score: `0.340953`;
- validation samples: `666`;
- validation profit rate: `0.6201`;
- validation avg net return: `0.002710`;
- class: `PAPER_ELIGIBLE`.

## Prima RUN

ACDC ha avviato PAPER execution `53`.

Stato al controllo:

- status: `BUY_STOPPED_DRAINING` dopo stop-buy operativo;
- current budget: `24.990092551554600000`;
- reserved budget: `75.074999614314700000`;
- realized profit: `0.065092165869300000`;
- posizioni aperte:
  - `OPGUSDC`;
  - `CATIUSDC`;
  - `OPENUSDC`.
- posizione chiusa:
  - `FOGOUSDC`, net profit `0.065092165869300000`.

Ogni posizione e' stata aperta con:

- `ml_advice_paper_eligible = 1`;
- `reversal_ml_score = 0.34095345345345346`;
- `ml_advice_duration_seconds = 680`;
- `ml_advice_safe_net_return = 0.002208333333333333`;
- `ml_advice_max_net_return = 0.007662177328843996`.

## Note Operative

- La chiamata paper signal ha superato il timeout iniziale da 5 secondi, ma ACDC ha comunque creato la PAPER. Il timeout DocBrown e' stato aumentato a 30 secondi.
- La PAPER `53` e' lasciata in draining: stop-buy applicato per evitare nuovi acquisti, SELL policy ancora attiva sulle posizioni aperte.
