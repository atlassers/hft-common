# REM Blank Rolling Validation Result

Data: 2026-06-19.

Batch: `session86-rolling-20260619-1430z`.
Endpoint: `POST /docbrown/rem/blank-candidates/REM_CURRENT/rolling-validation`.

## Parametri

- `from=2026-06-19T14:30:00Z`
- `to=2026-06-19T15:20:00Z`
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

- `strategicStatus=FAIL_SELECTION_BIAS`
- Righe persistite: `8640`
- Finestre: 5
- Righe per finestra: `1728`
- Nessun candidato `PASS_CANDIDATE` disponibile.

## Candidato Selezionato

`symbol=CUSDC`

- `selectionWindows=4`
- `selectionRows=24`
- `selectionAvgEndNetReturn=0.000382272744610865`
- `selectionStdDevEndNetReturn=0.000994933191196273`
- `selectionWorstWindowAvgEndNetReturn=-0.001186902208620121`
- `selectionPositiveWindowRate=0.75`
- `stabilityScore=-0.0010937548029586958`
- `holdoutRows=6`
- `holdoutAvgEndNetReturn=0.00023030303030303`
- `holdoutPositiveEndRate=0.8333333333333334`
- `holdoutWinRateLower95=0.43649056343635395`
- `status=FAIL_SELECTION_BIAS`
- ragioni:
  - almeno una finestra selection negativa;
  - Wilson holdout non supera 50%.

## Top Candidate Rilevanti

`ACTUSDC` e' il caso piu' interessante:

- `selectionAvgEndNetReturn=0.016537575126280152`
- `selectionWorstWindowAvgEndNetReturn=-0.000600960850092003`
- `holdoutAvgEndNetReturn=0.012827637786784366`
- `holdoutPositiveEndRate=1`
- `holdoutWinRateLower95=0.6096569663469354`
- fallisce per una finestra selection negativa.

Interpretazione: ACTUSDC mostra segnale reale nella finestra osservata, ma non passa il criterio preregistrato di stabilita' su tutte le finestre selection.

## Finestre

- `ROLLING_00`: `avgEndNetReturn=-0.002912710677186493`, `positiveEndRate=0.140625`, `positiveMfeRate=0.2934027777777778`.
- `ROLLING_01`: `avgEndNetReturn=-0.005329640602089813`, `positiveEndRate=0.056712962962962965`, `positiveMfeRate=0.14525462962962962`.
- `ROLLING_02`: `avgEndNetReturn=-0.003909867472283179`, `positiveEndRate=0.09490740740740741`, `positiveMfeRate=0.1412037037037037`.
- `ROLLING_03`: `avgEndNetReturn=-0.001257083124733942`, `positiveEndRate=0.2829861111111111`, `positiveMfeRate=0.4079861111111111`.
- `ROLLING_04`: `avgEndNetReturn=-0.004799515747028143`, `positiveEndRate=0.08969907407407407`, `positiveMfeRate=0.2175925925925926`.

## Decisione

Non promuovere alcuna PAPER.

La nuova validazione rolling dimostra che il problema non e' solo il singolo holdout sfortunato: l'universo resta negativo in tutte le finestre e i candidati migliori hanno instabilita' temporale.

La strada successiva non deve aggiungere guardie operative ad ACDC. Deve lavorare sulla definizione del candidato:

- passare da `symbol-only` a pattern con feature + regime;
- mantenere rolling validation obbligatoria;
- valutare una soglia di tolleranza preregistrata per micro-loss selection solo se giustificata scientificamente, non reattivamente.
