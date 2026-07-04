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

## Refinement 5 - Inventario Legacy Da Rimuovere Dal Path RT

Questa revisione rende esplicito cosa significa "togliamo tutto il resto".

La rimozione richiesta non e' cancellazione indiscriminata di classi o tabelle. E' rimozione dal path operativo
`REALTIME_BB_ADX_V1`. Dove il codice legacy resta nel repository, deve essere:

- irraggiungibile dal path RT;
- marcato legacy nella UI/documentazione;
- escluso da readiness RT;
- escluso da PAPER RT;
- escluso da forensics RT, salvo vista storica.

### Legacy hft-common Da Non Usare Nel Path RT

Rimuovere dagli import e dai payload RT:

- `BOLLINGER_CONTEXT_V1`;
- `BOLLINGER_ONLY`;
- `BOLLINGER_ONLY_V2`;
- `BB_REENTRY_MEAN_REVERSION_LONG`;
- `BB_SQUEEZE_BREAKOUT_LONG`;
- `BB_REENTRY_CONFIRMED`;
- `BB_UPPER_BREAKOUT_CONFIRMED`;
- `PAPER_ELIGIBLE`;
- `BB_ADVICE_*`;
- `LIVE_BB_*`;
- `HISTORY_BB_*`;
- `CONTRACT_*` quando il valore deriva da DocBrown;
- `SOURCE_GENERATION_ID`;
- `BB_ADVICE_SOURCE_GENERATION_ID`;
- `BB_ADVICE_PAPER_ELIGIBLE`;
- `BB_ADVICE_PAPER_WATCH_ELIGIBLE`;
- `BB_ADVICE_PAPER_BUY_ELIGIBLE`;
- `BB_ADVICE_ECONOMIC_SAFE_PASS`;
- `Q10_POSITIVE_MAX_NET_RETURN`;
- `ENTRY_FRICTION_NET_RETURN` se prodotto da DocBrown.

Sostituzioni:

- `BOLLINGER_CONTEXT_V1` -> `REALTIME_BB_ADX_V1`;
- `BB_REENTRY_MEAN_REVERSION_LONG` -> `RT_RANGE_REENTRY_LONG`;
- `BB_SQUEEZE_BREAKOUT_LONG` -> `RT_TTM_SQUEEZE_BREAKOUT_LONG`;
- `BB_REENTRY_CONFIRMED` -> `RT_REENTRY_CONFIRMED`;
- `BB_UPPER_BREAKOUT_CONFIRMED` -> `RT_TTM_BREAKOUT_CONFIRMED`;
- `bb_advice_*` -> `rt_*`.

### Legacy ACDC Da Escludere O Rifattorizzare

File/classi legacy operative:

- `acdc/src/main/java/it/mbc/hft/acdc/repository/LiveBbAdviceRepository.java`;
- `acdc/src/main/java/it/mbc/hft/acdc/service/OutcomeQualityModelService.java`;
- `acdc/src/main/java/it/mbc/hft/acdc/service/PreBuyWatchService.java`;
- `acdc/src/main/java/it/mbc/hft/acdc/service/CandidateSnapshotService.java`;
- `acdc/src/main/java/it/mbc/hft/acdc/service/SnapshotRankingService.java`;
- `acdc/src/main/java/it/mbc/hft/acdc/service/BbReadinessDiagnosticsService.java`;
- `acdc/src/main/java/it/mbc/hft/acdc/service/BbAdviceFeatures.java`;
- `acdc/src/main/java/it/mbc/hft/acdc/service/BbAdviceTrailingPolicy.java`;
- metodi legacy in `PaperRunService`:
  - `requireMlReady(...)`;
  - `bindExpectedSourceGeneration(...)`;
  - `expectedGenerationSnapshots(...)`;
  - `stopIfExpectedGenerationExhausted(...)`;
  - `sourceGenerationBlockReason(...)`;
  - qualunque lookup su `LiveBbAdviceRepository`;
  - qualunque filtro su `RemPromotionClass.PAPER_ELIGIBLE`;
  - qualunque decisione basata su `source_generation_id`.

Decisione implementativa:

```text
Per RT creare path parallelo esplicito dentro ACDC:
RealtimePaperRunService oppure ramo PaperRunService isolato da strategy_family.
```

Scelta preferita:

```text
Creare RealtimePaperRunService.
PaperRunService legacy resta per Context V1 finche' non viene spento.
AcDcResource/Kenshiro chiamano RealtimePaperRunService solo per action RT.
```

Motivo:

- riduce rischio di contaminazione legacy;
- evita `if strategy == RT` sparsi in un servizio gia' molto carico;
- rende testabile che il path RT non importi `LiveBbAdvice`.

### Legacy GuardEvaluator Da Escludere

Operator legacy da non usare nei guard RT:

- `BB_ADVICE_TAKE_PROFIT_EXIT`;
- `BB_ADVICE_DYNAMIC_TRAILING_EXIT`;
- `BB_ADVICE_NO_MFE_DECAY_EXIT`;
- `BB_ADVICE_POSITIVE_DURATION_EXIT`;
- `BB_ADVICE_LOSS_CAP_EXIT`;
- `BB_ADVICE_TIMEOUT_EXIT`;
- `BB_REENTRY_CAPTURE_EXIT` se legge feature `bb_*`/contract legacy;
- `BB_REENTRY_FAILED_EXIT` se legge feature `bb_*`/contract legacy;
- `BB_BREAKOUT_FAILED_EXIT` se legge feature `bb_*`/contract legacy;
- `BB_BREAKOUT_PROTECT_EXIT` se legge feature `bb_*`/contract legacy.

Operator RT da creare o sostituire con feature booleane:

- `RT_REENTRY_CAPTURE_EXIT`;
- `RT_REENTRY_FAILED_EXIT`;
- `RT_BREAKOUT_PROTECT_EXIT`;
- `RT_BREAKOUT_FAILED_EXIT`;
- `RT_BREAKOUT_TRAILING_EXIT`;
- `RT_CHANDELIER_EXIT`;
- `RT_LOSS_CAP_EXIT`;
- `RT_TIMEOUT_EXIT`.

Scelta preferita:

```text
Calcolare booleane in RealtimeIndicatorService/RealtimeExitFeatureService.
Usare operator generici nei guard.
Creare operator RT solo per Chandelier se il confronto richiede prezzo/stop dinamico non rappresentabile con FEATURE_LTE.
```

### Legacy DB ACDC Da Escludere

Tabelle legacy da non leggere nel path RT:

- `acdc_live_bb_advice`;
- eventuali tabelle DocBrown mirrored/advice;
- runtime config `bb.live_advice.*`;
- runtime config `bb.context.*` per soglie Context V1;
- runtime status `bb.management.round_robin.*`;
- runtime status `bb.management.research.*`.

Tabelle ancora ammesse:

- `acdc_shared_runtime_config`, solo chiavi `rt.*`, `market.*`, PAPER/runtime comuni;
- `acdc_guard_definition`, solo guard del profilo RT;
- `acdc_guard_threshold_override`, solo override RT;
- `acdc_paper_run`;
- `acdc_paper_position`;
- `acdc_paper_decision`;
- `acdc_trade_candle`;
- `acdc_paper_sell_diagnostics`;
- `acdc_paper_post_sell_forensics`;
- `acdc_run_execution`.

Nuove chiavi DB obbligatorie:

```text
rt.strategy.enabled
rt.strategy.family
rt.bb.period
rt.bb.stddev
rt.keltner.period
rt.keltner.atr_multiplier
rt.adx.period
rt.adx.range.max
rt.adx.trend.min
rt.adx.rising.lookback
rt.volume.confirmation.min
rt.obv.slope.lookback
rt.chandelier.period
rt.chandelier.atr_multiplier
rt.loss_cap.max_quote
rt.trailing.arm
rt.trailing.distance
rt.timeout.reentry.seconds
rt.timeout.breakout.seconds
```

### Legacy Kenshiro Da Rimuovere Dal Workflow RT

Action legacy da non mostrare come step RT:

- `AUTO_BOLLINGER_START`;
- `AUTO_BOLLINGER_STOP` se resta legata al ciclo DocBrown;
- `RUN_RESEARCH`;
- `RESEARCH_STATUS`;
- `LIVE_SCORE`;
- `ROLLING_VALIDATION`;
- `ROLLING_SELECTION_ATTRIBUTION_AUDIT`;
- `ROLLING_PROMOTION`;
- `APPLY_BOLLINGER_CONTEXT_V1`;
- `APPLY_BOLLINGER_ONLY`.

Metodi/aree da non invocare dal workflow RT:

- chiamate downstream verso DocBrown;
- `rollingPromotion(...)`;
- `autoAutomationStart(...)` legacy;
- pre-promotion live score;
- readiness basata su active advice;
- conteggio `PAPER_ELIGIBLE`.

Nuove action RT:

- `APPLY_REALTIME_BB_ADX_V1`;
- `REALTIME_PAPER_START`;
- `REALTIME_PAPER_STOP_BUY`;
- `REALTIME_PAPER_STOP`;
- `REALTIME_REFRESH_DIAGNOSTICS`;
- `REALTIME_READINESS`.

### Legacy hft-fe Da Rimuovere Dalla Vista RT

Elementi UI legacy da non mostrare nella pagina RT:

- nav diretta DocBrown nel workflow operativo;
- "Research";
- "Live score";
- "Rolling validation";
- "Promotion";
- "PAPER_ELIGIBLE";
- "Advice age";
- "Advice generation";
- metriche `bb_advice_paper_watch_eligible`;
- metriche `bb_advice_paper_buy_eligible`;
- model status;
- selection bias;
- score breakdown DocBrown.

Le pagine DocBrown possono restare nel menu legacy/config, ma devono essere etichettate come:

```text
Legacy / diagnostic only / not used by REALTIME_BB_ADX_V1
```

### Legacy Script Da Rimuovere Dal Path Operativo

Script da non usare per RT:

- `acdc/scripts/acdc-run-rem-ml.sh`;
- `run-docbrown-research.sh`;
- ogni script che chiama DocBrown research;
- ogni script che legge `acdc_live_bb_advice` come sorgente operativa;
- ogni script che menziona rolling/promotion senza `DIAGNOSTIC_ONLY`.

Ogni script legacy deve avere una delle due destinazioni:

1. archiviato/spostato sotto cartella legacy;
2. lasciato dove si trova ma con banner:

```text
LEGACY_DIAGNOSTIC_ONLY
NOT_USED_BY_REALTIME_BB_ADX_V1
MUST_NOT_START_PAPER
```

## Refinement 6 - Sequenza Implementativa Senza Ambiguita'

Questa revisione fissa l'ordine di sviluppo. Codex non deve scegliere un ordine diverso.

### Fase 0 - Preflight Legacy

Output richiesto prima del codice:

- lista grep `LiveBbAdvice`;
- lista grep `BB_ADVICE`;
- lista grep `DocBrown`;
- lista grep `PAPER_ELIGIBLE`;
- lista grep `RUN_RESEARCH|LIVE_SCORE|ROLLING_PROMOTION`;
- mappa file legacy da toccare.

Stop condition:

```text
Se emerge un altro path operativo verso DocBrown non censito nel documento, aggiornare il documento prima del codice.
```

### Fase 1 - hft-common

Implementare:

- `REALTIME_BB_ADX_V1`;
- feature key `rt_*`;
- reason RT;
- setup/trigger RT;
- eventuali operator RT;
- classificazioni evidenza RT:
  - `VALID_REALTIME_STRATEGIC_EVIDENCE`;
  - `NEGATIVE_REALTIME_ENTRY_SIGNAL`;
  - `NEGATIVE_REALTIME_EXIT_SIGNAL`;
  - `NEGATIVE_REALTIME_FINANCIAL_SIGNAL`;
  - `INVALID_REALTIME_EVIDENCE`.

Verifica:

```bash
cd hft-common && mvn -q -DskipTests install
```

### Fase 2 - ACDC Indicatori

Implementare:

- `RealtimeIndicatorService`;
- record interno `RealtimeIndicatorSnapshot` se utile;
- calcolo ADX/DMI;
- calcolo Keltner;
- calcolo TTM squeeze;
- calcolo OBV;
- calcolo Chandelier;
- data quality RT.

Test obbligatori:

- serie piatta: ADX basso, no breakout;
- serie trend up: +DI > -DI, ADX crescente;
- squeeze on: Bollinger dentro Keltner;
- squeeze fired up: Bollinger esce sopra Keltner e close sopra upper;
- Chandelier: stop = highestHigh(period) - ATR * multiplier.

### Fase 3 - ACDC Realtime Runtime

Implementare:

- `RealtimePaperRunService`;
- `RealtimeWatchService`;
- `RealtimeReadinessService`;
- `RealtimeExitFeatureService` se separare exit features da indicatori migliora chiarezza.

Il nuovo service deve:

- leggere universo da config/snapshot, non da advice;
- creare run PAPER;
- valutare entry RT;
- aprire posizioni;
- valutare exit RT;
- persistere decisioni e posizioni come oggi;
- usare telegram idempotente esistente;
- usare budget/sizing esistente.

Il nuovo service non deve:

- importare `LiveBbAdvice`;
- importare `LiveBbAdviceRepository`;
- importare `RemPromotionClass`;
- importare `OutcomeQualityModelService`;
- chiamare `candidateSnapshotService`;
- chiamare `snapshotRankingService` se ranking deriva da advice/ML;
- chiamare `BbReadinessDiagnosticsService`.

### Fase 4 - DB ACDC

Migration:

```text
V100__realtime_bb_adx_v1.sql
```

La migration deve:

- aggiungere config `rt.*`;
- aggiungere guard RT ENTRY/EXIT;
- non modificare globalmente guard legacy;
- non attivare RT se codice non pronto;
- aggiungere descrizioni chiare per FE/config.

### Fase 5 - Kenshiro

Implementare:

- action RT;
- readiness RT;
- workflow RT;
- proxy verso ACDC RT endpoints;
- stato senza DocBrown.

Endpoint downstream ACDC da introdurre o usare:

- `/acdc/realtime/readiness/{profileKey}`;
- `/acdc/realtime/paper/start/{profileKey}`;
- `/acdc/realtime/paper/stop-buy/{profileKey}`;
- `/acdc/realtime/paper/stop/{profileKey}`;
- `/acdc/realtime/diagnostics/{profileKey}`.

### Fase 6 - FE

Implementare:

- tab/pagina RT;
- pannello indicatori;
- pannello readiness;
- controlli PAPER RT;
- trade detail RT;
- marker legacy sulle vecchie superfici.

### Fase 7 - Script

Implementare:

- `diagnose-rt-readiness.sh`;
- `diagnose-rt-indicators.sh`;
- `diagnose-rt-run.sh`;
- `diagnose-rt-trade-forensics.sh`.

Aggiornare script legacy con banner o spostamento.

### Fase 8 - Verifica Finale Prima PAPER

Comandi minimi:

```bash
cd hft-common && mvn -q -DskipTests install
cd ../acdc && mvn -q test && mvn -q -DskipTests package
cd ../kenshiro && mvn -q test && mvn -q -DskipTests package
cd ../hft-fe && npm run check && npm run build
```

Poi:

- deploy ACDC;
- deploy Kenshiro;
- deploy FE;
- verificare MySQL;
- verificare `/management/state`;
- verificare readiness RT;
- solo dopo avviare PAPER RT da `/management`.

## Refinement 7 - Contratto Di Readiness RT

Questa revisione elimina l'ambiguita' su cosa significhi "pronto".

`REALTIME_BB_ADX_V1` e' pronto solo se tutti i campi sotto sono veri:

```text
rt_strategy_family_active = true
rt_strategy_enabled = true
rt_docbrown_path_disabled = true
rt_live_advice_unused = true
rt_indicator_lookback_ready = true
rt_data_quality_ready = true
rt_synthetic_backfill = false
rt_entry_guards_ready = true
rt_exit_guards_ready = true
rt_management_actions_ready = true
rt_fe_contract_ready = true
paper_running = false
open_positions = 0
real_forbidden = true
```

Blocker obbligatori:

- `RT_STRATEGY_NOT_ENABLED`;
- `RT_DOCBROWN_PATH_STILL_ACTIVE`;
- `RT_LIVE_ADVICE_STILL_REFERENCED`;
- `RT_INDICATOR_LOOKBACK_INSUFFICIENT`;
- `RT_DATA_GAP_TOO_WIDE`;
- `RT_SYNTHETIC_BACKFILL_BLOCKED`;
- `RT_ENTRY_GUARDS_MISSING`;
- `RT_EXIT_GUARDS_MISSING`;
- `RT_MANAGEMENT_ACTIONS_MISSING`;
- `RT_FE_CONTRACT_MISSING`;
- `RT_PAPER_ALREADY_RUNNING`;
- `RT_OPEN_POSITIONS_PRESENT`;
- `REAL_FORBIDDEN`.

Kenshiro `/management/state` deve esporre:

```json
{
  "strategyFamily": "REALTIME_BB_ADX_V1",
  "rtReady": true,
  "rtBlockers": [],
  "rtDiagnostics": {
    "sourceBucket": "binance-microbar",
    "intervalSeconds": 5,
    "candleState": "CLOSED",
    "candleCount": 0,
    "maxGapSeconds": 0,
    "syntheticBackfill": false,
    "docbrownPathDisabled": true,
    "liveAdviceUnused": true,
    "entryGuards": [],
    "exitGuards": []
  }
}
```

ACDC readiness endpoint deve essere la fonte tecnica; Kenshiro aggrega e mostra.

## Refinement 8 - Autonomia Implementativa E Cose Da Non Chiedere

Questa revisione esplicita le decisioni gia' prese. Durante l'implementazione non serve chiedere conferma su questi
punti:

1. Il nuovo path si chiama `REALTIME_BB_ADX_V1`.
2. DocBrown non viene rimosso dal repository.
3. DocBrown e ML sono esclusi dal path decisionale RT.
4. Il path RT non usa `acdc_live_bb_advice`.
5. Il path RT non usa `PAPER_ELIGIBLE`.
6. Il path RT non usa rolling validation/promotion.
7. Il path RT non usa `q10_positive_max_net_return`.
8. Il path RT non usa soglie adattive DocBrown.
9. ACDC e' owner degli indicatori, WATCH, BUY, SELL RT.
10. Influxer e' owner dei dati OHLCV, non della strategia.
11. Kenshiro e' owner orchestrazione `/management`.
12. FE e' owner visualizzazione/controlli, non logica strategica.
13. REAL resta vietata.
14. PAPER parte solo da `/management`.
15. Nessuno script avvia PAPER.
16. SELL RT deve esistere prima della prima PAPER RT.
17. Se una feature richiesta manca, BUY fallisce chiusa.
18. Se synthetic backfill e' true, BUY/SELL strategici falliscono chiusi.
19. Se la run RT perde, non si cambia soglia al volo: si produce forensics.
20. Se servono nuove soglie, vanno in documento + hft-common/DB `rt.*` + migration.

Domande ancora ammesse solo se bloccanti:

- fonte dati OHLCV assente o non verificabile;
- formula di un indicatore non implementabile per dati mancanti;
- conflitto tecnico tra schema DB reale e piano;
- test formula fallito per ambiguita' matematica non risolta dalle fonti.

## Refinement 9 - Definition Of Done Per Intervento

Questa revisione dettaglia cosa deve essere vero alla fine di ogni gruppo di sviluppo.

### DoD hft-common

- `REALTIME_BB_ADX_V1` compilante.
- Costanti `rt_*` presenti.
- Reason RT presenti.
- Nessun riuso necessario di `BB_ADVICE_*` nel nuovo codice.
- Test/compile completato.
- Commit/push MS.

### DoD influxer

- Dati OHLCV reali verificati.
- `synthetic_backfill` preservato.
- Nessun calcolo strategico aggiunto.
- Diagnostica bucket/cadence aggiornata se necessario.
- Deploy se codice toccato.

### DoD ACDC Indicatori

- Unit test formule passano.
- Snapshot RT contiene tutte le feature obbligatorie.
- Data quality blocker visibili.
- Nessun import DocBrown/advice nei nuovi servizi RT.

### DoD ACDC Runtime

- PAPER RT parte senza righe active in `acdc_live_bb_advice`.
- `RealtimePaperRunService` non importa classi advice/ML.
- WATCH RT usa solo feature `rt_*`.
- BUY RT salva policy `rt_*`.
- SELL RT salva reason `EXIT_RT_*`.
- Telegram BUY/SELL resta idempotente.
- MySQL operativo, non H2, usato per verifica.

### DoD Kenshiro

- `/management/state` espone `rtReady`.
- Workflow RT non mostra DocBrown.
- Action RT invocano endpoint RT ACDC.
- Action legacy restano solo legacy.
- PAPER RT bloccata se `rtReady=false`.

### DoD hft-fe

- UI RT mostra indicatori e readiness.
- UI RT non mostra live-score/promotion/research.
- Trade detail mostra feature RT.
- Vecchie pagine DocBrown marcate legacy.
- `npm run check` e build passano.

### DoD Script

- Script RT sono `DIAGNOSTIC_ONLY`.
- Script legacy sono marcati legacy/non operativi.
- Nessuno script PAPER.
- Contract script passano.

### DoD PAPER

- Avvio da `/management`.
- Stato iniziale: `rtReady=true`, `paperRunning=false`, `openPositions=0`.
- Stato finale: stop governato, no open positions.
- Report per setup.
- Distribuzione ENTRY blocker.
- Distribuzione EXIT reason.
- PnL netto.
- MFE/MAE.
- Classificazione evidenza.

## Legacy Removal Checklist Finale

Prima di dichiarare il path RT pronto, questi comandi devono dare zero occorrenze nei nuovi file RT o nelle superfici RT:

```bash
rg -n "LiveBbAdvice|LiveBbAdviceRepository|acdc_live_bb_advice" acdc/src/main/java/it/mbc/hft/acdc/service/*Realtime*
rg -n "BB_ADVICE|bb_advice|PAPER_ELIGIBLE|source_generation_id|q10_positive" acdc/src/main/java/it/mbc/hft/acdc/service/*Realtime*
rg -n "RUN_RESEARCH|LIVE_SCORE|ROLLING_VALIDATION|ROLLING_PROMOTION" kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice
rg -n "DocBrown|docbrown|live-score|promotion|PAPER_ELIGIBLE|bb_advice" hft-fe/src/routes/management hft-fe/src/lib/components/management
```

Eccezioni ammesse:

- file/documenti legacy;
- pagine legacy esplicitamente etichettate;
- diagnostiche `LEGACY_DIAGNOSTIC_ONLY`;
- test che verificano che il path RT non usi legacy.

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
