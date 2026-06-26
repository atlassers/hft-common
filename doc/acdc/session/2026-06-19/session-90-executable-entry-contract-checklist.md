# Session 90 - Executable Entry Contract Checklist

Data: 2026-06-19.

Obiettivo: refinement minimo della sessione 89. Applicare un contratto di ingresso eseguibile: una BUY long non deve essere ammessa se il rendimento netto immediatamente eseguibile rispetto all'entry reference dell'advice e' gia' minore o uguale alla loss-cap ML.

## Ipotesi Pre-Registrata

- Problema osservato: sessione 89 ha 4 loss, con advice freschi e live revalidation passante.
- Evidenza causale: `adviceToBuyNetMove` era circa `-0.0028` sui trade falliti, uguale alla `ml_advice_loss_cap_net_return`.
- Ipotesi: il modello pubblica advice non eseguibili perche' la loss-cap e' consumata dalla frizione iniziale di esecuzione.
- Refinement ammesso: enforcement del contratto ML esistente, non nuova guardia arbitraria.
- Metrica primaria: riduzione `BAD_ADVICE` zero-MFE causati da ingresso gia' alla loss-cap.
- Criterio di rigetto: nessun trade perche' tutti gli advice sono non eseguibili, oppure trade ancora con `adviceToBuyNetMove <= loss_cap`.

## Stato

- Codice ACDC executable entry contract: `WIP`.
- Config shared runtime: `WIP`.
- Build ACDC: `DONE`.
- Commit/push ACDC: `DONE`.
- Deploy Docker ACDC: `DONE`.
- RUN PAPER endpoint-driven: `DONE`.
- Forensics post-RUN: `DONE`.
- Analisi finale: `DONE_WIP_NEXT_SELL_REFINEMENT`.

## Checklist

- [x] Analizzare sessione 89 e identificare causa non hardware.
- [x] Verificare che `entry_price_reference` viene scritto al prezzo di promotion/scoring.
- [x] Verificare formula forensics `adviceToBuyNetMove = returnRate(advicePrice,buyPrice) - friction`.
- [x] Aggiungere diagnostica:
  - `ml_advice_entry_price_move`;
  - `ml_advice_entry_friction_net_return`;
  - `ml_advice_adverse_entry_move`;
  - `ml_advice_adverse_entry_loss_cap`;
  - `ml_advice_adverse_entry_pass`.
- [x] Far fallire `ml_advice_paper_eligible` se `ml_advice_adverse_entry_move <= ml_advice_loss_cap_net_return`.
- [x] Aggiungere config `rem.ml.live_advice.entry_friction_net_return=0.0028`.
- [x] Build ACDC senza test H2.
- [x] Commit/push.
- [x] Deploy Docker ACDC.
- [x] RUN PAPER endpoint-driven.
- [x] Diagnostics/forensics.
- [x] Result report.

## Evento Intermedio

- RUN iniziale interrotta dopo il ciclo 1 per evidenza diagnostica sufficiente.
- Promotion ciclo 1: `TREEUSDC`, `AVAXUSDC`.
- ACDC ha respinto `AVAXUSDC` con:
  - `ml_advice_adverse_entry_move=-0.0028`;
  - `ml_advice_adverse_entry_loss_cap=-0.0028`;
  - `ml_advice_adverse_entry_pass=0`.
- Diagnosi: ACDC sta applicando correttamente il contratto eseguibile, ma DocBrown pubblicava ancora advice con `loss_cap_net_return` hardcoded uguale alla frizione minima.
- Refinement successivo nello stesso step:
  - DocBrown deve pubblicare una loss-cap dinamica derivata dal q10 della `minNetReturn` dei casi positivi non-zero-MFE dello stesso simbolo/batch;
  - fallback ammesso: frizione esecutiva + buffer minimo configurabile.

## Esito RUN Post-Fix

- Finestra: `2026-06-19T20:16:42Z` - `2026-06-19T21:50:42Z`.
- Cicli: 21.
- Execution PAPER: `2`.
- Trade: 6.
- Win/loss ACDC: 3 / 3.
- Net execution: `+0.03695405041633`.
- Forensics:
  - zero post-buy MFE: 0 / 6;
  - GOOD_FLOW: 1 / 6;
  - BAD_SELL: 4 / 6;
  - BAD_ADVICE: 1 / 6.
- Classificazione: `PARTIAL_PASS_ENTRY_CONTRACT_FAIL_SELL_CAPTURE`.
- Anomalia endpoint: `POST /acdc/paper/stop/REM_CURRENT` ha restituito 500 dopo `stop-buy`; DB mostra comunque execution `2` `COMPLETED`, `reserved_budget=0`.
- Prossimo punto scientifico:
  - non aggiungere nuovi filtri BUY;
  - analizzare/refinare SELL capture e coerenza forensics/runtime MFE.
