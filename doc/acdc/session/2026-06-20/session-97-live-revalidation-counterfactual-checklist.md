# Session 97 - Live Revalidation Counterfactual Checklist

Data: 2026-06-20.

## Obiettivo

Verificare scientificamente se il gate `ml_advice_live_revalidation_pass=0` sta:

- salvando da trade perdenti;
- bloccando trade profittevoli;
- oppure producendo un filtro ambiguo/non calibrato.

## Vincoli

- Endpoint-driven.
- Nessuna modifica a BUY/SELL/trailing.
- Nessun allentamento guardie senza counterfactual.
- Build, commit, push e deploy Docker prima della validazione operativa.
- Charter aggiornabile solo se il risultato e' scientificamente robusto.

## Checklist Livello 1

- [x] L1.1 Creare checklist.
- [x] L1.2 Implementare endpoint counterfactual promoted-blocked.
- [x] L1.3 Build ACDC.
- [x] L1.4 Commit/push ACDC.
- [x] L1.5 Deploy Docker ACDC.
- [x] L1.6 Validare endpoint su PAPER `7`.
- [x] L1.7 Analizzare GOOD_BLOCK / BAD_BLOCK / AMBIGUOUS_BLOCK.
- [x] L1.8 Decidere se aggiornare charter.
- [x] L1.9 Implementare simulazione shadow `ignoreFeatures`.
- [x] L1.10 Validare combinazioni candidate.
- [ ] L1.11 Commit/push documentazione finale.

## Checklist Livello 2

### L2.1 Dati

- [x] Leggere `PaperDecision` per execution.
- [x] Filtrare `reason=ML_ADVICE_NOT_PAPER_ELIGIBLE`.
- [x] Richiedere `ml_advice_id` presente.
- [x] Estrarre feature di fallimento live revalidation.

### L2.2 Replay

- [x] Usare prezzo decisione come entry counterfactual.
- [x] Usare `createdAt` decisione come entry time.
- [x] Leggere tick futuri Influx.
- [x] Calcolare net return con fee runtime.
- [x] Calcolare max net return e end net return.

### L2.3 Classificazione

- [x] `BAD_BLOCK_TARGET`: max net return raggiunge safe target advice.
- [x] `BAD_BLOCK_POSITIVE_MFE`: max net return positivo ma sotto safe target.
- [x] `GOOD_BLOCK_ZERO_MFE`: max net return <= 0.
- [x] `GOOD_BLOCK_END_LOSS`: end net return <= 0 senza target.
- [x] `NO_FUTURE_DATA`: dati insufficienti.

### L2.4 Verdict

- [x] Conteggio per verdict.
- [x] Conteggio feature fallite.
- [x] Net/MFE medio per gruppo.
- [x] Raccomandazione del Consiglio.

### L2.5 Ricalibrazione Shadow

- [x] Aggiungere parametro endpoint `ignoreFeatures`.
- [x] Calcolare `wouldPassWithIgnoredFeatures`.
- [x] Calcolare GOOD/BAD/AMBIGUOUS tra advice sbloccati.
- [x] Testare combinazioni sospette.
- [x] Decidere se una combinazione e' candidata a PAPER.

## Stato Realtime

- Stato corrente: `READY_FOR_CONTROLLED_REFINEMENT`.
- Ultimo aggiornamento: shadow `ignoreFeatures` validato su PAPER `7` e aggregato su execution `4,6,7`. La rimozione isolata di `reversal_pre_trough_drop` non recupera GOOD_BLOCK e sblocca solo BAD_BLOCK nel campione disponibile. La combinazione ampia sblocca anche GOOD_BLOCK, ma con prevalenza BAD_BLOCK, quindi non e' candidata.

## Risultato Validazione PAPER 7

Endpoint:

`GET /diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=7&horizonSeconds=900`

Artifact:

`/tmp/session97-live-revalidation-counterfactual-exec7-dedup.json`

Metriche:

- Advice bloccati unici: 5.
- Decisioni bloccate totali: 72.
- Replayed: 5.
- No future data: 0.
- GOOD_BLOCK: 1.
- BAD_BLOCK: 3.
- AMBIGUOUS_BLOCK: 1.

Dettaglio:

- `ATUSDC` advice `142`: GOOD_BLOCK, max net return `-0.002`, end `-0.006842797783933518`.
- `AIGENSYNUSDC` advice `144`: AMBIGUOUS_BLOCK, max `0.000644856278366112`, sotto safe target.
- `BABYUSDC` advice `145`: BAD_BLOCK, max `0.002179916317991632`, safe `0.001049952591778151`.
- `AIGENSYNUSDC` advice `146`: BAD_BLOCK, max `0.004036253776435045`, safe `0.001642561532681941`.
- `HEIUSDC` advice `147`: BAD_BLOCK, max `0.006294280442804428`, safe `0.003`.

Feature failure summary:

- `reversal_quality`: 1 fail, 1 GOOD_BLOCK.
- `reversal_distance_from_trough`: 3 fail, 1 BAD_BLOCK, 1 GOOD_BLOCK, 1 AMBIGUOUS_BLOCK.
- `reversal_slope_delta`: 2 fail, 1 BAD_BLOCK, 1 AMBIGUOUS_BLOCK.
- `reversal_pre_trough_drop`: 3 fail, 2 BAD_BLOCK, 1 AMBIGUOUS_BLOCK.
- `reversal_trough_age_seconds`: 3 fail, 2 BAD_BLOCK, 1 AMBIGUOUS_BLOCK.

## Risultato Shadow Ignore Features

Endpoint:

`GET /diagnostics/acdc/paper/live-revalidation-counterfactual?executionIds=7&horizonSeconds=900&ignoreFeatures=...`

Artifact:

`/tmp/session97-ignore-feature-grid.tsv`

PAPER `7`:

| Ignored features | Would pass | GOOD | BAD | AMBIGUOUS |
|---|---:|---:|---:|---:|
| `reversal_pre_trough_drop` | 1 | 0 | 1 | 0 |
| `reversal_trough_age_seconds` | 0 | 0 | 0 | 0 |
| `reversal_pre_trough_drop,reversal_trough_age_seconds` | 1 | 0 | 1 | 0 |
| `reversal_pre_trough_drop,reversal_trough_age_seconds,reversal_distance_from_trough` | 2 | 0 | 2 | 0 |
| `reversal_pre_trough_drop,reversal_trough_age_seconds,reversal_slope_delta` | 2 | 0 | 2 | 0 |
| `reversal_pre_trough_drop,reversal_trough_age_seconds,reversal_distance_from_trough,reversal_slope_delta` | 4 | 0 | 3 | 1 |

Aggregato execution `4,6,7`:

| Ignored features | Total blocked | GOOD | BAD | AMBIGUOUS | Would pass | Would pass GOOD | Would pass BAD | Would pass AMBIGUOUS |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| none | 32 | 11 | 17 | 4 | 0 | 0 | 0 | 0 |
| `reversal_pre_trough_drop` | 32 | 11 | 17 | 4 | 1 | 0 | 1 | 0 |
| `reversal_pre_trough_drop,reversal_trough_age_seconds` | 32 | 11 | 17 | 4 | 3 | 0 | 3 | 0 |
| `reversal_pre_trough_drop,reversal_trough_age_seconds,reversal_distance_from_trough,reversal_slope_delta` | 32 | 11 | 17 | 4 | 11 | 2 | 8 | 1 |

Decisione:

- nessuna combinazione e' validata per enforcement come allentamento generale;
- `reversal_pre_trough_drop` e' candidata solo a rimozione conservativa dal set di hard live revalidation, perche' non dimostra capacita' di bloccare GOOD in questo campione;
- la rimozione deve essere testata in PAPER endpoint-driven senza modifiche a BUY/SELL/trailing.

## Decisione Charter

Non aggiornare il charter come regola operativa: il gate live revalidation blocca troppi advice che avrebbero raggiunto il safe target.

Nuovo vincolo operativo consigliato prima di qualsiasi enforcement:

- ogni modifica a `live_revalidation_ranges` deve essere validata con counterfactual BAD_BLOCK/GOOD_BLOCK;
- target minimo provvisorio: BAD_BLOCK <= GOOD_BLOCK su piu' execution, non su singola run;
- evitare feature che bloccano prevalentemente BAD_BLOCK (`reversal_pre_trough_drop`, `reversal_trough_age_seconds`) finche' non sono ricalibrate.
