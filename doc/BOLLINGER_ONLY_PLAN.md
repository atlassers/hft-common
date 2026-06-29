# Bollinger Only Plan

Data: 2026-06-28.

## Scopo

Questo piano migliora l'AS-IS senza cambiare famiglia strategica.

Il vincolo resta:

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

Le decisioni operative restano solo Bollinger:

```text
bb_*
```

Non entrano EMA, RSI, MACD, volume ratio, ADX, ATR, OBV, MFI o market-structure come filtri decisionali. Possono essere
aggiunti solo come diagnostica esplicita non decisionale, se utile dopo le run.

## Diagnosi AS-IS

Le PAPER `37` e `38` hanno validato il meccanismo tecnico ma non la qualita' economica:

- WATCH ha comprato quando `bb_buy_contract_pass=1`.
- Il contratto separato `history_*`, `live_*`, `entry_*`, `exit_*` e' completo.
- Il profilo `2h/1m` e' operativo.
- Entrambe le run hanno comprato `SUSDC`.
- Entrambe hanno avuto MFE netto `0`.
- Entrambe sono uscite in `EXIT_ML_ADVICE_LOSS_CAP`.

Conclusione: il problema prioritario non e' che WATCH ignora Bollinger. Il problema e' che il flag unico
`bb_buy_contract_pass` e' troppo debole/ambiguo per distinguere segnali Bollinger buoni da rientri inutili.

## Obiettivo

Rendere Bollinger-only piu' rigoroso senza introdurre indicatori esterni:

- distinguere i setup Bollinger;
- evitare un unico flag generico;
- rendere il contratto ML/live/WATCH leggibile;
- misurare quale setup produce expectancy;
- ridurre zero-MFE e loss-cap su segnali formalmente validi.

## Decisione Del Consiglio

Saggio ascoltatore:

- non cambiare ancora famiglia strategica;
- prima pulire il significato di Bollinger-only.

Scienziato severo:

- `bb_buy_contract_pass=1` non basta come modello scientifico;
- ogni trade deve avere un setup Bollinger dichiarato.

Mediano pragmatico:

- usare solo `bb_*`, ma separare almeno due logiche:
  - mean reversion;
  - squeeze/breakout.

Decisione unica:

Implementare `BOLLINGER_ONLY_V2`: stessa pipeline, solo feature `bb_*`, ma con setup esplicito e trigger WATCH
setup-specifici.

## Setup Ammessi

### BB_REENTRY_MEAN_REVERSION_LONG

Idea:

- il prezzo ha violato la banda inferiore;
- rientra dentro le bande;
- il target naturale e' middle band o safe target derivato dal contratto.

Feature Bollinger richieste:

- `bb_lower_breach`
- `bb_reentry_confirmed`
- `bb_percent_b`
- `bb_reentry_age_seconds`
- `bb_middle`
- `bb_lower`
- `bb_bandwidth`
- `bb_bandwidth_delta`
- `bb_middle_slope`

Trigger WATCH:

```text
setup = BB_REENTRY_MEAN_REVERSION_LONG
AND bb_lower_breach=1 in history/live contract
AND current bb_reentry_confirmed=1
AND current bb_percent_b >= contract_min_percent_b
AND current bb_percent_b <= contract_max_percent_b
AND abs(bb_middle_slope) <= contract_max_middle_slope_abs
AND bb_reentry_age_seconds <= contract_max_reentry_age_seconds
```

### BB_SQUEEZE_BREAKOUT_LONG

Idea:

- le bande sono compresse;
- il prezzo rompe sopra la banda superiore;
- il movimento va comprato solo quando l'espansione Bollinger e' iniziata.

Feature Bollinger richieste:

- `bb_squeeze`
- `bb_bandwidth`
- `bb_bandwidth_percentile`
- `bb_bandwidth_delta`
- `bb_expansion`
- `bb_upper_breach`
- `bb_percent_b`
- `bb_upper`
- `bb_middle_slope`

Trigger WATCH:

```text
setup = BB_SQUEEZE_BREAKOUT_LONG
AND history/live bb_squeeze=1 OR bb_bandwidth_percentile <= contract_max_bandwidth_percentile
AND current bb_upper_breach=1
AND current bb_percent_b >= contract_min_breakout_percent_b
AND current bb_bandwidth_delta > 0
AND current bb_expansion=1
AND current bb_middle_slope >= contract_min_middle_slope
```

## Interventi Per Modulo

### hft-common

Interventi:

- aggiungere enum `BollingerSetupType`:
  - `BB_REENTRY_MEAN_REVERSION_LONG`
  - `BB_SQUEEZE_BREAKOUT_LONG`
- aggiungere enum `BollingerTriggerType`:
  - `BB_REENTRY_CONFIRMED`
  - `BB_UPPER_BREAKOUT_CONFIRMED`
- aggiungere costanti JSON in `RemConstants`:
  - `bb_setup`
  - `bb_trigger`
  - `bb_bandwidth_delta`
  - `bb_bandwidth_percentile`
  - `bb_middle_slope`
  - `bb_min_percent_b`
  - `bb_max_percent_b`
  - `bb_min_breakout_percent_b`
  - `bb_max_bandwidth_percentile`
  - `bb_max_middle_slope_abs`
  - `bb_min_middle_slope`
  - `bb_max_reentry_age_seconds`

Test/documentazione:

- test di costanti/enum se presenti nel repo;
- aggiornare `STRATEGIC_REM_HANDOFF.md` solo dopo approvazione.

### docbrown

Interventi:

- `InfluxSnapshotService`:
  - calcolare `bb_bandwidth_delta`;
  - calcolare `bb_bandwidth_percentile` sulla finestra disponibile;
  - calcolare `bb_middle_slope`;
  - mantenere aggregazione `1m` e feature window `20m`.
- `BlankRemCandidateService`:
  - generare candidate separate per setup;
  - non usare piu' un solo candidato generico `feature:bb_buy_contract_pass=1`;
  - salvare nel candidate/advice `bb_setup` e `bb_trigger`;
  - calcolare metriche separate per setup: supporto, avg end return, safe hit, zero-MFE, loss-cap proxy.
- `RollingPaperPromotion`:
  - promuovere advice setup-specifiche;
  - pubblicare `history_*` e `live_*` per le nuove key `bb_*`;
  - mantenere `pre_buy_watch_required=true`.

Test:

- test unitari su calcolo `bb_bandwidth_delta`, percentile e middle slope;
- test di promotion con due setup distinti;
- test che nessun payload operativo nuovo emetta `reversal_*`.

### acdc

Interventi:

- `PreBuyWatchService`:
  - sostituire il trigger unico `bb_buy_contract_pass` con dispatch su `bb_setup`;
  - mantenere `bb_buy_contract_pass` solo come compatibilita' diagnostica o derivato finale;
  - non limitare WATCH o BUY con cap numerici su posizioni/osservazioni concorrenti;
  - l'unico limite ammesso alla BUY e' il budget disponibile, insieme alle regole di sizing/exchange;
  - produrre reason distinte:
    - `WATCH_WAITING_BB_REENTRY_CONTRACT`
    - `WATCH_WAITING_BB_BREAKOUT_CONTRACT`
    - `WATCH_BB_SETUP_MISSING`
    - `WATCH_BB_CONTRACT_INCOMPLETE`
- `OutcomeQualityModelService` / feature mapping:
  - propagare `bb_setup`, `bb_trigger` e nuove key `bb_*`;
  - scrivere `entry_bb_setup`, `entry_bb_trigger` nel `policy_json`.
- Guard/ranking DB:
  - mantenere guardie tecniche minime;
  - rimpiazzare ranking generico `rank_bb_buy_contract_pass` con ranking setup-aware se necessario.
- SELL:
  - per primo giro non cambiare policy SELL;
  - mantenere target/loss/no-MFE/trailing derivati dal contratto.

Test:

- WATCH compra mean reversion solo se trigger reentry passa;
- WATCH compra breakout solo se trigger breakout passa;
- WATCH non compra se setup manca;
- WATCH non compra se contratto setup incompleto.

### kenshiro

Interventi:

- `/management/state`:
  - esporre conteggi per setup:
    - active reentry advice;
    - active breakout advice;
    - WATCH waiting per setup;
    - BUY per setup.
- diagnostics:
  - mostrare latest selected setup;
  - mostrare zero-MFE/loss-cap per setup nelle ultime PAPER.
- orchestration:
  - nessun cambio di sequenza;
  - mantenere `AUTO_AB_START` PAPER-only.

Test:

- state summary include setup counts;
- no SHADOW branch reintroduced.

### hft-fe

Interventi:

- `/management`:
  - mostrare setup advice (`BB_REENTRY_MEAN_REVERSION_LONG` / `BB_SQUEEZE_BREAKOUT_LONG`);
  - mostrare WATCH reason setup-specifica;
  - distinguere `bb_setup` e `bb_trigger` nella vista contract;
  - nessuna nuova pagina.

Test:

- `npm run check`;
- `npm run build`.

### DB / Migration

Interventi ACDC:

- aggiungere eventuali guard/ranking setup-aware;
- non riattivare tabelle legacy;
- non creare tabelle nuove se i JSON advice/policy bastano.

Interventi DocBrown:

- nessuna tabella nuova obbligatoria se `acdc_rem_observation_candidate.feature_json` e `acdc_live_ml_advice.advice_json`
  bastano.

Principio:

- prima usare JSON contract versionato;
- creare colonne/tabelle solo se diagnostica o query management diventano troppo costose.

## Validazione

Fase 1 - Technical validation:

- build hft-common, docbrown, acdc, kenshiro, hft-fe;
- deploy container;
- refresh diagnostics;
- generare advice;
- verificare payload con `bb_setup` e senza `reversal_*`.

Fase 2 - PAPER small sample:

- almeno 3 PAPER per setup se disponibili;
- non mischiare setup nella stessa conclusione scientifica;
- run senza REAL.

Metriche minime:

- PnL quote;
- net return;
- MFE;
- zero-MFE rate;
- loss-cap rate;
- WATCH expired rate;
- trigger pass/fail reason;
- expectancy per setup.

## Exit Criteria

Promuovibile solo se:

- almeno un setup produce MFE positivo ripetuto;
- zero-MFE rate scende rispetto a PAPER `37`/`38`;
- loss-cap rate non domina;
- contract completo in forensics;
- runtime finale pulito.

## Set Minimo Ex Novo

Il repository installabile per Bollinger-only non deve includere dump DB storici o backup operativi.

Sono ammessi:

- sorgenti runtime;
- migration necessarie a creare lo schema corrente;
- script necessari a build, deploy, diagnostica e PAPER;
- documentazione corrente del processo Bollinger-only.

Non sono ammessi:

- dump `hft` / `back_test`;
- backup SQL generati durante reset operativi;
- migration o script che creano e poi cancellano strutture di strategie ritirate;
- payload di esempio con `shadow`, `reversal`, `scalping`, `backtest` o contratti non `bb_*`, salvo documentazione storica esplicitamente marcata come tale.

## Rischi

- Restare Bollinger-only puo' non bastare nei mercati direzionali o illiquidi.
- Squeeze breakout senza volume puo' produrre falsi segnali.
- Mean reversion senza filtro regime puo' comprare crolli.
- Se i setup non vengono separati, le forensics restano inutili.

## Decisione Richiesta

Scegliere questo piano se si vuole:

- restare fedeli al vincolo Bollinger-only;
- ridurre ambiguita' del segnale prima di introdurre altri indicatori;
- ottenere evidenza pulita per setup.
