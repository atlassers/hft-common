# REM Feature Regime Rolling Checklist

Data: 2026-06-19.

Obiettivo: superare il limite `symbol-only` della rolling validation aggiungendo candidate globali basate su feature/regime, senza avviare PAPER se non passa il filtro scientifico.

## Stato Generale

- Stato: `DONE_PASS_CANDIDATE_REQUIRES_PREFLIGHT_THEN_PAPER_FAIL_LIVE_DECAY`.
- Codice feature/regime: `DONE`.
- Build DocBrown: `DONE`.
- Commit/push codice: `DONE`.
- Deploy Docker: `DONE`.
- Run endpoint: `DONE`.
- Risultato scientifico: `PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`.
- PAPER successiva: `FAIL_LIVE_DECAY_BAD_ADVICE`, documentata in `session-88-rolling-paper-trial-result.md`.

## DONE

- [x] Estesa rolling validation oltre `symbol-only`.
- [x] Aggiunte candidate feature:
  - `reversal_quality` quantile 50/75;
  - `raw_volume` quantile 50/75;
  - `reversal_direction_score > 0`;
  - `reversal_slope_short > 0`;
  - `reversal_slope_delta > 0`;
  - `reversal_pre_trough_drop < 0`.
- [x] Aggiunti pattern combinati:
  - `quality>=median AND slope_short>0`;
  - `quality>=median AND slope_delta>0`;
  - `quality>=median AND volume>=median`;
  - `drop<0 AND slope_short>0`;
  - `direction>0 AND volume>=median`.
- [x] Parsing feature da `acdc_rem_observation_candidate.feature_json`.
- [x] Build DocBrown completata senza test H2: `./mvnw -DskipTests package`.
- [x] Codice DocBrown committato e pushato: `561eff6 Add feature regime rolling candidates`.
- [x] DocBrown deployato in Docker.
- [x] Endpoint rolling feature/regime eseguito su MySQL/Influx reali.
- [x] Batch valido persistito su MySQL: `session87-feature-rolling-20260619-1510z`.
- [x] Risultato documentato in `doc/session/session-87-feature-regime-rolling-result.md`.
- [x] Endpoint promotion PAPER breve implementato:
  - `POST /docbrown/rem/blank-candidates/{profileKey}/rolling-paper-promotion`;
  - validita' breve;
  - max buy age breve;
  - entry reference corrente;
  - drift massimo 0.15%.
- [x] Codice promotion committato e pushato: `da38715 Add rolling PAPER promotion endpoint`.

## TODO

- [x] Commit/push DocBrown.
- [x] Deploy DocBrown container.
- [x] Eseguire endpoint rolling feature/regime su MySQL/Influx reali.
- [x] Verificare persistenza batch su MySQL.
- [x] Documentare `PASS_CANDIDATE`, `FAIL_SELECTION_BIAS` o `INCONCLUSIVE`.

## Risultato Endpoint

- Batch valido: `session87-feature-rolling-20260619-1510z`.
- Righe persistite: `8640`.
- `strategicStatus=PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`.
- Candidato selezionato: `symbol=EDENUSDC`.
- `selectionAvgEndNetReturn=0.017884026856629875`.
- `selectionWorstWindowAvgEndNetReturn=0.004955701032645089`.
- `holdoutAvgEndNetReturn=0.021754406457162503`.
- `holdoutPositiveEndRate=1`.
- `holdoutWinRateLower95=0.6096569663469354`.
- Secondo candidato positivo: `BANKUSDC`.
- Controllo negativo utile: `GENIUSUSDC` positivo in selection ma fallito in holdout.

## Regola

Nessuna PAPER se il risultato non e' `PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`.

Nota successiva: la PAPER controllata ha fallito per decadimento live dell'advice. Da ora `PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT` e' necessario ma non sufficiente: serve live revalidation prima di promotion/BUY.
