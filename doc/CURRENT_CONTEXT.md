# Current Context

Ultimo aggiornamento: 2026-07-04 CEST.

Snapshot operativo corrente del workspace `/home/mbc/Documenti/ws/java/hft`.

## Gerarchia Documentale

1. `hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`
2. `hft-common/doc/archived/REALTIME_BOLLINGER_ADX_NO_ML_PLAN.md` come piano candidato non ancora implementato.
3. `hft-common/doc/archived/REALTIME_BB_ADX_PARAMETER_VERIFICATION_PLAN.md` come piano tecnico per OHLC-Wilder e
   verifica parametrica completa.
4. `hft-common/doc/archived/REALTIME_BB_ADX_OPTUNA_PARAMETER_SEARCH_PLAN.md` come piano offline di ricerca
   parametrica Optuna/Pareto.
5. `hft-common/doc/archived/REALTIME_BB_ADX_NATIVE_F4E954_STRATEGY_INTEGRATION_PLAN.md` come piano di integrazione
   della strategia target selezionata da Melo.
6. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`
7. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_A1_LITERATURE_ALIGNMENT_PLAN.md`
8. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`
9. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_PLAN.md`
10. `hft-common/doc/CURRENT_CONTEXT.md`
11. `hft-common/doc/STRATEGIC_REM_HANDOFF.md`
12. `hft-common/doc/archived/BOLLINGER_ONLY_PLAN.md`

Se i documenti confliggono, prevale il charter; poi
`archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`; poi
`archived/BOLLINGER_CONTEXT_V1_A1_LITERATURE_ALIGNMENT_PLAN.md`; poi
`archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`; poi `archived/BOLLINGER_CONTEXT_V1_PLAN.md`.

I documenti sotto `doc/acdc`, `doc/docbrown`, `doc/influxer`, `doc/hft-fe` e `doc/kenshiro` sono memoria storica di
sessione. Possono spiegare perche' esistono microbar, REAL, SHADOW o reversal legacy, ma non sono fonte operativa se
contraddicono la gerarchia sopra.

Nota 2026-07-05: `REALTIME_BOLLINGER_ADX_NO_ML_PLAN.md` e' il documento candidato per sostituire l'AS-IS legacy con
un path realtime rule-based senza DocBrown/ML nel decision path. Non e' ancora implementato e non autorizza PAPER RT
finche' gli interventi del piano non sono completati.

Nota 2026-07-05: `REALTIME_BB_ADX_PARAMETER_VERIFICATION_PLAN.md` dettaglia lo sviluppo tecnico successivo:
ADX/DMI/ATR Wilder OHLC, soglie non ufficiali parametrizzate, matrice fattoriale completa da 6,220,800 combinazioni e
replay storico causale. Non autorizza modifiche runtime o nuove RUN: e' il documento da verificare prima degli sviluppi.

Nota 2026-07-05: l'implementazione V107 ha materializzato su MySQL `acdc_rt_parameter_verification_profile` con
`6,220,800` profili `PENDING_REPLAY` e hash distinti. Il precedente totale `1,036,800` era il conteggio di ogni
sottoinsieme `rsi_cap_mode x loss_cap_mode`, non della matrice completa P1-P12.

Nota 2026-07-05: `REALTIME_BB_ADX_OPTUNA_PARAMETER_SEARCH_PLAN.md` definisce il laboratorio offline Python per
selezionare profili candidati dalla matrice tramite Optuna constrained multi-objective optimization, TPE/MOTPE,
cross-check NSGA-II, walk-forward e controlli anti-overfitting. Il laboratorio non e' parte del normale ciclo trading:
non avvia PAPER, non modifica runtime e non autorizza RUN senza revisione del Consiglio.

Nota 2026-07-08: `REALTIME_BB_ADX_NATIVE_F4E954_STRATEGY_INTEGRATION_PLAN.md` adotta il profilo Melo nativo
`f4e954f0b552ce864535fc2b` come strategia target `REALTIME_BB_ADX_NATIVE_F4E954`. Il profilo e' il migliore tra
`51.840` configurazioni native Bollinger verificate esaustivamente, ma non era promuovibile per trade aperti e assenza
di chiusure validation/holdout. L'integrazione e' quindi PAPER-only e richiede valori centralizzati in `hft-common`,
parita' Melo/ACDC e rimozione dei gate legacy non inclusi nel profilo.

Nota 2026-07-05, implementazione RT:

- `REALTIME_BB_ADX_V1` implementato come path PAPER rule-based in ACDC, senza DocBrown/ML/advice nel decision path RT;
- ACDC calcola Bollinger 20/2, Keltner/TTM squeeze, ADX/DMI, ATR14/ATR22, volume ratio e OBV slope sugli snapshot
  della cadence decisionale dichiarata;
- Kenshiro espone `rtReady`, `rtBlockers`, `rtDiagnostics` e action `/management` `PAPER_REALTIME_START`;
- hft-fe mostra readiness RT e action PAPER RT;
- config MySQL `rt.*` introdotta da migrazioni ACDC V100/V101;
- deploy verificato su `acdc-vpn`, `kenshiro-local`, `hft-fe-local`;
- RUN PAPER RT 130: 7 BUY/7 SELL, net `-0.383745952491981200`; ha esposto falsi positivi range flat con
  volume/ATR/ADX zero;
- correzione V101: range reentry richiede contesto vivo e edge minimo verso middle band;
- RUN PAPER RT 131: 3 BUY breakout-only/3 SELL, net `-0.149999999561200000`; i range flat sono stati eliminati, ma i
  breakout hanno chiuso a timeout allo stesso prezzo pagando solo fee;
- classificazione Consiglio: implementazione end-to-end riuscita, evidenza finanziaria iniziale negativa;
- contenimento 2026-07-05: `rt.strategy.enabled=false`, nessuna PAPER attiva, nessuna posizione aperta;
- correzione V102: il breakout RT ora richiede RSI context valido, assenza di chaos/volume spike, follow-through
  positivo sull'ultima candela (`last_close_return`) e distanza massima dalla upper band (`upper_band_edge_pct`) prima
  del BUY. Obiettivo: impedire BUY breakout statici/estesi che non hanno edge netto e chiudono a timeout pagando solo
  fee;
- nuova PAPER RT non autorizzata finche' build/deploy V102 e readiness MySQL non risultano verificati.
- RUN PAPER RT 132 post-V102: breakout sporchi bloccati, ma 2 BUY range hanno perso net
  `-0.097676967607800000`; entrambi avevano `last_close_return < 0` e `-DI > +DI`, quindi non erano veri recuperi;
- correzione V103: se `rt.entry.range.percent_b_recovery_required=true`, il BUY range richiede
  `last_close_return > rt.entry.range.min_last_close_return` prima dell'ingresso. Default `0`, cioe' serve una candela
  decisionale positiva dopo lo stress sotto banda.
- RUN PAPER RT 133 post-V103: 2 BUY range/2 SELL, net `-0.233721663969951900`; entrambi i BUY avevano
  `last_close_return > 0`, ma `+DI < -DI`, `max_net_return=0` e uscita `RT_EXIT_RANGE_LOSS_CAP`;
- correzione V104 codice: `rt.entry.range.minus_di_dominance_block=true` ora blocca ogni BUY range con `-DI > +DI`,
  non solo quando ADX supera `rt.entry.range.adx_max`. Una reentry range e' autorizzabile solo se il recupero non resta
  direzionalmente bearish.
- RUN PAPER RT 134 post-V104: 1 BUY breakout/1 SELL, net `-0.078973317632300000`; il BUY ERAUSDC aveva
  `last_close_return=0.0011614402`, `upper_band_edge_pct=0.0005803831`, `rsi14=75`, `regime_chaos=0`,
  `volume_spike_risk=0`, ma `max_net_return=0` e uscita `RT_EXIT_BREAKOUT_CHANDELIER_STOP`;
- contenimento finale 2026-07-05 00:32 UTC: `rt.strategy.enabled=false`, nessuna PAPER attiva, nessuna posizione aperta;
- classificazione Consiglio: V102/V103/V104 hanno migliorato il filtraggio dei falsi positivi evidenti, ma
  `REALTIME_BB_ADX_V1` no-ML resta `NEGATIVE_OPERATIONAL_SIGNAL` su evidenza PAPER. Non autorizzare altre PAPER RT
  finche' non esiste una nuova ipotesi documentata e verificabile; evitare ulteriori micro-soglie incrementali senza
  backtest/replay causale sui casi 126/130/131/132/133/134.
- decisione Consiglio 2026-07-05 A3: nuovo esperimento coerente su barre decisionale `20s` per WATCH/BUY/SELL
  strategica, calcolate da `binance-microbar` reale 5s via aggregazione 20s chiusa. Il documento scientifico
  `archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md` esplicita i 7 disallineamenti e i valori reali del profilo 20s.
  Nessuna RUN e' valida se `decision_interval_seconds != 20`, se il source bucket cade su `binance` 1m, o se
  `decision_synthetic_backfill=1`.
- RUN PAPER RT 135 post-A3 20s: 1 BUY breakout MIRAUSDC, MFE `0.010974025974025975`, ma SELL a
  `RT_EXIT_BREAKOUT_FALSE_BREAKOUT` con netto negativo `-0.049999999957600000`. Classificazione:
  `VALID_STRATEGIC_EVIDENCE` per BUY/provenance 20s, `NEGATIVE_SELL_CAPTURE_SIGNAL` per capture breakout mancante.
  Correzione A3.1 SELL: `RT_EXIT_BREAKOUT_UPPER_BAND_PROFIT` quando una posizione breakout gia' aperta ha
  `max_net_return >= min_executable_entry_edge`, `net_return > 0` e `%B < 1.0`.
- RUN PAPER RT 136 post-V105: 1 BUY range MIRAUSDC, `volume_ratio_1m_20m=0.3091023688218919`,
  `volume_confirmation=0`, `max_net_return=0`, uscita `RT_EXIT_RANGE_LOSS_CAP`, netto
  `-0.097301136264000000`. Classificazione: `VALID_STRATEGIC_EVIDENCE` per provenance 20s,
  `NEGATIVE_RANGE_LIQUIDITY_SIGNAL` per BUY range su barra troppo scarica. Correzione A3.2 range:
  `rt.entry.range.min_volume_ratio=0.50`, reason `RT_ENTRY_BLOCKED_RANGE_VOLUME`.
- RUN PAPER RT 137 post-V106: 2 BUY breakout/2 SELL, 2 win, netto `+0.170766512778600000`; entrambe le uscite sono
  `RT_EXIT_BREAKOUT_UPPER_BAND_PROFIT`. Trade: `2ZUSDC` netto `+0.018237704883600000`,
  `EIGENUSDC` netto `+0.152528807895000000`. `RT_ENTRY_BLOCKED_RANGE_VOLUME=3619` conferma che il floor range ha
  bloccato le barre scariche. Notifiche Telegram idempotenti: `buy_notified=2`, `sell_notified=2`. Classificazione:
  `VALID_STRATEGIC_EVIDENCE`, `POSITIVE_SELL_CAPTURE_SIGNAL`, `POSITIVE_FINANCIAL_SIGNAL_SMALL_SAMPLE`.

## Vincoli Correnti

- REAL vietata.
- PAPER solo da `/management`.
- La sorgente advice runtime resta `hft.acdc_live_bb_advice`.
- Bollinger resta il segnale centrale: setup e trigger sono obbligatori.
- A2 corregge A0/A1: la letteratura Bollinger non impone 1m. La cadence decisionale e' un parametro dichiarato e
  deve essere identica in DocBrown, WATCH/BUY, SELL strategica e forensics.
- Profilo operativo A3 corrente: `bb.decision.interval_seconds=20`, `decision_source_bucket=binance-microbar`,
  `decision_synthetic_backfill=0`.
- Profilo 1m resta ammesso solo se dichiarato e coerente end-to-end.
- Context V1 aggiunge regime, trend, momentum, volume e risk come feature contrattuali esplicite.
- WATCH compra solo se passano trigger Bollinger setup-specifico e gate Context V1.
- SELL A3 resta setup-specifica Bollinger: range cattura middle/upper band; breakout protegge profitto quando, dopo
  MFE positivo minimo, il prezzo rientra sotto la upper band con netto ancora positivo.
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

AS-IS codice verificato prima dell'intervento A0, memoria storica di regressione superata da A2:

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

Implementazione A0 locale verificata con build, memoria storica del profilo 1m:

- `hft-common` espone costanti A0, bucket, decision/entry/SELL/replay metadata, classificazioni evidenza e blocker;
- `InfluxTick` trasporta `syntheticBackfill`;
- influxer marca `synthetic_backfill=false` sulle scritture normali e `true` sui microbar di backfill sintetico;
- nel profilo A0 storico DocBrown leggeva indicatori/live-score da `binance` 1m chiuso, con lookback decisionale almeno
  sufficiente per EMA50 e volume ratio 1m/20m, e pubblicava metadata decisionali numerici nel contract advice;
- nel profilo A0 storico ACDC WATCH/BUY leggeva snapshot decisionali da `binance` 1m chiuso e falliva chiusa sui blocker
  A0;
- nel profilo A0 storico ACDC SELL strategica usava lo stesso snapshot decisionale 1m chiuso e falliva chiusa sui blocker
  A0; A2 ha poi riammesso microbar 5s reale come cadence strategica dichiarata;
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

Stato SELL post-RUN 122:

- Analisi Consiglio su SOL/XRP/BTC/DOGE/ADA: tutte le uscite RUN 122 sono state `EXIT_BB_LOSS_CAP`.
- La SELL attiva copre target, trailing, loss-cap e timeout, ma non implementa ancora l'invalidation/capture
  setup-specifica richiesta dal documento scientifico (`SELL/ACDC: esce per target/loss/invalidation`).
- Il semplice tag della upper band non deve diventare segnale SELL autonomo: la regola ufficiale Bollinger vieta di
  trattare il tag banda come signal isolato.
- Correzione documentale approvata dal Consiglio in
  `archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`: introdurre SELL setup-specifica solo su posizioni gia' aperte:
  `EXIT_BB_REENTRY_CAPTURE`, `EXIT_BB_REENTRY_FAILED`, `EXIT_BB_BREAKOUT_FAILED`, `EXIT_BB_BREAKOUT_PROTECT`.
- Reentry capture deve riconoscere la middle band/SMA20 come primo target naturale della mean-reversion e area `0.80`
  come capture alta, sempre con `net_return >= 0` perche' il netto include gia' le fee.
- Breakout protect deve usare Chandelier/ATR come protezione primaria delle posizioni vincenti; `%B < 0.80` e' solo
  conferma Bollinger di perdita della fascia alta, non trigger isolato.
- Le nuove regole non sono nuovi blocker BUY; devono ridurre uscite tardive a loss-cap e impedire che target nullo
  lasci la posizione affidata solo a loss-cap/timeout.
- Nessuna nuova PAPER come evidenza finanziaria prima dell'implementazione/deploy/verifica della SELL setup-specifica
  mancante.

Stato A2.2 BUY economic feasibility + SELL setup-specifica implementato e verificato il 2026-07-04:

- hft-common espone operator/constant condivise per SELL setup-specifica e gate economico BUY.
- DocBrown `BlankRemCandidateService` pubblica `bb_advice_economic_safe_pass`, `min_executable_entry_edge`,
  `q10_positive_max_net_return`, `entry_friction_net_return` e calcola `bb_advice_paper_buy_eligible` come:
  trigger Bollinger setup-specifico AND gate economico.
- ACDC `OutcomeQualityModelService` e `PreBuyWatchService` ricalcolano il gate a runtime prima del BUY; il path finale
  non puo' piu' trasformare un trigger Bollinger valido in BUY se `bb_advice_economic_safe_pass=0`.
- Reason nuovo runtime: `WATCH_ECONOMIC_EDGE_BLOCKED`.
- ACDC SELL implementa:
  `EXIT_BB_REENTRY_CAPTURE`, `EXIT_BB_REENTRY_FAILED`, `EXIT_BB_BREAKOUT_FAILED`, `EXIT_BB_BREAKOUT_PROTECT`.
- MySQL operativo dopo deploy: Flyway ACDC v97 applicata; EXIT guards attivi a priorita' 11-14; no-MFE resta
  `DISABLED`.
- Test completati:
  - `hft-common`: `mvn -q -DskipTests install`;
  - `docbrown`: `mvn -q test`;
  - `acdc`: `mvn -q test`.
- Container redeployati/verificati: `docbrown`, `acdc-vpn`.

RUN 123-126:

- RUN 123: `COMPLETED`, 9 posizioni, PnL `-0.391383616078811400`; SELL setup-specifica operativa ma BUY ancora
  ammetteva target zero/edge sotto costo.
- RUN 124: `COMPLETED`, 3 posizioni, PnL `-0.157064235827783000`; conferma diagnostica del problema di ingressi
  economicamente deboli.
- RUN 125: `COMPLETED`, 3 posizioni, PnL `-0.171742968719700000`; classificazione
  `VALID_DIAGNOSTIC_EVIDENCE`: ha dimostrato il bug residuo in `PreBuyWatchService`, dove
  `bb_advice_paper_buy_eligible` veniva riscritto con il solo `triggerAudit.passed()`.
- RUN 126: `STOPPED` da `/management` dopo stop-buy e nessuna posizione aperta, 1 BUY/1 SELL, PnL
  `+0.053202478245000000`, `buy_notified=1`, `sell_notified=1`.
- RUN 126 entry: `EPICUSDC`, `bb_advice_economic_safe_pass=1`, `bb_advice_paper_buy_eligible=1`,
  `bb_target_net_return=0.007660251046025105`, `sell_target_zero_take_profit_disabled=0`.
- RUN 126 exit: `EXIT_BB_REENTRY_CAPTURE`.
- RUN 126 blocker BUY:
  `WATCH_ECONOMIC_EDGE_BLOCKED=47`, prova che il gate economico filtra senza azzerare i BUY utili.
- Classificazione RUN 126: `VALID_STRATEGIC_EVIDENCE` tecnica per A2.2 e SELL setup-specifica; evidenza finanziaria
  positiva ma su campione minimo, quindi non sufficiente da sola per promozione strategica.

## Stato Audit Cadence/Costanti MS968

Verifica Consiglio 2026-07-04:

- audit ripetuto due volte su ML/DocBrown, ACDC WATCH/BUY, ACDC SELL/forensics, script diagnostici, Kenshiro,
  hft-fe e DB runtime;
- valori Bollinger ufficiali centralizzati in `hft-common`:
  BB20, deviazione standard 2, `%B` lower/middle/upper, reentry max `0.80`, breakout min `%B >= 1`;
- default operativi condivisi centralizzati in `hft-common`:
  cadence 60s/5s, candle count minimo 60, max gap 90s, staleness 120s, min executable entry edge `0.0005`,
  default SELL setup-specifici;
- FE mantiene solo soglie visuali/UI nella classe TypeScript locale `ManagementUiConstants`;
- migration ACDC V98 aggiunta/applicata: `bb.decision.interval_seconds=5` e descrizioni A2 per bucket decisionale;
- script diagnostici ACDC riallineati ad A2 corrente: `DIAGNOSTIC_ONLY`, `binance-microbar`, `interval_seconds=5`,
  `synthetic_backfill=false`;
- `STRATEGIC_REM_HANDOFF.md` riallineato: `1m_alignment_ready` resta alias storico del readiness cadence, non obbligo
  di profilo 1m;
- deploy verificato su `docbrown`, `acdc-vpn`, `kenshiro-local`, `hft-fe-local`;
- MySQL operativo conferma Flyway ACDC V98 success, `bb.decision.interval_seconds=5`, `market.microbar.seconds=5`,
  `market.influx.microbar_bucket=binance-microbar`;
- `/management/state` conferma `globalStatus=BB_READY`, `bbReady=true`, `oneMinuteAlignmentReady=true`,
  `a0Blockers=[]`, `blockers=[]`, `paperRunning=false`, `openPositions=0`, advice corrente con
  `decision_source_bucket=binance-microbar`, `decision_interval_seconds=5`, `decision_synthetic_backfill=0`.

## Stato Context Literature Alignment MS969

Verifica Consiglio 2026-07-05:

- Gli indicatori Context V1 restano parte del contratto: EMA, RSI, volume ratio, ATR/risk e regime non vengono rimossi
  perche' filtrano falsi positivi del Bollinger puro.
- Il loro uso e' vincolato come conferma/rischio, non come sostituto del trigger Bollinger.
- Soglie operative centralizzate in `hft-common`:
  - breakout RSI: `50 <= RSI <= 75`;
  - reentry RSI cap: `RSI <= 62`;
  - breakout volume ratio min: `1.30`;
  - squeeze: `bb_bandwidth_percentile <= 0.20`, non BandWidth assoluto;
  - range: `abs(ema50_slope) <= 0.0015`, `bb_bandwidth_percentile <= 0.70`, `atr_pct <= 0.015`;
  - chaos: `atr_pct > 0.025` oppure `volume_ratio > 3.00`.
- DocBrown genera `contract_min_breakout_rsi` con floor `50` e cap `75`, mantenendo l'adattivita' storica solo dentro
  la banda letteratura-allineata.
- ACDC e DocBrown generano feature live/storiche con gli stessi default condivisi.
- MySQL Flyway ACDC V99 aggiunge/aggiorna le chiavi runtime:
  `bb.context.breakout.min_rsi`, `bb.context.breakout.max_rsi`,
  `bb.context.breakout.min_volume_ratio`, `bb.context.reentry.max_rsi`,
  `bb.context.reentry.max_percent_b`.
- Deploy verificato su `docbrown` e `acdc-vpn`; MySQL operativo conferma V99 success.
- AUTO PAPER post-MS969:
  - generation fresca: `management-rolling-20260704T222720Z`;
  - execution PAPER: `128`;
  - stato finale governato: `paperRunning=false`, `openPositions=0`, `automationStatus=STOPPED`;
  - posizioni: 3 BUY / 3 SELL, netto `-0.473849438024239200`;
  - notifiche Telegram: 3 BUY notificate, 3 SELL notificate, una per posizione;
  - `HMSTRUSDC` breakout: `contract_min_breakout_rsi=50`, `contract_max_breakout_rsi=75`,
    `contract_min_volume_ratio=1.3`, `economic=1`, uscita `EXIT_BB_LOSS_CAP`, netto
    `-0.136071223434139200`;
  - reentry: 2 posizioni, uscite `EXIT_BB_LOSS_CAP` e `EXIT_BB_REENTRY_FAILED`, netto complessivo negativo;
  - tutti i trade hanno `max_net_return=0`, quindi non c'e' stata finestra positiva persa dal trailing.
- Classificazione Consiglio:
  - `VALID_STRATEGIC_EVIDENCE` tecnica per allineamento soglie e ripristino BUY post-MS969;
  - `NEGATIVE_FINANCIAL_SIGNAL` per execution 128;
  - punto successivo non deve essere nuova soglia BUY casuale: serve analisi forensics su perche' i BUY economic-safe
    entrano senza MFE e finiscono a loss/failure.

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

Aggiornamento operativo 2026-07-08:

- Implementata e deployata la strategia `REALTIME_BB_ADX_NATIVE_F4E954` derivata da Melo/Optuna.
- Profilo nativo centralizzato in `hft-common`: hash `f4e954f0b552ce864535fc2b`, `bb_period=20`,
  `bb_stddev_multiplier=2.00`, soglie Bollinger relative e loss cap statico `0.0050`.
- ACDC Flyway operativo migrato a V108; MySQL conferma `rt.strategy.family=REALTIME_BB_ADX_NATIVE_F4E954`.
- PAPER execution `140` avviata da `/management/actions/PAPER_REALTIME_START` e fermata da
  `/management/actions/PAPER_STOP`.
- Esito execution `140`: `STOPPED`, budget `100`, PnL `0`, posizioni `0`.
- Decisioni entry: `1400` REJECT, tutte `RT_ENTRY_BLOCKED_DATA_QUALITY`.
- Classificazione Consiglio aggiornata dopo audit MS985: `INVALID_STRATEGIC_EVIDENCE`.
  La run 140 ha esposto un bug semantico: il path nativo usava ancora quality gate del vecchio runtime
  (`decision_max_gap_seconds`/Wilder readiness) prima dei gate Melo, quindi applicava vecchia implementazione con nuovi
  parametri.
- Correzione MS985: `RealtimeDecisionService` usa readiness nativa dedicata, non blocca piu' per gap diagnostico o
  `ohlc_wilder_indicators`, e in SELL preferisce i valori congelati nel `policyJson` della posizione.
- PAPER execution `141` post-correzione: `COMPLETED`, nessuna posizione aperta, budget finale
  `99.843014361840330000`, PnL `-0.156985638159670000`.
- Execution `141` decisioni native: 5 BUY (`3` `RT_NATIVE_RANGE_REENTRY`, `2` `RT_NATIVE_BREAKOUT`) e 5 SELL
  (`1` `RT_NATIVE_RANGE_MIDDLE_CAPTURE`, `1` `RT_NATIVE_BREAKOUT_FALSE_BREAKOUT`, `3` `RT_NATIVE_TIMEOUT`).
  Nessuna entry e' stata respinta da `RT_ENTRY_BLOCKED_DATA_QUALITY`.
- Classificazione Consiglio execution `141`: `VALID_NATIVE_PATH_EVIDENCE`, `NEGATIVE_FINANCIAL_SIGNAL_SMALL_SAMPLE`.
  La strategia dedicata ora viene realmente esercitata; l'esito economico forward iniziale e' negativo.
- Report cadence 2026-07-08:
  `archived/REALTIME_BB_ADX_NATIVE_F4E954_CADENCE_SENSITIVITY_REPORT.md`.
  Classificazione: `CADENCE_DIAGNOSTIC_ONLY`, `MIXED_INCONCLUSIVE_ON_1M_FILTER`, `DO_NOT_PROMOTE`, `DO_NOT_DISCARD`.
  La run 141 non basta a provare che 1m filtri rumore: per range il profilo resta interessante, per breakout il rischio
  dominante e' ritardo/assenza di MFE sufficiente ad armare protect. Prossimo step tecnico: replay diagnostico causale
  20s vs 60s sugli stessi raw candles/simboli della execution 141.

## Moduli

- `hft-common`: charter, contratti, enum, costanti e reason Context V1.
- `docbrown`: produzione candidate/advice Bollinger con feature context.
- `acdc`: WATCH, BUY, SELL, forensics PAPER e ContextGateAudit.
- `kenshiro`: orchestrazione `/management`, strategy family e diagnostica.
- `hft-fe`: cockpit `/management`.
- `influxer`: garantisce OHLCV/microbar; nel blocco A0 deve distinguere ruoli bucket, gap e synthetic backfill.

## Prossimo Step Operativo

1. Prima di ogni nuova PAPER leggere `/management/state` e richiedere readiness cadence true tramite
   `oneMinuteAlignmentReady=true`/alias storico `1m_alignment_ready=true`, `a0Blockers=[]`, `blockers=[]`,
   `paperRunning=false`, `openPositions=0`.
2. Verificare che il profilo decisionale corrente sia coerente end-to-end: per A2 corrente
   `bb.decision.interval_seconds=5`, `decision_source_bucket=binance-microbar`, `decision_synthetic_backfill=0`;
   il profilo 1m e' ammesso solo se dichiarato e coerente su DocBrown, WATCH/BUY, SELL e forensics.
3. A1/A2.2/SELL setup-specifica sono implementati: ogni nuova PAPER deve essere analizzata tramite
   `a1BuyDiagnostics`, `/trades`, reason SELL e policy economica prima di qualunque giudizio finanziario.
4. Avviare PAPER solo tramite `/management`, mai da script.
5. Dopo ogni PAPER classificare separatamente:
   - evidenza A0/readiness;
   - evidenza WATCH/BUY;
   - evidenza SELL/forensics;
   - evidenza finanziaria.
6. Se una RUN non apre BUY/SELL, classificarla `INCONCLUSIVE` per performance anche se valida come evidenza tecnica A0;
   se trigger full-pass = 0, classificarla anche `NEGATIVE_OPERATIONAL_SIGNAL` per BUY trigger.
7. Monitorare densita' replay microbar: `interval_seconds` effettivo e `max_gap_seconds` devono essere letti come
   diagnostica di timing, non come fonte strategica.
