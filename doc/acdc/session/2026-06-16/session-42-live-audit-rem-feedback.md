# Session 42 - REM Live Audit Feedback

Data: 2026-06-16

## Vincolo

La pipeline resta outcome-first:

- DocBrown produce regole/advice ML.
- ACDC consuma solo regole promosse.
- BUY e SELL devono essere guidate dall'advice ML.
- Le SHADOW/PAPER live non sono training primario, ma falsificazione operativa delle regole.
- Ogni perdita live deve diventare feedback per DocBrown, non soglia manuale in ACDC.

## Implementazione

DocBrown legge le posizioni ACDC chiuse da:

- `acdc_shadow_position`
- `acdc_paper_position`

e calcola live-audit sulle regole ML:

- `zero_mfe_loss`: posizione chiusa in perdita senza escursione netta positiva;
- `under_safe_non_profit`: posizione non profittevole che non raggiunge il safe target previsto dall'advice ML.

Configurazione condivisa DB:

- `rem.ml.live_audit.limit=200`
- `rem.ml.live_audit.min.samples=1`
- `rem.ml.live_audit.max.zero_mfe_loss_rate=0.34`
- `rem.ml.live_audit.max.under_safe_non_profit_rate=0.34`
- `rem.ml.live_audit.penalty.weight=1.0`

## Risultati

### SHADOW 39

- Status: `COMPLETED`
- Budget finale: `99.841779131076650000`
- Realized: `-0.158220868923350000`
- Posizioni:
  - `BIOUSDC`: timeout, net `-0.058220868986750000`
  - `ANIMEUSDC`: timeout, net `-0.049999999998000000`
  - `ATUSDC`: timeout, net `-0.049999999938600000`

Lettura: non erano piu' loss immediate sempre a MFE zero, ma segnali che non raggiungevano il safe target. Per questo e' stato introdotto `under_safe_non_profit`.

### DocBrown post-SHADOW 39

- `scannedPoints=5000`
- `good=2001`
- `bad=2999`
- `promotedSignatures=216`
- Regole ML valutate: `265`
- Regole ML promosse: `99`

Rigetti live-audit osservati:

- `ALLOUSDC`
- `BANKUSDC`
- `BANANAS31USDC`
- `ANIMEUSDC`
- `ATUSDC`
- fallback `UNKNOWN/GLOBAL`

### SHADOW 40

- Status: `COMPLETED`
- Budget finale: `99.750565065216800000`
- Realized: `-0.249434934783200000`
- Posizioni:
  - `AVNTUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.005678793241600000`, max net `0`
  - `2ZUSDC`: `EXIT_ML_ADVICE_LOSS_CAP`, net `-0.193756141809600000`, max net `0.001225747578267584`, safe `0.010679110857794244`
  - `0GUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.049999999732000000`, max net `0.001352348993288591`, safe `0.003734006734006734`

Lettura:

- `AVNTUSDC` e' falso positivo pieno: nessun MFE positivo.
- `2ZUSDC` e `0GUSDC` respirano, ma molto meno del safe target previsto.
- Il problema residuo non e' aggiungere una SELL manuale, ma calibrare/promuovere meglio safe target, durata e loss-cap nel modello.

### DocBrown post-SHADOW 40

- `scannedPoints=5000`
- `good=2100`
- `bad=2900`
- `promotedSignatures=235`
- Regole ML valutate: `405`
- Regole ML promosse: `46`

Effetto osservato:

- `0GUSDC` rigettata sulle regole con live-audit sample.
- `AVNTUSDC` rigettata sulle regole con live-audit sample.
- Diverse regole `2ZUSDC` rigettate per `under_safe_non_profit=1`.

Problema trovato:

- Il `policy_json` ACDC non salvava le rule-key usate dalla BUY.
- DocBrown poteva associare una posizione live a una regola solo tramite range feature.
- Questo lasciava possibile che una regola candidata, come `2ZUSDC quality_structure`, restasse promossa se non matchava il range live, anche se lo stesso simbolo era stato falsificato.

## Fix contratto dati

ACDC ora scrive nel `policy_json` un flag numerico per ogni regola ML matchata:

- `reversal_ml_rule_curve_reversal=1`
- `reversal_ml_rule_early_rebound=1`
- `reversal_ml_rule_pullback_rebound_volume=1`
- `reversal_ml_rule_acceleration_reversal=1`
- `reversal_ml_rule_quality_structure=1`

DocBrown usa quel flag come prova diretta nel live-audit. Per le run vecchie resta il fallback a range feature.

## Verifica

- ACDC: `./mvnw -q -Dtest=RemCurrentConfigurationTest test`
- DocBrown: `./mvnw -q test`
- ACDC: `./mvnw -q package -DskipTests`
- ACDC container rebuildato e riavviato con `docker compose --env-file docker/vpn/.env -f docker/vpn/compose.yml up -d --build acdc`

## Prossimo passo

La prossima SHADOW scientifica deve:

- partire dopo il rebuild ACDC con flag rule-key;
- bloccare nuovi BUY dopo pochi minuti;
- essere seguita da job DocBrown;
- verificare che ogni regola usata in BUY riceva live-audit diretto.

Solo dopo questa verifica si puo' valutare una PAPER RUN.
