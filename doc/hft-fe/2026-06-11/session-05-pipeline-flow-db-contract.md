# Sessione 5 - Pipeline Flow DB Contract

## Piano 1 - Vista Flow Operativa DB-Only

Data: 2026-06-11 14:25 CEST.

Obiettivo: trasformare il business requirement della pipeline in una pagina FE unica, leggibile e agganciata a Kenshiro, senza fallback mocked.

Azioni completate:

- Integrato `BUSINESS_REQUIRIMENTS.md` con matrice endpoint: keep, create, deprecated.
- Aggiunto tipo `PipelineFlow` con summary e step `input/output/records/missingData`.
- Aggiunta API FE `pipelineApi.flow()` senza fallback locale.
- Aggiunto proxy SvelteKit `GET /backoffice/pipeline/flow`.
- Creata pagina `/pipeline/flow` con timeline step-by-step: input, cosa fa, output, righe DB e dati mancanti.
- Aggiunto link `Flow` nella navigazione Pipeline.

Verifiche:

- `npm run check` OK.
- `npm run build` OK.
- `docker compose up -d --build` OK, container `hft-fe-local` up su `5173`.
- Proxy FE validato: `GET http://localhost:5173/backoffice/pipeline/flow?executionMode=PAPER&limit=3` restituisce `BACKOFFICE_AGGREGATE`, 10 step e execution PAPER reale.

Note:

- La nuova pagina non usa fallback `LOCAL_FALLBACK`: se Kenshiro non risponde, mostra errore.
- Le route legacy restano disponibili ma sono documentate come deprecated per esperienza primaria.

## Piano 2 - Chain Orizzontale E Dialog Config

Data: 2026-06-11 14:50 CEST.

Obiettivo: riallineare la pagina al comportamento richiesto dall'utente: macro-step grafici orizzontali in alto, dettaglio singolo sotto, righe DB chiuse di default e configurazioni in dialog leggibile.

Azioni completate:

- Aggiornato `BUSINESS_REQUIRIMENTS.md` con requisiti UI espliciti per chain orizzontale, dettaglio selezionato, DB rows espandibili e dialog configurazioni.
- Sostituita la timeline verticale con una chain orizzontale scrollabile di macro step cliccabili.
- Il contenitore sotto la chain mostra solo lo step selezionato.
- Input/output mostrano campi principali compatti e aprono il payload completo in dialog.
- Le righe DB sono in una sezione `details` chiusa di default.
- Il dettaglio completo di ogni riga DB si apre in dialog.
- La dialog si chiude col bottone `Chiudi` o cliccando il backdrop.
- I payload complessi e le stringhe JSON parsabili, come `params_json`, sono mostrati come key/value indentati invece che come JSON grezzo inline.

Verifiche:

- `npm run check` OK, 0 errori e 0 warning.
- `npm run build` OK.
- `docker compose up -d --build` OK, container `hft-fe-local` up su `5173`.
- `GET http://localhost:5173/pipeline/flow` HTTP 200.
- Proxy `GET http://localhost:5173/backoffice/pipeline/flow?executionMode=PAPER&limit=3` OK con 10 step reali.

## Piano 3 - Card Righe DB Identificative

Data: 2026-06-11 14:56 CEST.

Obiettivo: rendere le righe DB piu' leggibili, evidenziando gli estremi identificativi principali invece di mostrare chip equivalenti.

Azioni completate:

- Aggiornato `BUSINESS_REQUIRIMENTS.md` con il requisito per card DB gerarchiche.
- Ogni riga DB ora mostra una card con:
  - titolo primario: `symbol` se presente, altrimenti identificativo migliore;
  - sottotitolo: `sourceTable`, side, event type o pipeline stage;
  - reason/status in evidenza quando presente;
  - metrica primaria separata: `score`, `price`, `buy_price`, `sell_avg_price`, `net_profit_quote`, `net_return` o fallback su id;
  - dettagli secondari raggruppati sotto.
- Il dettaglio completo resta apribile in dialog.

Verifiche:

- `npm run check` OK, 0 errori e 0 warning.
- `npm run build` OK.
- `docker compose up -d --build` OK, container `hft-fe-local` up su `5173`.
- `GET http://localhost:5173/pipeline/flow` HTTP 200.
- Proxy flow PAPER OK con 10 step reali.

### Fix 2026-06-11 15:18 CEST

Il requisito "meta' spazio" e' stato chiarito: due step visibili nell'area, senza raddoppiare larghezza o altezza dei button.

Correzioni:

- Sostituito `min-w-[calc(50%-0.375rem)]` con `basis-[calc(50%-0.375rem)] shrink-0`, cosi' il layout usa flex-basis reale senza forzare dimensioni eccessive.
- Rimossa la `scale-[1.02]` dallo stato active e sostituita con ring/shadow.
- Hover piu' leggero con `translate-y`, senza ingrandire la card.
- Ridotti padding, badge numerico e rimossa la riga timestamp dal nodo per contenere l'altezza.

Verifiche:

- `npm run check` OK, 0 errori e 0 warning.
- `npm run build` OK.
- `docker compose up -d --build` OK.
- `/pipeline/flow` HTTP 200.

### Fix 2026-06-11 15:30 CEST

La correzione precedente usava ancora `flex-basis: 50%`, quindi su desktop continuava a produrre card troppo larghe.

Correzioni:

- Rimosso `basis-[calc(50%-0.375rem)]`.
- Impostate larghezze esplicite compatte: `w-44 sm:w-48 lg:w-52`.
- Aggiornato il BR: i macro step devono avere larghezza controllata, non percentuale sul viewport.

Verifiche:

- `npm run check` OK, 0 errori e 0 warning.
- `npm run build` OK.
- `docker compose up -d --build` OK.
- `/pipeline/flow` HTTP 200 dopo startup container.

### Fix 2026-06-11 15:40 CEST

L'utente ha chiesto di dimezzare ulteriormente la dimensione attuale e ha segnalato che il click sui nodi lasciava selezionato `Mercato e dati live`.

Correzioni:

- Macro step ridotti da `w-44 sm:w-48 lg:w-52` a `w-24 sm:w-28 lg:w-28`.
- Badge numero/status, padding e testo ridotti.
- Stato selezione convertito da `selectedStepId` a `selectedStepIndexValue`, cosi' click, `aria-pressed`, classe active e dettaglio sotto usano la stessa fonte di verita'.
- Il fallback al primo step resta solo quando cambia il flow e l'indice precedente non e' piu' valido.

Verifiche:

- `npm run check` OK, 0 errori e 0 warning.
- `npm run build` OK.
- `docker compose up -d --build` OK.
- `/pipeline/flow` HTTP 200.
- Proxy flow PAPER OK con 10 step reali.

### Fix 2026-06-11 15:41 CEST

L'utente ha segnalato che il badge `WAIT` era ancora troppo grande, che gli underscore non dovevano uscire nel testo e che la selezione restava apparentemente bloccata sul primo nodo.

Correzioni:

- Badge status dei macro step ridotto a `text-[9px]` con padding compatto.
- Aggiunta normalizzazione `labelText(...)` per sostituire gli underscore con spazi nei testi visibili: status, reason, titoli record, metriche, chiavi sintetiche e dialog key/value.
- La classe active dei macro step ora riceve un booleano diretto `index === selectedStepIndexValue`; cosi' Svelte rivaluta la classe a ogni click e non dipende da una funzione che legge stato interno.

Verifiche:

- `npm run check` OK, 0 errori e 0 warning.
- `npm run build` OK.
- `docker compose up -d --build` OK.
- Browser check con Chrome headless: al caricamento solo il nodo 1 ha `aria-pressed=true`; dopo click sul nodo 2 il nodo 1 passa a `false` e il nodo 2 passa a `true`.
- Browser check testo: `NO_SYMBOLS` e `NO_CANDIDATES` sono visualizzati come `NO SYMBOLS` e `NO CANDIDATES`.

## Piano 4 - Step Half Width E Rail DB Hover

Data: 2026-06-11 15:08 CEST.

Obiettivo: migliorare la leggibilita' della chain orizzontale e rendere le righe DB degli step operativi piu' esplorabili.

Azioni completate:

- Aggiornato `BUSINESS_REQUIRIMENTS.md` con:
  - macro step larghi circa meta' dello spazio;
  - titoli su due righe e dettagli via hover/title;
  - hover/click visibili e coerenti su tutti gli step;
  - righe DB dallo step 3 in poi come rail orizzontale colorato.
- La chain step usa card larghe meta' vista con scroll orizzontale, hover scale/shadow/border e stato active con ring.
- Le card step mostrano actor e timestamp, con title browser per il dettaglio sintetico.
- Dallo step 3 in poi le righe DB sono ordinate per timestamp recente e mostrate in un rail orizzontale.
- Le card del rail mostrano in compatto item principale e metrica primaria; su hover aumentano larghezza e mostrano reason/dettagli secondari.
- I primi due step mantengono la card tecnica completa precedente.

Verifiche:

- `npm run check` OK, 0 errori e 0 warning.
- `npm run build` OK.
- `docker compose up -d --build` OK, container `hft-fe-local` up su `5173`.
- `GET http://localhost:5173/pipeline/flow` HTTP 200.
- Proxy flow PAPER OK con 10 step reali.
