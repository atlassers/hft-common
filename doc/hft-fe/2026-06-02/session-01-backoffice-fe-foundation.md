# Sessione 1 - Backoffice FE Foundation

## Piano 1 - Documentazione Funzionale

User story: `US-001`.

Decisioni:

- Creare `project.md` prima del codice per fissare scopo, vincoli e contratti REST.
- Creare `profile-users-story.md` per collegare ogni feature a sessione/piano/user story.
- Tenere il diario separato in `diary/` con indice `OPERATIVE_PLANS.md`.

Esito: completato.

## Piano 2 - Scaffold SvelteKit Minimo

User story: `US-002`.

Decisioni:

- `npx` non e disponibile nell'ambiente locale, quindi lo scaffold viene creato manualmente.
- Dipendenze UI limitate a SvelteKit, Tailwind CSS, Flowbite Svelte e Flowbite.
- Nessun componente condiviso nella prima release: le pagine restano autonome.

Esito: completato.

## Piano 3 - Auth Username Password

User story: `US-003`.

Decisioni:

- Sessione salvata in `localStorage` con token bearer.
- Predisposizione Keycloak tramite variabili `VITE_AUTH_REALM`, `VITE_AUTH_CLIENT_ID`, `VITE_AUTH_BASE_URL`.
- Prima release usa endpoint username/password `POST /auth/login`.

Esito: completato.

## Piano 4 - Client REST Tipizzati

User story: `US-004`.

Decisioni:

- Un solo wrapper `http.ts` gestisce base URL, bearer token, JSON e redirect su 401.
- Client separati per auth, docbrown, hft, config, pipeline e dashboard.
- Endpoint non ancora presenti lato BE sono marcati come backoffice attesi nei documenti e integrati nei client.

Esito: completato.

## Piano 5 - Dashboard Operativa

User story: `US-005`.

Decisioni:

- La dashboard prova prima l'endpoint aggregato backoffice.
- Se l'endpoint aggregato non esiste, compone dati da account Binance e paper summary.
- PAPER e REAL sono visualizzati come categorie distinte.

Esito: completato.

## Piano 6 - Pagina Docbrown

User story: `US-006`.

Decisioni:

- La pagina Docbrown mostra compiti, endpoint, configurazioni modello e azioni backtest.
- Le request operative sono precompilate con valori prudenti e modificabili.

Esito: completato.

## Piano 7 - Pagina HFT

User story: `US-007`.

Decisioni:

- La pagina HFT aggrega gate runtime, paper guard, paper gate, Sheldon, Stan e trade positions.
- BUY/SELL REAL non vengono esposti come pulsanti rapidi nella prima release.

Esito: completato.

## Piano 8 - Configurazioni DB E Globali

User story: `US-008`.

Decisioni:

- Config normalizzate renderizzate con slider `0...1`.
- Config non normalizzate renderizzate con input numerico.
- Config globali renderizzate come chiave/valore editabile.

Esito: completato.

## Piano 9 - Pipeline End-To-End

User story: `US-009`.

Decisioni:

- La pagina pipeline usa endpoint backoffice aggregati attesi.
- Mostra valori Docbrown, simboli promossi/bocciati e acquisti come sezioni separate.

Esito: completato.

## Piano 10 - Verifica Tecnica

User story: `US-010`.

Decisioni:

- Script `dev`, `build`, `check` definiti in `package.json`.
- Verifica non eseguita per assenza di Node/npm/npx nell'ambiente corrente.

Esito: bloccato da ambiente locale.

## Aggiornamento Piano 10 - Verifica Tecnica Post Installazione Node

Decisioni:

- Tentata installazione OS-level con `sudo -n apt-get update`; bloccata per password sudo richiesta.
- Installazione disponibile globalmente per l'utente tramite `~/.local/bin`: Node `v24.16.0`, npm/npx `11.13.0`.
- Aggiornate dipendenze SvelteKit/Flowbite/Tailwind a versioni correnti risolte da npm.
- Aggiunto `@types/node` per eliminare warning TypeScript.
- Non applicato `npm audit fix --force`: npm propone un cambio breaking non coerente.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato con successo.
- `npm audit --audit-level=low`: 3 vulnerabilita low severity residue su dipendenza transitiva `cookie` via SvelteKit; nessun fix sicuro automatico disponibile senza `--force`.

Esito: completato con limite OS-level dovuto a sudo non interattivo.
