# REM Blank Rolling Validation Checklist

Data: 2026-06-19.

Obiettivo: ridurre selection bias del blank generator con validazione rolling multi-finestra, scegliendo candidate solo da piu' finestre di selection e verificandole su holdout non visto.

## Stato Generale

- Stato: `DONE_FAIL_SELECTION_BIAS`.
- Endpoint rolling: `DONE_CODE`.
- Build DocBrown: `DONE`.
- Commit/push codice: `DONE`.
- Deploy Docker: `DONE`.
- Run endpoint: `DONE`.
- Risultato scientifico: `FAIL_SELECTION_BIAS`.

## DONE

- [x] Aggiunto endpoint `POST /docbrown/rem/blank-candidates/{profileKey}/rolling-validation`.
- [x] Aggiunti DTO request/report rolling.
- [x] Aggiunto repository query per batch candidate osservazionali.
- [x] Implementata generazione finestre rolling.
- [x] Implementata selezione su tutte le finestre tranne holdout.
- [x] Implementato holdout finale non visto.
- [x] Implementato stability score:
  - expectancy selection;
  - penalita' varianza;
  - penalita' finestra peggiore negativa;
  - penalita' supporto insufficiente.
- [x] Implementato status:
  - `PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`;
  - `FAIL_SELECTION_BIAS`;
  - `INCONCLUSIVE`.
- [x] Build DocBrown completata senza test H2: `./mvnw -DskipTests package`.
- [x] Codice DocBrown committato e pushato: `175826e Add blank REM rolling validation`.
- [x] Fix transazione rolling committato e pushato: `fa06ac0 Avoid nested transaction timeout in rolling validation`.
- [x] Preferenza candidati PASS committata e pushata: `20d4e56 Prefer passing rolling candidates`.
- [x] DocBrown deployato in Docker.
- [x] Endpoint rolling eseguito su MySQL/Influx reali.
- [x] Persistenza batch rolling verificata su MySQL.
- [x] Risultato documentato in `doc/session/session-86-blank-rolling-validation-result.md`.

## TODO

- [x] Commit/push DocBrown.
- [x] Deploy DocBrown container.
- [x] Eseguire endpoint rolling su MySQL/Influx reali.
- [x] Persistenza righe candidate rolling verificata su MySQL.
- [x] Documentare report e decisione:
  - candidate promuovibile;
  - failure per selection bias;
  - inconclusive per dati insufficienti.

## Risultato Endpoint

- Batch: `session86-rolling-20260619-1430z`.
- Righe persistite: `8640`.
- Finestre: 5 da 10 minuti.
- Righe per finestra: `1728`.
- `strategicStatus=FAIL_SELECTION_BIAS`.
- Candidato selezionato: `symbol=CUSDC`.
- Candidato selezionato:
  - `selectionAvgEndNetReturn=0.000382272744610865`;
  - `selectionWorstWindowAvgEndNetReturn=-0.001186902208620121`;
  - `holdoutAvgEndNetReturn=0.00023030303030303`;
  - `holdoutPositiveEndRate=0.8333333333333334`;
  - `holdoutWinRateLower95=0.43649056343635395`.
- Candidate interessante ma non promuovibile: `ACTUSDC`, holdout positivo ma una finestra selection negativa.
- Decisione: nessuna PAPER.

## Criterio Scientifico

Una candidate non e' promuovibile se:

- vince solo per un picco singolo in selection;
- ha una finestra selection negativa;
- ha holdout medio non positivo;
- Wilson 95% del win-rate holdout non supera 50%;
- supporto per finestra insufficiente.

Nessuna PAPER deve partire se il rolling endpoint non produce `PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`.
