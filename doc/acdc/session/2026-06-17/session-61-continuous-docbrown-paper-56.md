# Session 61 - Continuous DocBrown PAPER 56

Data: 2026-06-17

## Obiettivo

Integrare DocBrown in modalita' ML continua e avviare una PAPER ACDC monitorando i cicli ML.

Vincolo strategico: REM outcome-first. DocBrown produce consulenze e validazioni, ACDC esegue BUY/SELL simulati in PAPER senza trade reali Binance.

## Codice Eseguito

- ACDC commit attivo: `4f5df7a` (`MS669: document containerized DocBrown PAPER run 55`)
- DocBrown commit attivo: `946ccb1` (`MS671: limit REM live revalidation to running PAPER`)

DocBrown e' stato avviato containerizzato con:

- `DOCBROWN_REM_SCHEDULER_ENABLED=true`
- `DOCBROWN_REM_SCHEDULER_INTERVAL=180s`
- `DOCBROWN_REM_LIVE_REVALIDATION_ENABLED=true`
- `DOCBROWN_REM_LIVE_REVALIDATION_INTERVAL=15s`
- `DOCBROWN_ACDC_SHADOW_SIGNAL_ENABLED=true`
- `DOCBROWN_ACDC_BASE_URL=http://host.docker.internal:8091`

Il container DocBrown e' stato fermato dopo la conclusione della PAPER per evitare nuove aperture automatiche.

## PAPER 56

- Execution: `56`
- Tipo: `PAPER`
- Stato: `COMPLETED`
- Started at: `2026-06-17 18:45:36`
- Buy stopped at: `2026-06-17 19:06:42`
- Completed at: `2026-06-17 19:14:23`
- Initial budget: `100`
- Current budget: `99.260703664936230200`
- Reserved budget: `0`
- Realized profit quote: `-0.739296335063769800`

## Cicli ML Osservati

DocBrown ha eseguito full mining ripetuto, non un singolo batch:

- `18:45:47` promotedRules `45`
- `18:49:03` promotedRules `76`
- `18:51:58` promotedRules `125`
- `18:54:59` promotedRules `144`
- `18:57:58` promotedRules `151`
- `19:00:57` promotedRules `157`
- `19:03:58` promotedRules `153`
- `19:06:57` promotedRules `143`
- `19:09:59` promotedRules `133`
- `19:12:49` promotedRules `122`

I signal successivi sono stati respinti da ACDC con HTTP `500` perche' una PAPER era gia' attiva. Questo comportamento evita aperture di execution parallele, ma non impedisce che la stessa execution continui ad aprire nuovi simboli fino allo stop-buy.

## Scoring

- Trades: `12`
- Wins: `4`
- Losses: `8`
- Take profit exits: `3`
- Positive duration exits: `1`
- Loss cap exits: `5`
- Timeout exits: `3`
- Net PnL: `-0.739296335063769800`

Trade detail:

| Symbol | Exit | Net quote | Max net return | Hold sec |
| --- | --- | ---: | ---: | ---: |
| ASTERUSDC | TAKE_PROFIT | `0.086289222360000000` | `0.003451568894952251` | `195` |
| ATUSDC | POSITIVE_DURATION | `0.109993593439600000` | `0.004399743754003844` | `355` |
| ALLOUSDC | TIMEOUT | `-0.179651636901000000` | `0` | `503` |
| ILVUSDC | TAKE_PROFIT | `0.170367642970000000` | `0.006814705882352941` | `350` |
| ASTERUSDC | LOSS_CAP | `-0.185549521940000000` | `0.002066485753052917` | `527` |
| ARUSDC | LOSS_CAP | `-0.197345130912000000` | `0` | `186` |
| BIGTIMEUSDC | TIMEOUT | `-0.049999999995900000` | `0` | `752` |
| MAGICUSDC | LOSS_CAP | `-0.309615384168300000` | `0` | `69` |
| BBUSDC | LOSS_CAP | `-0.157188841144500000` | `0` | `491` |
| BIOUSDC | LOSS_CAP | `-0.155350112406400000` | `0` | `272` |
| 1MBABYDOGEUSDC | TIMEOUT | `-0.057304767475669800` | `0` | `727` |
| FLUXUSDC | TAKE_PROFIT | `0.186058601110400000` | `0.007442344045368620` | `472` |

## Evidenza Scientifica

La modifica ha risolto il problema precedente: DocBrown ora fa cicli ML continui durante la run.

Il risultato non e' migliorato per un motivo preciso: la live revalidation oggi osserva le posizioni aperte e logga decadimento MFE/adverse movement, ma ACDC non consuma ancora queste informazioni come exit advice operativa.

Esempio rilevante:

- `ASTERUSDC` position `79` ha mostrato MFE decay ripetuto da `18:51:56` a `18:58:11`.
- Il max net return era gia' stato positivo (`0.002066485753052917`), poi il segnale e' decaduto.
- ACDC ha chiuso solo dopo in `LOSS_CAP`, con net `-0.185549521940000000`.

Questo dimostra che il ML runtime ha identificato in anticipo almeno un deterioramento utile, ma l'informazione non era collegata al processo SELL.

## Conclusione

La pipeline ora e':

1. DocBrown full mining continuo: funzionante.
2. DocBrown live revalidation: funzionante e limitata alle PAPER RUNNING.
3. ACDC BUY su advice: funzionante.
4. ACDC SELL su exit policy advice iniziale: funzionante.
5. ACDC SELL su revalidation live DocBrown: mancante.

Il prossimo step scientifico non e' cambiare soglie di ingresso: serve trasformare la live revalidation da log diagnostico a advice di uscita consumabile da ACDC, mantenendo outcome-first e senza introdurre tuning manuale arbitrario.

