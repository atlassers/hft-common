# Current Context

Ultimo aggiornamento: 2026-07-04 CEST.

Snapshot operativo corrente del workspace `/home/mbc/Documenti/ws/java/hft`.

## Gerarchia Documentale

1. `hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`
2. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`
3. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_A1_LITERATURE_ALIGNMENT_PLAN.md`
4. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`
5. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_PLAN.md`
6. `hft-common/doc/CURRENT_CONTEXT.md`
7. `hft-common/doc/STRATEGIC_REM_HANDOFF.md`
8. `hft-common/doc/archived/BOLLINGER_ONLY_PLAN.md`

Se i documenti confliggono, prevale il charter; poi
`archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`; poi
`archived/BOLLINGER_CONTEXT_V1_A1_LITERATURE_ALIGNMENT_PLAN.md`; poi
`archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`; poi `archived/BOLLINGER_CONTEXT_V1_PLAN.md`.

I documenti sotto `doc/acdc`, `doc/docbrown`, `doc/influxer`, `doc/hft-fe` e `doc/kenshiro` sono memoria storica di
sessione. Possono spiegare perche' esistono microbar, REAL, SHADOW o reversal legacy, ma non sono fonte operativa se
contraddicono la gerarchia sopra.

## Vincoli Correnti

- REAL vietata.
- PAPER solo da `/management`.
- La sorgente advice runtime resta `hft.acdc_live_bb_advice`.
- Bollinger resta il segnale centrale: setup e trigger sono obbligatori.
- A2 corregge A0/A1: la letteratura Bollinger non impone 1m. La cadence decisionale e' un parametro dichiarato e
  deve essere identica in DocBrown, WATCH/BUY, SELL strategica e forensics.
- Profilo operativo A2 corrente: `bb.decision.interval_seconds=5`, `decision_source_bucket=binance-microbar`,
  `decision_synthetic_backfill=0`.
- Profilo 1m resta ammesso solo se dichiarato e coerente end-to-end.
- Context V1 aggiunge regime, trend, momentum, volume e risk come feature contrattuali esplicite.
- WATCH compra solo se passano trigger Bollinger setup-specifico e gate Context V1.
- SELL fase 1 resta invariato rispetto a Bollinger-only, per isolare l'effetto dei gate di ingresso.
- SELL strategica ragiona sulla stessa cadence decisionale del BUY; microbar 5s e' strategica solo nel profilo A2
  dichiarato e solo se non sintetica.
- Il loss cap quote-aware puo' usare prezzo eseguibile intraminuto solo come protezione economica meccanica, separata
  dagli indicatori Bollinger/context.
- WATCH e BUY non hanno cap numerici concorrenti; l'unico limite ammesso all'acquisto e' budget/exchange sizing.
- Le stringhe operative devono stare in enum/costanti.
- MySQL e container deployati sono obbligatori per validazione operativa.

## Processo

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

Setup ammessi:

- `BB_REENTRY_MEAN_REVERSION_LONG`
- `BB_SQUEEZE_BREAKOUT_LONG`

Trigger ammessi:

- `BB_REENTRY_CONFIRMED`
- `BB_UPPER_BREAKOUT_CONFIRMED`

Regimi Context V1:

- `REGIME_RANGE`
- `REGIME_SQUEEZE`
- `REGIME_EXPANSION`
- `REGIME_TREND_UP`
- `REGIME_TREND_DOWN`
- `REGIME_CHAOS`

## Stato Implementativo

Completato e pushato:

- rename runtime contract `LiveBbAdvice` / `acdc_live_bb_advice`;
- readiness `BB_READY`;
- guard/operator SELL con prefisso `BB_ADVICE_*`;
- namespace config operativo `bb.*`;
- rimozione del ramo operativo parallelo dal management;
- pulizia dei residui management in Kenshiro;
- rimozione del laboratorio Python DocBrown non usato dal runtime Quarkus;
- documentazione root FE/Kenshiro riallineata al ciclo Bollinger-only;
- piano `archived/BOLLINGER_CONTEXT_V1_PLAN.md` armonizzato con l'AS-IS dopo le RUN PAPER;
- `archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md` promosso a charter strategico operativo.

In corso:

- A2 correzione cadence e soglie ufficiali dopo regressione certa RUN 118-120;
- nessuna nuova PAPER prima di implementazione/deploy A2;
- distinzione obbligatoria tra evidenza di provenance, `NEGATIVE_OPERATIONAL_SIGNAL` BUY trigger e
  `INCONCLUSIVE` finanziario.

Blocco corrente vincolante:

```text
A2 - Correzione Cadence E Soglie Ufficiali Bollinger
```

Policy A2 corrente:

- la cadence decisionale e' dichiarata da `bb.decision.interval_seconds`;
- default operativo: `5s`, bucket `binance-microbar`, `synthetic_backfill=0`;
- profilo alternativo ammesso: `60s`, bucket `binance`;
- `binance-realtime` resta diagnostica/UI e non puo' autorizzare BUY;
- SELL strategica usa la stessa cadence del BUY;
- il decision snapshot deve esporre bucket, interval, candle state, feature window, candle count, max gap e staleness;
- il lookback decisionale deve coprire EMA50 e volume ratio sulla cadence dichiarata;
- microbar synthetic da backfill 1m espanso a 5s devono essere marcate e non usate come fonte decisionale;
- nessuna nuova RUN PAPER prima di readiness cadence vera.

AS-IS codice verificato prima dell'intervento A0, memoria storica di regressione:

- DocBrown `InfluxSnapshotService` usa ancora `microbarBucketName()` per storico/live feature;
- ACDC `InfluxSnapshotService` usa ancora `microbarBucketName()` per historical/current snapshot e preferisce microbar
  nel replay source fallback;
- ACDC SELL usa `PaperRunService.exitSnapshot(...)` sullo snapshot corrente del paper loop; finche' quello snapshot e'
  microbar-driven, anche SELL strategica non e' A0-compliant;
- `GuardEvaluator` contiene gia' loss cap quote-aware tramite `NET_LOSS_QUOTE`, ma non espone ancora metadati separati
  decisione/esecuzione SELL;
- influxer `ShortRetentionBucketBackfillJob` espande candele 1m in microbar 5s interpolati; serve flag
  `synthetic_backfill` prima che il replay possa essere interpretato correttamente;
- Kenshiro `/management/state` espone `bbReady`, ma non ancora `1m_alignment_ready` e blocker A0 dedicati;
- hft-fe `/trades` mostra replay Influx/MySQL, ma non ancora `source_bucket`, `interval_seconds`, `candle_count`,
  `max_gap_seconds`, `synthetic_backfill` in modo vincolante;
- script diagnostici ACDC sono `DIAGNOSTIC_ONLY`, ma non stampano ancora la provenance dati completa richiesta da A0.1;
- `acdc_shared_runtime_config` descrive ancora microbar come condivisa da trading e ML;
- hft-fe contiene superfici legacy con selettore REAL, mentre Kenshiro blocca `REAL_RUN`.
- non esiste ancora un owner/runtime contract esplicito per `1m_alignment_ready`.

Implementazione A0 locale verificata con build:

- `hft-common` espone costanti A0, bucket, decision/entry/SELL/replay metadata, classificazioni evidenza e blocker;
- `InfluxTick` trasporta `syntheticBackfill`;
- influxer marca `synthetic_backfill=false` sulle scritture normali e `true` sui microbar di backfill sintetico;
- DocBrown legge indicatori/live-score da `binance` 1m chiuso, con lookback decisionale almeno sufficiente per EMA50 e
  volume ratio 1m/20m, e pubblica metadata decisionali numerici nel contract advice;
- ACDC WATCH/BUY legge snapshot decisionali da `binance` 1m chiuso e fallisce chiusa sui blocker A0;
- ACDC SELL strategica usa lo stesso snapshot decisionale 1m chiuso e fallisce chiusa sui blocker A0; microbar resta
  replay/forensics/timing;
- replay ACDC/Kenshiro espone `source_bucket`, `interval_seconds`, `candle_count`, `max_gap_seconds`,
  `synthetic_backfill`;
- Kenshiro `/management/state` espone `oneMinuteAlignmentReady`, `a0Blockers`, `a0Diagnostics` e blocca PAPER se A0
  non e' pronto;
- hft-fe `/management` mostra readiness/blocker A0, `/trades` mostra replay metadata, e la superficie REAL e' rimossa
  dal layout/dashboard;
- script diagnostici ACDC stampano `DIAGNOSTIC_ONLY` e provenance dati.

Stato A0 deployato e verificato il 2026-07-04:

- container aggiornati/verificati: `hft-fe-local`, `kenshiro-local`, `docbrown`, `acdc-vpn`, `influxer`;
- `/management/state` espone `oneMinuteAlignmentReady=true` e alias JSON `1m_alignment_ready=true`;
- `a0Blockers=[]` e `blockers=[]`;
- active advice PAPER_ELIGIBLE promossa da `management-rolling-20260704T010535Z` contiene metadata decisionali:
  `decision_source_bucket=binance`, `decision_interval_seconds=60`, `decision_candle_state=CLOSED`,
  `decision_candle_count=89`, `decision_max_gap_seconds=60`, `decision_synthetic_backfill=0`;
- RUN PAPER 118 e' partita solo dopo readiness A0 vera, tramite `/management`;
- RUN PAPER 118 e' stata fermata tramite `/management/actions/PAPER_STOP`;
- RUN PAPER 118: `STOPPED`, `positions=0`, `openPositions=0`, PnL `0`; MySQL operativo mostra 2400 decisioni ENTRY
  `HOLD`, con 2370 `WATCH_WAITING_BUY_CONTRACT` e 30 `WATCH_OPENED_WAITING_BUY_CONTRACT`;
- tutte le 30 WATCH della RUN 118 sono state riconciliate a `ABANDONED` con reason
  `WATCH_ABANDONED_BY_PAPER_TERMINAL_RECONCILIATION`;
- `/trades`/detail espone replay metadata e candle metadata: `source_bucket`, `interval_seconds` effettivo,
  `candle_count`, `max_gap_seconds`, `synthetic_backfill`;
- campione replay verificato: `source_bucket=binance-microbar`, `synthetic_backfill=false`; l'intervallo effettivo puo'
  risultare superiore a 5s quando il replay persistito non e' denso, e in quel caso `max_gap_seconds` diventa parte
  dell'evidenza diagnostica;
- classificazione RUN 118: `VALID_STRATEGIC_EVIDENCE` per readiness/contratto A0 e avvio PAPER governato;
  `NEGATIVE_OPERATIONAL_SIGNAL` per BUY trigger perche' `TriggerAudit` non passa mai; `INCONCLUSIVE` per performance
  finanziaria per assenza di BUY/SELL.

Stato A1 progettato il 2026-07-04:

- documento vincolante:
  `archived/BOLLINGER_CONTEXT_V1_A1_LITERATURE_ALIGNMENT_PLAN.md`;
- evidenza MySQL RUN 118: 2400 decisioni ENTRY, 0 BUY, 0 posizioni;
- reason dominanti: 2370 `WATCH_WAITING_BUY_CONTRACT`, 30 `WATCH_OPENED_WAITING_BUY_CONTRACT`;
- reentry: 1680 decisioni, 0 full pass; failure principali `reentry_confirm_fail=1401`,
  `percent_b_fail=1261`, `lower_breach_fail=1120`;
- breakout: 720 decisioni, 0 full pass; failure principali `percent_b_fail=720`, `middle_slope_fail=720`,
  `upper_breach_fail=536`;
- decisione Consiglio: mantenere A0 1m chiusa, correggere A1 su trigger BUY, breach bar-by-bar, distinction
  `PAPER_WATCH_ELIGIBLE`/`PAPER_BUY_ELIGIBLE`, reason granulari.

Stato A1 implementato e deployato il 2026-07-04:

- container aggiornati/verificati: `docbrown`, `acdc-vpn`, `kenshiro-local`, `hft-fe-local`; `hft-common` installato
  localmente per le costanti condivise;
- DocBrown e ACDC calcolano breach Bollinger bar-by-bar con bande contemporanee e pubblicano
  `latest_lower_breach_at_epoch_seconds`, `latest_upper_breach_at_epoch_seconds`, `reentry_zone_state`,
  `breakout_zone_state`;
- DocBrown pubblica `bb_advice_paper_watch_eligible` e `bb_advice_paper_buy_eligible`; la classe DB
  `PAPER_ELIGIBLE` resta alias transitorio compatibile per advice osservabili;
- ACDC ricalcola runtime `bb_advice_paper_buy_eligible` dal live snapshot 1m chiuso e separa:
  - WATCH osservabile: `bb_advice_paper_watch_eligible=1`;
  - BUY comprabile: trigger setup-specifico passato;
  - context gate separato da trigger gate;
- `bb_advice_freshness_contract_pass` ora indica completezza/freshness del contract, non target economico positivo;
  `bb_advice_economic_safe_pass` resta audit economico separato;
- `PreBuyWatchService` emette reason granulari A1:
  `WATCH_WAITING_REENTRY_LOWER_BREACH`, `WATCH_WAITING_REENTRY_RECOVERY`, `WATCH_REENTRY_OVEREXTENDED`,
  `WATCH_REENTRY_MIDDLE_SLOPE_BLOCKED`, `WATCH_REENTRY_AGE_EXPIRED`,
  `WATCH_WAITING_BREAKOUT_UPPER_BREACH`, `WATCH_WAITING_BREAKOUT_PERCENT_B`,
  `WATCH_WAITING_BREAKOUT_EXPANSION`, `WATCH_BREAKOUT_MIDDLE_SLOPE_BLOCKED`,
  `WATCH_CONTEXT_REGIME_BLOCKED`, `WATCH_CONTEXT_MOMENTUM_BLOCKED`,
  `WATCH_CONTEXT_VOLUME_BLOCKED`, `WATCH_CONTEXT_RISK_BLOCKED`;
- Kenshiro `/management/runs/{executionId}` espone `a1BuyDiagnostics` con:
  `entryDecisions`, `acceptedBuys`, `buyTriggerFailDistribution`, `buyContextFailDistribution`,
  `triggerReasonCodes`, `setupFailMatrix`;
- hft-fe `/management` mostra la tab A1 nel dettaglio run; `/trades` mostra eligible WATCH/BUY, reason code,
  breach timestamp e stati zona;
- script diagnostico aggiunto: `acdc/scripts/diagnose-a1-buy-blockers.sh`, `DIAGNOSTIC_ONLY`, richiede
  `MYSQL_PASSWORD` o `MYSQL_HFT_PASSWORD`, non avvia PAPER.

RUN PAPER A1:

- RUN 119, avviata da `/management`, ha esposto un blocker residuo certo:
  `bb_advice_freshness_contract_pass=0` per target economico `0`, mascherato da
  `WATCH_OPENED_WAITING_BUY_CONTRACT`; classificazione `VALID_DIAGNOSTIC_A1_EVIDENCE`, non finanziaria;
- dopo correzione del gate comune, RUN 120 e' partita da `/management` con generation
  `management-rolling-20260704T054632Z`, fermata da `/management/actions/PAPER_STOP`;
- RUN 120: `STOPPED`, `entryDecisions=720`, `acceptedBuys=0`, `positions=0`, `openPositions=0`;
- distribuzione trigger RUN 120:
  - `WATCH_REENTRY_OVEREXTENDED=295`;
  - `WATCH_WAITING_BREAKOUT_PERCENT_B=286`;
  - `WATCH_REENTRY_AGE_EXPIRED=24`;
  - `WATCH_WAITING_REENTRY_LOWER_BREACH=24`;
  - `WATCH_REENTRY_MIDDLE_SLOPE_BLOCKED=21`;
  - `WATCH_WAITING_REENTRY_RECOVERY=20`;
  - `WATCH_WAITING_BREAKOUT_EXPANSION=4`;
  - `WATCH_BREAKOUT_MIDDLE_SLOPE_BLOCKED=2`;
- classificazione RUN 120:
  - `VALID_STRATEGIC_EVIDENCE` per A0 readiness e metadata A1;
  - `NEGATIVE_OPERATIONAL_SIGNAL` per assenza di BUY con trigger fail granulari;
  - `INCONCLUSIVE` per performance finanziaria per assenza di BUY/SELL.

Stato A2.1 implementato e verificato il 2026-07-04:

- DocBrown `BlankRemCandidateService` non usa piu' `bb_middle_slope >= 0` come hard-blocker del setup
  `BB_SQUEEZE_BREAKOUT_LONG`.
- La definizione breakout A2 usata dalla selection e' allineata al documento scientifico:
  upper breach, `%B >= 1` nel trigger runtime, `bb_bandwidth_delta > 0` e `bb_expansion > 0`; `middle_slope` resta
  feature audit/context, non condizione Bollinger primaria.
- Verifica regressiva sul batch fallito `management-rolling-20260704T154310Z`:
  `breakout_old=863`, `breakout_a2=1552`, `reentry=2258`.
- Test DocBrown mirato `BlankRemCandidateServiceTest`: `12/12` passati.
- DocBrown buildato e redeployato nel container `docbrown`.
- Nuovo ciclo `/management` dopo deploy:
  `management-rolling-20260704T180407Z`, `PASS_CANDIDATE`, selected
  `feature:bb_setup=squeeze_breakout_long`, `PROMOTED=30`.
- RUN PAPER 122 avviata da `/management`, stop-buy e stop governati da `/management`, poi drain completato:
  `COMPLETED`, `positions=5`, `openPositions=0`, `closedPositions=5`.
- Telegram BUY/SELL idempotente verificato su RUN 122: `buy_notified=5`, `sell_notified=5`, una notifica per posizione
  aperta/chiusa.
- RUN 122 PnL finale: `net_profit_quote=-0.415733138049780000`. Classificazione Consiglio:
  `VALID_STRATEGIC_EVIDENCE` per fix A2.1 e ripristino BUY/SELL; `NEGATIVE_FINANCIAL_SIGNAL` sul campione RUN 122.
- Warning residuo da analizzare dopo RUN 122: `A0_DECISION_GAP_TOO_WIDE` compare in `a1BuyDiagnostics`; non blocca il
  fix A2.1, ma va trattato come prossimo punto di qualita' dati/cadence.

## Stato Live Verificato

Ultimo stato consolidato prima dell'implementazione Context V1:

```text
paperRunning = false
openPositions = 0
automation = stopped
REAL = vietata
```

Le RUN PAPER 82-91 hanno prodotto 9 trade reali: 3 WIN, 6 LOSS, netto `-0.5464600973`. Il filtro diagnostico
Context V1 avrebbe tenuto 2 trade con netto `-0.1464585003`, migliorando il campione ma restando negativo.

## Moduli

- `hft-common`: charter, contratti, enum, costanti e reason Context V1.
- `docbrown`: produzione candidate/advice Bollinger con feature context.
- `acdc`: WATCH, BUY, SELL, forensics PAPER e ContextGateAudit.
- `kenshiro`: orchestrazione `/management`, strategy family e diagnostica.
- `hft-fe`: cockpit `/management`.
- `influxer`: garantisce OHLCV/microbar; nel blocco A0 deve distinguere ruoli bucket, gap e synthetic backfill.

## Prossimo Step Operativo

1. Prima di ogni nuova PAPER leggere `/management/state` e richiedere `1m_alignment_ready=true`, `a0Blockers=[]`,
   `blockers=[]`, `paperRunning=false`, `openPositions=0`.
2. A1 e' implementato: ogni nuova PAPER deve essere analizzata tramite `a1BuyDiagnostics` e `/trades` prima di
   qualunque giudizio finanziario.
3. Avviare PAPER solo tramite `/management`, mai da script.
4. Dopo ogni PAPER classificare separatamente:
   - evidenza A0/readiness;
   - evidenza WATCH/BUY;
   - evidenza SELL/forensics;
   - evidenza finanziaria.
5. Se una RUN non apre BUY/SELL, classificarla `INCONCLUSIVE` per performance anche se valida come evidenza tecnica A0;
   se trigger full-pass = 0, classificarla anche `NEGATIVE_OPERATIONAL_SIGNAL` per BUY trigger.
6. Monitorare densita' replay microbar: `interval_seconds` effettivo e `max_gap_seconds` devono essere letti come
   diagnostica di timing, non come fonte strategica.
