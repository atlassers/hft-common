# Sessione 03 - Scenario Runtime Controls

Avvio sessione: 2026-06-02.

Motivo nuova sessione: la sessione 02 ha raggiunto 10 piani operativi.

## Piano 1 - Scenario PAPER REAL End-to-End E Avvio Run

User story: `S03-P01-US-021`.

Decisioni:

- Il selettore `PAPER RUN` / `REAL RUN` non deve essere solo visuale: la scelta ora viene letta dal client HTTP e propagata alle chiamate `GET` HFT/Docbrown con `executionMode` e `mode` in query string.
- Dashboard e Pipeline ascoltano l'evento `hft-execution-mode-change` e ricaricano i dati quando cambia scenario.
- La Dashboard evidenzia lo scenario corrente, separa dati PAPER e REAL e usa badge scuri ad alto contrasto.
- La Pipeline filtra le execution per scenario, usa badge stato leggibili in dark mode e mostra dataset fallback separati PAPER/REAL.
- Aggiunta action `Avvia RUN {PAPER|REAL}` su Pipeline con `POST /backoffice/pipeline/executions` e body `{ executionMode }`.
- Il fallback locale implementa la `POST` e crea una execution `RUNNING` per testare il flusso senza backend/DB reale.
- Esteso il test browser per verificare cambio REAL su Dashboard e avvio run REAL su Pipeline.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build` e `docker compose up -d --force-recreate`: completati.
- `npm run test:browser`: login, navigazione completa, cambio REAL, Dashboard REAL e avvio run REAL in Pipeline; nessun errore console/network.
- `docker compose logs`: nessun 404/500/CORS residuo.

Esito: completato.

## Piano 2 - Verifica Sezioni E Aggancio Dati HFT

User story: `S03-P02-US-022`.

Decisioni:

- Ricontrollate le sezioni Dashboard, Pipeline, Docbrown, HFT e configurazioni confrontando le API FE con i controller reali HFT/Docbrown presenti nel workspace.
- Dashboard spostata su composizione da endpoint HFT gia esposti: `/crypto/account-info/by-assets`, `/paper-trading/summary`, `/paper-trading/runs`, `/trade-position`.
- Pipeline resa degradabile: usa prima `/backoffice/pipeline/*`; se assente deriva una vista parziale da `/paper-trading/runs` e `/trade-position`.
- Ogni dato derivato o fallback ora espone `dataSource` e, dove necessario, `partial`, cosi i mock non sembrano dati reali completi.
- Aggiunto fallback locale same-origin per `/crypto/account-info/by-assets`, per mantenere lo stesso contratto dell'HFT reale anche nel Docker FE.
- Le configurazioni DB/globali mostrano esplicitamente `LOCAL_FALLBACK` quando arrivano dal fallback FE.
- Creato `missing-hft-endpoints.md` con endpoint mancanti e contratti attesi da far aggiungere al backend.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build` e `docker compose up -d --force-recreate`: completati.
- `npm run test:browser`: login, navigazione completa, cambio REAL, Dashboard REAL e avvio run REAL in Pipeline; nessun errore console/network.
- `docker compose logs`: nessun 404/500/CORS residuo.

Esito: completato.

## Piano 3 - Link Endpoint Aggregati HFT Docbrown

User story: `S03-P03-US-023`.

Decisioni:

- La Dashboard ora usa come sorgente primaria `GET /backoffice/dashboard?executionMode=...`.
- La Pipeline usa come sorgente primaria gli endpoint aggregati `/backoffice/pipeline/executions` e `/backoffice/pipeline/executions/{executionUuid}`.
- L'action `Avvia RUN` resta collegata a `POST /backoffice/pipeline/executions`.
- Le configurazioni DB/globali restano collegate direttamente agli endpoint `/backoffice/config/{component}/{scope}`.
- Il client API legge anche variabili runtime pubbliche `PUBLIC_HFT_API_BASE`, `PUBLIC_DOCBROWN_API_BASE`, `PUBLIC_AUTH_API_BASE`.
- Docker non espone piu il FE su porta `8080`, per non collidere con HFT. Il FE resta su `5173`.
- Default Docker aggiornati ai root path Quarkus reali: HFT `http://localhost:8080/hft`, Docbrown `http://localhost:8083/docbrown`.
- Sostituito `missing-hft-endpoints.md` con `backend-endpoints.md`, per documentare endpoint ormai integrati e collegati.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build --no-cache` e `docker compose up -d --force-recreate`: completati.
- Container FE attivo solo su `5173`.
- Test live sugli endpoint reali non completabile perche `localhost:8080` e `localhost:8083` rifiutano connessione nel momento della verifica.

Esito: completato lato FE; verifica live da ripetere con HFT e Docbrown avviati.

## Piano 4 - Regole Commit FE E Current Context

User story: `S03-P04-US-024`.

Decisioni:

- Aggiunta regola permanente FE: a fine attivita' committare e pushare i file FE modificati.
- Definita convention commit FE incrementale senza zeri: `FE1. <commit-message>`, `FE2. <commit-message>`, ecc.
- Creato `diary/CURRENT_CONTEXT.md` come contesto operativo breve da leggere all'inizio di ogni nuova richiesta sul modulo `hft-fe`.
- `CURRENT_CONTEXT.md` replica la logica del file HFT: regole permanenti, repo, stato runtime, endpoint, file chiave, verifiche e prossimo passo logico.
- Aggiornato `diary/OPERATIVE_PLANS.md` con le nuove regole permanenti FE.

Verifiche:

- Verificato che non esistono commit FE precedenti con prefisso `FE`; il prossimo commit usa `FE1`.
- Nessun test codice necessario per modifiche solo documentali/regole, oltre alle verifiche gia' completate nel Piano 3 per i file applicativi ancora nel commit corrente.

Esito: completato.

## Piano 5 - Navigazione FE Con Endpoint Kenshiro

User story: `S03-P05-US-025`.

Decisioni:

- Confermato collegamento FE a Kenshiro per Dashboard, Pipeline e Config tramite target API `kenshiro`.
- Aggiunto `PUBLIC_KENSHIRO_API_BASE` a `.env.example`.
- Docker locale lascia `PUBLIC_HFT_API_BASE` e `PUBLIC_DOCBROWN_API_BASE` vuote di default, cosi le action granulari non ancora migrate a Kenshiro usano i fallback same-origin e restano navigabili quando HFT/Docbrown non sono esposti su host.
- Il test browser non avvia piu' REAL RUN: verifica l'action `Avvia RUN PAPER`, rispettando la regola di sicurezza sulle REAL RUN.
- Avviato Kenshiro live su `localhost:8085/kenshiro` con datasource MySQL locali per testare gli endpoint backoffice reali.

Verifiche:

- `kenshiro ./mvnw test`: 4 test OK.
- `GET /kenshiro/backoffice/dashboard?executionMode=PAPER`: HTTP 200.
- `GET /kenshiro/backoffice/pipeline/executions?limit=5&executionMode=PAPER`: HTTP 200.
- `POST /kenshiro/backoffice/pipeline/executions` con `{ "executionMode": "PAPER" }`: HTTP 200, execution `RUNNING` restituita.
- `GET /kenshiro/backoffice/config/hft/db`: HTTP 200.
- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build --no-cache` e `docker compose up -d --force-recreate`: completati.
- `npm run test:browser`: login, navigazione Dashboard, Pipeline, Docbrown, HFT, configurazioni DB/globali, cambio REAL solo in lettura, action `Avvia RUN PAPER`, action Docbrown/HFT fallback e salvataggi config; nessun errore console/network.
- Log FE Docker: nessun 404/500/CORS residuo.

Esito: completato.

## Piano 6 - Current Context Workspace Unico

User story: `S03-P06-US-026`.

Decisioni:

- Il context operativo breve FE non vive piu' in `hft-fe/diary/CURRENT_CONTEXT.md`.
- Il context unico del workspace e' `/home/mbc/Documenti/ws/java/hft/CURRENT_CONTEXT.md`.
- La sezione `HFT FE` del context root contiene regole FE, ultimo commit, runtime, endpoint Kenshiro e file chiave.
- Lo storico FE resta in `hft-fe/diary/OPERATIVE_PLANS.md` e nelle sessioni FE.

Risultato:

- Rimosso `hft-fe/diary/CURRENT_CONTEXT.md`.
- Aggiornato `hft-fe/diary/OPERATIVE_PLANS.md` per puntare al context root.

Verifiche:

```text
find /home/mbc/Documenti/ws/java/hft -maxdepth 3 -name CURRENT_CONTEXT.md
```

Esito atteso: un solo file root `CURRENT_CONTEXT.md`.

## Piano 7 - Pipeline Runtime Check DB-Only

User story: `S03-P07-US-027`.

Decisioni:

- Aggiunto endpoint Kenshiro `GET /backoffice/pipeline/runtime-check?executionMode=...` per esporre uno stato runtime periodico leggendo direttamente lo schema HFT.
- Kenshiro include nelle execution anche gli `execution_uuid` presenti solo in `paper_trading_event`, cosi una REAL RUN viva senza BUY/SELL compare comunque in Pipeline.
- Il dettaglio Pipeline non cade piu' su `UNKNOWN` quando non esiste ancora una `trade_position`: usa gli eventi runtime HFT per ricostruire modo, stato, timestamp, promossi/bocciati e BUY.
- Il merge tra eventi e run/posizioni evita doppi conteggi BUY, usando il massimo tra viste dello stesso ciclo.
- La pagina `/pipeline` effettua polling ogni 30 secondi su execution e runtime check, mantenendo la selection corrente quando resta disponibile.
- La lista `Executions` mostra timestamp come dato primario e `execution_uuid` piu' piccolo sotto.
- I box metriche `Promossi`, `Bocciati`, `Acquisti` ora hanno colori testo espliciti ad alto contrasto.
- Il box runtime mostra controlli DB-only, versione Flyway, ultimo ciclo Stan e reason BUY/SELL piu' recenti.

Verifiche:

- `kenshiro ./mvnw test`: 7 test OK.
- `hft-fe npm run check`: 0 errori, 0 warning.

Esito: completato.

## Piano 8 - Porta Kenshiro 8085 E Policy MS

User story: `S03-P08-US-028`.

Decisioni:

- Porta Kenshiro spostata da `8084` a `8085`.
- FE Docker, `.env.example` e contratto backend aggiornati a `http://localhost:8085/kenshiro`.
- La policy commit FE con prefisso `FE` e' sostituita dalla policy globale `MS<n>`.

Verifiche:

- `npm run check`: 0 errori, 0 warning.
- `npm run build`: completato.
- `docker compose build hft-fe && docker compose up -d --force-recreate hft-fe`: completato.
- Container `hft-fe-local` espone `PUBLIC_KENSHIRO_API_BASE=http://localhost:8085/kenshiro`.

Esito: completato.

## Piano 9 - Dashboard Scoped E Contrasto Accessibile

User story: `S03-P09-US-029`.

Decisioni:

- La dashboard mostra solo le metriche dello scenario selezionato: `PAPER BUY/SELL` se il selettore e' PAPER, `REAL BUY/SELL` se e' REAL.
- Il trend usa solo la serie del mode selezionato; i punti SVG non leggono piu' sempre `paperNet`.
- `loadDashboard` normalizza comunque i totali ricevuti dal backend, azzerando la famiglia non selezionata come difesa lato FE.
- Il saldo Binance viene arricchito usando l'endpoint HFT esistente `GET /hft/crypto/account-info/by-assets`, con asset `USDC,USDT,BTC,ETH,BNB`.
- Default FE per HFT API: `http://localhost:8081/hft`, anche in Docker e `.env.example`.
- Migliorata accessibilita' cromatica delle card richieste sostituendo superfici bianche/chiare con gradienti `slate/cyan` e testo esplicito chiaro.
- La card stato account usa la stessa famiglia cromatica scura ad alto contrasto invece di `bg-blue-50`.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

GET http://localhost:5173/dashboard -> HTTP 200
GET http://localhost:8081/hft/crypto/account-info/by-assets?assets=USDC,USDT -> JSON + CORS OK
```

Esito: completato.

## Piano 10 - Contrasto Globale Tooltip Trend E Sorting Balance

User story: `S03-P10-US-030`.

Decisioni:

- Estesi gli override dark globali in `src/app.css` per superfici chiare `bg-white`, `bg-slate-50`, `bg-gray-50`, `bg-blue-50`, `bg-cyan-50` e gradienti `from-white/via-cyan-50/to-slate-100`.
- Sostituito il bottone dashboard `bg-cyan-300 text-slate-950` con `bg-cyan-700 text-white`, evitando una combinazione cromatica poco coerente con il resto della pagina.
- Aggiunto tooltip hover sui punti del grafico `Trend <mode> net` per mostrare il netto del punto selezionato.
- Ordinati i balance di `Account Binance corrente` per saldo totale decrescente (`free + locked`).
- Mantenuta compatibilita' a11y del grafico con ruolo SVG/circle, `aria-label` e fallback `<title>`.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

docker compose build && docker compose up -d --force-recreate
hft-fe-local ricreato e avviato

GET http://localhost:5173/dashboard -> HTTP 200
```

Esito: completato.

## Piano 11 - Pipeline Operativa Leggibile

User story: `S03-P11-US-031`.

Decisioni:

- Ridisegnato il box runtime DB-only di `/pipeline` come pannello operativo scuro: metriche candidate/accettate/buffer/posizioni, checklist leggibile e ultimo ciclo Stan in card distinte.
- Corretto l'hover della lista `Executions`: niente piu' sfondo bianco su testo chiaro; selected e hover restano su palette `slate/cyan` ad alto contrasto.
- Il dettaglio execution usa KPI scuri per `Docbrown`, `Promossi`, `Bocciati`, `Acquisti` con numeri grandi, label esplicite e colori semantici.
- `Valori Docbrown`, `Promossi`, `Bocciati` e `Acquisti` non sono piu' righe testo: ogni item e' una card strutturata con simbolo, metrica, valore, outcome/reason e dati BUY.
- Aggiunta formattazione uniforme dei valori numerici per evitare output grezzo poco leggibile.
- Verifica contrasto manuale sui colori principali: white/slate-950 20.17:1, slate-300/slate-950 13.59:1, cyan-100/cyan-950 11.97:1, emerald-100/emerald-950 13.36:1, rose-100/rose-950 13.02:1, amber-100/amber-950 13.45:1.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

docker compose build && docker compose up -d --force-recreate
hft-fe-local ricreato e avviato

GET http://localhost:5173/pipeline -> HTTP 200
```

Esito: completato.

## Piano 12 - Liste Dettaglio Pipeline Espandibili

User story: `S03-P12-US-032`.

Decisioni:

- Le quattro liste del dettaglio pipeline (`Valori Docbrown`, `Promossi`, `Bocciati`, `Acquisti`) mostrano al massimo 5 elementi in vista compatta.
- Ogni lista ha un toggle indipendente `Mostra tutti (...)` / `Mostra solo 5` per espandere il box quando serve leggere l'intero contenuto.
- I toggle sono disabilitati quando la lista contiene 5 elementi o meno, mostrando comunque il conteggio corrente.
- L'espansione viene resettata quando si seleziona una nuova execution, si avvia una run o cambia scenario PAPER/REAL.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

docker compose build && docker compose up -d --force-recreate
hft-fe-local ricreato e avviato

GET http://localhost:5173/pipeline -> HTTP 200
```

Esito: completato.

## Piano 13 - Docbrown View Operativa E Sorgente Dati

User story: `S03-P13-US-033`.

Decisioni:

- Verificato che `/docbrown` nel container FE non usa un BE Docbrown reale: `PUBLIC_DOCBROWN_API_BASE` e `VITE_DOCBROWN_API_BASE` sono vuote, quindi `apiFetch('docbrown', ...)` chiama i route handler locali SvelteKit `/backtest/*`.
- La pagina mostra ora un badge esplicito `LOCAL MOCK / FALLBACK` con base `route locali /backtest/*`; se verra' valorizzata la base Docbrown mostrera' `BACKEND DOCBROWN`.
- L'esito iniziale non renderizza piu' JSON `null`: mostra stato `IN ATTESA` e una callout operativa.
- Gli esiti Walk-forward, HFT signals e Supervised sono presentati con KPI, riepilogo budget/cash/win rate/analyzed e card strategie; il JSON completo resta disponibile solo in `<details>` tecnico.
- L'esito Reverse oracle e' presentato con KPI dedicati e card backtest oracle.
- `Supervised config` e `Legacy config` sono trasformate da JSON grezzo a card chiave-valore leggibili.
- La pagina adotta la palette scura ad alto contrasto gia' usata su Dashboard/Pipeline.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

docker compose build && docker compose up -d --force-recreate
hft-fe-local ricreato e avviato

GET http://localhost:5173/docbrown -> HTTP 200
POST http://localhost:5173/backtest/walk-forward -> runUuid local-docbrown-run-001, trainingMetadata.modelType LOCAL_FALLBACK
```

Esito: completato.

## Piano 14 - Docbrown Page Su Kenshiro DB-Only

User story: `S03-P14-US-034`.

Decisioni:

- `docbrownApi` non usa piu' target `docbrown`: tutte le chiamate pagina `/docbrown` passano da Kenshiro.
- Aggiornati endpoint FE verso `/backoffice/docbrown/*` per supervised/legacy config, policy deployable, walk-forward, reverse-oracle, HFT signals e supervised signals.
- Il badge pagina ora mostra `KENSHIRO DB-ONLY` quando `PUBLIC_KENSHIRO_API_BASE` e' configurata.
- Aggiornato `backend-endpoints.md`: gli endpoint Docbrown page sono ora Kenshiro; le action POST restituiscono summary DB-only dell'ultimo `backtest_run`, non avviano Docbrown.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

docker compose build && docker compose up -d --force-recreate
hft-fe-local ricreato e avviato

GET http://localhost:5173/docbrown -> HTTP 200, badge KENSHIRO DB-ONLY
```

Esito: completato.

## Piano 15 - Ultimo Ciclo Stan Espandibile

User story: `S03-P15-US-035`.

Decisioni:

- Il box `Ultimo ciclo Stan` in `/pipeline` mostra al massimo 5 decisioni runtime in vista compatta.
- Aggiunto toggle indipendente `Mostra tutti (...)` / `Mostra solo 5`, allineato al comportamento delle liste del dettaglio execution.
- Il toggle e' disabilitato quando le decisioni sono 5 o meno e mostra il conteggio corrente.
- L'espansione viene resettata al cambio scenario PAPER/REAL.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

docker compose build && docker compose up -d --force-recreate
hft-fe-local ricreato e avviato

GET http://localhost:5173/pipeline -> HTTP 200
```

Esito: completato.

## Piano 16 - Pipeline REAL Batch E Config DB

User story: `S03-P16-US-036`.

Decisioni:

- Estesi i tipi `PipelineRuntimeCheck` con `realRuntimeConfig`, `docbrownBatchRuns` e `generatedParameters`.
- `/pipeline` mostra ora un blocco `Config REAL DB` con BUY REAL, refresh, universe, BALANCED, expected return, profit factor e cooldown.
- `/pipeline` mostra `Ultimo batch Docbrown` con timestamp di esecuzione, status, selected, trades, win rate, net, profit factor e range dati.
- Aggiunto `Storico batch Docbrown` con le ultime 5 run persistite da Kenshiro.
- Aggiunto `Valori generati` con i parametri attivi per scenario, inclusi win rate, net, score, target, stop e max hold.
- Il fallback FE inizializza i nuovi array a vuoto, evitando errori se Kenshiro non e' ancora aggiornato.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
build completata

docker compose up -d --build
hft-fe-local ricreato e avviato

GET http://localhost:5173/pipeline -> HTTP 200
```

Esito: completato.
## Piano 5 - Pipeline Scout Optimizer Visibility

User story: `FE-S03-P05-US-005`.

Decisioni:

- La pagina `/pipeline` distingue i batch Docbrown standard dai tuning `SCALPING_SCOUT`.
- Il box "Ultimo batch Docbrown" mostra:
  - stato optimizer;
  - config applicata/non applicata;
  - `momentum5_min`;
  - `volume_ratio_min`;
  - `min_replay_win_rate`;
  - `profit_target`;
  - `stop_loss`;
  - `min_replay_net_return`.
- Lo storico batch mostra una riga compatta dei parametri tuning principali.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK
```

## Piano 17 - Dashboard/Pipeline MS414 Alignment

User story: `FE-S03-P17-US-017`.

Decisioni:

- `/dashboard` ora mostra una linea zero nel grafico trend net, per distinguere visivamente punti positivi e negativi.
- Ridotta la dimensione dei punti del grafico trend per rendere la serie meno rumorosa.
- `/pipeline` mostra `stop_loss_grace_seconds` e `min_quote_volume` nei parametri generati.
- Il box `Ultimo batch Docbrown` mostra anche la grace stop e il moltiplicatore di quote volume quando presenti nei best params.
- Lo spazio sotto `Ultimo ciclo Stan` viene riutilizzato con un riepilogo compatto dei parametri attivi.
- `Executions` mostra di default le ultime 5 execution; `Mostra tutte` espande con scroll verticale.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK
```

Esito: completato.

## Piano 18 - Dashboard Zero Axis And Pipeline Executions Fallback

User story: `FE-S03-P18-US-018`.

Decisioni:

- `/dashboard` usa una scala simmetrica attorno allo zero per il grafico trend net: la linea `0` resta al centro verticale e i punti negativi sono sempre sotto l'asse.
- `/pipeline` non lascia piu' vuota la lista `Executions` se la chiamata primaria o il fallback HFT non producono elementi.
- La runtime check Kenshiro viene fusa nella lista execution come run corrente deduplicata.
- Se il dettaglio execution non e' disponibile, il FE costruisce un dettaglio parziale dalla runtime check corrente.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK
```

Esito: completato.

## Piano 19 - Kenshiro Same-Origin Proxy For Pipeline

User story: `FE-S03-P19-US-019`.

Decisioni:

- `/dashboard` e `/pipeline` non chiamano piu' Kenshiro direttamente dal browser: usano il target `kenshiro-local` same-origin.
- I route Svelte `/backoffice/dashboard`, `/backoffice/pipeline/executions`, `/backoffice/pipeline/executions/[uuid]` e `/backoffice/pipeline/runtime-check` sono proxy reali verso Kenshiro.
- In Docker il proxy usa `KENSHIRO_INTERNAL_API_BASE=http://host.docker.internal:8085/kenshiro` per evitare l'errore `localhost` dentro container.
- Il proxy mantiene fallback automatico da `localhost` a `host.docker.internal` se la fetch server-side fallisce.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK

GET http://localhost:5173/backoffice/pipeline/executions?limit=50&executionMode=REAL&mode=REAL -> HTTP 200, 50 item
GET http://localhost:5173/backoffice/pipeline/runtime-check?executionMode=REAL&mode=REAL -> HTTP 200, OPERATIVE
GET http://localhost:5173/backoffice/dashboard?executionMode=REAL&mode=REAL -> HTTP 200, BACKOFFICE_AGGREGATE
```

Esito: completato.

## Piano 20 - Reactive Executions Rendering

User story: `FE-S03-P20-US-020`.

Decisioni:

- Confrontata la versione precedente: il rendering di `Executions` era stato spostato su una funzione `visibleExecutions()` chiamata direttamente nel template.
- Sostituita la funzione con variabile reattiva `$: visibleExecutions = ...`, dipendente esplicitamente da `executions` ed `expandedExecutions`.
- Aggiunta key stabile nell'`each`: `(execution.executionUuid)`.
- Il label del toggle e' calcolato in `$: executionsLabel`, così resta legato al numero reale di elementi ricevuti.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK
```

Esito: completato.

## Piano 21 - Executions List Limit 13

User story: `FE-S03-P21-US-021`.

Decisioni:

- `Executions` usa un limite dedicato `EXECUTIONS_LIST_LIMIT = 13`, separato dalle liste di dettaglio che restano a 5.
- Il toggle compare solo se le execution sono piu' di 13.
- Il label mostra il totale reale ricevuto (`Mostra tutte (N)`) oppure `N executions` quando non serve espandere.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK
```

Esito: completato.

## Piano 22 - Inline Executions Toggle Label

User story: `FE-S03-P22-US-022`.

Decisioni:

- Rimosso `executionsLabel` come variabile reattiva separata: restava a `0 executions` mentre la lista era gia' aggiornata.
- Il bottone `Executions` calcola il testo inline dal valore corrente di `executions.length`, come gia' fanno le action `Mostra tutti` delle liste dettaglio.
- La logica visuale resta: 13 item visibili, `Mostra tutte (N)` quando N > 13, `Mostra solo 13` quando espanso.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK
```

Esito: completato.

## Piano 23 - Pipeline Chain Timeline

User story: `FE-S03-P23-US-023`.

Decisioni:
- `/pipeline` mostra una nuova sezione `Chain pipeline` dentro il box runtime.
- La chain e' una retta orizzontale con step cliccabili letti da Kenshiro `runtimeCheck.pipelineSteps`.
- Ogni step selezionato visualizza:
  - soglie / guard.
  - esito check.
  - dati acquisto.
  - dati vendita.
  - config completa dello step.
- Aggiunta visualizzazione di `catastrophicStopLoss` nei parametri generati e nei tuning batch.
- Il FE normalizza `pipelineSteps` a lista vuota se il backend non e' ancora aggiornato, evitando regressioni durante deploy rolling.

Verifiche:

```text
npm run check
svelte-check found 0 errors and 0 warnings

npm run build
OK
```

Esito: completato.

Deploy locale MS437:
- Immagine `hft-fe:local` ricostruita e `hft-fe-local` ricreato.
- Verifica proxy `GET /backoffice/pipeline/runtime-check?executionMode=REAL&mode=REAL`: HTTP 200, `pipelineSteps=5`.

## Piano 24 - Stan Cycle Horizontal Box

User story: `FE-S03-P24-US-024`.

Decisioni:
- `/pipeline`: `Ultimo ciclo Stan` non occupa piu' una colonna dedicata del runtime check.
- La sezione e' stata spostata sotto `Checklist operativa` come box orizzontale compatto.
- Le ultime 5 decisioni sono rese come card in griglia orizzontale; `Mostra tutti` espande lo stesso box quando ci sono piu' elementi.
- Rimossa la colonna laterale duplicata con il riepilogo dei parametri attivi, gia' presente in `Valori generati`.

Verifiche:
- `npm run check` OK.
- `npm run build` OK.

Esito: completato.

## Piano 25 - Trade Chain BUY SELL View

User story: `FE-S03-P25-US-025`.

Decisioni:
- `/pipeline` mostra ora `Chain trade BUY / SELL` sotto la chain pipeline generale.
- Ogni card rappresenta una posizione/trade per simbolo e mostra stato, netto e timestamp BUY.
- Selezionando un trade, la UI mostra una linea con i nodi:
  - check Stan precedenti al BUY.
  - BUY effettivo.
  - SELL effettivo se presente.
- Ogni nodo e' espandibile e mostra outcome, dati BUY, dati SELL e config/params disponibili.

Verifiche:
- `npm run check` OK.
- `npm run build` OK.

Esito: completato.

## Piano 26 - Pipeline Payload Readability

User story: `FE-S03-P26-US-026`.

Decisioni:
- `/pipeline` non mostra piu' payload JSON grezzi nei dettagli degli step e delle chain trade.
- I payload vengono appiattiti e raggruppati in sezioni logiche: Generale, Mercato e dati, Runtime, Soglie guard, Rischio e uscita, Replay e qualita, Altri dati.
- Ogni gruppo mostra label leggibili, valori formattati e un centroide quando sono presenti piu' metriche numeriche correlate.
- Gli stati tecnici con underscore vengono convertiti in label compatte e wrappabili, incluse decisioni Stan, batch Docbrown, step pipeline, chain trade e dettaglio execution.
- Aumentato il padding verticale delle chain orizzontali per evitare che l'hover tagli la parte superiore delle card.

Verifiche:
- `npm run check` OK.
- `npm run build` OK.

Esito: completato.

## Piano 27 - Compact Pipeline Payload Dialog

User story: `FE-S03-P27-US-027`.

Decisioni:
- `/pipeline` mantiene i payload raggruppati, ma la vista inline e' stata compattata in card con pochi valori chiave e conteggio campi.
- Il dettaglio completo dei payload si apre in un dialog unico; aprire un nuovo payload sostituisce quello precedente.
- I nodi della chain trade non usano piu' espansioni multiple inline: il click apre direttamente il dialog del nodo selezionato.
- Rimossi dettagli inline pesanti per ridurre altezza e rumore visivo nelle chain.

Verifiche:
- `npm run check` OK.
- `npm run build` OK.

Esito: completato.

## Piano 28 - Pipeline Subpages Split

User story: `FE-S03-P28-US-028`.

Decisioni:
- `/pipeline` diventa dashboard operativa: runtime, checklist, ultimo ciclo Stan, chain pipeline e configurazioni generiche.
- Aggiunte sottopagine:
  - `/pipeline/trades` per `Chain trade BUY / SELL`.
  - `/pipeline/executions` per storico executions e dettaglio run.
- Il contenuto comune e' stato estratto in `src/lib/components/pipeline/PipelinePage.svelte`, parametrizzato con `view=overview|trades|executions`.
- Il menu laterale include ora le nuove voci e l'highlight di `/pipeline` resta esatto, senza attivarsi sulle sottopagine.

Verifiche:
- `npm run check` OK.
- `npm run build` OK.

Esito: completato.

## Piano 29 - Pipeline Menu Subitems

User story: `FE-S03-P29-US-029`.

Decisioni:
- Nel menu laterale, `Executions` e `Trades` sono ora subitem di `Pipeline`.
- Ordine invertito rispetto alla vista precedente: prima `Executions`, poi `Trades`.
- Aggiunto tipo esplicito `NavItem` per supportare `badge` e `children` opzionali senza errori Svelte/TS.

Verifiche:
- `npm run check` OK.
- `npm run build` OK.

Esito: completato.

## Piano 30 - Pipeline Detail Pages Cleanup

User story: `FE-S03-P30-US-030`.

Decisioni:
- Le informazioni generiche della dashboard `/pipeline` non vengono piu' duplicate nelle sottopagine.
- `/pipeline/trades` mostra solo il dettaglio della chain trade, senza header runtime, metriche, checklist, ultimo ciclo Stan o execution UUID.
- `/pipeline/executions` mostra solo storico/dettaglio executions, senza blocco runtime generico.
- `Avvia RUN` e fonte dati restano solo sulla dashboard pipeline.

Verifiche:
- `npm run check` OK.
- `npm run build` OK.

Esito: completato.
