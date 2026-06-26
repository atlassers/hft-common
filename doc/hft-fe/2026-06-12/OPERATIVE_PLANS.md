# HFT-FE Operative Plans

Indice delle sessioni operative del backoffice frontend.

## Regole Operative

- Non eseguire ordini REAL dal frontend senza conferma esplicita dell'utente e supporto backend sicuro.
- Default osservabilita: distinguere sempre PAPER e REAL.
- Fail closed: senza token non invocare API operative.
- Usare fetch nativo e DTO TypeScript espliciti.
- Non introdurre componenti riutilizzabili se non usati almeno due volte.
- Aprire una nuova sessione se passa piu di un'ora dall'ultimo commit o se si superano 10 piani nella sessione corrente.
- A fine attivita' su `hft-fe`, committare e pushare sempre salvo richiesta esplicita contraria nel turno corrente.
- I commit `hft-fe` seguono la policy globale `MS<n>: <message>`.
- Aggiornare `/home/mbc/Documenti/ws/java/hft/CURRENT_CONTEXT.md` a ogni sviluppo; leggerlo come punto di partenza di ogni nuova richiesta sul modulo FE.

## Sessioni Operative

- [Sessione 7 - Best Winner Controls](session-07-best-winner-controls.md)
  - [Piano 1 - Pagina Configurazione Best Winner](session-07-best-winner-controls.md#piano-1---pagina-configurazione-best-winner)
  - [Piano 2 - Browser Test Best Winner](session-07-best-winner-controls.md#piano-2---browser-test-best-winner)
- [Sessione 5 - Pipeline Flow DB Contract](session-05-pipeline-flow-db-contract.md)
  - [Piano 1 - Vista Flow Operativa DB-Only](session-05-pipeline-flow-db-contract.md#piano-1-vista-flow-operativa-db-only)
  - [Piano 2 - Chain Orizzontale E Dialog Config](session-05-pipeline-flow-db-contract.md#piano-2-chain-orizzontale-e-dialog-config)
  - [Piano 3 - Card Righe DB Identificative](session-05-pipeline-flow-db-contract.md#piano-3-card-righe-db-identificative)
  - [Piano 4 - Step Half Width E Rail DB Hover](session-05-pipeline-flow-db-contract.md#piano-4-step-half-width-e-rail-db-hover)
- [Sessione 4 - Training Guard Map](session-04-training-guard-map.md)
  - [Piano 1 - Mappa Offline Training Verso Realtime Guard](session-04-training-guard-map.md#piano-1-mappa-offline-training-verso-realtime-guard)
  - [Piano 2 - Execution-Scoped Pipeline View](session-04-training-guard-map.md#piano-2-execution-scoped-pipeline-view)
- [Sessione 1 - Backoffice FE Foundation](session-01-backoffice-fe-foundation.md)
  - [Piano 1 - Documentazione Funzionale](session-01-backoffice-fe-foundation.md#piano-1-documentazione-funzionale)
  - [Piano 2 - Scaffold SvelteKit Minimo](session-01-backoffice-fe-foundation.md#piano-2-scaffold-sveltekit-minimo)
  - [Piano 3 - Auth Username Password](session-01-backoffice-fe-foundation.md#piano-3-auth-username-password)
  - [Piano 4 - Client REST Tipizzati](session-01-backoffice-fe-foundation.md#piano-4-client-rest-tipizzati)
  - [Piano 5 - Dashboard Operativa](session-01-backoffice-fe-foundation.md#piano-5-dashboard-operativa)
  - [Piano 6 - Pagina Docbrown](session-01-backoffice-fe-foundation.md#piano-6-pagina-docbrown)
  - [Piano 7 - Pagina HFT](session-01-backoffice-fe-foundation.md#piano-7-pagina-hft)
  - [Piano 8 - Configurazioni DB E Globali](session-01-backoffice-fe-foundation.md#piano-8-configurazioni-db-e-globali)
  - [Piano 9 - Pipeline End-To-End](session-01-backoffice-fe-foundation.md#piano-9-pipeline-end-to-end)
  - [Piano 10 - Verifica Tecnica](session-01-backoffice-fe-foundation.md#piano-10-verifica-tecnica)
- [Sessione 2 - Docker Runtime Locale](session-02-docker-runtime-locale.md)
  - [Piano 1 - Docker Compose Locale Funzionante](session-02-docker-runtime-locale.md#piano-1-docker-compose-locale-funzionante)
  - [Piano 2 - Auth Locale Docker](session-02-docker-runtime-locale.md#piano-2-auth-locale-docker)
  - [Piano 3 - Chrome Cached Auth Compatibility](session-02-docker-runtime-locale.md#piano-3-chrome-cached-auth-compatibility)
  - [Piano 4 - Auth CORS Locale](session-02-docker-runtime-locale.md#piano-4-auth-cors-locale)
  - [Piano 5 - Dashboard CORS E Fallback](session-02-docker-runtime-locale.md#piano-5-dashboard-cors-e-fallback)
  - [Piano 6 - Pipeline Fallback Locale](session-02-docker-runtime-locale.md#piano-6-pipeline-fallback-locale)
  - [Piano 7 - Navigazione Browser Completa E Fallback Endpoint](session-02-docker-runtime-locale.md#piano-7-navigazione-browser-completa-e-fallback-endpoint)
  - [Piano 8 - Layout Sidebar Flowbite Docs](session-02-docker-runtime-locale.md#piano-8-layout-sidebar-flowbite-docs)
  - [Piano 9 - Dashboard Account Strutturato](session-02-docker-runtime-locale.md#piano-9-dashboard-account-strutturato)
  - [Piano 10 - Selettore PAPER REAL E Tema Dark Flowbite](session-02-docker-runtime-locale.md#piano-10-selettore-paper-real-e-tema-dark-flowbite)
- [Sessione 3 - Scenario Runtime Controls](session-03-scenario-runtime-controls.md)
  - [Piano 1 - Scenario PAPER REAL End-to-End E Avvio Run](session-03-scenario-runtime-controls.md#piano-1-scenario-paper-real-end-to-end-e-avvio-run)
  - [Piano 2 - Verifica Sezioni E Aggancio Dati HFT](session-03-scenario-runtime-controls.md#piano-2-verifica-sezioni-e-aggancio-dati-hft)
  - [Piano 3 - Link Endpoint Aggregati HFT Docbrown](session-03-scenario-runtime-controls.md#piano-3-link-endpoint-aggregati-hft-docbrown)
  - [Piano 4 - Regole Commit FE E Current Context](session-03-scenario-runtime-controls.md#piano-4-regole-commit-fe-e-current-context)
  - [Piano 5 - Navigazione FE Con Endpoint Kenshiro](session-03-scenario-runtime-controls.md#piano-5-navigazione-fe-con-endpoint-kenshiro)
  - [Piano 6 - Current Context Workspace Unico](session-03-scenario-runtime-controls.md#piano-6-current-context-workspace-unico)
  - [Piano 7 - Pipeline Runtime Check DB-Only](session-03-scenario-runtime-controls.md#piano-7-pipeline-runtime-check-db-only)
  - [Piano 8 - Porta Kenshiro 8085 E Policy MS](session-03-scenario-runtime-controls.md#piano-8-porta-kenshiro-8085-e-policy-ms)
  - [Piano 9 - Dashboard Scoped E Contrasto Accessibile](session-03-scenario-runtime-controls.md#piano-9-dashboard-scoped-e-contrasto-accessibile)
  - [Piano 10 - Contrasto Globale Tooltip Trend E Sorting Balance](session-03-scenario-runtime-controls.md#piano-10-contrasto-globale-tooltip-trend-e-sorting-balance)
  - [Piano 11 - Pipeline Operativa Leggibile](session-03-scenario-runtime-controls.md#piano-11-pipeline-operativa-leggibile)
  - [Piano 12 - Liste Dettaglio Pipeline Espandibili](session-03-scenario-runtime-controls.md#piano-12-liste-dettaglio-pipeline-espandibili)
  - [Piano 13 - Docbrown View Operativa E Sorgente Dati](session-03-scenario-runtime-controls.md#piano-13-docbrown-view-operativa-e-sorgente-dati)
  - [Piano 14 - Docbrown Page Su Kenshiro DB-Only](session-03-scenario-runtime-controls.md#piano-14-docbrown-page-su-kenshiro-db-only)
  - [Piano 15 - Ultimo Ciclo Stan Espandibile](session-03-scenario-runtime-controls.md#piano-15-ultimo-ciclo-stan-espandibile)
  - [Piano 16 - Pipeline REAL Batch E Config DB](session-03-scenario-runtime-controls.md#piano-16-pipeline-real-batch-e-config-db)
