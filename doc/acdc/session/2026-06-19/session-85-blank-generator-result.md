# REM Blank Generator Result

Data: 2026-06-19.

Batch: `session85-blank-20260619-1530z`.
Endpoint: `POST /docbrown/rem/blank-candidates/REM_CURRENT/generate`.

Finestre UTC:

- `DIAGNOSIS_SET`: 2026-06-19T14:30:00Z -> 2026-06-19T14:45:00Z.
- `SELECTION_SET`: 2026-06-19T14:45:00Z -> 2026-06-19T15:00:00Z.
- `HOLDOUT_SET`: 2026-06-19T15:00:00Z -> 2026-06-19T15:15:00Z.

Parametri: `horizonSeconds=900`, `featureWindowMinutes=15`, `cadenceSeconds=60`, `symbolLimit=120`, `perSymbolLimit=8`, `replaceBatch=true`.

## Esito

- `strategicStatus=FAIL`
- Righe persistite: `6912`
- Split: `DIAGNOSIS_SET=2304`, `SELECTION_SET=2304`, `HOLDOUT_SET=2304`
- Modello estratto da selection: `symbol=REUSDC`
- Holdout modello: `holdoutAvgEndNetReturn=-0.01804653440989564`, `holdoutPositiveEndRate=0`
- Ragioni: ritorno medio holdout non positivo; Wilson 95% non supera 50%.

## Metriche Globali

- `positiveMfeRate=0.25925925925925924`
- `zeroMfeRate=0.7407407407407407`
- `positiveEndRate=0.14641203703703703`
- `avgMaxNetReturn=-0.000407129610213825`
- `avgMinNetReturn=-0.00556266686346583`
- `avgEndNetReturn=-0.003368016221182593`
- `medianEndNetReturn=-0.0028`
- `p10EndNetReturn=-0.008565262822371088`
- `p90EndNetReturn=0.001281262772440195`
- Wilson win-rate 95%: `[0.13827419372599253, 0.15494270123585852]`

## Split

`DIAGNOSIS_SET`: `avgEndNetReturn=-0.003228740002222837`, `positiveEndRate=0.1284722222222222`, `positiveMfeRate=0.2703993055555556`.

`SELECTION_SET`: `avgEndNetReturn=-0.005298076832853686`, `positiveEndRate=0.051215277777777776`, `positiveMfeRate=0.10546875`.

`HOLDOUT_SET`: `avgEndNetReturn=-0.001577231828471255`, `positiveEndRate=0.2595486111111111`, `positiveMfeRate=0.4019097222222222`.

## Baseline

- `NO_TRADE`: migliore del modello globale, delta vs all `+0.003368016221182593`.
- `ALL_CANDIDATES`: `avgEndNetReturn=-0.003368016221182593`.
- `RANDOM_TOP_VOLUME`: `avgEndNetReturn=-0.002957001834453523`, ancora negativo.
- `NAIVE_REVERSAL`: `avgEndNetReturn=-0.004304746730440943`, peggiore di all.
- `SCORE_ONLY`: `avgEndNetReturn=-0.003732004990203567`, peggiore di all.
- `FRESH_ONLY`: uguale ad all candidate.
- `SYMBOL_ONLY`: positivo come baseline descrittiva, ma la regola scelta solo su selection (`REUSDC`) fallisce su holdout.

## Decisione

Il generatore vuoto ha funzionato e ha misurato candidate senza soglie predittive premature. Nessun modello e' promuovibile:

- l'universo quasi non filtrato e' negativo;
- le baseline semplici non battono `NO_TRADE`;
- il miglior simbolo selezionato da `SELECTION_SET` e' un falso positivo su `HOLDOUT_SET`;
- alcuni simboli positivi in selection restano positivi in holdout, ma sceglierli dopo aver visto holdout sarebbe leakage.

Decisione operativa: nessuna PAPER automatica. Il prossimo passo scientifico e' aumentare il numero di finestre e usare selezione regolarizzata su piu' finestre di selection prima di guardare holdout.
