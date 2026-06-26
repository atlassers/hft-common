# Sessione 17 - HFT-FE Runtime Actions ACDC

Data: 2026-06-15.

## Obiettivo

Far partire ogni RUN da `hft-fe`, sostituendo l'endpoint operativo legacy HFT/Kenshiro con ACDC.

## Decisione

Gli script operativi stanno in `acdc/scripts` e non vengono usati come backend operativo: restano wrapper verso `hft-fe`.
HFT resta riferimento storico e non deve ricevere nuovi script di run.
La rotta FE `POST /backoffice/best-winner/actions/{action}` viene spostata su ACDC.

## Implementazione ACDC

- Aggiunta route compatibile:
  - `POST /backoffice/best-winner/actions/{action}`.
- Aggiunto service `BestWinnerActionService`.
- Profilo operativo di default: `REM_CURRENT`.
- `start-run`:
  - `DRY` esegue il dry run;
  - `SHADOW` esegue la shadow run;
  - `PAPER` esegue la paper run;
  - `REAL` viene respinto.
- `stop-run`:
  - `PAPER` con budget riservato attiva `stop-buy` per drenare le SELL;
  - `PAPER` senza budget riservato chiude l'execution;
  - `DRY`/`SHADOW` sono idempotenti.
- `stop-containers` viene respinto: lo stop dei container resta fuori dal FE.

## Implementazione FE

- `hft-fe` inoltra la route action ad ACDC tramite `proxyAcdc`.
- Il selettore FE supporta `DRY`, `SHADOW`, `PAPER`, `REAL`.
- Gli script wrapper ACDC sono:
  - `scripts/acdc-start-dry-run.sh`;
  - `scripts/acdc-start-shadow-run.sh`;
  - `scripts/acdc-start-paper-run.sh`;
  - `scripts/acdc-start-real-run.sh`;
  - `scripts/acdc-stop-run.sh`;
  - `scripts/acdc-stop-containers.sh`.
- Base ACDC configurabile con:
  - `ACDC_INTERNAL_API_BASE`;
  - `PUBLIC_ACDC_API_BASE`;
  - `VITE_ACDC_API_BASE`.

## Sicurezza

- Nessuna REAL RUN avviata.
- Nessun ordine Binance.
- Il blocco REAL e' esplicito nella risposta action.
