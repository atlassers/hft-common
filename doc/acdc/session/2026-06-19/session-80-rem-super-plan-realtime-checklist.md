# REM Super Plan Realtime Checklist

Data: 2026-06-19.

Obiettivo: implementare il super piano scientifico/ingegneristico REM senza introdurre guardie reattive non validate.

## Stato

- [x] Checklist realtime creata.
- [x] Diagnostica forense con classificazione causale completa.
- [x] Preflight scientifico con ipotesi/dataset/baseline.
- [x] Report multi-finestra e baseline minime.
- [x] Intervallo d'incertezza multi-finestra richiesto dall'acceptance.
- [x] Advice top-N/freshness/target realistico in DocBrown.
- [x] ACDC freshness contract e SELL capture audit con timing MFE/trailing.
- [x] Build, commit, push.
- [x] Deploy Docker.
- [x] Verifica endpoint.

## Regole Di Avanzamento

- Ogni step operativo passa da endpoint o FE.
- Ogni gruppo di modifiche codice viene rilasciato in container prima della verifica.
- Nessuna nuova guardia operativa puo' essere accettata senza ipotesi, baseline, split temporale e report forense.
- Ogni esito deve distinguere BAD_ADVICE, LATE_BUY, BAD_SELL, BAD_TARGET, BAD_EXECUTION, GOOD_FLOW e INCONCLUSIVE_DATA.

## Log

- 2026-06-19: checklist inizializzata.
- 2026-06-19: avviata estensione diagnostica forense DocBrown con classificazione causale.
- 2026-06-19: diagnostica forense estesa e build DocBrown OK; avviato preflight scientifico.
- 2026-06-19: preflight scientifico implementato e build DocBrown OK; avviato report multi-finestra/baseline.
- 2026-06-19: report multi-finestra con baseline NO_TRADE implementato e build DocBrown OK; avviato top-N/freshness/target realistico.
- 2026-06-19: top-N/freshness/target realistico implementato in DocBrown e build OK; avviato contratto freshness ACDC.
- 2026-06-19: freshness contract ACDC implementato, V61 config aggiunta, build ACDC OK; avviato commit/push/deploy.
- 2026-06-19: commit/push completati (`docbrown:e05afa2`, `acdc:89e83c8`); avviato deploy Docker.
- 2026-06-19: deploy Docker completato, ACDC Flyway V61 applicata; avviata verifica endpoint.
- 2026-06-19: preflight endpoint PASS con warning su baseline finale; forensics endpoint restituisce classificazioni causali; multi-window endpoint operativo; scoring live verifica top-N con `savedAdvice=5`; advice active contengono freshness/target contract.
- 2026-06-19: completata estensione finale prima della verifica: DocBrown espone incertezza 95% cross-window e ACDC persiste timing MFE, retention/fallout trailing e loss-cap dinamico nelle SELL PAPER.

## Esito Primo Gruppo

- DocBrown commit: `e05afa2`.
- ACDC commit: `89e83c8`.
- Deploy Docker completato.
- ACDC migration `V61` applicata su MySQL.
- Nessuna PAPER/REAL RUN avviata in questo gruppo.

## Limiti Operativi Per Verifica Finale

- Baseline automatiche implementate: `NO_TRADE`, `RANDOM_TOP_VOLUME`, `NAIVE_REVERSAL`.
- SELL capture persistita in ACDC con `max_net_return`, `max_net_return_at_seconds`, capture ratio, target hit, trailing armed, retention/fallout, dynamic loss cap e tempi BUY->MFE/MFE->SELL.
- La verifica finale deve usare almeno due finestre valutabili, altrimenti l'incertezza multi-finestra resta `INCONCLUSIVE`.
- Forensics su execution storiche puo' restare `INCONCLUSIVE_DATA` se Influx non conserva microbar sufficienti; la verifica conclusiva va fatta su RUN nuova con executionId forniti.
