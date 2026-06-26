# Session 99 - Live Revalidation Slope Delta Refinement Checklist

Data: 2026-06-20.

## Obiettivo

Verificare e, se validato, rimuovere `reversal_slope_delta` dal set di feature usate come hard gate live in `live_revalidation_ranges`.

## Razionale Scientifico

La sessione 98 ha dimostrato:

- `reversal_pre_trough_drop` fuori dal hard gate produce un trade completo profittevole;
- il gate residuo non e' finale: execution `9` ha ancora bloccato molte opportunity;
- `reversal_slope_delta` e' candidato perche' in execution `9` la sua rimozione isolata avrebbe sbloccato un advice profittevole e nessun advice perdente.

Controfattuale multi-execution pre-implementazione:

| Execution | Ignore | Would pass | GOOD_BLOCK | BAD_BLOCK | AMBIGUOUS |
|---|---|---:|---:|---:|---:|
| 4 | `reversal_slope_delta` | 0 | 0 | 0 | 0 |
| 6 | `reversal_slope_delta` | 0 | 0 | 0 | 0 |
| 7 | `reversal_slope_delta` | 0 | 0 | 0 | 0 |
| 9 | `reversal_slope_delta` | 1 | 0 | 1 | 0 |
| 4,6,7,9 | `reversal_slope_delta` | 1 | 0 | 1 | 0 |

Nota: in questa diagnostica `BAD_BLOCK` significa che il gate ha bloccato un advice che avrebbe prodotto opportunity profittevole; quindi e' evidenza contro la feature come hard gate.

## Vincoli

- Nessuna modifica a BUY.
- Nessuna modifica a SELL.
- Nessuna modifica al trailing.
- Nessuna modifica a ranking/mining/round-robin.
- Modifica unica ammessa: rimuovere `reversal_slope_delta` da `LIVE_REVALIDATION_FEATURES`.
- Build, commit, push e Docker deploy prima della PAPER.
- PAPER endpoint-driven.
- Charter aggiornabile solo se la run produce dati migliorativi o almeno non peggiorativi.

## Checklist Livello 1

- [x] L1.1 Eseguire controfattuale multi-execution.
- [x] L1.2 Pre-registrare piano e criteri.
- [x] L1.3 Implementare rimozione singola in DocBrown.
- [x] L1.4 Build DocBrown.
- [x] L1.5 Commit/push DocBrown.
- [x] L1.6 Deploy Docker DocBrown.
- [x] L1.7 Eseguire PAPER endpoint-driven.
- [x] L1.8 Analizzare trade, blocchi residui e timeline.
- [x] L1.9 Decidere PASS/FAIL.
- [x] L1.10 Aggiornare charter solo se PASS.
- [x] L1.11 Commit/push documentazione finale.

## Checklist Livello 2

### L2.1 Codice

- [x] Rimuovere `reversal_slope_delta` da `LIVE_REVALIDATION_FEATURES`.
- [x] Verificare che `reversal_pre_trough_drop` resti fuori.
- [x] Verificare che `reversal_slope_delta` resti disponibile come feature diagnostica.

### L2.2 PAPER

- [x] Universo ACDC 288 verificato.
- [x] DocBrown Docker nuovo verificato.
- [x] Start PAPER da endpoint.
- [x] Rolling validation DocBrown da endpoint.
- [x] Promotion DocBrown da endpoint.
- [x] ACDC PAPER tick/scheduler da endpoint/container.
- [x] Stop-buy e drain ordinati.

### L2.3 Analisi

- [x] Advice promossi.
- [x] BUY/SELL.
- [x] Net PnL.
- [x] Exit reasons.
- [x] Timeline round-robin -> advice -> BUY -> SELL.
- [x] Counterfactual live blocks post-run.
- [ ] Ignore-feature grid post-run.

## Stato Realtime

- Stato corrente: `FAIL_REVERTED`.
- Ultimo aggiornamento: execution `10` fermata anticipatamente dopo evidenza sufficiente. Risultato finale: 6 trade, 3 win, 3 loss, net `-0.04795612367618`; 2 loss-cap zero/low-MFE. `reversal_slope_delta` e' stato ripristinato nel live hard gate, build/push/deploy DocBrown completati.

## Note Realtime

- Ciclo 1: 7 advice promossi, 2 BUY (`JUPUSDC`, `WALUSDC`), `slope_delta` assente dai pass/fail live.
- `JUPUSDC`: chiusa con `EXIT_ML_ADVICE_DYNAMIC_TRAILING`, net `+0.1958169290336`, max net return `0.014715551181102363`.
- Ciclo 2: nuova BUY `YGGUSDC`; `YGGUSDC` chiusa con `EXIT_ML_ADVICE_DYNAMIC_TRAILING`, net `+0.04239733628424`.
- Ciclo 3: nuove BUY `RAYUSDC`, `METUSDC`, `SAGAUSDC`; `SAGAUSDC` chiusa in loss-cap, net `-0.12410979224952`, max net return `0`.
- Cicli 4-5: capitale parzialmente bloccato; `RAYUSDC`, `WALUSDC`, `METUSDC` in HOLD con MFE corrente `0`.
- Stop anticipato: reserved `0`, realized `-0.04795612367618`.
- Risultato finale execution `10`: 6 trade, 3 win, 3 loss, avg net return `-0.000319707491498765`, loss-cap exits `2`.
- Counterfactual post-run: 23 advice bloccati, GOOD_BLOCK `10`, BAD_BLOCK `6`, AMBIGUOUS `7`.
- Verdict: FAIL. La rimozione di `slope_delta` aumenta BUY e trade, ma introduce zero-MFE/loss-cap e capitale bloccato. Nessun aggiornamento charter come regola positiva.
- Rollback: DocBrown commit `910a678`, container `docbrown:latest` redeployato. Stato attivo: `reversal_pre_trough_drop` fuori dal gate, `reversal_slope_delta` dentro.
