# Session 89 - Live Revalidation Long PAPER Result

Data: 2026-06-19.

## Vincolo

Esecuzione conforme al charter REM:

- modifiche committate, pushate e deployate in Docker prima della prova;
- prova operativa endpoint-driven, senza usare H2;
- nessuna guardia reattiva non pre-registrata;
- analisi finale con forensics e classificazione causale.

## Modifiche Implementate

- DocBrown: `a2c14e6 Add rolling advice live revalidation contract`.
  - Aggiunge `live_revalidation_ranges` negli advice rolling.
  - I range sono calcolati sui quantili 10/90 delle righe storiche profittevoli dello stesso simbolo/batch.
- ACDC: `c7da8cb Enforce live advice revalidation ranges`.
  - Richiede `age && drift && live_revalidation` per rendere un advice PAPER eleggibile.
  - Salva diagnostica `ml_advice_live_revalidation_*` nelle decisioni/posizioni.
- ACDC: `ccf51ae Serialize PAPER runs by profile`.
  - Evita deadlock e duplicate same-symbol position serializzando la run PAPER per profilo.

## Run

- Profilo: `REM_CURRENT`.
- Execution: `1`.
- Finestra loop: `2026-06-19T17:30:16Z` - `2026-06-19T19:03:06Z`.
- Durata effettiva: circa 93 minuti.
- Cicli: 21.
- Entry point:
  - DocBrown rolling validation endpoint.
  - DocBrown rolling PAPER promotion endpoint.
  - ACDC PAPER run/stop endpoints.
  - ACDC/DocBrown diagnostics endpoint.
- Evidenze locali:
  - `/tmp/session89-long-run/`
  - `/tmp/session89_forensics_execution_1.json`
  - `/tmp/session89_acdc_paper_scoring_execution_1.json`
  - `/tmp/session89_acdc_sell_capture_execution_1.json`

## Risultato Operativo

La correzione runtime e' positiva:

- deadlock non riprodotto;
- duplicate same-symbol open positions non riprodotte;
- PAPER stoppato da endpoint con posizioni aperte pari a zero;
- i container `acdc` e `docbrown` sono rimasti attivi.

Il risultato strategico e' negativo:

| Position | Symbol | Exit | Net return | Max net return ACDC | Esito forensics |
| --- | --- | --- | ---: | ---: | --- |
| 5 | DOLOUSDC | EXIT_ML_ADVICE_LOSS_CAP | -0.003181316515569570 | 0 | BAD_ADVICE |
| 6 | METUSDC | EXIT_ML_ADVICE_DYNAMIC_TRAILING | -0.002727074235807860 | 0.000908296943231441 | BAD_SELL |
| 7 | ETHFIUSDC | EXIT_ML_ADVICE_LOSS_CAP | -0.004830028328611898 | 0 | BAD_ADVICE |
| 8 | EIGENUSDC | EXIT_ML_ADVICE_LOSS_CAP | -0.003284061696658098 | 0 | BAD_ADVICE |

Sintesi sessione 89:

- trade: 4;
- win: 0;
- loss: 4;
- net: `-0.350562017441750000`;
- classificazione: `FAIL_STRATEGIC_BAD_ADVICE_BAD_SELL`.

## Verifica Del Nuovo Gate

Le 4 BUY accettate hanno passato il contratto live:

| Symbol | Advice id | Advice age | Live revalidation | Drift pass | Freshness contract |
| --- | ---: | ---: | ---: | ---: | ---: |
| DOLOUSDC | 6 | 4s | pass | pass | pass |
| METUSDC | 10 | 6s | pass | pass | pass |
| ETHFIUSDC | 29 | 8s | pass | pass | pass |
| EIGENUSDC | 28 | 18s | pass | pass | pass |

Conclusione: il gate e' implementato e osservabile, ma non risolve il problema strategico. Misura la coerenza del pattern live rispetto al campione profittevole, ma non impedisce una BUY quando il prezzo live e' gia' caduto fino alla loss-cap del contratto.

## Forensics Causale

Forensics DocBrown sui 4 trade della sessione 89:

- `DOLOUSDC`:
  - advice age 5s;
  - `adviceToBuyNetMove=-0.0028000000001976`;
  - `postBuyMaxNetReturn=-0.0028000000001976`;
  - `zeroPostBuyMfe=true`;
  - classificazione `BAD_ADVICE`.
- `METUSDC`:
  - advice age 6s;
  - `adviceToBuyNetMove=-0.0028000000010144`;
  - `postBuyMaxNetReturn=0.023400873361431016`;
  - `targetHitAfterBuy=true`;
  - `captureRatio=-0.11653728447172339`;
  - classificazione `BAD_SELL`.
- `ETHFIUSDC`:
  - advice age 10s;
  - `adviceToBuyNetMove=-0.002800000004208`;
  - `postBuyMaxNetReturn=-0.002800000004208`;
  - `zeroPostBuyMfe=true`;
  - classificazione `BAD_ADVICE`.
- `EIGENUSDC`:
  - advice age 19s;
  - `adviceToBuyNetMove=-0.0028000000023968`;
  - `postBuyMaxNetReturn=-0.002371550987829534`;
  - `zeroPostBuyMfe=true`;
  - classificazione `BAD_ADVICE`.

Il dato comune non e' la lentezza del ML: gli advice erano freschi. Il dato comune e' che la BUY e' avvenuta con `adviceToBuyNetMove` gia' circa pari alla loss cap (`-0.0028`). Questo rende l'ingresso non scientificamente accettabile: si compra quando il contratto di rischio e' gia' consumato.

## Blocco Successivo

Dopo i loss iniziali, la protezione sessione ha impedito molte altre BUY:

| Reason | Count |
| --- | ---: |
| PAPER_SESSION_ZERO_MFE_COOLDOWN | 97332 |
| REVERSAL_ML_RULE_MISSING | 44531 |
| ML_ADVICE_NOT_PAPER_ELIGIBLE | 336 |
| ACCEPTED | 4 |
| EXIT_ML_ADVICE_LOSS_CAP | 3 |
| EXIT_ML_ADVICE_DYNAMIC_TRAILING | 1 |

Questo spiega perche' la run ha prodotto pochi trade nella parte finale. Non va contato come successo strategico: e' una protezione runtime dopo evidenza di zero-MFE, non capacita' predittiva.

## Problemi Aperti

1. Contratto di ingresso direzionale mancante.
   - ACDC valida eta', drift assoluto e range feature.
   - Non valida se il movimento tra `ml_advice_entry_price_reference` e prezzo live e' gia' oltre la `ml_advice_loss_cap_net_return`.
   - Il refinement corretto e' usare la loss-cap ML come vincolo direzionale del contratto, non una soglia statica nuova.

2. Discrepanza `METUSDC`.
   - ACDC runtime registra `maxNetReturn=0.000908296943231441`.
   - DocBrown forensics ricostruisce `postBuyMaxNetReturn=0.023400873361431016`.
   - Questo richiede verifica del tracking runtime MFE/snapshot cadence: o ACDC perde micro-movimenti, o la forensics usa una finestra/tick source non equivalente.

## Decisione Scientifica

La sessione non valida il modello corrente.

Il prossimo step ammesso dal charter e' un refinement minimo e motivato:

- implementare in ACDC il controllo direzionale di ingresso usando campi gia' ML-based:
  - `ml_advice_entry_price_reference`;
  - `ml_advice_loss_cap_net_return`;
  - prezzo live al momento della decisione;
- rifiutare una BUY long se il rendimento netto corrente rispetto all'entry reference e' gia' minore o uguale alla loss cap del contratto;
- aggiungere diagnostica esplicita:
  - `ml_advice_adverse_entry_move`;
  - `ml_advice_adverse_entry_pass`;
  - `ml_advice_adverse_entry_loss_cap`;
- verificare `METUSDC` confrontando MFE runtime ACDC e MFE ricostruita DocBrown sulla stessa fonte dati e stessa finestra.

Questa non e' una nuova guardia arbitraria: e' enforcement del contratto ML esistente. Se un advice dice che la perdita massima tollerata e' `-0.0028`, non ha senso comprare quando il mercato e' gia' a `-0.0028` rispetto al riferimento dell'advice.
