# Session 65 - DocBrown PAPER preflight no advice

Data: 2026-06-18

## Obiettivo

Avviare una PAPER dopo lo snapshot auditabile della consulenza ML, senza cambiare soglie o configurazioni raggiunte con RUN 57.

## Stato servizi

- ACDC containerizzato: UP su `:8091`.
- Flyway ACDC: V47 `success=1`.
- DocBrown containerizzato: UP su `:8083`.
- DocBrown scheduler:
  - full mining: `180s`;
  - live revalidation: `15s`;
  - ACDC paper signal: abilitato verso `http://host.docker.internal:8091`.

## Cicli DocBrown osservati

DocBrown ha completato quattro cicli full-mining:

- `2026-06-18 07:54:19 UTC`: `samples=5000`, `promotedRules=0`;
- `2026-06-18 07:57:08 UTC`: `samples=5000`, `promotedRules=0`;
- `2026-06-18 07:59:59 UTC`: `samples=5000`, `promotedRules=0`;
- `2026-06-18 08:02:59 UTC`: `samples=5000`, `promotedRules=0`.

## Evidenza band

Le band generate nella finestra corrente sono rimaste `DIAGNOSTIC`:

- coverage `0.20..1.00`;
- distinct min `6/7`;
- precision good circa `0.51/0.52`;
- avg MFE circa `0.0017/0.0018`;
- score circa `0.20/0.22`.

La band scientificamente valida della RUN 57 resta `ACTIVE`:

- coverage `0.20..0.364640883977900570`;
- distinct min `8`;
- precision good `0.651960784313725490`;
- avg MFE `0.002572863897570207`;
- score `1.415501366825712014`.

## Decisione

Non e' stata avviata una PAPER manuale perche' non esisteva nessuna rule `PROMOTED` con `advice_valid_until > UTC_TIMESTAMP()`.

Avviare PAPER senza advice violerebbe la pipeline attuale:

1. DocBrown identifica band e advice `PAPER_ELIGIBLE`.
2. DocBrown invia il primo signal ad ACDC.
3. ACDC apre PAPER e salva lo snapshot advice/rule/band nel `policy_json`.

## Stato finale

DocBrown resta in esecuzione continua. Se un ciclo successivo promuove una rule `PAPER_ELIGIBLE`, DocBrown chiamera' automaticamente:

`POST /acdc/profiles/REM_CURRENT/research-batches/{researchBatchId}/paper-signal`

e ACDC avviera' la PAPER.

## Lettura scientifica

La mancanza di BUY in questa finestra non e' un errore runtime: e' il modello che non ha trovato reversal con qualita' sufficiente rispetto alla band attiva. Questo preserva il risultato della RUN 57 e impedisce di monetizzare segnali piu' deboli solo per produrre attivita'.
