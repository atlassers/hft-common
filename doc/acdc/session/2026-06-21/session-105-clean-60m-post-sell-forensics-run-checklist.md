# Session 105 - Clean 60m Post SELL Forensics Run Checklist

Data: 2026-06-21.

## Obiettivo

Eseguire una RUN PAPER pulita di circa `60m` sulla baseline candidata `98`, senza tuning durante la run, usando il nuovo forensics post-SELL persistente.

## Vincoli

- PAPER only.
- Avvio/stop da endpoint.
- MySQL/container, non H2.
- Nessun tuning BUY/SELL/gate/ranking/trailing durante la run.
- Forensics post-SELL persistente per ogni posizione venduta dopo il deploy.
- Verdict post-SELL valido solo se il dato resta granulare (`maxGapSeconds <= 15`).

## Checklist Livello 1

- [x] L1.1 Verificare runtime container.
- [x] L1.2 Fermare execution `15` vuota nata dallo scheduler.
- [x] L1.3 Avviare nuova PAPER pulita da endpoint.
- [ ] L1.4 Eseguire finestra attiva circa `60m`.
- [x] L1.5 Stop-buy/stop da endpoint per run non valide.
- [x] L1.6 Drain fino a reserved `0`.
- [x] L1.7 Raccogliere endpoint diagnostici.
- [x] L1.8 Analizzare trade e post-SELL forensics.
- [x] L1.9 Aggiornare verdict e prossima decisione.

## Checklist Livello 2

### L2.1 Runtime

- [x] ACDC container `acdc-vpn` up.
- [x] DocBrown container up.
- [x] Influxer up.
- [x] InfluxDB up.
- [x] MySQL up.
- [x] Grafana up.

### L2.2 Metriche

- [x] Execution id.
- [x] Durata effettiva.
- [x] BUY count.
- [x] SELL count.
- [x] Win/loss count.
- [x] Net PnL.
- [x] Reserved finale.
- [x] Sell-capture.
- [x] Timeline.
- [x] Scoring.
- [x] Counterfactual live revalidation.
- [x] Post-SELL forensics persistente.

## Stato Realtime

- Stato corrente: `FAILED_CLEAN_RUN_PREREQ_ML_TIMEOUT`.
- Execution pulita: non ottenuta.
- Artifact runtime: `/tmp/session105-clean-60m-run`.

## Log Realtime

- `2026-06-21T02:31:xx+02:00`: execution `15` fermata pulitamente, zero posizioni, budget invariato.
- `2026-06-21T02:31:xx+02:00`: execution `16` avviata da endpoint; primo tick: `opened=0`, `closed=0`, `accepted=0`, `rejected=200`.
- `2026-06-21T02:34:xx+02:00`: execution `16` fermata per run non valida: regole ML assenti, zero posizioni, budget invariato.
- `2026-06-21T02:35:xx+02:00`: DocBrown rebuild/redeploy container completato.
- `2026-06-21T02:37:xx+02:00`: avviato prerequisito DocBrown research/mining dal path reale `/docbrown/rem/research/REM_CURRENT/run`.
- `2026-06-21T02:47:xx+02:00`: dopo circa `10m` DocBrown research ancora `RUNNING`, `acdc_reversal_ml_rule=0`; run da 60m non ancora partita.
- `2026-06-21T03:07:xx+02:00`: DocBrown research fallito per timeout transazionale/rollback; `acdc_reversal_ml_rule=0`.
- `2026-06-21T03:08:xx+02:00`: DocBrown riavviato per liberare il job wedged.
- `2026-06-21T03:09:xx+02:00`: raccolta execution `17`, partita dallo scheduler mentre il prerequisito era in crisi; non e' considerata run pulita.

## Risultato Reale

La run pulita da `60m` non e' stata ottenuta.

Motivo:

- ACDC aveva `acdc_reversal_ml_rule=0`;
- DocBrown espone gli endpoint sotto `/docbrown`, non root;
- DocBrown research/mining e' partito ma ha superato il limite transazionale, andando in rollback;
- quindi il prerequisito ML pesante non ha prodotto regole promosse.

Durante l'attesa e' partita/completata una execution `17` non pulita:

- durata: circa `31m`;
- trade: `2`;
- win/loss: `0/2`;
- net PnL: `-0.451812168661600000`;
- reserved finale: `0`;
- posizioni aperte finali: `0`;
- exit: `2/2` `EXIT_ML_ADVICE_LOSS_CAP`;
- max MFE: `0` per entrambi i trade.

Trade:

- `COOKIEUSDC`: net return `-0.012627659574468085`, max net return `0`, loss-cap dopo circa `102s`.
- `EGLDUSDC`: net return `-0.005444827586206897`, max net return `0`, loss-cap dopo circa `283s`.

Post-SELL forensics persistente:

- rows: `2`;
- entrambe `COMPLETED`;
- entrambe `INCONCLUSIVE_GRANULARITY`;
- source `binance-microbar`, ma `localMaxGapSeconds=60`, quindi non abbastanza granulare per verdict second-level;
- nessuna evidenza valida di reversal post-sell recuperabile con questo dato.

Counterfactual:

- blocked advice: `3`;
- good blocks: `0`;
- bad blocks: `2`;
- ambiguous blocks: `1`.

Verdict:

`FAILED_CLEAN_RUN_PREREQ_ML_TIMEOUT`.

Decisione:

- non classificare la baseline sulla execution `17`;
- prima di una nuova run pulita bisogna rendere il training DocBrown completabile entro tempo operativo oppure separare il mining pesante dalla finestra PAPER;
- il dato piu' importante della sessione e' infrastrutturale/scientifico: il sistema non era in stato pronto per la run da 60m.
