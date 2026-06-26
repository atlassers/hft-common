# Session 67 - Signature Paper Eligible

Data: 2026-06-18

## Obiettivo

Includere senza stravolgere la logica attuale alcune signature SYMBOL scartate dal miner multivariato ma risultate profittevoli in validazione temporale.

## Decisione

Non sono state abbassate le soglie esistenti e non e' stata cambiata la promozione `PAPER_ELIGIBLE`.

E' stata aggiunta una corsia separata:

- `PAPER_ELIGIBLE`: regole multivariate DocBrown gia' promosse.
- `SIGNATURE_PAPER_ELIGIBLE`: fallback PAPER per signature SYMBOL robuste quando il miner multivariato non promuove nulla.

ACDC considera entrambe acquistabili in PAPER tramite `ml_advice_paper_eligible = 1`, ma mantiene audit separato:

- `ml_advice_promotion_class_code = 1`: `PAPER_ELIGIBLE`
- `ml_advice_promotion_class_code = 2`: `SIGNATURE_PAPER_ELIGIBLE`

## Vincoli scientifici

La nuova corsia:

- usa solo scope `SYMBOL`;
- esclude `GLOBAL`, `FAMILY`, `UNKNOWN`;
- richiede TRAIN e VALIDATION sufficienti;
- riusa le soglie DB condivise:
  - `rem.ml.min.validation.samples`;
  - `rem.ml.min.validation.profit_rate`;
  - `rem.ml.advice.min.safe_net_return`;
  - `rem.ml.advice.min.max_net_return`;
  - `rem.ml.advice.min.profit_probability`;
- incorpora una band qualita' dinamica:
  - prima prova la band `ACTIVE`;
  - se `ACTIVE` non produce candidate promuovibili, usa l'ultima band `DIAGNOSTIC` del ciclo corrente;
- usa outcome gia' netti di fee, slippage e dust.

## Implementazione

DocBrown:

- aggiunto `SignaturePaperAdvicePromotionService`;
- il ciclo REM ora esegue:
  1. outcome mining;
  2. dynamic band discovery;
  3. multivariate reversal ML;
  4. fallback signature PAPER advice solo se non ci sono promozioni multivariate.
- il fallback signature e' ACTIVE-first, ma puo' usare la DIAGNOSTIC corrente quando la band ACTIVE storica non intercetta il regime corrente.

ACDC:

- `OutcomeQualityModelService` accetta `SIGNATURE_PAPER_ELIGIBLE` come paper eligible;
- il codice audit distingue la classe con valore `2`;
- aggiunto test dedicato.

## Verifiche

- `docbrown`: `./mvnw -q test`
- `acdc`: `./mvnw -q -Dtest=OutcomeQualityModelServiceTest test`
