# Session 55 - Outcome-Driven REM Quality Bands

Data: 2026-06-17.

## Obiettivo

Integrare nella pipeline ML il risultato scientifico della sessione 54:

- la coverage monotona `>= 0.50` non separava GOOD da BAD;
- le zone piu' profittevoli erano fasce outcome-driven;
- PAPER deve usare advice con data quality coerente con outcome storici, non soglie arbitrarie.

## Fascia Introdotta

Nuova qualita' ML per `PAPER_ELIGIBLE`:

- coverage band: `0.20 <= reversal_data_coverage_ratio < 0.50`;
- distinct price points: `>= 20`;
- max gap: `<= 60s`;
- high-volume boundary diagnostico: `volume_confirmation >= 1.50`;
- band ratio minimo: `0.50`.

La rule passa data quality se almeno il `50%` dei campioni matchati ricade nella fascia.

Razionale:

- coverage `0.20-0.50` aveva good-rate e MFE migliori di coverage alta;
- distinct alto era la componente piu' coerente nelle combinazioni migliori;
- volume alto migliora la lettura ma non e' hard gate singolo;
- ratio `0.50` richiede maggioranza dei campioni matchati nella fascia.

## Configurazione DB

Nuova migration:

`V43__rem_ml_outcome_driven_data_quality_bands.sql`

Nuove chiavi:

- `rem.ml.data_quality.outcome_band.coverage.min = 0.20`;
- `rem.ml.data_quality.outcome_band.coverage.max_exclusive = 0.50`;
- `rem.ml.data_quality.outcome_band.distinct_price_points.min = 20`;
- `rem.ml.data_quality.outcome_band.volume.high_min = 1.50`;
- `rem.ml.data_quality.outcome_band.min_ratio = 0.50`.

Le vecchie chiavi `rem.ml.data_quality.min.*` restano per guardie runtime/infrastrutturali, ma il miner DocBrown usa le fasce outcome-driven per classificare `PAPER_ELIGIBLE`.

## Implementazione DocBrown

DocBrown ora calcola `DataQualityAdvice` con:

- `outcome_band_ratio`;
- `outcome_band_high_volume_ratio`;
- `passes`.

Il `rule_json.data_quality` contiene:

- `mode = OUTCOME_DRIVEN_BANDS`;
- `outcome_band_passes`;
- `outcome_band_ratio`;
- `outcome_band_high_volume_ratio`;
- i parametri DB usati.

`promotion_class = PAPER_ELIGIBLE` richiede ancora:

- validation samples minimi;
- validation profit rate;
- validation average net return positivo;
- advice economics;
- live audit;
- nuova data quality outcome-driven.

## Verifiche

Build:

- ACDC: `./mvnw -q test && ./mvnw -q package` OK;
- DocBrown: `./mvnw -q test && ./mvnw -q package` OK.

Runtime:

- ACDC container ricostruito/riavviato;
- Flyway reale applicato a `v43`;
- endpoint `/acdc/profiles` OK.

DocBrown rigenerato con signal ACDC disabilitato:

- outcome scanned points: `5000`;
- ML evaluated rules: `275`;
- ML promoted rules: `15`.

Classi prodotte:

| Class | Count | Avg Score | Avg Profit Rate | Avg Return |
| --- | ---: | ---: | ---: | ---: |
| `PAPER_ELIGIBLE` | `2` | `1.961686` | `0.8750` | `0.018939` |
| `PURE_REVERSAL_OBSERVED` | `13` | `0.184274` | `0.2308` | `0.001141` |

Rule `PAPER_ELIGIBLE`:

| Symbol | Rule | Score | Validation Samples | Profit Rate | Avg Return | Band Ratio | High Volume Ratio | Duration | Validity |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `ALLOUSDC` | `curve_reversal` | `3.430185` | `17` | `1.0000` | `0.031521` | `1.0` | `0.0588` | `585s` | `330s` |
| `BANANAS31USDC` | `quality_structure` | `0.493188` | `20` | `0.7500` | `0.006357` | `1.0` | `0.35` | `305s` | `60s` |

## Lettura

Questa e' la prima rigenerazione in cui la pipeline produce advice `PAPER_ELIGIBLE`.

Il risultato e' coerente con l'obiettivo:

- le advice PAPER-ready sono poche;
- hanno validation samples sufficienti;
- hanno ritorno medio netto positivo;
- hanno outcome-band ratio pieno;
- le osservazioni rumorose restano `PURE_REVERSAL_OBSERVED`.

## Prossimo Step

Non e' stata avviata PAPER in questa sessione perche' il mining e' stato eseguito con signal disabilitato e le advice sono temporali.

Prossimo passo operativo:

1. committare/pushare questa iterazione;
2. rilanciare DocBrown con signal abilitato;
3. far partire ACDC sul primo advice promosso;
4. se almeno una advice e' `PAPER_ELIGIBLE`, avviare una PAPER breve controllata, non SHADOW esplorativa;
5. misurare MFE netto, net profit e reason di SELL.
