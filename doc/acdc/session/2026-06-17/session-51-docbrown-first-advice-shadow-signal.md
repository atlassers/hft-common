# Session 51 - DocBrown First Advice Shadow Signal

Data: 2026-06-17.

## Obiettivo

Far partire ACDC appena DocBrown trova il primo advice REM promosso.

Vincolo aggiornato:

- nessuna attesa di `N` advice;
- il primo advice promosso puo' essere il piu' profittevole;
- gli advice successivi restano ordinati/rankati da ACDC via DB;
- DocBrown resta ricerca/ML, ACDC resta runtime/trading.

## Implementazione

ACDC espone:

`POST /acdc/profiles/{profileKey}/research-batches/{researchBatchId}/shadow-signal`

Il payload accettato contiene:

- `source`;
- `profileKey`;
- `researchBatchId`;
- `scopeType`;
- `scopeKey`;
- `ruleKey`.

L'endpoint avvia o riusa una SHADOW RUN per il profilo richiesto.

DocBrown:

- genera un `researchBatchId` per ogni mining run;
- salva ogni rule in transazione separata `REQUIRES_NEW`;
- segnala ACDC solo dopo il commit del primo advice `PROMOTED`;
- invia al massimo un signal per batch;
- continua il mining anche se ACDC non risponde.

Configurazione DocBrown:

- `DOCBROWN_ACDC_SHADOW_SIGNAL_ENABLED`;
- `DOCBROWN_ACDC_BASE_URL`.

## Correzione Transazionale

Il mining DocBrown non puo' segnalare ACDC prima che la rule promossa sia visibile a DB.

Per questo la persistenza delle rule e' stata estratta in un servizio dedicato con transazioni `REQUIRES_NEW`.
Il flusso corretto e':

1. DocBrown calcola candidate;
2. salva/committa la singola rule;
3. se la rule e' il primo `PROMOTED` del batch, chiama ACDC;
4. ACDC legge da DB advice gia' visibili.

## Verifiche

Build e test:

- ACDC: `./mvnw -q test && ./mvnw -q package`;
- DocBrown: `./mvnw -q test && ./mvnw -q package`.

Verifica runtime:

- ACDC container avviato su `8091`;
- Vault inizialmente sealed, risolto con unseal locale;
- DocBrown lanciato con signal abilitato;
- mining REM su `REM_CURRENT`.

Risultato mining:

- `evaluatedRules`: `190`;
- `promotedRules`: `11`;
- generated at: `2026-06-17T08:15:04.238173401Z`.

Il signal ha avviato SHADOW `50` alle `2026-06-17 08:14:36`, quindi prima della chiusura completa del mining.

## Esito SHADOW 50

La run ha aperto due posizioni e poi e' stata messa in stop-buy per drenare:

| Symbol | Opened | Closed | Net | Max Net Return | Exit |
| --- | --- | --- | ---: | ---: | --- |
| `2ZUSDC` | `08:14:56` | `08:16:02` | `-0.124816176340380000` | `0` | `EXIT_ML_ADVICE_LOSS_CAP` |
| `AAVEUSDC` | `08:15:10` | `08:19:13` | `-0.089418494980000000` | `0` | `EXIT_ML_ADVICE_LOSS_CAP` |

Execution finale:

- id: `50`;
- status: `COMPLETED`;
- started: `2026-06-17 08:14:36`;
- completed: `2026-06-17 08:19:13`;
- budget finale: `99.785765328679620000`;
- realized profit quote: `-0.214234671320380000`.

## Lettura

Il problema di timing e' risolto: ACDC parte sul primo advice promosso e non aspetta la fine del batch.

Il problema di qualita' economica non e' risolto: i due trade hanno avuto `max_net_return = 0`, quindi non hanno prodotto neanche una micro-escursione netta positiva prima del loss cap.

La prossima iterazione deve lavorare sul miner ML/advice quality, non sul trigger runtime:

- penalizzare o non promuovere pattern che nel live audit hanno loss con MFE netto zero;
- verificare se i segnali promossi sono reversal veri o curve stale/spigolose;
- mantenere BUY solo su advice ML visibili a DB e rankati outcome-first.
