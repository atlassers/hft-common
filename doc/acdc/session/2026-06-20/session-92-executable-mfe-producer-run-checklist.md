# Session 92 - Executable MFE Producer Run Checklist

Data: 2026-06-20.

Obiettivo: prossimo giro fino alle 08:00. Refinement minimo lato DocBrown producer per ridurre PAPER advice con `BAD_ADVICE` zero-MFE, poi RUN lunga endpoint-driven.

## Ipotesi Pre-Registrata

- Sessione 91: 2/3 trade `BAD_ADVICE`, entrambi zero post-buy MFE.
- Problema dominante: non SELL puro, ma advice pubblicati senza sufficiente probabilita' storica di MFE eseguibile.
- Refinement ammesso: il producer DocBrown pubblica PAPER advice solo se il campione rolling ha:
  - `executable_mfe_rate >= 0.70`;
  - `q10_positive_max_net_return >= 0.0005`.
- Non ammesso: aggiungere nuove guardie BUY runtime o filtri simbolo-specifici.

## Stato

- DocBrown producer executable MFE gate: `DONE`.
- Config shared runtime ACDC: `DONE`.
- Build DocBrown: `DONE`.
- Build ACDC: `DONE`.
- Commit/push: `DONE`.
- Deploy Docker: `DONE`.
- RUN PAPER fino alle 08:00: `DONE`.
- Stop endpoint-driven: `DONE`.
- Forensics/report finale: `DONE`.

## Esito RUN

- Execution: `4`.
- Orario: `2026-06-20T03:39:01Z` -> `2026-06-20T06:00:25Z` (`05:39:01` -> `08:00:25` CEST).
- Cicli rolling: `32`.
- Rolling PASS: `25`.
- Rolling FAIL selection bias: `7`.
- Promoted rows: `41`.
- Trade: `8`.
- Win/Loss: `5/3`.
- Net profit quote: `-0.05284424302425`.
- Zero post-buy MFE: `3/8`.
- BAD_ADVICE: `3/8`.
- BAD_SELL: `3/8`.
- BAD_TARGET: `1/8`.
- GOOD_FLOW: `1/8`.
- Avg advice age seconds: `7.25`.
- Avg capture ratio forensics: `0.2318751430349971`.

Verdetto: `FAIL_NET_NEGATIVE_PARTIAL_ADVICE_IMPROVEMENT`. Il producer gate migliora il problema BAD_ADVICE rispetto alla sessione 91, ma non lo risolve. Il run non valida ancora un modello profittevole: resta perdita netta, con residuo BAD_ADVICE zero-MFE e capture SELL insufficiente su parte dei trade vincenti.

## Checklist

- [x] Aggiungere metriche advice:
  - `executable_mfe_rate`;
  - `q10_positive_max_net_return`;
  - `min_executable_entry_edge`.
- [x] Bloccare publication PAPER advice se il campione rolling non supera i requisiti di MFE eseguibile.
- [x] Aggiungere config permanenti:
  - `rem.ml.live_advice.min_executable_entry_edge=0.0005`;
  - `rem.ml.live_advice.min_executable_mfe_rate=0.70`.
- [x] Build.
- [x] Commit/push.
- [x] Deploy Docker.
- [x] RUN fino alle 08:00.
- [x] Stop endpoint-driven.
- [x] Forensics.
- [x] Report finale sessione.
