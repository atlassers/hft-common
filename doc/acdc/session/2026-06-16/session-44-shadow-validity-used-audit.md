# Session 44 - SHADOW Validity Used Audit

Data: 2026-06-16

## Scopo

Verificare live:

- consulenze DocBrown fresche con `advice_valid_from/until`;
- filtro ACDC su consulenze non scadute;
- marcatura `used` delle regole `SYMBOL`;
- retroazione DocBrown dopo SHADOW.

## Preparazione

DocBrown job:

- endpoint: `POST /docbrown/rem/research/REM_CURRENT/run`;
- regole ML valutate: `275`;
- regole ML promosse: `19`;
- regole promosse con validita': `19`;
- `active_now` ACDC prima della SHADOW: `19`.

Prima regola promossa osservata:

- `BERAUSDC` `pullback_rebound_volume`;
- validita' circa `2026-06-16 17:09:30 UTC` -> `2026-06-16 17:14:30 UTC`.

## SHADOW 41

Avvio:

- script: `./scripts/acdc-start-shadow-run.sh`;
- execution: `41`;
- data source: `INFLUX`;
- evaluated: `200`;
- accepted/opened: `3`;
- BUY stop applicato subito con `./scripts/acdc-stop-run.sh SHADOW`.

Posizioni aperte:

- `BERAUSDC`, rules `2`, safe `0.012748031496062993`, duration `665`, loss cap `-0.006937007874015748`;
- `ALTUSDC`, rules `1`, safe `0.004575757575757576`, duration `705`, loss cap `-0.003`;
- `ARKMUSDC`, rules `1`, safe `0.002061460592913955`, duration `640`, loss cap `-0.01023065798987708`.

Marcatura `used` verificata:

- `ALTUSDC` `pullback_rebound_volume`;
- `ARKMUSDC` `acceleration_reversal`;
- `BERAUSDC` `curve_reversal`;
- `BERAUSDC` `pullback_rebound_volume`.

## Risultato

Execution `41`:

- status `COMPLETED`;
- budget finale `99.759050864421420000`;
- realized `-0.240949135578580000`.

SELL:

- `ALTUSDC`: `EXIT_ML_ADVICE_LOSS_CAP`, net `-0.086890694236580000`, max net `0.003902511078286558`, safe `0.004575757575757576`;
- `ARKMUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.104058441342000000`, max net `0`;
- `BERAUSDC`: `EXIT_ML_ADVICE_TIMEOUT`, net `-0.050000000000000000`, max net `0.001902343750000000`, safe `0.012748031496062993`.

Lettura:

- `ALTUSDC` quasi raggiunge il safe target ma poi chiude in loss: `under_safe_non_profit`;
- `ARKMUSDC` non genera MFE positiva: `zero_mfe_loss`;
- `BERAUSDC` genera MFE positiva, ma lontanissima dal safe target: `under_safe_non_profit`.

## DocBrown Post-Run

DocBrown job post SHADOW `41`:

- regole ML valutate: `250`;
- regole ML promosse: `36`;
- regole promosse con validita': `36`.

Regole usate dalla SHADOW e poi respinte:

- `ALTUSDC` `pullback_rebound_volume`: `liveAuditSamples=1`, `underSafeNonProfitRate=1`;
- `ARKMUSDC` `acceleration_reversal`: `liveAuditSamples=1`, `zeroMfeLossRate=1`, `underSafeNonProfitRate=1`;
- `BERAUSDC` `curve_reversal`: `REJECTED`;
- `BERAUSDC` `pullback_rebound_volume`: `REJECTED`.

Nota:

- Alcune regole diverse sugli stessi simboli possono essere promosse se non erano le rule-key usate nella BUY falsificata.
- Questo e' coerente con il contratto attuale: il live-audit e' rule-key specifico, non symbol-ban.

## Conclusione

La nuova logica `advice_valid_from/until` + `used` funziona.

La SHADOW non e' profittevole, ma produce feedback utile e preciso:

- non c'e' problema di riuso della stessa consulenza puntuale;
- il problema resta nella qualita' del segnale ML prima della BUY;
- DocBrown deve continuare a promuovere/rigettare su rule-key e outcome live, non su soglie manuali ACDC.
