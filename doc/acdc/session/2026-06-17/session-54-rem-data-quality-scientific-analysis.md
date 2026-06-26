# Session 54 - REM Data Quality Scientific Analysis

Data: 2026-06-17.

## Obiettivo

Analizzare perche' dopo la classificazione `PAPER_ELIGIBLE` nessuna rule promossa da DocBrown risulta idonea a PAPER.

Vincolo:

- solo analisi;
- nessun cambio soglie;
- nessun cambio codice;
- nessuna SHADOW/PAPER avviata.

## Stato Di Partenza

Dopo la rigenerazione DocBrown della sessione 53:

- ML evaluated rules: `345`;
- ML promoted rules: `21`;
- `PAPER_ELIGIBLE`: `0`;
- `PURE_REVERSAL_OBSERVED`: `21`.

Sulle `21` promosse:

- advice economics passate: `21/21`;
- validation profit rate passata: `21/21`;
- validation average net return positiva: `21/21`;
- validation sample minimo passato: `8/21`;
- data quality strict passata: `0/21`.

Quindi il blocco PAPER nasce dalla data quality strict.

## Metodo

Sono state eseguite query read-only su MySQL.

Analisi:

1. ricostruzione dei campioni `VALIDATION` matchati dalle `21` rule promosse;
2. calcolo dei pass-rate per:
   - `reversal_data_coverage_ratio`;
   - `reversal_distinct_price_points`;
   - `reversal_max_gap_seconds`;
   - `reversal_volume_confirmation`;
3. confronto GOOD/BAD sull'intero dataset;
4. bucket analysis di coverage e volume;
5. matrice coverage/volume/distinct.

Nota: la ricostruzione usa il campione corrente a DB e puo' non coincidere perfettamente con il sottoinsieme interno usato durante il mining, ma e' sufficiente per isolare la direzione statistica del problema.

## Risultato Per Rule Promosse

Nessuna rule promossa si avvicina al requisito attuale del `75%` di campioni strict.

Migliori strict ratio osservati:

| Rule | Strict Ratio | Coverage Pass | Distinct Pass | Gap Pass | Volume Pass |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ALGOUSDC/curve_reversal` | `0.167` | `0.222` | `0.889` | `1.000` | `0.556` |
| `ALLOUSDC/acceleration_reversal` | `0.154` | `0.231` | `1.000` | `1.000` | `0.462` |
| `ALLOUSDC/quality_structure` | `0.128` | `0.436` | `1.000` | `1.000` | `0.385` |
| `ALLOUSDC/early_rebound` | `0.107` | `0.429` | `1.000` | `1.000` | `0.357` |
| `ALLOUSDC/curve_reversal` | `0.087` | `0.304` | `1.000` | `1.000` | `0.478` |

Componente dominante del blocco:

- `coverage`;
- poi `volume`;
- `gap` quasi sempre passa;
- `distinct` passa su molte rule, ma non su quelle piu' sparse.

## Dataset Globale

Pass-rate componenti sull'intero dataset:

| Component | Pass Rate |
| --- | ---: |
| coverage | `0.1258` |
| distinct price points | `0.6048` |
| max gap | `0.9809` |
| volume confirmation | `0.3783` |
| strict all-in | `0.0202` |

Confronto GOOD/BAD:

| Label | Count | Avg Coverage | Avg Distinct | Avg Gap | Avg Volume | Strict Pass |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| BAD | `29800` | `0.239` | `8.9` | `54.2` | `1.014` | `0.0207` |
| GOOD | `13388` | `0.229` | `10.4` | `55.7` | `1.038` | `0.0188` |

Lettura:

- strict quality non separa GOOD da BAD;
- i GOOD hanno strict pass leggermente inferiore ai BAD;
- la data quality strict attuale non e' predittore diretto di profitto.

## Coverage

Bucket coverage:

| Coverage Bucket | Count | Good Rate | Avg MFE | Max MFE |
| --- | ---: | ---: | ---: | ---: |
| `<0.10` | `9226` | `0.1873` | `-0.001469` | `0.025654` |
| `0.10-0.20` | `10861` | `0.3469` | `-0.000522` | `0.035019` |
| `0.20-0.30` | `2067` | `0.3919` | `0.001049` | `0.044957` |
| `0.30-0.40` | `1217` | `0.4224` | `0.000816` | `0.041248` |
| `0.40-0.50` | `1013` | `0.4038` | `0.000191` | `0.018988` |
| `0.50-0.70` | `1194` | `0.3183` | `-0.000308` | `0.022695` |
| `>=0.70` | `2316` | `0.1697` | `-0.001625` | `0.020881` |

Conclusione:

- coverage alta non migliora l'outcome;
- i bucket migliori sono `0.20-0.50`;
- una soglia monotona `coverage >= 0.50` non ha razionale predittivo sui dati correnti.

## Volume

Bucket volume:

| Volume Bucket | Count | Good Rate | Avg MFE | Max MFE |
| --- | ---: | ---: | ---: | ---: |
| `<0.50` | `9279` | `0.3126` | `-0.000646` | `0.043099` |
| `0.50-0.80` | `4864` | `0.3910` | `-0.000009` | `0.035542` |
| `0.80-1.05` | `12709` | `0.2494` | `-0.001052` | `0.044957` |
| `1.05-1.50` | `9780` | `0.3131` | `-0.000680` | `0.044028` |
| `1.50-2.50` | `5650` | `0.3565` | `-0.000251` | `0.033907` |
| `>=2.50` | `907` | `0.3749` | `0.000135` | `0.037141` |

Conclusione:

- volume confirmation ha un segnale piu' plausibile della coverage, ma non monotono;
- `>=1.05` e' troppo debole come garanzia PAPER;
- i casi molto alti (`>=2.50`) migliorano, ma sono rari.

## Matrice Empirica

La combinazione piu' promettente osservata:

| Coverage Band | Volume Band | Distinct Band | Count | Good Rate | Avg MFE |
| --- | --- | --- | ---: | ---: | ---: |
| coverage_mid | volume_high | distinct_high | `312` | `0.5385` | `0.002602` |
| coverage_mid | volume_low | distinct_high | `755` | `0.5219` | `0.001715` |
| coverage_mid | volume_mid | distinct_high | `383` | `0.4961` | `0.001427` |

Dove:

- `coverage_mid`: `0.20 <= coverage < 0.50`;
- `distinct_high`: `distinct_price_points >= 20`;
- `volume_high`: `volume_confirmation >= 1.50`.

Lettura:

- la qualita' utile per REM non sembra essere "massima continuita'";
- sembra essere "coverage sufficiente, molti prezzi distinti, volume non necessariamente basso";
- coverage troppo alta puo' descrivere mercati iperattivi o trend rumorosi, non reversal migliori.

## Promosse Correnti E Qualita' Empirica

Le rule promosse che piu' ricadono nella zona empiricamente promettente:

| Rule | Good Rate | Avg MFE | Empirical Quality Ratio |
| --- | ---: | ---: | ---: |
| `BIOUSDC/quality_structure` | `0.6923` | `0.005796` | `0.692` |
| `CFGUSDC/acceleration_reversal` | `0.6154` | `0.002643` | `0.590` |
| `ALLOUSDC/acceleration_reversal` | `0.6154` | `0.002023` | `0.538` |

Queste non sono `PAPER_ELIGIBLE` per la strict attuale, ma hanno caratteristiche piu' coerenti con la matrice empirica.

## Conclusioni

1. Il blocco `PAPER_ELIGIBLE=0` e' reale, non e' un bug runtime.
2. La causa principale e' la quality guard strict, soprattutto coverage.
3. La coverage monotona `>=0.50` non ha razionale scientifico come predittore di profitto sui dati correnti.
4. La strict attuale e' utile come guardia infrastrutturale anti-stale, ma non come criterio PAPER definitivo.
5. Il prossimo passo non deve essere "abbassare soglia coverage".
6. Il prossimo passo deve essere costruire una metrica outcome-driven di data quality:
   - coverage band, non soglia monotona;
   - distinct price points alto;
   - volume confirmation come feature non lineare;
   - validazione temporale su split.

## Prossimo Step Scientifico

Analisi consigliata prima di sviluppo:

1. confrontare `900s` vs `1800s` sull'horizon per capire se i reversal migliori richiedono 30 minuti;
2. costruire una matrice feature/outcome per data quality:
   - coverage band;
   - distinct band;
   - volume band;
   - famiglia simbolo;
   - MFE netto;
   - time-to-target;
3. definire `PAPER_ELIGIBLE` non da soglie manuali, ma da una nuova firma validata temporalmente.

Fino a quel momento PAPER resta correttamente bloccata.
