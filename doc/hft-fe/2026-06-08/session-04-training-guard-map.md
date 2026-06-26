# Sessione 4 - Training Guard Map

## Piano 1 - Mappa Offline Training Verso Realtime Guard

Data: 2026-06-08.

Obiettivo: rendere esplicita in `/pipeline` la differenza tra milioni di righe offline e guardia realtime.

Modifica:

- Aggiunta sezione overview `Offline training -> real-time guard`.
- La UI mostra quattro step: dataset offline, training gate, promozione DB, BUY realtime.
- La UI chiarisce che le righe candidate/outcome non sono segnali live da comprare uno a uno.
- La UI mostra evidenza operativa corrente: finestra 12h, 7.496.100 outcome, `Policy READY=0`, gross/net negativi e REAL safety OFF.

Decisione:

- La vista deve aiutare a capire se il training ha prodotto una configurazione compatta promuovibile.
- Se `READY=0`, la guardia runtime deve rimanere fail-closed.

## Piano 2 - Execution-Scoped Pipeline View

Data: 2026-06-08 17:25 CEST.

Obiettivo: rendere la UI pipeline coerente con una singola execution selezionata, preparando la lettura operativa prima di una PAPER RUN.

Interventi:

- Aggiunta route `/pipelines` come entrypoint principale della pipeline.
- Spostata la lista execution dentro `/pipelines`; `/pipeline/executions` usa la execution selezionata dalla listbox alta.
- Persistita la execution selezionata per scenario (`PAPER`/`REAL`) in localStorage, separata dal mode globale.
- Aggiunta listbox execution in alto accanto al selettore `PAPER RUN` / `REAL RUN`.
- Allineate `/pipelines`, `/pipeline/executions` e `/pipeline/trades` alla stessa execution selezionata.
- Ridisegnati gli step pipeline con tre payload espliciti: `Configurazione associata`, `Input`, `Output`.
- Ridisegnata la trade chain per execution: nodi BUY/SELL con config/input/output e riepilogo finale con trade, chiusi, aperti, win, loss, win rate e net profit.
- Aggiornate label e gruppi payload per costanti tecniche, centroidi normalizzati, scaler, cluster e trailing profit.

Verifiche:

- `npm run check` -> 0 errori, 0 warning.
- `npm run build` -> OK.

Nota operativa: nessuna PAPER RUN e nessuna REAL RUN avviata in questo intervento.

### Fix - Execution Selector Globale

Data: 2026-06-08 17:45 CEST.

Correzione UI richiesta:

- Spostata la listbox execution dal pannello `Pipeline dashboard` al layout globale, a destra dello switch `PAPER RUN` / `REAL RUN`.
- La listbox mostra solo la data nel formato `YYYY-MM-DD HH:MM:SS`.
- La selezione resta persistita per scenario e viene propagata alle pagine pipeline tramite evento globale.
- `/pipelines` non mostra piu' il dettaglio completo di `/pipeline/executions`; resta solo la lista execution e l'overview runtime.

Verifiche:

- `npm run check` -> 0 errori, 0 warning.
- `npm run build` -> OK.
- `docker compose up -d --build` -> `hft-fe-local` ricreato.
- HTTP `/pipelines` -> 200.
- HTTP `/pipeline/executions` -> 200.
