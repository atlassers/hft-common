# Sessione 7 - Best Winner Controls

## Piano 1 - Pagina Configurazione Best Winner

Obiettivo: aggiungere al FE una sezione operativa per configurare il modello best-winner multi-finestra e comandare la PAPER RUN salvata.

Modifiche:

- aggiunta pagina `/best-winner`;
- aggiunto item sidebar `Best winner`;
- aggiunti tipi `BestWinner*`;
- aggiunta API client `bestWinnerApi`;
- aggiunti proxy SvelteKit:
  - `/backoffice/best-winner/window-config`;
  - `/backoffice/best-winner/signatures`;
  - `/backoffice/best-winner/actions/[action]`.

UI:

- slider su `max_window_minutes`;
- calcolo immediato finestre derivate;
- input puntuali per ratio/override;
- bottoni `Avvia PAPER`, `Stop run`, `Stop container`;
- firme mostrate come card con simbolo, net ora, profit factor e condizioni leggibili.

Verifiche:

- `npm run check` OK;
- `npm run build` OK;
- `npm run test:best-winner` OK;
- container `hft-fe-local` rebuild/restart;
- pagina servita su `http://localhost:5173/best-winner`;
- endpoint proxy configurazione verificato con dati reali.

## Piano 2 - Browser Test Best Winner

Obiettivo: cementare il componente `/best-winner` e la sua logica senza toccare DB o avviare run reali.

Test aggiunto:

- `scripts/best-winner-browser.mjs`;
- script npm `test:best-winner`.

Copertura:

- mock HTTP di `/backoffice/best-winner/*`;
- rendering card firma con simbolo, net ora, profit factor e condizioni leggibili;
- calcolo finestre derivate da `max_window_minutes`;
- salvataggio config con payload `PUT`;
- bottone `Avvia PAPER` con chiamata `POST /actions/start-run` e output visualizzato.

Bug trovato e risolto:

- il form non inviava se alcuni valori ratio DB avevano precisione non compatibile con `step`;
- gli input frazionari ora usano `step="any"`.

## Piano 3 - Run Control FE-Only

Obiettivo: rendere esplicito che ogni azione runtime parte dal FE, anche quando viene invocata tramite shortcut locale.

Modifiche:

- aggiunto selettore modalita' `PAPER/DRY/REAL` nella pagina `/best-winner`;
- `bestWinnerApi.action(...)` invia sempre `{ executionMode, allowRealRun }`;
- il bottone start mostra la modalita' selezionata;
- il browser test verifica che il body dello start contenga `executionMode: PAPER` e `allowRealRun: false`.

Verifiche:

- `npm run check` OK;
- `npm run build` OK;
- `npm run test:best-winner` OK su dev server e su container `hft-fe-local`.
