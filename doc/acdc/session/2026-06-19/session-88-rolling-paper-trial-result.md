# REM Rolling PAPER Trial Result

Data: 2026-06-19.

## Premessa

La rolling validation `session87-feature-rolling-20260619-1510z` aveva prodotto candidati statisticamente promuovibili:

- `EDENUSDC`: holdout positivo, `holdoutAvgEndNetReturn=0.021754406457162503`.
- `BANKUSDC`: holdout positivo, `holdoutAvgEndNetReturn=0.006971147066215723`.

La prova PAPER serviva a verificare il passaggio critico: candidato rolling valido -> advice live fresco -> BUY immediata -> SELL gestita.

## Promotion

Endpoint:

`POST /docbrown/rem/blank-candidates/REM_CURRENT/rolling-paper-promotion`

Batch sorgente:

`session87-feature-rolling-20260619-1510z`

Simboli promossi:

- `EDENUSDC`
- `BANKUSDC`

Prima promotion:

- `validitySeconds=120`
- `maxBuyAgeSeconds=20`
- Esito ACDC: advice presenti ma BUY rifiutate per superamento del contratto di freschezza.

Seconda promotion:

- `validitySeconds=180`
- `maxBuyAgeSeconds=60`
- Advice promossi:
  - `EDENUSDC`, `adviceId=3`
  - `BANKUSDC`, `adviceId=4`
- Diagnostica live advice ACDC: advice attivi e non scaduti.

## PAPER

Endpoint:

`POST /acdc/paper/run/REM_CURRENT`

Esito operativo:

- BUY accettate su `EDENUSDC`.
- BUY accettate su `BANKUSDC`.
- Tutte le posizioni sono state poi chiuse in perdita.

Posizioni chiuse osservate:

| Symbol | Buy | Sell | Net result |
| --- | ---: | ---: | ---: |
| EDENUSDC | 0.0544 | 0.0541 | negativo |
| BANKUSDC | 0.0405 | 0.0401 | negativo |
| EDENUSDC | 0.0544 | 0.0541 | negativo |
| BANKUSDC | 0.0405 | 0.0401 | negativo |

Nota operativa: durante i cicli ravvicinati sono comparse aperture duplicate sugli stessi simboli. Questo rende il totale economico peggiore del necessario, ma non cambia la classificazione causale: anche la prima coppia di trade non ha generato MFE positivo.

## Forensics

Endpoint:

`GET /docbrown/rem/live-advice/paper-executions/forensics?executionIds=1`

Sintesi:

- `trades=4`
- `targetHitTrades=0`
- `zeroPostBuyMfeTrades=4`
- `badAdviceTrades=4`
- `lateBuyTrades=0`
- `badSellTrades=0`
- `badTargetTrades=0`
- `badExecutionTrades=0`
- `goodFlowTrades=0`
- `avgAdviceAgeSeconds=14.25`
- `avgAdviceToBuyNetMove=-0.0028000000004304`
- `avgPostAdviceMaxNetReturn=-0.0028000000004304`
- `avgPostBuyMaxNetReturn=-0.006647812273487771`
- `avgPostBuyEndNetReturn=-0.011414742193603966`
- `avgCaptureRatio=0`
- `executionNetProfitQuote=-0.968792891131`

Classificazione causale:

- `BAD_ADVICE` su 4 trade su 4.
- Motivo: `Advice never produced executable net MFE`.
- Exit: `EXIT_ML_ADVICE_LOSS_CAP`.

## Interpretazione Scientifica

Questa prova non dimostra un problema primario della SELL o del trailing.

Il trailing non ha avuto occasione di lavorare perche':

- il post-buy MFE e' stato zero su tutti i trade;
- il massimo rendimento netto dopo BUY e' rimasto negativo;
- il target non e' mai stato raggiunto;
- l'uscita e' avvenuta per loss cap, non per mancata cattura di profitto.

Il problema osservato e':

`rolling candidate valido offline` non implica `opportunita' ancora viva al momento della promotion/BUY`.

Il dato chiave e':

- `avgAdviceToBuyNetMove=-0.0028000000004304`

Questo significa che, tra advice e BUY, il movimento netto medio era gia' nell'area del loss cap. Quindi il candidato era statisticamente valido nella finestra storica, ma decaduto nel punto operativo.

## Problemi Operativi Separati

Sono emersi anche problemi non scientifici ma da correggere prima di altre prove:

- apertura duplicata sullo stesso simbolo durante cicli/retry ravvicinati;
- deadlock MySQL su aggiornamento `acdc_run_execution.linked_run_id` durante chiamate PAPER sovrapposte;
- necessita' di idempotenza o lock endpoint per evitare run concorrenti sullo stesso profilo.

Questi problemi non spiegano il `BAD_ADVICE`, ma rendono la prova piu' rumorosa e vanno corretti.

## Decisione

Esito:

`FAIL_LIVE_DECAY_BAD_ADVICE`

Decisione vincolante:

- Non fare REAL.
- Non ripetere PAPER sullo stesso modello senza live revalidation.
- Non considerare `PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT` sufficiente per comprare.

## Prossimo Step Richiesto

Prima della prossima prova:

- aggiungere live revalidation gate alla promotion o al BUY;
- validare che il regime corrente sia ancora quello osservato nella rolling validation;
- validare che il prezzo corrente non sia gia' decaduto contro l'entry reference;
- bloccare aperture duplicate per stesso profilo/simbolo;
- rendere l'endpoint PAPER idempotente o protetto da lock anti-concorrenza.

