# Session 43 - ML Advice Validity And Used State

Data: 2026-06-16

## Obiettivo

Evitare che ACDC usi consulenze ML stale e impedire il riuso dello stesso segnale puntuale quando la regola e' specifica per simbolo.

## Implementazione

Tabella `acdc_reversal_ml_rule` estesa con:

- `advice_valid_from`
- `advice_valid_until`
- `used_at`
- `used_execution_id`

Configurazione condivisa:

- `rem.ml.advice.validity.seconds=300`

DocBrown valorizza `advice_valid_from` e `advice_valid_until` quando promuove una regola. Gli stessi valori sono scritti anche nel blocco `advice` del `rule_json`.

ACDC carica solo regole:

- `PROMOTED`;
- con `advice_valid_from <= now`;
- con `advice_valid_until > now`;
- non usate se `scope_type='SYMBOL'`.

Quando SHADOW/PAPER aprono una posizione, ACDC marca come `used` le regole simbolo-specifiche che hanno contribuito alla BUY, usando i flag `reversal_ml_rule_<ruleKey>=1` salvati nel `policy_json`.

Le regole `FAMILY` e `GLOBAL` non vengono marcate `used`, perche' non rappresentano una consulenza puntuale su un solo simbolo.

## Verifica

- ACDC `./mvnw -q -Dtest=RemCurrentConfigurationTest test`: completato.
- DocBrown `./mvnw -q test`: completato.
- ACDC `./mvnw -q package -DskipTests`: completato.
- DocBrown `./mvnw -q package -DskipTests`: completato.
- Container ACDC rebuildato e riavviato.
- Flyway reale ACDC a versione `36`.
- Config reale presente:
  - `rem.ml.advice.validity.seconds=300`.

## Job DocBrown

Eseguito `POST /docbrown/rem/research/REM_CURRENT/run`.

Risultato:

- regole ML valutate: `300`;
- regole ML promosse: `17`;
- regole promosse con validita': `17`;
- regole promosse marcate used: `0`.

Primo esempio report:

- `ALTUSDC` `acceleration_reversal`;
- `adviceValidFrom=2026-06-16T16:59:31.558642026Z` circa;
- `adviceValidUntil=2026-06-16T17:04:31.558642026Z` circa.

Check runtime DB:

- `active_now=17` secondo il filtro ACDC su validita' e `used_at`.

## Nota Operativa

La finestra di validita' della consulenza non e' la durata del trade:

- `advice_valid_from/until` indica fino a quando ACDC puo' aprire una BUY usando quella consulenza;
- `ml_advice_duration_seconds` indica per quanto tempo il trade puo' restare aperto dopo la BUY.
