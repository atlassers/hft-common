# Session 64 - REM advice snapshot audit

Data: 2026-06-18

## Obiettivo

Rendere ogni posizione PAPER/SHADOW auditabile rispetto alla consulenza ML che ha autorizzato la BUY, senza cambiare le soglie operative raggiunte con RUN 57.

## Cambi ACDC

- `acdc_reversal_ml_rule` espone ora `band_model_id`.
- Java migration `V47__snapshot_rem_advice_band_model` aggiunge la colonna `band_model_id` solo se manca.
- `OutcomeQualityModelService` arricchisce le feature con metadati numerici della rule selezionata:
  - `ml_advice_rule_id`;
  - `ml_advice_scope_type_code`;
  - `ml_advice_scope_key_hash`;
  - `ml_advice_rule_key_hash`;
  - `ml_advice_rule_json_hash`;
  - `ml_advice_band_model_id`;
  - `ml_advice_valid_from_epoch_seconds`;
  - `ml_advice_valid_until_epoch_seconds`;
  - `ml_advice_promotion_class_code`;
  - `ml_advice_band_model_version_hash`.
- `PaperRunService` e `ShadowRunService` salvano nel `policy_json` anche:
  - `ml_advice_age_seconds_at_buy`.

## Nota tecnica

Il `policy_json` resta una mappa numerica `Map<String, BigDecimal>`, quindi i campi testuali sono rappresentati con codici o CRC32 hash stabili. Questo mantiene compatibilita' con scoring, diagnostica e replay esistenti.

La V47 e' una Java migration per gestire in modo idempotente ambienti dove `band_model_id` sia gia' presente sul DB reale.

## Perimetro

Non sono state modificate:

- soglie runtime;
- configurazioni PAPER/SHADOW/REAL;
- ranking;
- exit policy;
- logica di promozione `PAPER_ELIGIBLE`.

## Verifica

- `./mvnw -q -Dtest=OutcomeQualityModelServiceTest test`: OK.
- `./mvnw -q package`: OK.
- Container ACDC rebuild: OK.
- Endpoint `GET /acdc/profiles`: OK.
- Flyway reale MySQL: V47 `success=1`.

## Prossimo passo

Ricostruire il container ACDC per applicare la migration V47 sul DB reale, poi avviare una PAPER solo dopo aver confermato che DocBrown continua a produrre advice `PAPER_ELIGIBLE` con la configurazione Strict Dynamic Band.

## Nota operativa MySQL

Durante il primo rebuild la migration SQL iniziale ha trovato `band_model_id` gia' presente sul DB reale e Flyway ha registrato V47 come fallita. Il fix e' stato convertire V47 in Java migration idempotente, fermare il container ACDC per evitare il restart loop, eliminare solo la riga Flyway fallita `success=0`, e riavviare ACDC. La V47 corretta e' stata poi applicata con successo.
