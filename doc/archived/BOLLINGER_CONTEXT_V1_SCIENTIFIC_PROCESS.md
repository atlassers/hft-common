# Bollinger Context V1 Scientific Process

Data: 2026-07-03.

Documento scientifico di supporto al charter strategico operativo:

```text
hft-common/doc/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md
```

Scopo: descrivere il processo completo, le formule degli indicatori, la responsabilita' di ogni step e il motivo per
cui i valori vanno dedotti dallo storico ma applicati in realtime.

## Processo

Pipeline vincolante:

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

Responsabilita':

- ML/DocBrown: costruisce candidate e contratto.
- live-score: aggiorna advice e rende disponibile contratto compatibile.
- WATCH/ACDC: ricalcola current state e decide BUY.
- BUY/ACDC: esegue PAPER e congela `entry_*`.
- SELL/ACDC: esce per target/loss/invalidation, senza MFE come condizione.
- forensics: confronta contratto, current state, entry, exit e outcome.

Formula decisionale:

```text
candidate = selection storica
contract = soglie storiche setup-specifiche
current_state = indicatori realtime ricalcolati
BUY = current_state soddisfa contract
```

Il contratto non e' la decisione. La decisione e' solo il confronto vivo dentro WATCH.

## ML / Research Workflow

Nel ciclo attuale, `ML` non significa script Python o laboratorio esterno. Il laboratorio storico `docbrown/python` non
e' piu' parte del runtime ed e' stato rimosso.

Nel processo scientifico `ML` significa:

```text
DocBrown Quarkus research + candidate generation + rolling validation + rolling promotion
```

Gli output ammessi del layer ML sono:

- candidate;
- setup;
- trigger atteso;
- soglie contrattuali;
- regime ammesso;
- metriche storiche;
- advice PAPER-eligible.

Gli output vietati del layer ML sono:

- BUY diretto;
- SELL diretto;
- avvio PAPER fuori `/management`;
- uso di valori storici/advice come verita' realtime del BUY.

### Endpoint Scientifici DocBrown

Endpoint del layer research/selection:

```text
POST /rem/research/{profileKey}/run
GET  /rem/research/{profileKey}/status
POST /rem/blank-candidates/{profileKey}/generate
POST /rem/blank-candidates/{profileKey}/rolling-validation
POST /rem/blank-candidates/{profileKey}/universe-triage
POST /rem/blank-candidates/{profileKey}/universe-scheduler
POST /rem/blank-candidates/{profileKey}/rolling-paper-promotion
POST /rem/live-advice/{profileKey}/score
```

Questi endpoint possono costruire e validare il contratto. Non possono autorizzare BUY senza WATCH.

### Endpoint Operativi Kenshiro

Endpoint operativo primario:

```text
POST /backoffice/management/actions/{action}
```

Azioni rilevanti:

```text
RUN_RESEARCH
RESEARCH_STATUS
UNIVERSE_PREFILTER
LIVE_SCORE
ROLLING_VALIDATION
ROLLING_PROMOTION
AUTO_BOLLINGER_START
PAPER_BOLLINGER_START
PAPER_STOP
```

Regola:

```text
ogni sequenza operativa che puo' portare a PAPER deve passare da /management
```

### Script ML

Script attuale:

```text
acdc/scripts/acdc-run-rem-ml.sh
```

Comportamento:

```text
curl -X POST ${DOCBROWN_BASE_URL}/rem/research/${PROFILE_KEY}/run
```

Output:

```text
acdc/target/rem-ml/candidate-*.json
acdc/target/rem-ml/latest.json
```

Classificazione scientifica:

```text
DIAGNOSTIC_ONLY
```

Motivo:

- invoca solo research;
- non esegue rolling validation;
- non esegue live-score;
- non esegue promotion;
- non verifica `/management/state`;
- non puo' garantire readiness PAPER.

Un eventuale script operativo end-to-end deve invocare solo `/management/actions/*`, cosi' la sequenza resta
tracciata, bloccabile e coerente con le regole PAPER-only.

## Fonti Scientifiche

Riferimenti usati:

- Bollinger Bands official rules: https://www.bollingerbands.com/bollinger-band-rules
- Bollinger BandWidth, StockCharts: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/bollinger-bandwidth
- Bollinger Band Squeeze, StockCharts: https://chartschool.stockcharts.com/table-of-contents/trading-strategies-and-models/trading-strategies/bollinger-band-squeeze
- Bollinger %B, TradingView: https://www.tradingview.com/support/solutions/43000501971-bollinger-bands-b-b/
- RSI / Wilder RSI, TC2000: https://help.tc2000.com/m/69404/l/747071-rsi-wilder-s-rsi
- RSI formula, Investopedia: https://www.investopedia.com/terms/r/rsi.asp
- ATR, StockCharts: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/average-true-range-atr
- ATR formula, Wikipedia: https://en.wikipedia.org/wiki/Average_true_range
- EMA formula, CMC Markets: https://www.cmcmarkets.com/en-gb/technical-analysis/exponential-moving-average
- OBV/volume confirmation, StockCharts: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/on-balance-volume-obv
- Chandelier Exit, StockCharts: https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-overlays/chandelier-exit

## Indicatori

### Bollinger Bands

Parametri standard:

```text
period = 20
multiplier = 2
```

Formule:

```text
middle = SMA(close, period)
stddev = standard_deviation(close, period)
upper = middle + multiplier * stddev
lower = middle - multiplier * stddev
```

Interpretazione:

- upper band = prezzo relativamente alto;
- lower band = prezzo relativamente basso;
- la banda da sola non e' segnale sufficiente;
- va combinata con momentum, volume, regime e trend.

### Percent B

Formula:

```text
percent_b = (price - lower) / (upper - lower)
```

Interpretazione:

```text
percent_b < 0      prezzo sotto lower band
percent_b = 0      prezzo su lower band
percent_b = 0.5    prezzo su middle band
percent_b = 1      prezzo su upper band
percent_b > 1      prezzo sopra upper band
```

Uso setup-specifico:

- reentry: vuole recupero da lower verso middle, non estensione sopra upper;
- breakout: puo' accettare `%B > 1`, ma solo con trend/volume/espansione.

Proposta reentry:

```text
0.20 <= percent_b <= 0.65 preferibile
percent_b <= 0.80 limite massimo operativo
percent_b > 1 blocco obbligatorio per reentry
```

Proposta breakout:

```text
percent_b >= 1
AND volume/trend/expansion pass
AND no chaos
```

### BandWidth

Formula:

```text
bandwidth = (upper - lower) / middle
```

Se si usa forma percentuale:

```text
bandwidth_pct = ((upper - lower) / middle) * 100
```

Uso:

- identifica compressione/squeeze;
- identifica espansione se aumenta nel tempo.

Derivati:

```text
bandwidth_delta = bandwidth_now - bandwidth_previous
bandwidth_percentile = percentile_rank(bandwidth_now, historical_bandwidth_window)
```

Uso setup-specifico:

- reentry: preferisce range controllato, bandwidth non estremo;
- breakout: preferisce squeeze precedente + expansion corrente.

### EMA

Formula:

```text
alpha = 2 / (n + 1)
EMA_t = (close_t - EMA_{t-1}) * alpha + EMA_{t-1}
```

Periodi usati:

```text
EMA9
EMA21
EMA50
```

Feature:

```text
ema9_gt_ema21 = EMA9 > EMA21 ? 1 : 0
close_gt_ema50 = close > EMA50 ? 1 : 0
ema50_slope = (EMA50_t - EMA50_{t-1}) / abs(EMA50_{t-1})
```

Uso:

- breakout: richiede trend long.
- reentry: richiede assenza di trend down forte e slope contenuta.

Proposta breakout:

```text
EMA9 > EMA21
AND close > EMA50
```

Proposta reentry:

```text
abs(ema50_slope) <= contract_max_ema50_slope_abs
AND NOT REGIME_TREND_DOWN
```

### RSI

Formula Wilder:

```text
gain_t = max(close_t - close_{t-1}, 0)
loss_t = max(close_{t-1} - close_t, 0)
avg_gain = WilderSmooth(gain, n)
avg_loss = WilderSmooth(loss, n)
RS = avg_gain / avg_loss
RSI = 100 - (100 / (1 + RS))
```

Periodo:

```text
n = 14
```

Uso setup-specifico:

- reentry: RSI deve indicare recupero ma non esaurimento;
- breakout: RSI deve confermare momentum ma non overextension estrema.

Proposta reentry:

```text
RSI <= 62
preferibile: 35 <= RSI <= 62
```

Proposta breakout:

```text
50 <= RSI <= 70/75
```

Nota: RSI `78` puo' essere troppo permissivo per micro-trade con fee alte, salvo evidenza storica per setup/simbolo.

### ATR

True Range:

```text
TR_t = max(
  high_t - low_t,
  abs(high_t - close_{t-1}),
  abs(low_t - close_{t-1})
)
```

ATR Wilder:

```text
ATR_t = ((ATR_{t-1} * (n - 1)) + TR_t) / n
```

Periodo:

```text
n = 14
```

Normalizzazione:

```text
atr_pct = ATR14 / price
```

Uso:

- non indica direzione;
- misura rischio/volatilita';
- permette stop e overextension normalizzati.

Proposta:

- reentry: `atr_pct` deve stare sotto limite range/chaos;
- breakout: `atr_pct` deve essere sufficiente ma non caotico;
- SELL fase 2: possibile stop tipo Chandelier, non in fase immediata.

### Volume Ratio

Formula operativa:

```text
volume_ratio_1m_20m = volume_current_1m / average(volume_1m, 20)
```

Se la base e' microbar, deve essere dichiarata:

```text
volume_ratio = volume_current_bucket / average(volume_bucket, window)
```

Uso:

- breakout richiede conferma volume;
- reentry deve evitare spike caotici.

Proposta breakout:

```text
volume_ratio_1m_20m >= 1.3
oppure >= 1.5 se il campione mostra falsi breakout
```

Proposta reentry:

```text
volume_ratio_1m_20m <= contract_max_reentry_volume_spike_ratio
```

### Regime

Regimi ammessi:

```text
REGIME_RANGE
REGIME_SQUEEZE
REGIME_EXPANSION
REGIME_TREND_UP
REGIME_TREND_DOWN
REGIME_CHAOS
```

Prima classificazione:

```text
REGIME_RANGE =
  abs(ema50_slope) <= max_range_ema50_slope_abs
  AND bandwidth_percentile <= max_range_bandwidth_percentile
  AND atr_pct <= max_range_atr_pct

REGIME_EXPANSION =
  bandwidth_delta > 0
  AND bb_expansion = 1

REGIME_TREND_UP =
  EMA9 > EMA21
  AND close > EMA50

REGIME_TREND_DOWN =
  EMA9 < EMA21
  AND close < EMA50

REGIME_CHAOS =
  atr_pct > max_chaos_atr_pct
  OR volume_ratio > max_chaos_volume_ratio
```

Uso:

- reentry compra solo in `REGIME_RANGE`;
- breakout compra in `REGIME_SQUEEZE`, `REGIME_EXPANSION` o `REGIME_TREND_UP`;
- ogni setup blocca `REGIME_TREND_DOWN` e `REGIME_CHAOS`.

## Setup Operativi

### Reentry Mean Reversion Long

Scopo:

```text
comprare recupero controllato dopo lower breach
```

Contratto storico:

```text
bb_setup = BB_REENTRY_MEAN_REVERSION_LONG
bb_trigger = BB_REENTRY_CONFIRMED
allowed_regime = REGIME_RANGE
contract_min_reentry_percent_b
contract_max_reentry_percent_b
contract_max_oversold_recovery_rsi
contract_max_ema50_slope_abs
contract_max_reentry_volume_spike_ratio
contract_max_range_atr_pct
contract_max_range_bandwidth_percentile
contract_max_net_loss_quote
```

WATCH realtime:

```text
setup recognized
AND bb_lower_breach = 1
AND bb_reentry_confirmed = 1
AND percent_b >= contract_min_reentry_percent_b
AND percent_b <= contract_max_reentry_percent_b
AND REGIME_RANGE = 1
AND REGIME_TREND_DOWN = 0
AND REGIME_CHAOS = 0
AND abs(ema50_slope) <= contract_max_ema50_slope_abs
AND rsi14 <= contract_max_oversold_recovery_rsi
AND volume_ratio <= contract_max_reentry_volume_spike_ratio
AND atr_pct <= contract_max_range_atr_pct
AND bandwidth_percentile <= contract_max_range_bandwidth_percentile
AND budget/sizing pass
```

Blocco obbligatorio:

```text
percent_b > 1
```

Motivo: per definizione, `%B > 1` significa prezzo sopra upper band; non e' piu' un ingresso di recupero controllato.

### Squeeze Breakout Long

Scopo:

```text
comprare espansione direzionale dopo compressione
```

Contratto storico:

```text
bb_setup = BB_SQUEEZE_BREAKOUT_LONG
bb_trigger = BB_UPPER_BREAKOUT_CONFIRMED
allowed_regime IN (REGIME_SQUEEZE, REGIME_EXPANSION, REGIME_TREND_UP)
contract_min_breakout_percent_b
contract_max_bandwidth_percentile
contract_min_middle_slope
contract_min_volume_ratio
contract_min_breakout_rsi
contract_max_breakout_rsi
contract_max_atr_pct
contract_max_net_loss_quote
```

WATCH realtime:

```text
setup recognized
AND bb_upper_breach = 1
AND percent_b >= contract_min_breakout_percent_b
AND bandwidth_delta > 0
AND bb_expansion = 1
AND bb_middle_slope >= contract_min_middle_slope
AND regime IN (SQUEEZE, EXPANSION, TREND_UP)
AND REGIME_TREND_DOWN = 0
AND REGIME_CHAOS = 0
AND EMA9 > EMA21
AND close > EMA50
AND contract_min_breakout_rsi <= rsi14 <= contract_max_breakout_rsi
AND volume_ratio >= contract_min_volume_ratio
AND atr_pct <= contract_max_atr_pct
AND budget/sizing pass
```

## SELL Senza MFE

MFE resta metrica forensics:

```text
max_net_return = max(net_return_t during position)
```

Non deve essere condizione SELL.

SELL fase immediata:

```text
EXIT_BB_TAKE_PROFIT
EXIT_BB_DYNAMIC_TRAILING
EXIT_BB_LOSS_CAP
EXIT_BB_TIMEOUT
```

Correzione richiesta:

```text
EXIT_BB_LOSS_CAP deve essere anche quote-aware
```

Formule:

```text
buy_quote = quantity * buy_price
sell_quote = quantity * current_price
buy_fee = buy_quote * fee_rate
sell_fee = sell_quote * fee_rate
gross_profit = sell_quote - buy_quote
net_profit = gross_profit - buy_fee - sell_fee
net_return = net_profit / buy_quote
net_loss_quote = abs(min(net_profit, 0))
```

Guardia proposta:

```text
sell if net_loss_quote >= contract_max_net_loss_quote
OR net_return <= bb_loss_cap_net_return
```

Motivo:

- su trade da circa 25 USDC, `bb_loss_cap_net_return = -0.008` consente circa `0.20` USDC di perdita;
- la tolleranza operativa indicata e' circa `0.05` USDC;
- una soglia quote-aware rende il rischio economico leggibile.

SELL fase 2, solo dopo evidenza:

Reentry invalidation:

```text
sell if price < lower
OR percent_b < 0
OR close < middle AND rsi14 deteriorates
```

Breakout invalidation:

```text
sell if price falls back below upper
AND bandwidth_delta <= 0
AND volume_confirmation decays
```

Chandelier/ATR opzionale:

```text
long_stop = highest_high_since_entry - k * ATR
```

Questa fase non deve essere implementata senza autorizzazione successiva.

## Storico Vs Realtime

### Da Deducere Dallo Storico

DocBrown deve dedurre:

- percentili `%B` per setup;
- percentili BandWidth;
- range RSI utile per setup;
- volume ratio baseline;
- ATR% normale e chaos;
- EMA slope normale;
- loss quote sostenibile;
- regime ammesso per setup.

Questi valori diventano:

```text
contract_*
history_*
live_contract_*
```

### Da Calcolare Realtime

ACDC WATCH deve calcolare:

- price;
- Bollinger live;
- `%B`;
- BandWidth e delta;
- EMA9/21/50;
- RSI14;
- ATR14 e `atr_pct`;
- volume ratio;
- regime;
- budget/sizing.

Questi valori diventano:

```text
current_state
entry_* al BUY
```

### Da Usare Solo Come Diagnostica

- MFE;
- post-sell forensics;
- watch timeout;
- promotion age;
- max buy age;
- ML duration;
- advice validity temporale post-promozione.

## Granularita Dati

Il processo scientifico richiede che i dati usati per decidere e quelli usati per visualizzare siano distinguibili.

Aggiornamento Consiglio 2026-07-04:

```text
per il ciclo operativo corrente Bollinger Context V1:
indicatori / contract / WATCH / BUY = 1m chiuso
microbar 5s = replay, diagnostica, timing, gap detection
```

La possibilita' teorica di usare una base microbar resta valida solo come esperimento futuro separato. Non e' la base
strategica corrente e non puo' alimentare nuove RUN PAPER Context V1 prima di un nuovo charter.

Campionamento:

- se gli indicatori sono calcolati su 1m, il contratto deve dichiararlo;
- se WATCH usa microbar, la finestra e la cadenza devono essere coerenti;
- se il replay mostra 1m, non puo' spiegare con precisione decisioni prese su 5s.
- nel ciclo corrente, se WATCH usa microbar per indicatori/BUY, la run e' classificata `PRE_A0_MIXED_GRANULARITY` e
  non e' evidenza strategica valida.
- nel ciclo corrente, la 1m decisionale deve provenire da candele chiuse nel bucket `binance`, non da aggregazione di
  microbar o realtime.
- la finestra decisionale deve contenere abbastanza candele 1m chiuse per il massimo periodo indicatore: EMA50 richiede
  almeno 50 barre, quindi una finestra operativa da 15 minuti e' insufficiente.

Metriche obbligatorie per replay:

```text
source_bucket
interval_seconds
candle_count
max_gap_seconds
synthetic_backfill = true/false
```

Metriche obbligatorie per decision snapshot:

```text
decision_source_bucket
decision_interval_seconds
decision_candle_state
decision_feature_window_minutes
decision_candle_count
decision_max_gap_seconds
decision_staleness_seconds
```

## Validazione Scientifica

Ogni RUN PAPER deve produrre:

```text
execution_id
symbol
setup
trigger
regime_at_watch
regime_at_buy
contract values
current values
entry values
exit values
BUY reason
SELL reason
net_profit_quote
net_return
net_loss_quote
MFE only as diagnostic
proposed block reason, if stricter model would block
```

Metriche aggregate:

- expectancy per setup;
- win/loss per setup;
- loss cap rate per setup;
- zero-MFE rate solo diagnostico;
- average net loss quote;
- context pass rate;
- blocked-by-regime;
- blocked-by-momentum;
- blocked-by-volume;
- blocked-by-risk;
- replay gap rate.

## Conclusione

Il processo scientificamente coerente non e':

```text
LOSS -> aggiungo un gate casuale -> meno BUY -> allento tutto
```

ma:

```text
definizione setup -> formule indicatori -> soglie storiche per setup -> verifica realtime -> BUY -> SELL economica
```

La strategia deve quindi restare Bollinger-centrica, ma con contratto Context V1 piu' severo e leggibile.

La modifica piu' importante non e' reintrodurre MFE. E':

```text
impedire BUY semanticamente sbagliati e limitare la perdita economica massima senza usare MFE come uscita
```
