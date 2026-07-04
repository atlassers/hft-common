# Realtime Bollinger ADX No-ML Plan

Data: 2026-07-05.

## Stato Del Documento

Questo documento progetta il nuovo ciclo strategico realtime rule-based.

Decisione richiesta:

```text
AS-IS = legacy.
DocBrown/ML escluso dal path decisionale realtime.
DocBrown non viene rimosso dal workspace, ma non puo' selezionare, generare, ordinare, bloccare o confermare BUY/SELL.
```

Questo documento non implementa codice. Deve diventare la traccia operativa degli sviluppi successivi: durante
l'implementazione Codex deve seguire i punti qui sotto e non inventare regole, soglie, enum o path alternativi.

## Fonti Esterne Da Cui Deriva La Strategia

Le fonti non vengono usate come garanzia di profitto. Vengono usate per evitare soglie arbitrarie e per assegnare a
ogni indicatore un ruolo tecnico coerente.

- John Bollinger, Bollinger Band rules: https://www.bollingerbands.com/bollinger-band-rules
  - Le bande definiscono alto/basso relativo.
  - Le decisioni devono usare indicatori complementari non collineari: momentum, volume, sentiment, open interest.
  - Un tag della banda non e' da solo un segnale BUY/SELL.
- StockCharts, Bollinger Band Squeeze:
  https://chartschool.stockcharts.com/table-of-contents/trading-strategies-and-models/trading-strategies/bollinger-band-squeeze
  - Squeeze = volatilita' bassa e bande strette.
  - Il break direzionale dopo compressione e' il segnale operativo, non la compressione isolata.
- StockCharts, TTM Squeeze:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/ttm-squeeze
  - Squeeze quando Bollinger Bands sono dentro Keltner Channels.
  - Lo squeeze "fires" quando le Bollinger Bands escono dai Keltner Channels.
- Interactive Brokers Campus, ADX/DMI:
  https://www.interactivebrokers.com/campus/trading-lessons/adx-dmi/
  - ADX misura forza del trend.
  - +DI sopra -DI indica pressione direzionale rialzista.
- Investopedia, ADX:
  https://www.investopedia.com/articles/trading/07/adx-trend-indicator.asp
  - ADX sotto circa 20 indica mercato laterale/debole.
  - ADX sopra circa 25 indica trend forte.
- StockCharts, On Balance Volume:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/on-balance-volume-obv
  - OBV puo' confermare trend e breakout tramite pressione volume cumulata.
- Chandelier Exit / ATR:
  https://www.barchart.com/education/technical-indicators/chandelier-exit
  - Stop/trailing classico basato su Highest High 22 - ATR(22) * 3 per posizioni long.

## Nuova Strategia Target

Nome operativo proposto:

```text
REALTIME_BB_ADX_V1
```

Pipeline target:

```text
influxer -> ACDC realtime indicators -> WATCH -> BUY -> SELL -> forensics -> Kenshiro/FE
```

Pipeline esclusa:

```text
DocBrown ML -> live-score -> rolling validation -> promotion -> acdc_live_bb_advice -> WATCH
```

Il path target e' realtime rule-based:

- ACDC calcola indicatori su barre chiuse dalla sorgente decisionale dichiarata.
- ACDC genera candidati WATCH internamente, senza advice DocBrown.
- WATCH osserva solo setup realtime validi.
- BUY avviene solo quando il trigger setup-specifico e i filtri confermativi passano.
- SELL usa regole setup-specifiche realtime: capture, invalidation, Chandelier/ATR trailing, loss cap.
- Kenshiro orchestra PAPER e mostra stato/readiness, ma non invoca DocBrown.
- FE mostra il ciclo realtime, non ML/research/promotion.

## Decisione Sul ML

Il ML non viene abolito dal repository.

Viene escluso dal path decisionale di `REALTIME_BB_ADX_V1`:

- non produce candidati BUY;
- non produce `PAPER_ELIGIBLE`;
- non genera `bb_advice_*`;
- non decide freshness/edge/economic safe pass;
- non ordina simboli nel runtime BUY;
- non blocca BUY/SELL;
- non governa PAPER.

DocBrown resta disponibile solo come:

- archivio storico;
- laboratorio offline separato;
- fonte diagnostica non decisionale, se esplicitamente marcata `DIAGNOSTIC_ONLY`;
- riferimento temporaneamente consultabile per migrazione dati, non per logica runtime.

Se in futuro si reintroduce ML, serve un nuovo documento. Non basta riattivare DocBrown o riusare `acdc_live_bb_advice`.

## Setup Ammessi

### Setup 1: Range Mean Reversion

Scopo:

```text
comprare rientro controllato da eccesso inferiore in mercato laterale/debole
```

Indicatori richiesti:

- Bollinger Bands 20, 2;
- %B;
- ADX 14;
- +DI 14;
- -DI 14;
- ATR 14 o ATR 22;
- volume ratio stessa cadence;
- opzionale OBV slope, solo come conferma diagnostica o filtro leggero dichiarato.

Setup:

```text
bb_lower_breach_recent = true
current_percent_b >= 0
current_percent_b <= 0.50
adx14 <= 20 oppure adx14 < 25 e adx14 non crescente forte
minus_di14 non domina plus_di14 in modo estremo
atr_pct sotto soglia chaos
volume non in spike chaos
```

Trigger BUY:

```text
lower breach precedente
AND prezzo rientra dentro le bande
AND percent_b sale rispetto alla barra precedente
AND close corrente > close precedente oppure close >= low + quota minima range barra
AND regime range/debole confermato da ADX
```

SELL:

```text
EXIT_RT_REENTRY_CAPTURE:
  net_return >= 0
  AND (percent_b >= 0.50 OR close >= bb_middle)

EXIT_RT_REENTRY_UPPER_CAPTURE:
  net_return > 0
  AND percent_b >= 0.80

EXIT_RT_REENTRY_FAILED:
  close torna sotto bb_lower
  OR percent_b < 0 dopo rientro
  OR net_loss_quote >= configured_loss_cap

EXIT_RT_TIMEOUT:
  tempo massimo setup-specifico senza capture
```

Note:

- Non usare RSI come hard gate nel nuovo setup base.
- Non usare target DocBrown.
- Non usare `q10_positive_max_net_return`.

### Setup 2: Squeeze Breakout Long

Scopo:

```text
comprare espansione rialzista dopo compressione di volatilita'
```

Indicatori richiesti:

- Bollinger Bands 20, 2;
- Keltner Channels, baseline EMA 20, ATR multiplier dichiarato;
- TTM squeeze state;
- ADX 14;
- +DI 14;
- -DI 14;
- volume ratio stessa cadence;
- OBV slope o OBV breakout confermativo;
- ATR/Chandelier exit.

Setup:

```text
ttm_squeeze_on_recent = true
AND ttm_squeeze_fired_up = true
AND bb_upper_breach = true
AND percent_b >= 1
AND adx14 >= 25 oppure adx14 rising con adx14 >= 20
AND plus_di14 > minus_di14
AND volume_ratio >= 1.30
AND obv_slope >= 0 oppure obv_breakout = true
```

Trigger BUY:

```text
close > bb_upper
AND bollinger exits keltner upward after squeeze
AND bandwidth_delta > 0
AND plus_di14 > minus_di14
AND ADX confirms trend strength/rising
AND volume/OBV confirms
```

SELL:

```text
EXIT_RT_BREAKOUT_PROTECT:
  net_return > 0
  AND close <= chandelier_long_stop

EXIT_RT_BREAKOUT_FAILED:
  close rientra sotto bb_upper
  AND percent_b < 0.80
  AND net_return <= 0 oppure plus_di14 <= minus_di14

EXIT_RT_BREAKOUT_TRAILING:
  max_net_return >= trailing_arm
  AND net_return <= max_net_return - trailing_distance

EXIT_RT_LOSS_CAP:
  net_loss_quote >= configured_loss_cap
```

Note:

- Il semplice tag upper non basta.
- Il volume ratio da solo non basta.
- L'ADX non genera BUY da solo; conferma solo che il breakout non e' laterale.

## Feature Realtime Da Calcolare

Tutte le feature devono essere calcolate su barre chiuse della cadence decisionale dichiarata.

Feature Bollinger:

- `rt_bb_period`;
- `rt_bb_stddev_multiplier`;
- `rt_bb_middle`;
- `rt_bb_upper`;
- `rt_bb_lower`;
- `rt_bb_percent_b`;
- `rt_bb_bandwidth`;
- `rt_bb_bandwidth_delta`;
- `rt_bb_lower_breach`;
- `rt_bb_upper_breach`;
- `rt_bb_reentry_confirmed`.

Feature Keltner/TTM:

- `rt_kc_period`;
- `rt_kc_atr_multiplier`;
- `rt_kc_middle`;
- `rt_kc_upper`;
- `rt_kc_lower`;
- `rt_ttm_squeeze_on`;
- `rt_ttm_squeeze_fired`;
- `rt_ttm_squeeze_fired_up`;
- `rt_ttm_squeeze_fired_down`.

Feature ADX/DMI:

- `rt_adx_period`;
- `rt_adx14`;
- `rt_plus_di14`;
- `rt_minus_di14`;
- `rt_adx_rising`;
- `rt_dmi_bullish`;
- `rt_dmi_bearish`.

Feature volume:

- `rt_volume_ratio`;
- `rt_volume_confirmation`;
- `rt_obv`;
- `rt_obv_slope`;
- `rt_obv_confirmation`.

Feature ATR/risk:

- `rt_atr14`;
- `rt_atr22`;
- `rt_atr_pct`;
- `rt_chandelier_long_stop`;
- `rt_net_return`;
- `rt_max_net_return`;
- `rt_net_loss_quote`.

Feature setup:

- `rt_setup`;
- `rt_trigger`;
- `rt_regime`;
- `rt_watch_required`;
- `rt_buy_eligible`;
- `rt_sell_reason`;
- `rt_decision_source_bucket`;
- `rt_decision_interval_seconds`;
- `rt_decision_candle_state`;
- `rt_decision_synthetic_backfill`.

## Costanti Nuove In hft-common

Creare costanti nuove con prefisso `RT_` o `REALTIME_`. Non riusare costanti `BB_ADVICE`, `LIVE_BB`, `HISTORY_BB`,
`CONTRACT_*` nate per DocBrown.

Enum/stringhe da introdurre:

- strategy family: `REALTIME_BB_ADX_V1`;
- setup:
  - `RT_RANGE_REENTRY_LONG`;
  - `RT_TTM_SQUEEZE_BREAKOUT_LONG`;
- trigger:
  - `RT_REENTRY_CONFIRMED`;
  - `RT_TTM_BREAKOUT_CONFIRMED`;
- regime:
  - `RT_REGIME_RANGE`;
  - `RT_REGIME_SQUEEZE`;
  - `RT_REGIME_TREND_UP`;
  - `RT_REGIME_TREND_DOWN`;
  - `RT_REGIME_CHAOS`;
- BUY reasons:
  - `RT_WATCH_OPENED`;
  - `RT_WATCH_WAITING_REENTRY`;
  - `RT_WATCH_WAITING_TTM_FIRE`;
  - `RT_WATCH_CONTEXT_ADX_BLOCKED`;
  - `RT_WATCH_CONTEXT_DMI_BLOCKED`;
  - `RT_WATCH_CONTEXT_VOLUME_BLOCKED`;
  - `RT_WATCH_CONTEXT_OBV_BLOCKED`;
  - `RT_WATCH_CONTEXT_RISK_BLOCKED`;
  - `RT_WATCH_CONFIRMED_BUY`;
- SELL reasons:
  - `EXIT_RT_REENTRY_CAPTURE`;
  - `EXIT_RT_REENTRY_UPPER_CAPTURE`;
  - `EXIT_RT_REENTRY_FAILED`;
  - `EXIT_RT_BREAKOUT_PROTECT`;
  - `EXIT_RT_BREAKOUT_FAILED`;
  - `EXIT_RT_BREAKOUT_TRAILING`;
  - `EXIT_RT_LOSS_CAP`;
  - `EXIT_RT_TIMEOUT`.

Default tecnici iniziali, da centralizzare:

```text
RT_BB_PERIOD = 20
RT_BB_STDDEV = 2
RT_KELTNER_PERIOD = 20
RT_KELTNER_ATR_MULTIPLIER = 1.5
RT_ADX_PERIOD = 14
RT_ADX_RANGE_MAX = 20
RT_ADX_TREND_MIN = 25
RT_VOLUME_CONFIRMATION_MIN = 1.30
RT_CHANDELIER_PERIOD = 22
RT_CHANDELIER_ATR_MULTIPLIER = 3
```

Nota: i default sono configurazione iniziale documentata da letteratura/prassi tecnica, non prova di edge. Ogni cambio
deve passare da PAPER.

## Interventi hft-common

File da modificare:

- `hft-common/src/main/java/it/mbc/hft/common/rem/constants/RemConstants.java`
- `hft-common/src/main/java/it/mbc/hft/common/rem/enums/RemStrategyFamily.java`
- eventuali enum setup/trigger/guard operator se presenti.

Interventi:

1. Aggiungere `REALTIME_BB_ADX_V1`.
2. Aggiungere setup/trigger/reason RT.
3. Aggiungere feature key RT.
4. Aggiungere operator guard RT solo se il `GuardEvaluator` non basta.
5. Marcare come legacy nel documento, non eliminare subito dal codice:
   - `BOLLINGER_CONTEXT_V1`;
   - `BB_REENTRY_MEAN_REVERSION_LONG`;
   - `BB_SQUEEZE_BREAKOUT_LONG`;
   - `BB_ADVICE_*`;
   - `LIVE_BB_*`;
   - `HISTORY_BB_*`;
   - `CONTRACT_*` DocBrown-derived.

Regola:

```text
Il nuovo runtime non deve leggere costanti legacy per decidere BUY/SELL.
```

## Interventi influxer

Obiettivo:

```text
fornire barre OHLCV chiuse, reali, con volume affidabile e synthetic_backfill esplicito
```

Interventi:

1. Verificare bucket decisionale:
   - `binance-microbar` per profilo 5s reale;
   - `binance` per profilo 60s;
   - vietato `binance-realtime` come fonte indicatori.
2. Garantire campi richiesti:
   - open;
   - high;
   - low;
   - close;
   - volume;
   - observed_at/open_time;
   - candle close semantics;
   - synthetic_backfill.
3. Non introdurre calcolo strategico in influxer.
4. Backfill sintetico resta solo replay/diagnostica; se `synthetic_backfill=true`, ACDC deve bloccare BUY/SELL
   strategici.

File/punti da verificare durante implementazione:

- job scrittura Binance normale;
- job microbar;
- `ShortRetentionBucketBackfillJob`;
- modello `InfluxTick` in `hft-common`.

## Interventi ACDC

### Nuovo servizio indicatori realtime

Creare servizio:

```text
RealtimeIndicatorService
```

Responsabilita':

- calcolare Bollinger;
- calcolare Keltner;
- calcolare TTM squeeze;
- calcolare ADX/DMI;
- calcolare ATR;
- calcolare volume ratio;
- calcolare OBV;
- produrre feature `rt_*`;
- fallire chiuso se il lookback e' insufficiente.

Non deve:

- leggere `LiveBbAdvice`;
- leggere `acdc_live_bb_advice`;
- leggere target/edge DocBrown;
- usare `OutcomeQualityModelService`;
- usare `bb_advice_*`.

### InfluxSnapshotService

File:

```text
acdc/src/main/java/it/mbc/hft/acdc/service/InfluxSnapshotService.java
```

Interventi:

1. Separare il calcolo legacy Context V1 dal nuovo calcolo RT.
2. Aggiungere metodo esplicito:

```text
realtimeSnapshot(symbol)
```

oppure equivalente, purche' il chiamante non possa confonderlo con snapshot DocBrown/advice.

3. Aggiungere metadata:
   - `rt_decision_source_bucket`;
   - `rt_decision_interval_seconds`;
   - `rt_decision_candle_state`;
   - `rt_decision_candle_count`;
   - `rt_decision_max_gap_seconds`;
   - `rt_decision_synthetic_backfill`.

4. Se `synthetic_backfill=true`, impostare blocker feature:

```text
rt_data_quality_pass = 0
rt_data_quality_reason = RT_SYNTHETIC_BACKFILL_BLOCKED
```

### Rimozione path DocBrown dal BUY

File:

```text
acdc/src/main/java/it/mbc/hft/acdc/service/PaperRunService.java
```

Interventi:

1. Sostituire `requireMlReady(...)` con `requireRealtimeReady(...)`.
2. Sostituire `bindExpectedSourceGeneration(execution)` con metadata runtime RT, non generation DocBrown.
3. Sostituire `expectedGenerationSnapshots(...)` con snapshot realtime su universo selezionato.
4. Rimuovere dal path RT:
   - `LiveBbAdviceRepository`;
   - `RemPromotionClass`;
   - `sourceGenerationBlockReason`;
   - `expectedGenerationExhausted`;
   - `bb_advice_paper_buy_eligible`;
   - `bb_advice_economic_safe_pass`.
5. Mantenere:
   - PAPER-only;
   - budget;
   - sizing;
   - telegram idempotente;
   - posizione singola per simbolo;
   - forensics;
   - replay.

### WATCH realtime

File legacy da sostituire o biforcare:

```text
acdc/src/main/java/it/mbc/hft/acdc/service/PreBuyWatchService.java
```

Decisione:

```text
Non estendere PreBuyWatchService con altro legacy.
Creare RealtimeWatchService oppure rifattorizzare PreBuyWatchService in modalita' RT senza dipendenze advice.
```

Interventi richiesti:

1. WATCH apre quando un setup RT e' osservabile:
   - range reentry candidate;
   - TTM squeeze candidate.
2. WATCH conferma solo trigger RT:
   - `RT_REENTRY_CONFIRMED`;
   - `RT_TTM_BREAKOUT_CONFIRMED`.
3. WATCH non legge:
   - `BB_ADVICE_PRE_BUY_WATCH_REQUIRED`;
   - `BB_ADVICE_WATCH_REQUIRED`;
   - `BB_ADVICE_ID`;
   - `BB_ADVICE_RULE_ID`;
   - `sourceGenerationId`;
   - `contract_*` DocBrown.
4. WATCH scrive reason RT.
5. WATCH fallisce chiusa se mancano ADX/DMI/Keltner/OBV/ATR richiesti dal setup.

### DecisionEngine e GuardEvaluator

File:

```text
acdc/src/main/java/it/mbc/hft/acdc/service/DecisionEngine.java
acdc/src/main/java/it/mbc/hft/acdc/service/GuardEvaluator.java
```

Interventi:

1. Introdurre guard RT separati da guard legacy.
2. Non riusare operator `BB_ADVICE_*`.
3. Aggiungere operator solo se necessario:
   - `RT_TTM_SQUEEZE_BREAKOUT_ENTRY`;
   - `RT_RANGE_REENTRY_ENTRY`;
   - `RT_CHANDELIER_EXIT`.
4. In alternativa mantenere operator generici `GTE/LTE/BETWEEN/PRESENT` e incapsulare setup in feature booleane
   calcolate da `RealtimeIndicatorService`.

Scelta consigliata:

```text
Calcolare feature booleane RT nel servizio indicatori.
Usare GuardEvaluator con operator generici.
Evitare operator complessi finche' possibile.
```

### SELL realtime

File:

```text
acdc/src/main/java/it/mbc/hft/acdc/service/GuardEvaluator.java
acdc/src/main/resources/db/migration/*
```

Interventi:

1. Disattivare per strategy family RT:
   - `BB_ADVICE_TAKE_PROFIT_EXIT`;
   - `BB_ADVICE_DYNAMIC_TRAILING_EXIT`;
   - `BB_ADVICE_NO_MFE_DECAY_EXIT`;
   - `BB_ADVICE_POSITIVE_DURATION_EXIT`;
   - `BB_ADVICE_LOSS_CAP_EXIT`;
   - `BB_ADVICE_TIMEOUT_EXIT`;
   - `BB_REENTRY_*` legacy se usa feature `bb_*`/contract legacy.
2. Aggiungere guard RT:
   - `EXIT_RT_REENTRY_CAPTURE`;
   - `EXIT_RT_REENTRY_FAILED`;
   - `EXIT_RT_BREAKOUT_PROTECT`;
   - `EXIT_RT_BREAKOUT_FAILED`;
   - `EXIT_RT_BREAKOUT_TRAILING`;
   - `EXIT_RT_LOSS_CAP`;
   - `EXIT_RT_TIMEOUT`.
3. Ogni SELL deve salvare:
   - setup RT;
   - trigger RT;
   - reason RT;
   - snapshot indicatori RT;
   - source bucket;
   - interval;
   - candle count;
   - synthetic flag.

### DB migration ACDC

Creare nuova migration:

```text
V100__realtime_bb_adx_v1.sql
```

Contenuto:

1. Inserire `bb.strategy.family=REALTIME_BB_ADX_V1` solo se l'implementazione e' completa; durante sviluppo usare
   config separata `rt.strategy.enabled=false`.
2. Inserire config RT:
   - `rt.bb.period=20`;
   - `rt.bb.stddev=2`;
   - `rt.keltner.period=20`;
   - `rt.keltner.atr_multiplier=1.5`;
   - `rt.adx.period=14`;
   - `rt.adx.range.max=20`;
   - `rt.adx.trend.min=25`;
   - `rt.volume.confirmation.min=1.30`;
   - `rt.chandelier.period=22`;
   - `rt.chandelier.atr_multiplier=3`;
   - `rt.loss_cap.max_quote`;
   - `rt.timeout.reentry.seconds`;
   - `rt.timeout.breakout.seconds`.
3. Inserire guard ENTRY RT.
4. Inserire guard EXIT RT.
5. Disabilitare guard legacy solo per profilo RT, non globalmente se Context V1 resta archiviato.

## Interventi Kenshiro

File principali:

```text
kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice/ManagementService.java
kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice/ManagementString.java
kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice/BackofficeDbService.java
```

Interventi:

1. Nuove action:
   - `APPLY_REALTIME_BB_ADX_V1`;
   - `REALTIME_PAPER_START`;
   - `REALTIME_PAPER_STOP_BUY`;
   - `REALTIME_PAPER_STOP`;
   - `REALTIME_REFRESH_DIAGNOSTICS`.
2. Rimuovere dal workflow RT:
   - `RUN_RESEARCH`;
   - `LIVE_SCORE`;
   - `ROLLING_VALIDATION`;
   - `ROLLING_PROMOTION`;
   - `AUTO_BOLLINGER_START` se implica DocBrown.
3. Nuovo workflow:

```text
Realtime readiness -> PAPER start -> run monitor -> forensics
```

4. Readiness RT:
   - ACDC reachable;
   - Influx bucket ready;
   - feature lookback sufficiente;
   - no synthetic backfill;
   - strategy family RT;
   - guard RT attive;
   - no paper running;
   - no open positions.
5. Backoffice legacy DocBrown:
   - resta accessibile solo in sezioni legacy/diagnostiche;
   - non deve comparire nella readiness RT;
   - non deve essere prerequisito RT.

## Interventi hft-fe

File/punti:

```text
hft-fe/src/lib/contracts/management.ts
hft-fe/src/routes/management/+page.svelte
hft-fe/src/lib/components/management/WorkflowMap.svelte
hft-fe/src/routes/+layout.svelte
hft-fe/src/routes/trades/+page.svelte
```

Interventi:

1. Aggiungere family `REALTIME_BB_ADX_V1`.
2. Nuovo workflow UI:
   - Realtime readiness;
   - Indicator snapshot;
   - PAPER controls;
   - Run monitor;
   - Forensics.
3. Rimuovere dalla vista RT:
   - DocBrown research;
   - live-score;
   - rolling validation;
   - promotion;
   - advice generation;
   - model status.
4. Mostrare per ogni trade:
   - setup RT;
   - trigger RT;
   - ADX;
   - +DI;
   - -DI;
   - TTM squeeze state;
   - Keltner upper/lower;
   - volume ratio;
   - OBV slope;
   - Chandelier stop;
   - source bucket;
   - interval;
   - synthetic flag.
5. Le pagine DocBrown esistenti vanno spostate/etichettate come legacy, non rimosse necessariamente.

## Script E Diagnostiche

Script legacy da non usare nel nuovo path:

- `acdc/scripts/acdc-run-rem-ml.sh`;
- `run-docbrown-research.sh`;
- script di rolling/promotion;
- script che leggono `acdc_live_bb_advice` come sorgente operativa.

Nuovi script diagnostici, tutti `DIAGNOSTIC_ONLY`:

- `diagnose-rt-readiness.sh`;
- `diagnose-rt-indicators.sh`;
- `diagnose-rt-run.sh`;
- `diagnose-rt-trade-forensics.sh`.

Ogni script deve stampare:

```text
DIAGNOSTIC_ONLY
strategy_family=REALTIME_BB_ADX_V1
source_bucket
interval_seconds
candle_state
candle_count
max_gap_seconds
synthetic_backfill
```

Nessuno script puo' avviare PAPER.

## Forensics

Ogni decisione ENTRY deve salvare:

- setup RT;
- trigger RT;
- tutte le feature RT;
- blocker reason RT;
- source/cadence metadata.

Ogni decisione EXIT deve salvare:

- reason RT;
- net return;
- max net return;
- Chandelier stop;
- ADX/DMI al momento exit;
- percent_b;
- BB/Keltner state;
- volume/OBV state.

Report obbligatorio post-run:

```text
trades
win/loss
net_profit_quote
avg_net_return
max_drawdown
MFE distribution
MAE distribution
exit reason distribution
entry blocker distribution
per-setup PnL
per-setup MFE>0 rate
```

## Criteri Di Accettazione

Prima PAPER RT:

- `REALTIME_BB_ADX_V1` presente in hft-common;
- ACDC non legge `acdc_live_bb_advice` nel path RT;
- PAPER RT parte senza DocBrown running;
- Kenshiro RT readiness non consulta DocBrown;
- FE RT non mostra ML come prerequisito;
- indicatori RT completi per almeno 60 barre o lookback richiesto maggiore;
- synthetic backfill bloccato;
- REAL ancora vietata.

PAPER RT valida:

- avviata solo da `/management`;
- container deployati;
- MySQL operativo;
- no H2;
- run classificata separatamente per setup;
- se 0 BUY, classificazione `NEGATIVE_OPERATIONAL_SIGNAL`;
- se BUY con `max_net_return=0` diffuso, classificazione `NEGATIVE_ENTRY_QUALITY_SIGNAL`;
- profitto non dichiarato da singolo trade isolato.

## Piano Implementativo

### Step 1 - Charter E Costanti

Repo:

- `hft-common`

Fare:

1. Aggiungere strategy family RT.
2. Aggiungere setup/trigger/reason/feature RT.
3. Documentare legacy constants da non usare nel path RT.
4. Test/compile hft-common.

Verifica:

- nessun codice RT deve importare costanti `BB_ADVICE_*`.

### Step 2 - Indicatori Realtime ACDC

Repo:

- `acdc`

Fare:

1. Creare `RealtimeIndicatorService`.
2. Calcolare Bollinger/Keltner/TTM/ADX/DMI/ATR/OBV/volume.
3. Aggiungere unit test su formule:
   - Bollinger;
   - Keltner;
   - TTM squeeze;
   - ADX/DMI;
   - Chandelier.
4. Aggiungere metadata data quality.

Verifica:

- snapshot RT completo su simbolo live;
- fail closed se dati insufficienti.

### Step 3 - WATCH/BUY RT Senza DocBrown

Repo:

- `acdc`

Fare:

1. Creare `RealtimeWatchService`.
2. Collegarlo a `PaperRunService` solo quando strategy family e' RT.
3. Escludere `LiveBbAdviceRepository` dal path RT.
4. Escludere `OutcomeQualityModelService` dal path RT.
5. Usare solo setup/trigger RT.

Verifica:

- PAPER RT puo' valutare simboli senza righe `acdc_live_bb_advice`.
- BUY reason e blocker sono RT.

### Step 4 - SELL RT

Repo:

- `acdc`

Fare:

1. Aggiungere guard EXIT RT.
2. Disabilitare exit legacy per profilo RT.
3. Implementare Chandelier/ATR trailing.
4. Persistire forensics RT.

Verifica:

- nessuna exit RT usa `BB_ADVICE_*`.

### Step 5 - Kenshiro Management

Repo:

- `kenshiro`

Fare:

1. Aggiungere action RT.
2. Nuovo workflow management RT.
3. Readiness senza DocBrown.
4. Bloccare PAPER RT se readiness RT false.

Verifica:

- `/management/state` mostra RT senza live-score/promotion.

### Step 6 - FE

Repo:

- `hft-fe`

Fare:

1. Nuovo pannello RT.
2. Nascondere ML nel workflow RT.
3. Mostrare indicatori RT.
4. Mostrare forensics RT.

Verifica:

- UI non propone DocBrown come step RT.

### Step 7 - Script

Repo:

- `acdc`

Fare:

1. Aggiungere script diagnostici RT.
2. Marcare script legacy come legacy/diagnostic only.
3. Verificare script contract.

### Step 8 - Deploy E PAPER

Fare:

1. Build tutti i repo toccati.
2. Deploy container.
3. Verificare MySQL runtime config.
4. Verificare `/management/state`.
5. Avviare PAPER RT solo da `/management`.
6. Monitorare BUY/SELL e stop governato.
7. Classificare run.

## Divieti Specifici

- Non usare DocBrown nel path RT.
- Non usare `acdc_live_bb_advice` come sorgente RT.
- Non usare `bb_advice_*` come feature RT.
- Non usare `q10_positive_max_net_return`.
- Non usare `source_generation_id`.
- Non usare `PAPER_ELIGIBLE`.
- Non usare rolling validation/promotion.
- Non usare script per avviare PAPER.
- Non usare REAL.

## Refinement 1 - Separazione Legacy/RT

Affinamento:

- Il codice legacy non va cancellato in blocco.
- Deve essere isolato dietro strategy family legacy.
- Il path RT deve essere riconoscibile da nomi `Realtime*` e feature `rt_*`.
- Ogni chiamata condivisa deve dichiarare se e' safe per RT.

Checklist aggiunta:

- cercare `BB_ADVICE_` nei file RT: zero occorrenze;
- cercare `LiveBbAdvice` nei file RT: zero occorrenze;
- cercare `DocBrown` nel workflow RT Kenshiro/FE: zero occorrenze operative.

## Refinement 2 - Qualita' Dati

Affinamento:

- ADX/DMI e Chandelier richiedono high/low affidabili.
- Se una microbar sintetica ricostruisce high/low in modo artificiale, non puo' essere strategica.
- Il documento impone quindi `synthetic_backfill=false` per ogni decisione RT.

Checklist aggiunta:

- confrontare `high >= max(open, close)` e `low <= min(open, close)`;
- verificare gap massimo;
- verificare numero barre sufficiente per BB20, Keltner20, ADX14 e Chandelier22.

## Refinement 3 - Uscite Prima Degli Ingressi

Affinamento:

- Non avviare PAPER RT se SELL RT non e' completa.
- Gli ultimi errori hanno mostrato BUY con `max_net_return=0`, ma una strategia priva di SELL completa peggiora la
  diagnosi.

Checklist aggiunta:

- `EXIT_RT_REENTRY_CAPTURE` testato;
- `EXIT_RT_REENTRY_FAILED` testato;
- `EXIT_RT_BREAKOUT_PROTECT` testato;
- `EXIT_RT_BREAKOUT_FAILED` testato;
- `EXIT_RT_BREAKOUT_TRAILING` testato;
- `EXIT_RT_LOSS_CAP` testato.

## Refinement 4 - Nessuna Soglia Mutante Non Documentata

Affinamento:

- Le soglie iniziali sono statiche e documentate.
- Nessun percentile storico o valore ML puo' modificarle nel runtime RT.
- Eventuali modifiche future devono passare da config `rt.*`, migration e documento.

Checklist aggiunta:

- nessun `contract_min_*` DocBrown in policy RT;
- nessun percentile storico in BUY RT;
- nessun `candidate_p10/p90` nel runtime RT;
- ogni valore soglia RT compare in hft-common o DB `rt.*`.

## Gate Finale - Cosa Deve Vedere Il Consiglio Prima Di Implementare

Prima di scrivere codice, il Consiglio deve confermare:

1. `REALTIME_BB_ADX_V1` sostituisce Context V1 nel path operativo PAPER.
2. DocBrown resta installato ma non operativo nel path RT.
3. Il primo sviluppo non cerca profitto via ML, ma verifica la strategia pubblicata rule-based.
4. Nessuna PAPER viene avviata finche':
   - indicatori RT sono implementati;
   - SELL RT e' implementata;
   - Kenshiro readiness RT e' vera;
   - FE mostra RT senza ML;
   - script legacy sono marcati non operativi.

Decisione finale del documento:

```text
Procedere con implementazione solo dopo aggiornamento charter/current context che promuova questo piano a vincolo
strategico operativo.
```
