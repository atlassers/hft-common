# Session 146 - Forward A/B Run Readiness Checklist

Data: 2026-06-21.

## Obiettivo

Arrivare allo stato `READY_TO_RUN_FORWARD_AB_98` dopo il fix Telegram SELL chart, senza avviare PAPER se `ML_READY=false`.

## Checklist

- [x] Verificare container base.
- [x] Verificare assenza run aperte.
- [x] Verificare assenza posizioni PAPER aperte.
- [x] Verificare DocBrown research status.
- [x] Verificare REM readiness.
- [x] Eseguire scoring live leggero DocBrown.
- [x] Eseguire rolling validation 2h.
- [x] Tentare promozione diagnostica del candidato selezionato.
- [x] Eseguire rolling validation su finestra corta.
- [x] Verificare `ML_READY` finale.
- [x] Non avviare PAPER con `ML_READY=false`.

## Evidenza

Container:

- `acdc-vpn` up;
- `docbrown` up;
- `mysql_container` up;
- `influxer` up;
- `influxdb` up;
- `grafana` up.

Stato runtime:

- run `RUNNING`: `0`;
- posizioni PAPER `OPEN`: `0`.

DocBrown research:

- status `COMPLETED`;
- runId `2`;
- 288 simboli processati;
- 17280 sample;
- nessun errore.

REM readiness:

- `PAPER_READY_WAITING_SIGNAL`;
- blocker: nessuno.

ML readiness finale:

- `ready=false`;
- blocker:
  - `LIVE_ADVICE_ACTIVE_MISSING`;
  - `PAPER_ELIGIBLE_ADVICE_ACTIVE_MISSING`.

## Scoring E Rolling

Live scoring:

- 5 advice salvate;
- tutte `PURE_REVERSAL_OBSERVED`;
- nessuna `PAPER_ELIGIBLE` valida per PAPER.

Rolling validation 2h:

- batch `session145-forward-ab-prep-20260621T074128Z`;
- `strategicStatus=FAIL_SELECTION_BIAS`;
- selected candidate `VANAUSDC`;
- holdout negativo;
- promozione diagnostica: `SKIPPED_NOT_PROMOTABLE`, `promotedRows=0`.

Rolling validation finestra corta:

- batch `session145-forward-ab-prep-short-20260621T074408Z`;
- `strategicStatus=FAIL_SELECTION_BIAS`;
- selected candidate `XVGUSDC`;
- holdout buono, ma almeno una finestra selection negativa;
- non promuovibile senza violare il contratto scientifico.

## Verdetto Del Consiglio

Saggio ascoltatore:

Il runtime e' pulito e il fix Telegram e' deployato. Non c'e' un problema operativo immediato: manca il segnale valido.

Scienziato severo:

La RUN non e' pronta. Avviarla con `ML_READY=false` o forzando candidate con selection negativa contaminerebbe la forward A/B.

Mediano pragmatico:

La prossima azione ammessa e' ripetere scoring/rolling promotion finche' compare almeno una advice `PAPER_ELIGIBLE` contract-active. Solo allora si puo' lanciare il gruppo `FORWARD_AB_98`.

## Stato

`BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`.
