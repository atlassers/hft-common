# Session 52 - REM Input/Output Scientific Report

Data: 2026-06-17.

## Obiettivo

Analizzare input e output prima di modificare soglie o logica.

Vincolo:

- nessuna nuova soglia viene validata senza razionale scientifico;
- l'advice ML deve indicare anche durata e validita' economica del segnale;
- la scadenza non deve coincidere necessariamente con la run successiva;
- se un reversal richiede 30 minuti per essere profittevole, il modello deve poterlo rappresentare;
- ACDC deve pescare progressivamente il meglio del meglio, con eventuale limite massimo di durata trade deciso dai dati.

## Input Del Miner

Dataset corrente:

- tabella: `acdc_outcome_training_sample`;
- campioni: `38189`;
- osservazioni: `2026-06-16 12:01:05` -> `2026-06-17 07:57:05`;
- horizon: `900s`;
- lookback configurato: `12h`;
- campionamento: `60s`;
- max samples: `5000`;
- source runtime: `binance-realtime` per simboli live, `binance-microbar` per feature/future.

Distribuzione campioni:

| Split | Label | Count | Avg MFE Netto | Max MFE Netto |
| --- | --- | ---: | ---: | ---: |
| TRAIN | BAD | 21840 | `-0.00259131` | `0` |
| TRAIN | GOOD | 9624 | `0.00350803` | `0.04495737` |
| VALIDATION | BAD | 4230 | `-0.00241274` | `-0.00000150` |
| VALIDATION | GOOD | 2494 | `0.00442625` | `0.02542074` |

Friction nel labeling:

- runtime fee: `0.001` per lato;
- slippage DocBrown default: `0.0005` per lato;
- frizione round-trip stimata: circa `0.003` netto, prima di eventuale dust.

Quindi un movimento lordo inferiore a circa `0.30%` non basta a produrre profitto netto.

## Output Del Miner

Regole correnti:

- `405` rule totali;
- `11` promosse;
- tutte le promosse sono `SYMBOL`;
- tutte le promosse hanno `data_quality.strict_passes = false`;
- `PURE_REVERSAL` sta permettendo la promozione anche quando la qualita' dati non passa i criteri strict.

Promosse principali:

| Symbol | Rule | Validation Samples | Profit Rate | Avg Return | Score | Duration | Validity | Strict |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| `2ZUSDC` | `curve_reversal` | 13 | `1.0000` | `0.009042` | `1.341304` | `535s` | `230s` | false |
| `2ZUSDC` | `early_rebound` | 12 | `1.0000` | `0.008911` | `1.341304` | `595s` | `230s` | false |
| `2ZUSDC` | `quality_structure` | 10 | `1.0000` | `0.008342` | `1.296041` | `655s` | `185s` | false |
| `AAVEUSDC` | `curve_reversal` | 4 | `0.7500` | `0.004165` | `0.820614` | `780s` | `475s` | false |
| `ARKMUSDC` | `pullback_rebound_volume` | 5 | `0.6000` | `0.000997` | `0.446088` | `715s` | `595s` | false |

Lettura:

- il ranking seleziona advice con aspettativa storica positiva;
- pero' il dataset che produce queste advice contiene molti campioni a qualita' microbar non strict;
- il caso `PURE_REVERSAL` e' utile per esplorare, ma non e' ancora PAPER-ready.

## Validita' E Durata Dei Reversal

Distribuzione globale dei GOOD:

| Metrica | P10 | P25 | P50 | P75 | P90 |
| --- | ---: | ---: | ---: | ---: | ---: |
| MFE netto | `0.000303` | `0.000955` | `0.002352` | `0.004908` | `0.008598` |
| Time to target | n/a | `200s` | `480s` | `725s` | `845s` |
| Validita' entry, solo >0 | `65s` | `155s` | `355s` | `580s` | `730s` |

Distribuzione per durata dei GOOD:

| Time to Target | Count | Avg MFE Netto |
| --- | ---: | ---: |
| `<=60s` | 434 | `0.002369` |
| `61-180s` | 1129 | `0.002760` |
| `181-300s` | 1246 | `0.003065` |
| `301-600s` | 3470 | `0.003651` |
| `601-900s` | 4498 | `0.004527` |

Conclusione scientifica:

- i reversal migliori non sono necessariamente immediati;
- il massimo rendimento medio cresce fino alla fascia `601-900s`;
- un limite trade troppo corto elimina proprio molti outcome migliori;
- per coprire casi da 30 minuti serve aumentare l'horizon del miner oltre `900s`, non inventare una soglia manuale.

## Output Live SHADOW 50

Execution:

- id: `50`;
- started: `2026-06-17 08:14:36`;
- completed: `2026-06-17 08:19:13`;
- realized: `-0.214234671320380000`.

Trade:

| Symbol | Rule Score | Probability | Samples | Safe Net | Max Net | Duration | Loss Cap | Live Max Net | Result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `2ZUSDC` | `1.341304` | `1.0` | 35 | `0.008413` | `0.010927` | `535s` | `-0.004873` | `0` | `-0.124816` |
| `AAVEUSDC` | `0.820614` | `0.75` | 4 | `0.005706` | `0.007168` | `780s` | `-0.003527` | `0` | `-0.089418` |

Il SELL ha rispettato l'advice:

- entrambe chiuse per `EXIT_ML_ADVICE_LOSS_CAP`;
- entrambe hanno avuto `max_net_return = 0`;
- quindi non hanno prodotto nemmeno una micro-escursione netta positiva.

## Replay 30 Minuti Post-BUY

Dati Influx futuri disponibili su `binance-microbar`.

Se avessimo mantenuto le posizioni fino a 30 minuti:

| Symbol | Max Gross Return | Time Max | Min Gross Return | End Gross Return |
| --- | ---: | --- | ---: | ---: |
| `2ZUSDC` | `0.00217865` | `08:27:50` | `-0.00857843` | `-0.00653595` |
| `AAVEUSDC` | `0.00118374` | `08:17:05` | `-0.00605024` | `-0.00355123` |

Con frizione round-trip circa `0.003`, entrambe restano non profittevoli nette anche trattenendole 30 minuti.

Quindi in SHADOW 50 il problema non e':

- loss cap troppo stretto;
- SELL troppo veloce;
- scadenza troppo corta.

Il problema e':

- advice ML falso positivo;
- promozione permissiva da `PURE_REVERSAL`;
- data quality strict non rispettata sulle rule promosse;
- validazione ancora insufficiente per distinguere reversal netti da rimbalzi lordi sotto frizione.

## Diagnosi

Il sistema non sta ancora pescando "il meglio del meglio".

Motivi principali:

1. `PURE_REVERSAL` promuove rule con `strict_passes=false`.
2. Alcune rule hanno validation sample troppo bassi per essere operative (`AAVEUSDC` ha 4 sample, `ARKMUSDC` 5).
3. Il modello usa profitto storico netto, ma non blocca abbastanza i pattern che live producono MFE netto zero.
4. La durata stimata e la validita' stimata esistono, ma sono derivate da horizon massimo `900s`; non possono ancora rappresentare bene casi profittevoli a 30 minuti.
5. Le soglie minime correnti (`safe_net_return`, `max_net_return`, `profit_probability`) sono economiche, ma da sole non bastano senza qualita' dati e robustezza statistica.

## Razionale Scientifico Per La Prossima Decisione

Non propongo soglie arbitrarie.

Le uniche leve razionalmente difendibili dai dati sono:

1. promuovere a PAPER solo advice con qualita' dati strict passata;
2. alzare la confidenza statistica usando un minimo validation sample coerente con lo scope;
3. penalizzare o bloccare rule che hanno live audit con loss e MFE netto zero;
4. aumentare l'horizon del miner se vogliamo catturare reversal da 30 minuti;
5. derivare `advice_valid_until` dai delayed-entry outcome, non dal ciclo schedulato;
6. derivare `max_hold_seconds` dalla distribuzione `time_to_target`, non da una costante.

Una possibile strategia scientifica, da validare prima di implementare:

- SHADOW resta esplorativa ma deve separare chiaramente `PURE_REVERSAL_OBSERVED` da `PAPER_ELIGIBLE`;
- PAPER compra solo `PAPER_ELIGIBLE`;
- `PAPER_ELIGIBLE` richiede:
  - net expected return sopra frizione;
  - validation sample sufficiente;
  - data quality strict;
  - live audit senza zero-MFE losses per quella rule/scope;
  - durata e scadenza derivate dai quantili storici del pattern.

## Raccomandazione

Prima di cambiare soglie:

1. decidere se vogliamo horizon `1800s` per includere reversal da 30 minuti;
2. rigenerare outcome su horizon `1800s`;
3. confrontare distribuzioni `900s` vs `1800s`;
4. promuovere solo advice con evidenza netta, robustezza statistica e data quality strict;
5. solo dopo fare SHADOW/PAPER.

Questo evita il ciclo "soglia nuova -> run -> loss -> soglia nuova".
