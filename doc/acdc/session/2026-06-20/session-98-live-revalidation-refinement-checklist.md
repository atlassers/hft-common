# Session 98 - Live Revalidation Conservative Refinement Checklist

Data: 2026-06-20.

## Obiettivo

Rimuovere in modo controllato una singola feature non validata dal gate live revalidation, senza cambiare BUY, SELL, trailing o triage.

Feature candidata:

- `reversal_pre_trough_drop`.

Motivo scientifico:

- nel controfattuale PAPER `7`, ignorarla sblocca 1 advice e questo e' BAD_BLOCK;
- nell'aggregato execution `4,6,7`, ignorarla sblocca 1 advice e questo e' BAD_BLOCK;
- quindi non ha evidenza attuale di salvare trade negativi come hard gate;
- le combinazioni piu' ampie sbloccano prevalentemente BAD_BLOCK e non sono candidate.

## Vincoli

- Endpoint-driven.
- Nessuna modifica a BUY.
- Nessuna modifica a SELL.
- Nessuna modifica al trailing.
- Nessuna modifica al fast universe triage.
- Build, commit, push, Docker deploy prima della PAPER.
- Charter aggiornabile solo dopo PAPER con trade/dati sufficienti e risultato migliorativo.

## Criteri di Accettazione

- DocBrown genera nuove advice senza `reversal_pre_trough_drop` dentro `live_revalidation_ranges`.
- ACDC continua ad applicare live revalidation sulle feature restanti.
- PAPER autotick produce dati sufficienti per valutare:
  - advice promossi;
  - BUY;
  - SELL;
  - net PnL;
  - zero-MFE;
  - loss-cap exits;
  - live revalidation blocks residui;
  - timeline round-robin -> advice -> BUY -> SELL.
- PASS operativo solo se il risultato non peggiora rispetto alla run PAPER `7` e mostra almeno un trade o segnali sbloccati valutabili.

## Checklist Livello 1

- [x] L1.1 Pre-registrare piano e criteri.
- [x] L1.2 Implementare rimozione singola in DocBrown.
- [x] L1.3 Build DocBrown.
- [x] L1.4 Commit/push DocBrown.
- [x] L1.5 Deploy Docker DocBrown.
- [x] L1.6 Eseguire PAPER endpoint-driven con autotick.
- [x] L1.7 Analizzare trade, blocchi residui e timeline.
- [x] L1.8 Decidere PASS/FAIL.
- [x] L1.9 Aggiornare charter solo se PASS.
- [x] L1.10 Commit/push documentazione finale.

## Checklist Livello 2

### L2.1 Codice

- [x] Rimuovere `reversal_pre_trough_drop` da `LIVE_REVALIDATION_FEATURES`.
- [x] Lasciare invariati scoring, mining, promotion, BUY, SELL e trailing.
- [x] Verificare che la feature resti disponibile come diagnostica, non come hard gate live.

### L2.2 Deploy

- [x] Build Maven DocBrown senza H2 operational test.
- [x] Commit/push.
- [x] Docker build.
- [x] Recreate container `docbrown`.
- [x] Verifica health/log startup.

### L2.3 PAPER

- [x] Start PAPER da endpoint/script endpoint-driven.
- [x] Scheduler/promoter DocBrown da endpoint.
- [x] ACDC PAPER run da endpoint.
- [x] Durata sufficiente per generare dati.
- [x] Stop ordinato.

### L2.4 Analisi

- [x] Sintesi advice promossi.
- [x] Sintesi BUY/SELL.
- [x] PnL netto.
- [x] Motivi reject residui.
- [x] Timeline per trade.
- [x] Confronto con PAPER `7`.

## Stato Realtime

- Stato corrente: `PASS_PROVISIONAL_REFINEMENT__LIVE_GATE_NOT_FINAL`.
- Ultimo aggiornamento: execution `9` completata con universo allineato a 288 simboli. Risultato: 1 BUY/SELL `HEIUSDC`, net `+0.3230705383788`, exit `EXIT_ML_ADVICE_DYNAMIC_TRAILING`, capture ratio `0.6091855097410218`. Counterfactual live blocks: 11 advice bloccati, 5 GOOD_BLOCK, 6 BAD_BLOCK.

## Note Realtime

- Execution `8`: non valida per giudicare il refinement. Cicli iniziali hanno promosso simboli fuori dai 200 snapshot ACDC (`SYNUSDC`, `TLMUSDC`) oppure bloccati da feature residua (`DOLOUSDC` su `reversal_distance_from_trough`). Stop ordinato via endpoint, reserved budget `0`.
- Execution `9`: valida per giudicare il refinement. ACDC redeployato con `ACDC_INFLUX_SHADOW_SYMBOL_LIMIT=288`; DocBrown e ACDC hanno lavorato sullo stesso universo.
- La rimozione di `reversal_pre_trough_drop` da `LIVE_REVALIDATION_FEATURES` e' tecnicamente verificata: la feature non compare piu' tra i pass/fail live nei tick PAPER.
- Il gate residuo resta selettivo: molti advice freschi falliscono su `reversal_slope_short`, `reversal_slope_delta`, `reversal_quality`, `reversal_trough_age_seconds`, `reversal_distance_from_trough` o `raw_volume`.
- PASS provvisorio: la modifica non ha impedito un flusso completo profittevole e ha prodotto un trade positivo.
- Non-PASS finale: la live revalidation complessiva blocca ancora opportunita' profittevoli e richiede prossimo refinement controfattuale.

## Risultato Execution 9

Artifact:

`/tmp/session98-live-revalidation-refinement-aligned-aligned-run`

Metriche operative:

- Execution: `9`.
- Durata: `2026-06-20T14:12:08Z` -> `2026-06-20T14:44:16Z`.
- Universo ACDC: 288 simboli.
- Advice promossi e valutati prima del BUY: 11 bloccati da live revalidation.
- Trade: 1.
- Wins: 1.
- Losses: 0.
- Net profit quote: `0.3230705383788`.
- Avg net return: `0.012922821576763483`.
- Exit: `EXIT_ML_ADVICE_DYNAMIC_TRAILING`.

Timeline trade `HEIUSDC`:

- Round-robin start: `2026-06-20T14:32:04Z`.
- Round-robin completed: `2026-06-20T14:32:58Z`.
- Advice created: `2026-06-20T14:32:59Z`.
- BUY decision/open: `2026-06-20T14:33:15Z`.
- SELL decision/close: `2026-06-20T14:44:13Z`.
- Round-robin -> advice: `55s`.
- Valid-from -> BUY: `16s`.
- Open -> close: `658s`.

SELL capture:

- Net return: `0.012922821576763485`.
- Max net return: `0.021213278008298756`.
- Safe net return: `0.003`.
- Capture ratio: `0.6091855097410218`.
- Trailing armed: `true`.
- Trailing retention: `0.8`.
- Trailing fallout: `0.2`.
- Time to MFE: `570s`.
- MFE -> SELL: `85s`.

Counterfactual live blocks execution `9`:

- Blocked advice: 11.
- Blocked decisions: 111.
- Replayed: 11.
- GOOD_BLOCK: 5.
- BAD_BLOCK: 6.
- AMBIGUOUS_BLOCK: 0.

Failure summary:

- `reversal_quality`: 5 fail, 3 BAD_BLOCK, 2 GOOD_BLOCK.
- `reversal_slope_delta`: 7 fail, 4 BAD_BLOCK, 3 GOOD_BLOCK.
- `reversal_trough_age_seconds`: 6 fail, 3 BAD_BLOCK, 3 GOOD_BLOCK.
- `reversal_distance_from_trough`: 6 fail, 2 BAD_BLOCK, 4 GOOD_BLOCK.
- `reversal_slope_short`: 5 fail, 3 BAD_BLOCK, 2 GOOD_BLOCK.
- `raw_volume`: 1 fail, 1 BAD_BLOCK, 0 GOOD_BLOCK.

Ignore-feature grid execution `9`:

| Ignored features | Would pass | GOOD_BLOCK | BAD_BLOCK | AMBIGUOUS |
|---|---:|---:|---:|---:|
| `reversal_slope_short` | 2 | 1 | 1 | 0 |
| `reversal_slope_delta` | 1 | 0 | 1 | 0 |
| `reversal_quality` | 0 | 0 | 0 | 0 |
| `reversal_trough_age_seconds` | 0 | 0 | 0 | 0 |
| `reversal_distance_from_trough` | 0 | 0 | 0 | 0 |
| `raw_volume` | 0 | 0 | 0 | 0 |
| `reversal_slope_short,reversal_slope_delta` | 4 | 1 | 3 | 0 |
| `reversal_slope_delta,reversal_trough_age_seconds,reversal_distance_from_trough` | 3 | 2 | 1 | 0 |
| all residual live features | 11 | 5 | 6 | 0 |

Decisione:

- `reversal_pre_trough_drop` resta esclusa dal live hard gate come refinement provvisorio.
- Non rimuovere combinazioni ampie.
- Prossimo candidato: analisi controfattuale multi-execution su `reversal_slope_delta`, perche' nella execution `9` la sua rimozione isolata avrebbe sbloccato 1 BAD_BLOCK e 0 GOOD_BLOCK.
