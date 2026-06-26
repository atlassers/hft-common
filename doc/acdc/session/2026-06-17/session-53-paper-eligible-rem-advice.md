# Session 53 - PAPER Eligible REM Advice

Data: 2026-06-17.

## Obiettivo

Separare in modo scientifico:

- `PURE_REVERSAL_OBSERVED`: advice utile per SHADOW/esplorazione;
- `PAPER_ELIGIBLE`: advice consumabile da PAPER.

Il razionale viene dalla sessione 52:

- SHADOW 50 ha comprato advice con `data_quality.strict_passes=false`;
- i trade non sarebbero diventati profittevoli netti nemmeno tenendoli fino a 30 minuti;
- quindi non serve stringere manualmente il loss cap, serve impedire a PAPER di consumare advice non validati.

## Implementazione

DocBrown ora scrive nel `rule_json`:

- `promotion_class = PAPER_ELIGIBLE` quando la rule promossa supera:
  - minimo validation samples;
  - validation profit rate;
  - validation average net return positivo;
  - advice economics;
  - data quality strict;
  - live audit;
- `promotion_class = PURE_REVERSAL_OBSERVED` negli altri casi promossi/osservabili.

ACDC ora legge `rule_json.promotion_class` e produce feature:

- `ml_advice_paper_eligible = 1` per `PAPER_ELIGIBLE`;
- `ml_advice_paper_eligible = 0` per `PURE_REVERSAL_OBSERVED` o vecchie rule senza classe.

ACDC sceglie prima la migliore rule `PAPER_ELIGIBLE` tra quelle matchate.
Solo se non esistono rule `PAPER_ELIGIBLE`, usa la migliore osservativa per SHADOW.
Questo evita che una rule osservativa con score alto oscuri una rule idonea a PAPER.

## Configurazione DB

Nuova migration ACDC:

`V42__paper_requires_eligible_ml_advice.sql`

Nuova guardia:

- guard key: `entry_ml_advice_paper_eligible`;
- feature: `ml_advice_paper_eligible`;
- operator: `GTE`;
- min threshold: `1`;
- rejection reason: `ML_ADVICE_NOT_PAPER_ELIGIBLE`.

Override:

- `SHADOW`: `DISABLED`;
- PAPER resta attiva.

Quindi:

- SHADOW puo' ancora osservare `PURE_REVERSAL_OBSERVED`;
- PAPER non compra advice osservativi o vecchi advice senza `promotion_class`.

## Test

Test mirati passati:

`./mvnw -q -Dtest=RemCurrentConfigurationTest,GuardEvaluatorTest,OutcomeQualityModelServiceTest test`

Copertura aggiunta:

- la nuova guardia esiste a DB;
- la guardia e' disabilitata via override per SHADOW;
- PAPER rigetta se `ml_advice_paper_eligible = 0`;
- ACDC deriva `ml_advice_paper_eligible` dal `rule_json`;
- una rule `PURE_REVERSAL_OBSERVED` con score piu' alto non oscura una rule `PAPER_ELIGIBLE` sullo stesso simbolo.

## Stato

Non e' stata lanciata una nuova SHADOW/PAPER dopo questa modifica.

ACDC container e' stato ricostruito/riavviato e Flyway ha applicato `V42` sul MySQL reale.

DocBrown e' stato rigenerato con signal ACDC disabilitato, quindi senza avviare run.

Risultato:

- outcome scanned points: `5000`;
- ML evaluated rules: `345`;
- ML promoted rules: `21`;
- `PAPER_ELIGIBLE`: `0`;
- `PURE_REVERSAL_OBSERVED`: `21`.

Diagnosi sulle `21` promosse:

- advice economics passate: `21/21`;
- validation profit rate passata: `21/21`;
- validation average net return positiva: `21/21`;
- validation sample minimo passato: `8/21`;
- data quality strict passata: `0/21`.

Quindi il blocco PAPER e' motivato dalla qualita' dati strict, non da una soglia economica arbitraria.
Le promosse correnti sono reversal osservabili in SHADOW, ma non ancora idonee a PAPER.

Prima di PAPER:

1. capire perche' la qualita' strict non passa sulle rule economicamente promettenti;
2. verificare se il problema e' reale illiquidita'/curve spigolose o calcolo troppo severo della data quality;
3. solo se esiste almeno una `PAPER_ELIGIBLE`, avviare PAPER.
