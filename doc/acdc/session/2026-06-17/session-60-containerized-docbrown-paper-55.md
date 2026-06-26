# Session 60 - Containerized DocBrown PAPER 55

Data: 2026-06-17

## Obiettivo

Monetizzare il baseline corrente senza modificare logica o configurazioni operative.

Vincoli rispettati:

- nessun cambio a soglie, ranking, guardie o regole ACDC;
- DocBrown avviato come container Quarkus, non come processo locale;
- PAPER avviata tramite signal DocBrown -> ACDC;
- stop/chiusura execution eseguita via hft-fe script;
- nessuna REAL RUN.

## Infrastruttura

- ACDC container: `acdc-vpn`, porta host `8091`.
- DocBrown container: `docbrown`, porta host `8083`.
- MySQL: `mysql_container`.
- hft-fe: `hft-fe-local`, porta host `5173`.

DocBrown e' stato containerizzato nel repo `docbrown` con commit:

- `MS668: containerize DocBrown Quarkus service`

Il vecchio service `docbrown-real-refresh` Python resta nel compose storico, ma non e' stato usato per questa RUN.

## Batch DocBrown

- Endpoint: `POST /docbrown/rem/research/REM_CURRENT/run`.
- Signal abilitato verso ACDC.
- Generated at: `2026-06-17T18:17:37.720891359Z`.
- Band version: `REM-BAND-2026-06-17T181713.839257579-e92cc0ed`.
- Evaluated rules: `305`.
- Promoted rules: `34`.

Nota: il DTO DocBrown non espone `promotionClass` nel report JSON sintetico, quindi il conteggio `PAPER_ELIGIBLE` non e' leggibile direttamente dal file di risposta. Il signal e' comunque arrivato ad ACDC e ha aperto PAPER execution `55`.

## PAPER Execution 55

- Execution id: `55`.
- Started at: `2026-06-17 18:17:21`.
- Completed at: `2026-06-17 18:29:40`.
- Status: `COMPLETED`.
- Initial budget: `100`.
- Current budget: `99.972036720735400000`.
- Reserved budget: `0`.
- Realized profit quote: `-0.027963279264600000`.
- Trades: `3`.
- Wins: `1`.
- Losses: `2`.

## Trades

| Symbol | Result | Net Profit | Net Return | Max Net Return | Safe Net | Duration | Hold | Exit |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| `ATUSDC` | WIN | `0.078406169440000000` | `0.003136246786632391` | `0.005704370179948586` | `0.00871875` | `530` | `534` | `EXIT_ML_ADVICE_POSITIVE_DURATION` |
| `ALLOUSDC` | LOSS | `-0.063669949932600000` | `-0.00254679802955665` | `0.0` | `0.005731241473396999` | `545` | `549` | `EXIT_ML_ADVICE_TIMEOUT` |
| `AVAXUSDC` | LOSS | `-0.042699498772000000` | `-0.00170798012277112` | `0.002818327974276527` | `0.00408912037037037` | `545` | `550` | `EXIT_ML_ADVICE_TIMEOUT` |

## Scoring RUN 53 + 54 + 55

- Total net profit quote: `0.654257780674484800`.
- Total trades: `10`.
- Wins: `7`.
- Losses: `3`.
- Take-profit exits: `4`.
- Positive-duration exits: `3`.
- Timeout exits: `3`.
- Loss-cap exits: `0`.

## Lettura

La RUN `55` non invalida il baseline, ma mostra un punto debole chiaro:

- il modello ha selezionato segnali che non hanno raggiunto safe target;
- `ATUSDC` ha monetizzato grazie alla positive-duration exit;
- `AVAXUSDC` ha avuto MFE positiva (`0.002818`) ma e' sceso sotto zero prima della durata ML;
- `ALLOUSDC` non ha mai avuto MFE positiva utile;
- nessun take-profit pieno e' stato raggiunto.

Questa evidenza conferma che una quota di loss e' normale, ma suggerisce che il prossimo miglioramento scientifico non deve cambiare l'entry a mano.
Deve invece misurare, offline, l'evaporazione MFE:

- quanti trade con MFE positiva diventano timeout negativi;
- se una exit anticipata su MFE/safe-decay avrebbe salvato `AVAXUSDC`;
- se `ALLOUSDC` era distinguibile gia' all'entry tramite feature ML, volume, continuita' dati o ranking.

## Decisione

Il baseline aggregato resta positivo, quindi non va scartato.
Pero' la prossima modifica non deve stringere soglie manuali.
Il prossimo passo scientifico e' un replay offline sulle RUN `53`, `54`, `55` che confronti:

- exit attuale;
- positive-duration attuale;
- exit anticipata quando MFE supera una frazione del safe target e poi decresce;
- filtro/ranking che penalizza advice con safe target troppo lontano rispetto a MFE attesa.

Nessuna nuova configurazione va promossa senza battere il baseline aggregato.
