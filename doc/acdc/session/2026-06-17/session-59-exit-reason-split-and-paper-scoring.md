# Session 59 - Exit Reason Split And PAPER Scoring

Data: 2026-06-17

## Obiettivo

Separare due casi che prima condividevano lo stesso reason:

- take-profit pieno: `net_return >= ml_advice_safe_net_return`;
- uscita positiva a fine durata: trade positivo alla scadenza della durata stimata dal modello, ma sotto `safe_net`.

Poi eseguire scoring offline contro le PAPER RUN `53` e `54`.

## Modifica Runtime

Aggiunta migration `V45__split_ml_advice_positive_duration_exit.sql`.

Guardie EXIT attive su `REM_CURRENT` dopo `V45`:

| Priority | Guard | Operator | Reason |
| ---: | --- | --- | --- |
| 10 | `exit_ml_advice_take_profit` | `ML_ADVICE_TAKE_PROFIT_EXIT` | `EXIT_ML_ADVICE_TAKE_PROFIT` |
| 15 | `exit_ml_advice_positive_duration` | `ML_ADVICE_POSITIVE_DURATION_EXIT` | `EXIT_ML_ADVICE_POSITIVE_DURATION` |
| 20 | `exit_ml_advice_loss_cap` | `ML_ADVICE_LOSS_CAP_EXIT` | `EXIT_ML_ADVICE_LOSS_CAP` |
| 30 | `exit_ml_advice_timeout` | `ML_ADVICE_TIMEOUT_EXIT` | `EXIT_ML_ADVICE_TIMEOUT` |

`ML_ADVICE_TAKE_PROFIT_EXIT` ora identifica solo il target pieno.
`ML_ADVICE_POSITIVE_DURATION_EXIT` identifica l'uscita positiva a fine durata.

La logica resta DB-driven: la nuova uscita e' una guardia configurata su DB, non un branch strategico hardcoded nel runtime.

## Endpoint Diagnostico

Aggiunto:

`GET /diagnostics/acdc/paper/scoring?executionIds=53,54`

Lo scoring:

- non modifica trade o execution;
- legge `acdc_paper_position`;
- legge la SELL accettata da `acdc_paper_decision`;
- riclassifica semanticamente le vecchie `EXIT_ML_ADVICE_TAKE_PROFIT`;
- aggrega run, trade, win/loss, net profit e distribuzione delle uscite.

## Verifiche

- `./mvnw -q -Dtest=RemCurrentConfigurationTest,GuardEvaluatorTest test`: OK.
- `./mvnw -q package`: OK.
- Container ACDC ricostruito e avviato.
- Flyway reale su MySQL applicato fino a `V45`.
- Endpoint profili OK: `GET /acdc/profiles`.
- Guardie EXIT reali verificate via `GET /acdc/profiles/REM_CURRENT/guards?phase=EXIT`.
- Scoring offline RUN `53,54`: OK.

## Scoring Offline RUN 53 + 54

Aggregato:

- execution: `53`, `54`;
- total trades: `7`;
- wins: `6`;
- losses: `1`;
- total net profit quote: `0.682221059939084800`;
- take-profit pieni: `4`;
- positive-duration exit: `2`;
- loss-cap exit: `0`;
- timeout exit: `1`.

RUN `53`:

- trades: `4`;
- wins: `3`;
- losses: `1`;
- net profit quote: `0.384220124584400000`;
- take-profit pieni: `3`;
- positive-duration exit: `0`;
- timeout: `1`.

Trade:

| Symbol | Net | Stored Reason | Semantic Reason |
| --- | ---: | --- | --- |
| `FOGOUSDC` | `0.065092165869300000` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `EXIT_ML_ADVICE_TAKE_PROFIT` |
| `OPGUSDC` | `0.293467581967500000` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `EXIT_ML_ADVICE_TAKE_PROFIT` |
| `OPENUSDC` | `0.075660376683600000` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `EXIT_ML_ADVICE_TAKE_PROFIT` |
| `CATIUSDC` | `-0.049999999936000000` | `EXIT_ML_ADVICE_TIMEOUT` | `EXIT_ML_ADVICE_TIMEOUT` |

RUN `54`:

- trades: `3`;
- wins: `3`;
- losses: `0`;
- net profit quote: `0.298000935354684800`;
- take-profit pieni: `1`;
- positive-duration exit: `2`;
- timeout: `0`.

Trade:

| Symbol | Net | Stored Reason | Semantic Reason |
| --- | ---: | --- | --- |
| `ENAUSDC` | `0.216542155182400000` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `EXIT_ML_ADVICE_TAKE_PROFIT` |
| `0GUSDC` | `0.032154605222000000` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `EXIT_ML_ADVICE_POSITIVE_DURATION` |
| `1000SATSUSDC` | `0.049304174950284800` | `EXIT_ML_ADVICE_TAKE_PROFIT` | `EXIT_ML_ADVICE_POSITIVE_DURATION` |

## Lettura Scientifica

La separazione conferma che la RUN `54` non e' stata una run con tre target pieni.
E' stata:

- un take-profit pieno forte (`ENAUSDC`);
- due trade salvati in profitto dalla durata ML (`0GUSDC`, `1000SATSUSDC`).

Questo e' un segnale utile:

- il modello identifica entry reversali che spesso diventano positive;
- non sempre raggiungono il safe target;
- la durata stimata e' gia' una variabile importante per monetizzare senza aspettare troppo;
- il prossimo tuning deve misurare separatamente target pieni e positive-duration exit.

Non bisogna eliminare la positive-duration exit: nelle RUN `53+54` contribuisce a due win e zero loss.
Bisogna invece capire se aumentare la quota di take-profit pieni senza perdere queste uscite positive.
