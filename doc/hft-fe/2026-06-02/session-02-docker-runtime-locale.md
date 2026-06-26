# Sessione 2 - Docker Runtime Locale

## Piano 1 - Docker Compose Locale Funzionante

User story: `S02-P01-US-011`.

Decisioni:

- Sostituito `@sveltejs/adapter-auto` con `@sveltejs/adapter-node` per produrre un server Node esplicito e adatto a Docker.
- Aggiunto `Dockerfile` dedicato al runtime locale.
- Aggiunto `docker-compose.yml` con servizio `hft-fe`, immagine `hft-fe:local`, container `hft-fe-local` e porta host `5173` verso porta container `3000`.
- Tentato Dockerfile multi-stage con `npm ci`; la fase `npm ci` dentro BuildKit e rimasta bloccata senza output per diversi minuti.
- Scelto Dockerfile pragmatico locale: copia `build/` e `node_modules` gia verificati localmente. E meno ideale per CI, ma soddisfa l'obiettivo operativo di Docker locale funzionante.
- Mantenuto `host.docker.internal` in compose per consentire al container di raggiungere backend locali se servira SSR/proxy; le chiamate attuali partono dal browser e usano i base URL build-time/default.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato con `@sveltejs/adapter-node`.
- `docker compose build --no-cache`: immagine `hft-fe:local` creata.
- `docker compose up -d`: container `hft-fe-local` avviato.
- `docker compose ps`: container `Up`, porta `0.0.0.0:5173->3000/tcp`.
- `curl http://localhost:5173/auth/login`: HTTP 200.
- `docker compose logs hft-fe`: `Listening on http://0.0.0.0:3000`.

Esito: completato.

## Piano 2 - Auth Locale Docker

User story: `S02-P02-US-012`.

Decisioni:

- Il `Failed to fetch` in login era causato dal tentativo del browser di chiamare un backend auth esterno non disponibile.
- Aggiunto endpoint locale SvelteKit `POST /api/auth/login` per la prima release username/password.
- Credenziali Docker locali configurabili via env `AUTH_DEV_USERNAME` e `AUTH_DEV_PASSWORD`.
- Default locale: `admin` / `admin`.
- Il client usa `/api/auth/login` se `VITE_AUTH_API_BASE` non e impostato; resta predisposto per auth esterna/Keycloak.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build`: completato.
- `docker compose up -d --force-recreate`: container avviato.
- `POST /api/auth/login` con `admin/admin`: HTTP 200 con token bearer locale.
- `POST /api/auth/login` con password errata: HTTP 401.

Esito: completato.

## Piano 3 - Compatibilita Cache Chrome Auth

User story: `S02-P03-US-013`.

Decisioni:

- Chrome stava eseguendo un chunk JS vecchio che chiamava `http://localhost:8080/auth/login`.
- Aggiunto alias endpoint `POST /auth/login` oltre a `POST /api/auth/login`.
- Aggiunta esposizione temporanea `8080:3000` in `docker-compose.yml` per supportare bundle browser cache vecchi.
- La porta canonica del FE resta `5173`; la porta `8080` e solo compatibilita locale e va rimossa quando HFT backend gira sulla stessa macchina.

Verifiche:

- `POST http://localhost:5173/api/auth/login` con `admin/admin`: HTTP 200.
- `POST http://localhost:8080/auth/login` con `admin/admin`: HTTP 200.
- `docker compose ps`: container espone `5173->3000` e `8080->3000`.

Esito: completato.

## Piano 4 - CORS Su Auth Compatibilita 8080

User story: `S02-P04-US-014`.

Decisioni:

- Il vecchio chunk Chrome chiamava `http://localhost:8080/auth/login` da origin `http://localhost:5173`, quindi il browser eseguiva preflight CORS.
- Aggiunto handler `OPTIONS` per `/api/auth/login` e `/auth/login`.
- Aggiunti header CORS su preflight, risposte 200, 400 e 401.

Verifiche:

- `OPTIONS http://localhost:8080/auth/login` con origin `http://localhost:5173`: HTTP 204 con `Access-Control-Allow-Origin: *`.
- `POST http://localhost:8080/auth/login` con origin `http://localhost:5173`: HTTP 200 con token locale.
- Container `hft-fe-local` riavviato e operativo.

Esito: completato.

## Piano 5 - CORS Globale E Dashboard Fallback

User story: `S02-P05-US-015`.

Decisioni:

- Dopo login, Chrome chiamava `http://localhost:8080/backoffice/dashboard` da origin `http://localhost:5173`.
- Aggiunto `src/hooks.server.ts` per CORS globale su tutte le risposte e su tutti i preflight `OPTIONS`.
- Aggiunto fallback locale `GET /backoffice/dashboard` per rendere il Docker navigabile anche senza backend HFT attivo.
- Aggiunti fallback locali minimi per `GET /crypto/account-info` e `GET /paper-trading/summary`.

Verifiche:

- `OPTIONS http://localhost:8080/backoffice/dashboard` con origin `http://localhost:5173`: HTTP 204 con CORS.
- `GET http://localhost:8080/backoffice/dashboard` con origin `http://localhost:5173`: HTTP 200 con JSON dashboard fallback.
- Container `hft-fe-local` riavviato e operativo.

Esito: completato.

## Piano 6 - Pipeline Fallback Locale

User story: `S02-P06-US-016`.

Decisioni:

- La pagina pipeline richiede endpoint aggregati non ancora presenti nel BE HFT.
- Aggiunti fallback locali Docker per `GET /backoffice/pipeline/executions` e `GET /backoffice/pipeline/executions/{executionUuid}`.
- I dati sono demo PAPER e servono solo per rendere la UI navigabile finche il BE espone i contratti reali.

Verifiche:

- `GET http://localhost:8080/backoffice/pipeline/executions?limit=50`: HTTP 200 con lista demo.
- `GET http://localhost:8080/backoffice/pipeline/executions/local-demo-paper-001`: HTTP 200 con dettaglio demo.
- `OPTIONS` sul path lista: HTTP 204 con CORS.
- Container `hft-fe-local` riavviato e operativo.

Esito: completato.

## Piano 7 - Navigazione Browser Completa E Fallback Endpoint

User story: `S02-P07-US-017`.

Decisioni:

- Aggiunti fallback locali per tutti gli endpoint invocati dalle pagine FE quando il backend reale non e collegato.
- Cambiati i default API client a same-origin per il Docker locale; eventuali backend reali vanno configurati con variabili `VITE_*` al build time.
- Rimossa la dipendenza operativa da `localhost:8081` perche la porta era gia allocata sul sistema.
- Aggiunti grafici SVG leggeri a dashboard e riepilogo pipeline, senza introdurre librerie runtime aggiuntive.
- Aggiunto `puppeteer-core` solo come dev dependency per pilotare Google Chrome locale nei test browser.
- Aggiunto script ripetibile `npm run test:browser`.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build`: completato.
- `docker compose up -d --force-recreate`: container operativo.
- `npm run test:browser`: login e navigazione Chrome headless su dashboard, pipeline, docbrown, hft, configurazioni DB/globali; click azioni Docbrown, Stan e salvataggi config; nessun console error, request failed o risposta HTTP >= 400.
- `docker compose logs`: nessun 404/500/CORS residuo dopo il test browser.

Esito: completato.

## Piano 8 - Layout Sidebar Flowbite Docs

User story: `S02-P08-US-018`.

Decisioni:

- Adeguato layout allo stile Flowbite docs: navbar superiore minimale, sidebar laterale `Browse backoffice`, gruppi di navigazione e contenuto centrale su fondo grigio chiaro.
- Rimossa la navigazione principale orizzontale come primaria; resta solo header con brand, stato PAPER safe e logout.
- Mantenuta compatibilita mobile con pulsante hamburger e overlay.
- Aggiornato `src/app.css` con base Flowbite/Tailwind piu vicina alla documentazione.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build` e `docker compose up -d --force-recreate`: completati.
- `npm run test:browser`: login e navigazione completa con sidebar; nessun errore console/network.
- Log container: nessun 404/500/CORS residuo.

Esito: completato.

## Piano 9 - Dashboard Account Strutturato

User story: `S02-P09-US-019`.

Decisioni:

- Rimosso dalla dashboard il messaggio tecnico grezzo sotto `Account Binance corrente`.
- Il fallback dashboard ora restituisce un account strutturato con `source`, `executionMode`, `updatedAt`, `status` e `balances`.
- La pagina mostra card balance leggibili per asset, free e locked, invece del JSON completo.
- Normalizzato il fallback client in `loadDashboard()` per evitare stringhe account legacy.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build` e `docker compose up -d --force-recreate`: completati.
- `npm run test:browser`: navigazione completa senza errori console/network.

Esito: completato.

## Piano 10 - Selettore PAPER REAL E Tema Dark Flowbite

User story: `S02-P10-US-020`.

Decisioni:

- Sostituito il badge statico `PAPER safe` con un selettore esplicito `PAPER RUN` / `REAL RUN` in alto a destra.
- Persistita la scelta in `localStorage` con chiave `hft-fe.executionMode`.
- Mantenuto badge stato accanto al selettore: `PAPER safe` oppure `REAL selected`.
- Impostato tema scuro di default sul layout applicativo per avvicinarsi al look Flowbite docs richiesto.
- Aggiunti override CSS dark per card/pannelli esistenti, cosi le pagine non restano in toni chiari.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build` e `docker compose up -d --force-recreate`: completati.
- `npm run test:browser`: navigazione completa; header mostra `PAPER RUN`, `REAL RUN`, `PAPER safe`; nessun errore console/network.
- Log container: nessun 404/500/CORS residuo.

Esito: completato.
