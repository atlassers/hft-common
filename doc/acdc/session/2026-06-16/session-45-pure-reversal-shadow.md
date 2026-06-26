# Session 45 - Pure Reversal SHADOW

Data: 2026-06-16

## Decisione

SHADOW diventa il laboratorio `PURE_REVERSAL`.

Non viene introdotta una nuova run mode:

- SHADOW serve per osservare reversal grezzi con promozione ML permissiva;
- PAPER resta la simulazione operativa e va usata dopo rigenerazione DocBrown in modalita' `STRICT`;
- validita' temporale advice e marcatura `used` restano attive in entrambi i casi.

## Implementazione

Nuova shared config:

- `rem.ml.promotion.mode=PURE_REVERSAL`

Modalita':

- `STRICT`: applica campioni minimi, profit-rate, safe target, validation avg e live-audit;
- `PURE_REVERSAL`: promuove regole derivate dai campioni GOOD di reversal senza bloccarle su quei gate.

DocBrown continua a calcolare:

- validation stats;
- advice;
- live-audit;
- validita' temporale.

In `PURE_REVERSAL`, il live-audit e' osservativo e non blocca la promozione.

## Verifica Build

- ACDC `./mvnw -q -Dtest=RemCurrentConfigurationTest test`: completato.
- DocBrown `./mvnw -q test`: completato.
- ACDC `./mvnw -q package -DskipTests`: completato.
- DocBrown `./mvnw -q package -DskipTests`: completato.
- ACDC container rebuildato e riavviato.
- DB reale a Flyway `v37`.

## DocBrown PURE_REVERSAL

Job:

- endpoint: `POST /docbrown/rem/research/REM_CURRENT/run`;
- regole ML valutate: `360`;
- regole promosse: `227`;
- regole promosse con validita': `227`;
- `active_now=227`.

Top iniziali:

- `2ZUSDC`;
- `BABYUSDC`;
- `AIXBTUSDC`.

## SHADOW 42

Avvio:

- execution: `42`;
- data source: `INFLUX`;
- evaluated: `200`;
- accepted/opened: `3`;
- stop-buy applicato subito.

BUY:

- `AIXBTUSDC`, rules `13`, safe `0.015600531443755537`, duration `330`, loss cap `-0.003`;
- `1INCHUSDC`, rules `8`, safe `0.011360313315926893`, duration `555`, loss cap `-0.003`;
- `COOKIEUSDC`, rules `10`, safe `0.015600531443755537`, duration `330`, loss cap `-0.005888086642599278`.

Risultato:

- status `COMPLETED`;
- budget finale `99.882435064994880000`;
- realized `-0.117564935005120000`.

SELL:

- `COOKIEUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.049999999993600000`, max net `0`;
- `AIXBTUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.049999999983120000`, max net `0`;
- `1INCHUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.017564935028400000`, max net `0`.

## Lettura

Il test e' molto netto:

- i reversal puri aumentano molto le regole promosse (`227`);
- aprono BUY facilmente;
- in questa finestra non hanno generato nessuna MFE positiva sui tre trade;
- la perdita e' rimasta contenuta grazie a timeout/fee, ma il segnale grezzo non e' profittevole.

Conclusione operativa:

- SHADOW `PURE_REVERSAL` e' utile come sorgente di campioni negativi/positivi grezzi;
- non va promosso a PAPER senza tornare a `STRICT`;
- il prossimo miglioramento scientifico deve confrontare reversal puri con MFE positiva vs MFE zero, non aggiungere soglie manuali in ACDC.

## Fix

Durante la SHADOW e' emersa una correzione minore:

- la marcatura `used` aggiornava anche vecchie righe `REJECTED` con stessa rule-key;
- corretto `ReversalMlRuleRepository.markSymbolAdviceUsed` per aggiornare solo `status='PROMOTED'`.
