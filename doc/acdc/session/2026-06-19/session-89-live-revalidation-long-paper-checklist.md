# Session 89 - Live Revalidation Long PAPER Checklist

Data: 2026-06-19.

Obiettivo: implementare il live revalidation gate richiesto dal charter e fare una prova PAPER lunga circa 90 minuti, endpoint-driven, per ottenere trade/dati e migliorare il modello senza deviare dal super piano.

## Ipotesi Pre-Registrata

- Ipotesi causale: parte dei `BAD_ADVICE` osservati nella sessione 88 deriva da advice rolling statisticamente validi ma non piu' compatibili con il regime live al momento della BUY.
- Metrica primaria attesa: riduzione dei trade `BAD_ADVICE` e dei trade zero post-buy MFE.
- Metrica secondaria da non peggiorare: non bloccare completamente la generazione di opportunita'; se non si fanno trade, la prova e' `INCONCLUSIVE_NO_TRADES`, non `PASS`.
- Dataset diagnosi: sessione 88, forensics `BAD_ADVICE=4/4`.
- Dataset selezione: rolling validation fresca della sessione 89.
- Dataset operativo: PAPER endpoint-driven circa 90 minuti.
- Baseline implicita: sessione 88 senza live revalidation range.
- Criterio accettazione: trade PAPER con forensics non inconclusiva e riduzione evidente di `BAD_ADVICE`/zero-MFE rispetto alla sessione 88, oppure rifiuti spiegati da `ml_advice_live_revalidation_pass=0`.
- Criterio rigetto: trade ancora classificati prevalentemente `BAD_ADVICE`, oppure nessun dato diagnostico sufficiente.

## Stato Generale

- Codice DocBrown live revalidation contract: `DONE`.
- Codice ACDC live revalidation enforcement: `DONE`.
- Build DocBrown: `DONE`.
- Build ACDC: `DONE`.
- Commit/push DocBrown: `DONE`.
- Commit/push ACDC: `DONE`.
- Deploy Docker DocBrown: `DONE`.
- Deploy Docker ACDC: `DONE`.
- Checklist realtime: `DONE`.
- Rolling validation fresca: `DONE_PASS`.
- Promotion PAPER: `DONE`.
- PAPER 90 minuti: `DONE_FAIL_STRATEGIC`.
- Forensics post-RUN: `DONE`.
- Analisi finale: `DONE`.

## DONE

- [x] DocBrown aggiunge `live_revalidation_ranges` in `advice_json`.
- [x] I range sono derivati da quantili 10/90 delle righe profittevoli dello stesso simbolo/batch.
- [x] ACDC richiede `age && drift && live_revalidation` prima di rendere `ml_advice_paper_eligible=1`.
- [x] ACDC salva feature diagnostiche `ml_advice_live_revalidation_*`.
- [x] Build senza test H2:
  - DocBrown: `./mvnw -DskipTests package`;
  - ACDC: `./mvnw -DskipTests package`.
- [x] Commit/push DocBrown: `a2c14e6 Add rolling advice live revalidation contract`.
- [x] Commit/push ACDC: `c7da8cb Enforce live advice revalidation ranges`.
- [x] Deploy Docker ACDC completato.
- [x] Deploy Docker DocBrown completato.
- [x] ACDC endpoint base verificato.
- [x] Rolling validation fresca eseguita:
  - batch `session89-live-revalidation-20260619-1630z`;
  - `strategicStatus=PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`;
  - selected `TUTUSDC`;
  - ulteriori PASS: `DOLOUSDC`, `TLMUSDC`.
- [x] Promotion PAPER eseguita:
  - `TUTUSDC` advice `5`;
  - `DOLOUSDC` advice `6`;
  - `TLMUSDC` advice `7`.
- [x] Prima apertura PAPER osservata:
  - `DOLOUSDC`;
  - buy `0.02537`;
  - advice `6`;
  - execution `1`.

## WIP

- [x] Eseguire rolling validation fresca da endpoint DocBrown.
- [x] Promuovere solo candidati `PASS_CANDIDATE`.
- [x] Avviare PAPER da endpoint ACDC/scheduler.
- [x] Eseguire cicli PAPER per circa 90 minuti, interrompendo/rilanciando solo se emergono problemi operativi.

## TODO

- [x] Salvare batch id, candidati, promotion rows e run ids.
- [x] Fermare BUY a fine finestra.
- [x] Chiudere/fermare PAPER da endpoint.
- [x] Eseguire diagnostics ACDC e forensics DocBrown.
- [x] Classificare esito: `FAIL_STRATEGIC_BAD_ADVICE_BAD_SELL`.
- [x] Aggiornare result report.

## Esito Finale

- Finestra operativa: `2026-06-19T17:30:16Z` - `2026-06-19T19:03:06Z`.
- Cicli endpoint-driven: 21.
- Trade sessione 89: 4.
- Win: 0.
- Loss: 4.
- Net sessione 89: `-0.350562017441750000`.
- Deadlock: non riprodotto.
- Duplicate same-symbol open positions: non riprodotte.
- BUY accettate con contratto live valido:
  - `DOLOUSDC`: advice age 4s, revalidation pass.
  - `METUSDC`: advice age 6s, revalidation pass.
  - `ETHFIUSDC`: advice age 8s, revalidation pass.
  - `EIGENUSDC`: advice age 18s, revalidation pass.
- Forensics:
  - `BAD_ADVICE`: `DOLOUSDC`, `ETHFIUSDC`, `EIGENUSDC`.
  - `BAD_SELL`: `METUSDC`.
- Motivo blocchi successivi dominante:
  - `PAPER_SESSION_ZERO_MFE_COOLDOWN`: 97332 decisioni.
- Classificazione: `FAIL_STRATEGIC_BAD_ADVICE_BAD_SELL`.
- Prossimo refinement ammesso dal charter:
  - correggere il contratto di ingresso con un controllo direzionale derivato dal contratto ML, non statico: una BUY long non deve essere eseguita se il movimento netto tra entry reference dell'advice e prezzo live e' gia' arrivato alla `ml_advice_loss_cap_net_return`;
  - investigare la discrepanza `METUSDC` tra MFE runtime ACDC (`0.000908296943231441`) e MFE forensics DocBrown (`0.023400873361431016`).
