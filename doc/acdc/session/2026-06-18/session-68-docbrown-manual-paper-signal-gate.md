# Session 68 - DocBrown Manual Paper Signal Gate

Data: 2026-06-18

## Obiettivo

Evitare che DocBrown apra automaticamente una nuova PAPER a ogni ciclo ML durante i test controllati.

## Implementazione

DocBrown ora supporta `docbrown.rem.acdc.shadow-signal.mode`, esposta via:

- `DOCBROWN_ACDC_SHADOW_SIGNAL_MODE=MANUAL`
- `DOCBROWN_ACDC_SHADOW_SIGNAL_MODE=SINGLE_SHOT`
- `DOCBROWN_ACDC_SHADOW_SIGNAL_MODE=ALWAYS`

Modalita':

- `MANUAL`: salva advice a DB ma non chiama ACDC.
- `SINGLE_SHOT`: invia un solo signal per processo.
- `ALWAYS`: comportamento precedente.

Il container DocBrown e' stato ricreato con:

- scheduler REM attivo;
- live revalidation attiva;
- signal enabled;
- signal mode `MANUAL`.

## Stato Runtime

- PAPER 58: completata, net `-0.558330059461302`.
- PAPER 59: completata, net `+0.289863600256154`.
- PAPER 60: partita prima del gate manuale, stop-buy applicato.

Da questo punto DocBrown puo' continuare a minare e aggiornare advice senza avviare nuove PAPER automaticamente.

