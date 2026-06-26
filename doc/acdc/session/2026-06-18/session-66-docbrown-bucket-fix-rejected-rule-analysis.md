# Session 66 - DocBrown bucket fix and rejected rule analysis

Data: 2026-06-18

## Obiettivo

Correggere i bucket Influx usati da DocBrown e capire se le rule scartate contenevano segnali buoni.

## Fix applicata

DocBrown `docker-compose.yml` usava nomi bucket con underscore:

- `binance_realtime`;
- `binance_microbar`.

I bucket reali sono con trattino:

- `binance-realtime`;
- `binance-microbar`.

Fix DocBrown:

- commit `MS681: align DocBrown Influx bucket names`;
- container DocBrown ricreato;
- env verificato:
  - `INFLUXDB_BUCKET=binance`;
  - `INFLUXDB_REALTIME_BUCKET=binance-realtime`;
  - `INFLUXDB_MICROBAR_BUCKET=binance-microbar`.

## Stato Influx

Bucket reali:

- `binance`: retention 48h;
- `binance-realtime`: retention 1h;
- `binance-microbar`: retention 1h.

Influxer era stato riavviato e ha completato il repair alle `2026-06-18 08:12:00 UTC`.

Copertura dopo repair:

- `binance-realtime`: 288 simboli negli ultimi 10 minuti;
- `binance-microbar`: 288 simboli negli ultimi 10 minuti.

## Cicli DocBrown dopo fix

Prima del repair completo:

- DocBrown generava band `DIAGNOSTIC`;
- `promotedRules=0`;
- nessuna PAPER avviata.

Dopo repair completo:

- batch manuale alle `2026-06-18 08:22 UTC`;
- outcome mining:
  - `samplesCreated=236`;
  - `samplesUpdated=4764`;
  - `good=1882`;
  - `bad=3118`;
  - `promotedSignatures=106`.

Questo indica che il reversal non era perso: lo stadio outcome vedeva segnali profittevoli.

## Band ACTIVE aggiornata

Nuova band attiva:

- id `26`;
- model version `REM-BAND-2026-06-18T082521.210264345-fce60ada`;
- coverage `0.15..0.50`;
- distinct price points min `15`;
- validation pass samples `126`;
- validation good/bad pass `74/52`;
- precision good `0.587301587301587302`;
- avg MFE `0.004461596414052210`;
- score `1.333866968727879278`;
- `strict_eligible=true`.

## Rule scartate

DocBrown ha prodotto 10 candidate `reversal_ml_rule` su `band_model_id=26`, tutte `REJECTED`.

Le migliori:

- `FAMILY UNKNOWN / pullback_rebound_volume`
  - validation samples `4`;
  - validation profit rate `0.75`;
  - validation avg return `0.015358637438845487`;
  - economics pass `true`;
  - data quality pass `true`;
  - live zero MFE loss rate `0.5714`;
  - live under-safe non-profit rate `0.8571`;
  - rejected.
- `FAMILY UNKNOWN / quality_structure`
  - validation samples `5`;
  - validation profit rate `0.60`;
  - validation avg return `0.009153489895106322`;
  - economics pass `true`;
  - data quality pass `true`;
  - live zero MFE loss rate `0.60`;
  - live under-safe non-profit rate `0.80`;
  - rejected.

## Interpretazione

Non abbiamo rifiutato rule buone in modo arbitrario.

Le candidate piu' promettenti passavano economics e data-quality, ma fallivano requisiti scientifici importanti:

- validation samples sotto `rem.ml.min.validation.samples=12`;
- live audit molto negativo rispetto ai limiti:
  - max zero MFE loss rate `0.34`;
  - max under-safe non-profit rate `0.34`.

Le global rule avevano piu' campioni, ma fallivano economics oppure live audit.

## Decisione

Non avviare PAPER finche' non compare una rule `PAPER_ELIGIBLE`.

La pipeline sta facendo il suo lavoro:

1. Influx corretto e popolato.
2. Outcome mining vede segnali profittevoli.
3. Reversal ML rifiuta quelli con poca numerosita' o live audit fragile.
4. ACDC non compra senza advice robusta.

## Prossimo passo

Lasciare DocBrown in esecuzione continua. Se la nuova band `26` resta valida e produce candidate con live audit accettabile, DocBrown inviera' automaticamente il `paper-signal` ad ACDC.
