# Session 41 - Loss Analysis e Clean Shadow

Data: 2026-06-16.

## Domanda

Capire se le loss osservate sono evitabili o se rappresentano una quota minima utile di rumore sulle SELL.

## Risposta Operativa

Le loss non sono utili in se'.
Sono il costo di falsificazione del segnale ML.

Devono:

- restare piccole;
- chiudersi con `EXIT_ML_ADVICE_LOSS_CAP`;
- non ripetersi sullo stesso tipo di segnale senza nuova evidenza ML;
- essere usate da DocBrown per migliorare la promozione futura delle regole, non per introdurre tuning manuale in ACDC.

## Evidenza Run 37

La run `37` e' stata la prima dopo V32, ma e' partita prima del fix advice-preservation.

Risultato finale:

- status `COMPLETED`;
- budget finale `99.853662838113222000`;
- realized `-0.146337161886778000`.

Trade:

- `BANKUSDC`: profitto `+0.069784172581200000`;
- `BANANAS31USDC`: loss `-0.208358151597152000`;
- `ALLOUSDC`: profitto `+0.138395774924800000`;
- `BANANAS31USDC`: profitto `+0.203541042204374000`;
- `ALLOUSDC`: loss `-0.349700000000000000`.

Lettura:

- la SELL funziona: vende sia in take-profit sia in loss-cap;
- il modello non e' ancora sufficientemente selettivo su tutti gli ingressi;
- il runtime non deve inventare soglie correttive, ma deve applicare in modo rigoroso l'advice della BUY.

## Fix

Bug corretto:

- prima, durante SELL, le feature live potevano sovrascrivere l'advice salvato in `policy_json`;
- ora `MlAdviceFeatures` preserva sempre l'advice della BUY:
  - durata;
  - safe return;
  - max return;
  - loss cap.

Test:

- aggiunto `MlAdviceFeaturesTest`;
- suite ACDC completata.

## Clean Shadow 38

Run avviata dopo il fix:

- execution `38`;
- status finale `COMPLETED`;
- budget finale `99.425476060175600000`;
- realized `-0.574523939824400000`.

Trade:

- `ALLOUSDC`:
  - `reversal_ml_rules=5`;
  - safe `0.002495878091431426`;
  - duration `600`;
  - loss-cap `-0.013983524712930604`;
  - SELL `EXIT_ML_ADVICE_LOSS_CAP`;
  - net `-0.405877859072400000`;
  - max netto `0`.
- `BANKUSDC`:
  - `reversal_ml_rules=2`;
  - safe `0.0136270783847981`;
  - duration `640`;
  - loss-cap `-0.003`;
  - SELL `EXIT_ML_ADVICE_LOSS_CAP`;
  - net `-0.168646080752000000`;
  - max netto `0`.

Conclusione:

- questa run e' scientificamente pulita sul runtime;
- il runtime ha fatto cio' che doveva: BUY solo con advice, SELL su loss-cap;
- il modello ha prodotto due falsi positivi live;
- entrambe le posizioni non hanno mai mostrato max netto positivo.

## Timeout Advice

Durante la 38 e' emerso un secondo rischio: se il prezzo resta flat e non tocca ne' profitto ne' loss-cap, la posizione puo' restare appesa.

Aggiunta `V33__ml_advice_timeout_exit.sql`:

- guardia `exit_ml_advice_timeout`;
- operator `ML_ADVICE_TIMEOUT_EXIT`;
- chiude quando `hold_seconds >= ml_advice_duration_seconds`.

Questa non e' una soglia manuale: applica la durata prevista dal modello.
