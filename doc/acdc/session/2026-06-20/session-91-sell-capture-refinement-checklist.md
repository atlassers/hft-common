# Session 91 - SELL Capture Refinement Checklist

Data: 2026-06-20.

Obiettivo: fixare il bug endpoint `stop` idempotente e procedere col prossimo refinement del charter: SELL/capture, senza aggiungere filtri BUY.

## Ipotesi Pre-Registrata

- Sessione 90 ha validato parzialmente il contratto entry: `zeroPostBuyMfeTrades=0/6`.
- Problema residuo: `BAD_SELL=4/6`.
- Ipotesi SELL: il trailing si arma su MFE troppo piccola, non necessariamente eseguibile, e puo' chiudere prima che il movimento abbia maturato.
- Refinement ammesso: usare la soglia forensics gia' esistente di MFE eseguibile (`0.0005`) come `min_arm_net_return`.
- Non ammesso: nuovi filtri BUY o nuove guardie su simboli specifici.

## Stato

- Fix endpoint stop idempotente: `DONE`.
- Build ACDC: `DONE`.
- Commit/push ACDC: `DONE`.
- Deploy Docker ACDC: `DONE`.
- Prova PAPER endpoint-driven: `DONE_FAIL_BAD_ADVICE_DOMINANT`.
- Forensics post-RUN: `DONE`.
- Report finale: `DONE`.

## Checklist

- [x] Rendere `/acdc/paper/stop/{profileKey}` idempotente se l'ultima execution PAPER e' gia' terminale.
- [x] Rendere `/acdc/shadow/stop/{profileKey}` idempotente per coerenza.
- [x] Verificare endpoint stop su execution PAPER gia' completata: HTTP 200.
- [x] Preservare diagnostica advice in SELL (`ml_advice_age_seconds`, freshness, revalidation, adverse entry).
- [x] Aggiornare trailing metadata: `min_arm_net_return=0.0005`, `retention_ratio=0.80`.
- [x] Build ACDC.
- [x] Commit/push.
- [x] Deploy Docker.
- [x] Eseguire nuova prova PAPER.
- [x] Analizzare forensics.

## Esito RUN

- Finestra: `2026-06-19T22:17:05Z` - `2026-06-19T23:33:49Z`.
- Execution PAPER: `3`.
- Trade: 3.
- Win/loss ACDC: 1 / 2.
- Net: `-0.178331401112224720`.
- Forensics:
  - BAD_ADVICE: 2 / 3;
  - BAD_SELL: 1 / 3;
  - GOOD_FLOW: 0 / 3;
  - zero post-buy MFE: 2 / 3.
- Fix endpoint stop:
  - verificato HTTP 200 su execution gia' terminale;
  - verificato HTTP 200 nel flusso reale dopo `stop-buy`.
- Esito refinement SELL:
  - diagnostica SELL corretta: `adviceAgeSeconds` e `freshnessContractPass` valorizzati;
  - `min_arm_net_return=0.0005` ha evitato arming su rumore sotto soglia;
  - MEGAUSDC ha chiuso positiva via dynamic trailing, ma DocBrown la classifica ancora `BAD_SELL` per capture ratio basso.
- Classificazione: `FAIL_BAD_ADVICE_DOMINANT_WITH_PARTIAL_SELL_IMPROVEMENT`.
