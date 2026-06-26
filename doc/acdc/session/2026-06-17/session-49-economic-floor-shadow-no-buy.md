# Session 49 - Economic Floor SHADOW No-Buy

Data: 2026-06-17.

## Obiettivo

Eseguire una SHADOW dopo i floor economici e il live audit comune introdotti nella sessione 48.

Regola operativa applicata:

- prima della run, workspace Git verificato pulito e allineato;
- dopo la run, sessione documentata, commit e push.

## Baseline

Tutti i repository risultavano allineati a origin prima della run.

Servizi:

- ACDC attivo su `8091`;
- hft-fe attivo su `5173`;
- MySQL e Influx attivi;
- DocBrown non attivo prima dell'avvio manuale.

## Mining DocBrown

DocBrown avviato localmente su `8083` con MySQL e Influx reali.

Job:

- endpoint: `POST /docbrown/rem/research/REM_CURRENT/run`;
- report: `/tmp/docbrown-rem-20260617-report.json`;
- generated at: `2026-06-17T06:34:08.926806561Z`.

Risultato:

- outcome samples: `5000`;
- GOOD: `1239`;
- BAD: `3761`;
- promoted signatures: `150`;
- ML evaluated rules: `390`;
- ML promoted rules: `30`;
- active advices: `30`.

Le advice attive erano tutte `SYMBOL` e rispettavano i floor:

- min safe return: `0.0022521008`;
- min max return: `0.0043529412`;
- min probability: `0.6153846154`;
- max live under-safe non-profit rate: `0`.

Top simboli promossi:

| Symbol | Rules | Best score | Min safe | Max expected | Best probability |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ALLOUSDC` | 5 | `2.543750000000000000` | `0.0192965440` | `0.0212948897` | `1.0000000000` |
| `CHIPUSDC` | 5 | `2.472727272727273000` | `0.0197272727` | `0.0248184480` | `1.0000000000` |
| `BREVUSDC` | 5 | `1.182800982800982800` | `0.0068280098` | `0.0068280098` | `1.0000000000` |
| `CRVUSDC` | 1 | `1.103490759753593400` | `0.0060349076` | `0.0060349076` | `1.0000000000` |
| `CFGUSDC` | 5 | `0.994646591384358000` | `0.0024234460` | `0.0075218855` | `1.0000000000` |
| `2ZUSDC` | 5 | `0.965027322404371600` | `0.0031906727` | `0.0079649123` | `1.0000000000` |
| `AIGENSYNUSDC` | 3 | `0.068486352357320100` | `0.0048512397` | `0.0098737542` | `1.0000000000` |
| `CUSDC` | 1 | `-0.274789915966386600` | `0.0022521008` | `0.0043529412` | `0.7368421053` |

Nota: `CUSDC` resta promosso per economia advice ma ha score negativo dopo penalizzazione. ACDC mantiene comunque guardia `entry_reversal_ml_score_positive`, quindi una advice con score negativo non deve consumare budget.

## SHADOW 49

Execution: `49`.

- start: `2026-06-17 06:34:44 UTC`;
- stop: `2026-06-17 06:50:28 UTC`;
- durata osservata: circa 15 minuti;
- status finale: `STOPPED`;
- BUY: `0`;
- posizioni aperte: `0`;
- posizioni chiuse: `0`;
- budget finale: `100.000000000000000000`;
- realized: `0`.

Reject finali:

- `REVERSAL_ML_RULE_MISSING`: `20082`;
- `REVERSAL_DATA_COVERAGE_LOW`: `100`;
- `REVERSAL_VOLUME_CONFIRMATION_LOW`: `18`.

## Lettura

La run non ha comprato perche' le 30 advice valide non si sono materializzate nel live universe durante la finestra osservata.

Questo e' coerente con l'obiettivo attuale:

- evitare BUY deboli;
- comprare solo quando il miner vede un reversal economicamente tradabile;
- non abbassare soglie durante la run per aumentare artificialmente il numero di trade.

Il risultato non prova profittabilita'.
Prova pero' che dopo i floor economici il sistema preferisce stare fermo invece di consumare budget su segnali non validati.

## Prossimo Passo

La prossima iterazione deve rispondere a una domanda precisa:

- le 30 advice promosse non appaiono nel live universe perche' sono rare ma corrette?
- oppure il miner sta promuovendo simboli/firme troppo isolate rispetto al flusso corrente?

Analisi proposta:

- confrontare i simboli promossi con i simboli valutati in SHADOW `49`;
- misurare per ciascun simbolo promosso se mancava snapshot, rule match, coverage o volume;
- non abbassare floor economici finche' questa distinzione non e' chiara.
