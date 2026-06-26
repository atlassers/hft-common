# Session 91 - SELL Capture Refinement Result

Data: 2026-06-20.

## Obiettivo

1. Fixare il bug endpoint: `POST /acdc/paper/stop/{profileKey}` non deve restituire 500 se `stop-buy` ha gia' drenato/completato l'execution.
2. Procedere col refinement SELL/capture senza aggiungere filtri BUY.
3. Fare una nuova prova PAPER endpoint-driven.

## Modifiche

ACDC:

- `e0628ef Make run stop endpoints idempotent`
  - `paper/stop` e `shadow/stop` ora ritornano il summary dell'ultima execution terminale se non c'e' una RUNNING.
- `417e734 Refine executable sell capture diagnostics`
  - SELL diagnostics eredita dal `policyJson` i campi advice/freshness/revalidation/adverse entry;
  - trailing arm passa da rumore infinitesimale a MFE netto eseguibile `0.0005`;
  - retention resta `0.80`.

## Verifica Bug

Il bug e' risolto.

- Prima: `POST /acdc/paper/stop/REM_CURRENT` dopo `stop-buy` su execution gia' completata ritornava `500`.
- Dopo fix:
  - chiamata su execution `2` gia' `COMPLETED`: HTTP `200`;
  - chiamata reale su execution `3` dopo `stop-buy`: HTTP `200`;
  - execution `3` chiusa con `status=COMPLETED`, `reserved_budget=0`.

## RUN

- Directory dati: `/tmp/session91-sell-capture-run`.
- Execution: `3`.
- Finestra: `2026-06-19T22:17:05Z` - `2026-06-19T23:33:49Z`.
- Durata: circa 77 minuti.
- Entry point:
  - DocBrown rolling validation endpoint;
  - DocBrown rolling PAPER promotion endpoint;
  - ACDC PAPER run/stop endpoints;
  - ACDC diagnostics endpoint;
  - DocBrown forensics endpoint.

## Risultato ACDC

| Id | Symbol | Exit | Net quote | Net return | Max net return |
| --- | --- | --- | ---: | ---: | ---: |
| 15 | IOUSDC | EXIT_ML_ADVICE_LOSS_CAP | -0.145324426828700000 | -0.005812977099236641 | 0 |
| 16 | NEIROUSDC | EXIT_ML_ADVICE_TIMEOUT | -0.049999999999904720 | -0.002000000000000000 | 0 |
| 17 | MEGAUSDC | EXIT_ML_ADVICE_DYNAMIC_TRAILING | 0.016993025716380000 | 0.000679721030042918 | 0.001751609442060086 |

Sintesi:

- trade: 3;
- win: 1;
- loss: 2;
- net: `-0.178331401112224720`;
- trailing armed: 1;
- targetHits ACDC sell-capture: 1;
- avgCaptureRatio ACDC: `0.129351710056151`.

## Forensics

Sintesi DocBrown:

- trades: 3;
- zeroPostBuyMfeTrades: 2;
- badAdviceTrades: 2;
- badSellTrades: 1;
- goodFlowTrades: 0;
- avgAdviceAgeSeconds: `9.666666666666666`;
- avgAdviceToBuyNetMove: `-0.002800000000870867`;
- avgPostBuyMaxNetReturn: `0.002359423469664373`;
- avgCaptureRatio: `0.020002399657912284`.

Dettaglio:

| Symbol | Classification | Reason |
| --- | --- | --- |
| IOUSDC | BAD_ADVICE | Advice never produced executable net MFE |
| NEIROUSDC | BAD_ADVICE | Advice never produced executable net MFE |
| MEGAUSDC | BAD_SELL | Post-BUY MFE existed but realized capture ratio was too low |

## Interpretazione

Il fix endpoint e' confermato.

Il refinement SELL e' solo parzialmente utile:

- ha migliorato la diagnostica SELL;
- ha impedito arming sotto MFE eseguibile;
- su MEGAUSDC ha prodotto una chiusura positiva;
- pero' forensics classifica comunque MEGAUSDC come `BAD_SELL`, perche' dopo la SELL esisteva MFE molto piu' alta.

La RUN non valida il modello operativo:

- 2 trade su 3 sono `BAD_ADVICE`;
- entrambi i BAD_ADVICE hanno zero post-buy MFE;
- il dato medio `adviceToBuyNetMove` e' ancora circa `-0.0028`, cioe' frizione/loss-cap iniziale.

## Decisione

Classificazione: `FAIL_BAD_ADVICE_DOMINANT_WITH_PARTIAL_SELL_IMPROVEMENT`.

Non proseguire aggiungendo filtri BUY reattivi.

Prossimo step scientifico:

1. Spostare a DocBrown/producer un vincolo di pubblicazione advice: non pubblicare PAPER advice se l'opportunita' netta corrente e' solo frizione, cioe' se `entry_friction_net_return` consuma l'intero margine iniziale senza un buffer positivo osservabile.
2. Usare il campione positivo per pubblicare un `min_executable_entry_edge` ML-based, non statico.
3. Continuare a misurare SELL, ma solo dopo aver ridotto i `BAD_ADVICE` zero-MFE.
