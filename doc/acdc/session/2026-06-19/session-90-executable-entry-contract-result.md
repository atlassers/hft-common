# Session 90 - Executable Entry Contract Result

Data: 2026-06-19.

## Obiettivo

Refinement minimo della sessione 89:

- impedire BUY quando il contratto ML nasce gia' alla loss-cap;
- rendere la loss-cap pubblicata da DocBrown eseguibile rispetto a fee, slippage e dust;
- fare una RUN lunga endpoint-driven senza introdurre guardie arbitrarie.

## Codice

ACDC:

- `ebb98d4 Enforce executable live advice entry contract`
  - aggiunge `ml_advice_entry_price_move`;
  - aggiunge `ml_advice_entry_friction_net_return`;
  - aggiunge `ml_advice_adverse_entry_move`;
  - aggiunge `ml_advice_adverse_entry_loss_cap`;
  - aggiunge `ml_advice_adverse_entry_pass`;
  - `ml_advice_paper_eligible=0` se `adverse_entry_move <= loss_cap`.
- `f0edcaa Configure dynamic live advice loss cap`
  - aggiunge config condivisa:
    - `rem.ml.live_advice.loss_cap.min_buffer=0.0002`;
    - `rem.ml.live_advice.loss_cap.max_abs=0.0080`.

DocBrown:

- `8985098 Derive live advice loss cap from positive drawdown`
  - rimuove loss-cap hardcoded `-0.0028`;
  - calcola loss-cap dal q10 della `minNetReturn` dei positivi non-zero-MFE dello stesso simbolo/batch;
  - fallback: frizione esecutiva + buffer minimo;
  - aggiunge metadata advice:
    - `entry_friction_net_return`;
    - `loss_cap_source`;
    - `loss_cap_positive_samples`.

## RUN

- Directory dati: `/tmp/session90-executable-entry-run2`.
- Execution: `2`.
- Finestra: `2026-06-19T20:16:42Z` - `2026-06-19T21:50:42Z`.
- Durata: circa 94 minuti.
- Cicli: 21.
- Entry point:
  - DocBrown rolling validation endpoint;
  - DocBrown rolling PAPER promotion endpoint;
  - ACDC PAPER run/stop-buy endpoint;
  - ACDC diagnostics endpoint;
  - DocBrown forensics endpoint.

## Risultato ACDC

| Id | Symbol | Exit | Net quote | Net return | Max net return |
| --- | --- | --- | ---: | ---: | ---: |
| 9 | JTOUSDC | EXIT_ML_ADVICE_DYNAMIC_TRAILING | 0.006476130617 | 0.000259045226130653 | 0.000384547738693467 |
| 10 | NMRUSDC | EXIT_ML_ADVICE_LOSS_CAP | -0.132064591750 | -0.005282584884994524 | 0 |
| 11 | MANTAUSDC | EXIT_ML_ADVICE_LOSS_CAP | -0.0917243434883 | -0.003668973747016706 | 0 |
| 12 | MANTAUSDC | EXIT_ML_ADVICE_DYNAMIC_TRAILING | -0.03507351172567 | -0.00140294047334449 | 0.000507650011953144 |
| 13 | CATIUSDC | EXIT_ML_ADVICE_TIMEOUT | 0.2535156245457 | 0.010140625 | 0.010140625 |
| 14 | CATIUSDC | EXIT_ML_ADVICE_TIMEOUT | 0.0358247422176 | 0.001432989690721649 | 0.001432989690721649 |

Sintesi ACDC:

- trade: 6;
- win: 3;
- loss: 3;
- net: `+0.03695405041633`;
- targetHits sell-capture: 2;
- trailingArmed: 4;
- avgMaxNetReturn: `0.00207763540689471`;
- avgNetReturn: `0.00024636013524943`.

## Forensics DocBrown

Sintesi execution 2:

- trades: 6;
- targetHitTrades: 3;
- zeroPostBuyMfeTrades: 0;
- badAdviceTrades: 1;
- badSellTrades: 4;
- goodFlowTrades: 1;
- avgAdviceAgeSeconds: `7.833333333333333`;
- avgAdviceToBuyNetMove: `-0.003266020353259532`;
- avgPostBuyMaxNetReturn: `0.005025519865697922`;
- avgCaptureRatio: `-0.27796997659876654`.

Classificazione:

| Symbol | Classification | Reason |
| --- | --- | --- |
| JTOUSDC | BAD_SELL | Post-BUY MFE existed but realized capture ratio was too low |
| NMRUSDC | BAD_SELL | Post-BUY MFE existed but realized capture ratio was too low |
| MANTAUSDC | BAD_SELL | Post-BUY MFE existed but realized capture ratio was too low |
| MANTAUSDC | BAD_SELL | Post-BUY MFE existed but realized capture ratio was too low |
| CATIUSDC | GOOD_FLOW | Advice, BUY and SELL captured executable movement |
| CATIUSDC | BAD_ADVICE | Advice never produced executable net MFE |

## Interpretazione Scientifica

Il refinement ha migliorato il problema della sessione 89:

- sessione 89: 4 trade, 0 win, 4 loss, net negativo;
- sessione 90 post-fix: 6 trade, 3 win, 3 loss, net leggermente positivo;
- sessione 89 aveva prevalenza zero post-buy MFE;
- sessione 90 ha `zeroPostBuyMfeTrades=0`.

Questo supporta l'ipotesi che la loss-cap non eseguibile fosse un problema reale.

Pero' il modello non e' ancora validato:

- 4 / 6 trade sono classificati `BAD_SELL`;
- 1 / 6 e' `GOOD_FLOW`;
- il profitto netto e' piccolo e dipende molto da CATIUSDC;
- il capture ratio forensics resta debole;
- ACDC runtime e DocBrown forensics non sono ancora perfettamente allineati sulla seconda CATIUSDC.

## Decisione

Classificazione: `PARTIAL_PASS_ENTRY_CONTRACT_FAIL_SELL_CAPTURE`.

Non bisogna aggiungere nuovi filtri BUY.

Il prossimo refinement deve concentrarsi su SELL/capture e su coerenza forensics-runtime:

1. Auditare per ogni posizione la sequenza di tick ACDC usata dalla SELL contro la sequenza DocBrown usata dalla forensics.
2. Separare `BAD_SELL` reale da classificazione forensics troppo severa quando il massimo si verifica dopo una exit gia' ragionevole.
3. Verificare perche' ACDC sell diagnostics mostra `adviceAgeSeconds=0` e `freshnessContractPass=false` nelle righe SELL pur avendo policyJson corretto in BUY.
4. Rivalutare trailing:
   - non come filtro BUY;
   - ma come regola di cattura: arm immediato, retention dinamica, e timeout solo se conserva profitto netto.

## Nota Operativa

`POST /acdc/paper/stop-buy/REM_CURRENT` ha chiuso correttamente execution 2:

- status DB: `COMPLETED`;
- currentBudget: `100.03695405041633`;
- reservedBudget: `0`;
- realizedProfitQuote: `0.03695405041633`.

Il successivo `POST /acdc/paper/stop/REM_CURRENT` ha restituito `500` perche' l'esecuzione risultava gia' completata. Questo e' un bug endpoint/ergonomia da correggere, non un errore della RUN.
