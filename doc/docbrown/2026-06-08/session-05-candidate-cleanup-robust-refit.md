# Session 05 - Candidate cleanup and robust refit

Data: 2026-06-08 CEST

## Obiettivo

Pulire i dati derivati non necessari, riprendere l'ottimizzazione dai file/run gia' presenti e identificare parametri pronti per una REAL RUN profittevole, senza avviare REAL RUN.

## Azioni

- Verificato che non esiste PostgreSQL nello stack runtime; il DB operativo e' MySQL `hft`.
- Pulite in modo conservativo solo le tabelle candidate derivate SCALPING_SCOUT.
- Ricostruito/riusato dataset 18 su finestra 12h con 25.096 osservazioni e 7.496.100 SELL-policy outcome.
- Corretto ranking del training per impedire che micro-campioni `WATCH_SAMPLE` vengano selezionati sopra candidate robuste.
- Aggiunto resume per SELL-policy thresholds e log di avanzamento su dataset/outcome/training.
- Rilanciato refit corretto con parametri DB `threshold_quantile_count=7` e `threshold_search_max_combinations=40000`.

## Risultati

- BUY threshold run 13: `WATCH_WIN_RATE`, all sample 25.096, EV `-0.00515796`, PF `0.064262`.
- SELL thresholds: 300 policy valutate, `readyCount=0`, tutte `WATCH_TRAIN_EV`.
- Best SELL: `pt=0.003|sl=0.008|hold=20|grace=2`, all sample 24.987, EV `-0.00512365`, PF `0.051329`.
- Outcome gross mean dataset 18: `-0.000183126`; net mean: `-0.005183126`.
- REAL storico DB: 58 trade chiusi, net `-3.41163512`, win-rate `24.14%`.

## Verdetto

Non esistono parametri pronti per REAL RUN sulla chain attuale. Il problema non e' solo fee/slippage: il segnale e' gia' negativo lordo. La REAL RUN deve restare bloccata finche' la generazione del segnale non migliora il gross return prima dei costi.

## DB-backed ranking

Dopo il refit ho spostato anche i rank semantici del training in `scalping_scout_candidate_dataset_config` tramite V48 idempotente, cosi' non restano numeri di ranking hardcoded nello script.
