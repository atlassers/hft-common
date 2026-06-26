# REM Feature Regime Rolling Result

Data: 2026-06-19.

Endpoint: `POST /docbrown/rem/blank-candidates/REM_CURRENT/rolling-validation`.

## Run Inconclusiva Per Retention

Batch: `session87-feature-rolling-20260619-1430z`.

- Finestre: 2026-06-19T14:30:00Z -> 2026-06-19T15:20:00Z.
- Righe persistite: `3456`.
- Le prime tre finestre erano gia' decadute da Influx.
- Esito: inconclusivo, non usato per decisione.

## Run Valida

Batch: `session87-feature-rolling-20260619-1510z`.

Parametri:

- `from=2026-06-19T15:10:00Z`
- `to=2026-06-19T16:00:00Z`
- `windowMinutes=10`
- `holdoutWindows=1`
- `horizonSeconds=900`
- `featureWindowMinutes=15`
- `cadenceSeconds=60`
- `symbolLimit=120`
- `perSymbolLimit=6`
- `minRowsPerWindow=4`
- `minSelectionWindows=3`
- `replaceBatch=true`

## Esito

- `strategicStatus=PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`
- Righe persistite: `8640`
- Finestre: 5
- Righe per finestra: `1728`
- Candidato selezionato: `symbol=EDENUSDC`

## Candidato Selezionato

`symbol=EDENUSDC`

- `selectionWindows=4`
- `selectionRows=24`
- `selectionAvgEndNetReturn=0.017884026856629875`
- `selectionStdDevEndNetReturn=0.009102326932547688`
- `selectionWorstWindowAvgEndNetReturn=0.004955701032645089`
- `selectionPositiveWindowRate=1`
- `stabilityScore=0.006230536457808343`
- `holdoutRows=6`
- `holdoutAvgEndNetReturn=0.021754406457162503`
- `holdoutPositiveEndRate=1`
- `holdoutWinRateLower95=0.6096569663469354`
- `status=PASS_CANDIDATE`

Per finestra:

- `ROLLING_00`: `avgEnd=0.004955701032645089`, `winRate=0.5000`
- `ROLLING_01`: `avgEnd=0.023244981150438359`, `winRate=1.0000`
- `ROLLING_02`: `avgEnd=0.014356918312661870`, `winRate=1.0000`
- `ROLLING_03`: `avgEnd=0.028978506930774181`, `winRate=1.0000`
- `ROLLING_04`: `avgEnd=0.021754406457162502`, `winRate=1.0000`

## Altri Segnali

`BANKUSDC` passa anch'esso:

- selection positiva su tutte le finestre;
- holdout `avgEnd=0.006971147066215723`;
- holdout `winRate=1.0000`.

`GENIUSUSDC` e' il controllo negativo utile:

- selection forte;
- holdout `avgEnd=-0.04711509185224369`;
- holdout `winRate=0`.

Questo conferma che il holdout sta bloccando falsi positivi e non e' solo decorativo.

## Nota Scientifica

La run ha trovato candidati promuovibili, ma i candidati promossi sono ancora `symbol-only`, non pattern feature/regime globali. I pattern feature/regime sono stati implementati e valutati nello stesso endpoint, ma non hanno battuto i migliori simboli in questa finestra.

Decisione:

- Non avviare REAL.
- Non avviare PAPER cieca.
- Prossimo step corretto: endpoint di preflight/promotion che congeli `symbol=EDENUSDC` e `symbol=BANKUSDC` come candidati PAPER osservazionali, con validita' breve e forensics obbligatoria.
