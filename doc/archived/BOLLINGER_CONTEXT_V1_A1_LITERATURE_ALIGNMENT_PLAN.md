# Bollinger Context V1 A1 Literature Alignment Plan

Data: 2026-07-04.

Documento progettuale vincolante per correggere la regressione operativa osservata dopo A0.

Questo documento estende, senza sostituire:

```text
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md
```

## Decisione Del Consiglio

A0 ha corretto la provenance temporale, ma ha esposto una regressione operativa certa:

```text
RUN PAPER 118 = nessun BUY per blocco WATCH_WAITING_BUY_CONTRACT.
```

La causa non e' A0 in se'. La causa e' che il runtime ACDC applica il contratto Bollinger/Context V1 come una
congiunzione troppo rigida di condizioni simultanee su candela 1m chiusa.

Decisione unica:

```text
A1 deve mantenere la fonte decisionale 1m chiusa, ma deve riallineare trigger, contratto e diagnostica BUY alla
letteratura Bollinger e alle formule del documento scientifico.
```

Non sono ammessi:

- rollback a `binance-microbar` come fonte strategica;
- uso di `binance-realtime` per autorizzare BUY;
- ammorbidimenti non misurati;
- nuove RUN PAPER prima che A1 esponga il motivo di blocco BUY con contatori granulari.

## Evidenza Certa Della Regressione

Fonte: MySQL operativo, RUN PAPER `118`, profilo `REM_CURRENT`.

Query diagnostiche eseguite su `acdc_paper_decision` e `acdc_paper_position`.

Risultato:

```text
phase = ENTRY
action = HOLD
accepted = false
reason = WATCH_WAITING_BUY_CONTRACT
count = 2370

phase = ENTRY
action = HOLD
accepted = false
reason = WATCH_OPENED_WAITING_BUY_CONTRACT
count = 30

positions = 0
```

Quindi:

```text
decisioni ENTRY totali = 2400
BUY accettati = 0
posizioni aperte = 0
SELL = 0
```

Distribuzione setup:

```text
reentry setup code 1 = 1680 decisioni
breakout setup code 2 = 720 decisioni
```

Failure reentry:

```text
reentry_total = 1680
lower_breach_fail = 1120
reentry_confirm_fail = 1401
percent_b_fail = 1261
middle_slope_fail = 880
age_fail = 66
full_pass = 0
```

Failure breakout:

```text
breakout_total = 720
upper_breach_fail = 536
percent_b_fail = 720
bandwidth_delta_fail = 394
expansion_fail = 394
bandwidth_percentile_fail = 0
middle_slope_fail = 720
full_pass = 0
```

Conclusione certa:

```text
RUN 118 non compra per fallimento sistematico del TriggerAudit, non per budget, non per SELL, non per exchange sizing,
non per A0 readiness e non per mancanza di advice PAPER_ELIGIBLE.
```

## Fonti Di Letteratura Vincolanti

Fonti gia' richiamate dal documento scientifico e confermate:

- Bollinger Bands official rules:
  `https://www.bollingerbands.com/bollinger-band-rules`
- Bollinger Bands / formula standard, StockCharts:
  `https://chartschool.stockcharts.com/table-of-contents/technical-overlays/bollinger-bands`
- Bollinger BandWidth, StockCharts:
  `https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/bollinger-bandwidth`
- Bollinger Band Squeeze, StockCharts:
  `https://chartschool.stockcharts.com/table-of-contents/trading-strategies-and-models/trading-strategies/bollinger-band-squeeze`
- Bollinger `%B`, TradingView:
  `https://www.tradingview.com/support/solutions/43000501971-bollinger-bands-b-b/`
- RSI/Wilder RSI:
  `https://help.tc2000.com/m/69404/l/747071-rsi-wilder-s-rsi`
- ATR, StockCharts:
  `https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/average-true-range-atr`
- EMA:
  `https://www.cmcmarkets.com/en-gb/technical-analysis/exponential-moving-average`
- OBV/volume confirmation, StockCharts:
  `https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/on-balance-volume-obv`
- Chandelier Exit, StockCharts:
  `https://chartschool.stockcharts.com/table-of-contents/technical-overlays/chandelier-exit`

Lettura operativa vincolante:

- Bollinger Bands definiscono se un prezzo e' relativamente alto o basso.
- Le bande da sole non bastano per una decisione robusta.
- Indicatori di momentum, volume, volatilita' e trend sono conferme, non sostituti del segnale Bollinger.
- Squeeze significa contrazione di volatilita'; il segnale operativo richiede successiva rottura della banda.
- ATR misura volatilita', non direzione.
- RSI misura momentum/condizione relativa, non autorizza da solo BUY.
- EMA descrive trend e bias, non sostituisce il trigger Bollinger.

## Formule Scientifiche Confermate

Le formule restano corrette:

```text
middle = SMA(close, 20)
stddev = standard_deviation(close, 20)
upper = middle + 2 * stddev
lower = middle - 2 * stddev
percent_b = (price - lower) / (upper - lower)
bandwidth = (upper - lower) / middle
```

Formule ausiliarie:

```text
EMA_t = (close_t - EMA_{t-1}) * (2 / (n + 1)) + EMA_{t-1}
RSI = 100 - (100 / (1 + RS))
TR_t = max(high_t - low_t, abs(high_t - close_{t-1}), abs(low_t - close_{t-1}))
ATR_t = ((ATR_{t-1} * 13) + TR_t) / 14
volume_ratio_1m_20m = volume_current_1m / average(volume_1m, 20)
```

Vincolo A0 confermato:

```text
Tutte queste formule strategiche devono essere calcolate su candele 1m chiuse da bucket `binance`.
```

## Difformita' Certe Tra Letteratura, Documento Scientifico E Runtime

### D1 - TriggerAudit Trasforma Bollinger In Una Congiunzione Sterilizzante

Codice:

```text
acdc/src/main/java/it/mbc/hft/acdc/service/PreBuyWatchService.java
```

Runtime reentry richiede contemporaneamente:

```text
bb_lower_breach = 1
bb_reentry_confirmed = 1
min_percent_b <= percent_b <= max_percent_b
abs(bb_middle_slope) <= max_middle_slope_abs
reentry_age_seconds <= max_reentry_age_seconds
```

Poi, solo dopo, richiede anche il ContextGate:

```text
REGIME_RANGE
not REGIME_CHAOS
not REGIME_TREND_DOWN
abs(ema50_slope) <= contract_max_ema50_slope_abs
rsi14 <= contract_max_oversold_recovery_rsi
volume_ratio <= contract_max_reentry_volume_spike_ratio
atr_pct <= contract_max_range_atr_pct
bandwidth_percentile <= contract_max_range_bandwidth_percentile
percent_b <= max_reentry_percent_b
```

Questa sequenza e' piu' restrittiva della lettura scientifica. La letteratura ammette conferme multiple, ma non impone
che tutte le conferme contrattuali storiche siano condizioni di trigger simultaneo prima della BUY.

Decisione A1:

```text
TriggerAudit deve contenere solo il segnale Bollinger setup-specifico minimo.
ContextGateAudit deve contenere le conferme di qualita'.
Ogni fallimento deve essere diagnosticato separatamente.
```

### D2 - Reentry Min Percent B E' Applicato Come Blocco, Ma Il Documento Lo Descrive Come Range Preferibile

Documento scientifico:

```text
0.20 <= percent_b <= 0.65 preferibile
percent_b <= 0.80 limite massimo operativo
percent_b > 1 blocco obbligatorio per reentry
```

Runtime:

```text
percent_b < contract_min_reentry_percent_b => fail trigger
percent_b > contract_max_reentry_percent_b => fail trigger
```

Evidenza RUN 118:

```text
reentry_percent_b_fail = 1261 / 1680
```

Decisione A1:

```text
Per reentry, percent_b > max e percent_b > 1 restano blocchi.
percent_b < min non deve essere blocco immediato se esiste lower breach/recovery valido; deve diventare stato
WATCH_WAITING_REENTRY_RECOVERY oppure context quality penalty.
```

Motivo:

- `%B` basso indica prezzo vicino/sotto lower band.
- In reentry, il sistema deve osservare recupero controllato, non scartare automaticamente un simbolo ancora vicino
  alla lower band.

### D3 - Lower/Upper Breach Sono Calcolati Contro Bande Correnti, Non Bande Contemporanee Alla Candela

Codice:

```text
InfluxSnapshotService.addBollingerFeatures(...)
lowerBreach = window.anyMatch(tick.price <= lower_corrente)
upperBreach = window.anyMatch(tick.price >= upper_corrente)
```

Difformita':

La letteratura interpreta il prezzo rispetto alle bande calcolate per la stessa finestra temporale. Il runtime invece
confronta candele passate della finestra con la banda calcolata sull'ultima finestra corrente.

Decisione A1:

```text
Ogni evento breach deve essere calcolato bar-by-bar:
lower_breach_t = close_t <= lower_t
upper_breach_t = close_t >= upper_t
```

Poi derivare:

```text
latest_lower_breach_at
latest_upper_breach_at
reentry_age_seconds = current_closed_bar_time - latest_lower_breach_at
breakout_age_seconds = current_closed_bar_time - latest_upper_breach_at
```

### D4 - ReentryConfirmed E' Troppo Debole Come Formula Ma Troppo Forte Come Gate

Runtime:

```text
reentryConfirmed = latestLowerBreach.isPresent()
  AND price >= lower_corrente
  AND percent_b >= 0
```

Problemi certi:

- usa `latestLowerBreach` derivato dalla banda corrente;
- richiede conferma come gate assoluto;
- non distingue recupero appena iniziato da recupero gia' troppo esteso;
- non conserva la sequenza evento: lower breach -> recupero verso middle.

Decisione A1:

```text
reentry_confirmed deve essere derivato da evento bar-by-bar:
latest_lower_breach_at presente
AND current_percent_b >= 0
AND current_percent_b <= reentry_max_percent_b
AND current_bar_time - latest_lower_breach_at <= max_reentry_age_seconds
```

Il minimo `%B` non deve bloccare il trigger. Deve servire a classificare:

```text
BELOW_RECOVERY_ZONE
IN_RECOVERY_ZONE
OVEREXTENDED
```

### D5 - Breakout Richiede Tutto, Ma La RUN 118 Dimostra Che Nessun Breakout Era Presente

Runtime breakout richiede:

```text
upper_breach = 1
percent_b >= min_breakout_percent_b
bandwidth_delta > 0
bb_expansion = 1
bandwidth_percentile <= max_bandwidth_percentile
middle_slope >= min_middle_slope
```

Evidenza RUN 118:

```text
breakout_total = 720
breakout_percent_b_fail = 720 / 720
breakout_middle_slope_fail = 720 / 720
breakout_upper_breach_fail = 536 / 720
```

Conclusione certa:

```text
Gli advice breakout promossi nella RUN 118 non erano breakout realtime acquistabili sulla candela 1m chiusa.
```

Decisione A1:

DocBrown non deve promuovere un setup breakout a PAPER_ELIGIBLE se il live-score corrente non mostra almeno:

```text
percent_b >= 1 OR upper_breach_current_or_recent = 1
bandwidth_delta >= 0
```

Se manca questo, l'advice deve restare:

```text
WATCH_CANDIDATE
```

non:

```text
PAPER_ELIGIBLE
```

### D6 - Promotion Confonde "Storicamente Valido" Con "Runtime Comprabile"

Formula scientifica corretta:

```text
candidate = selection storica
contract = soglie storiche setup-specifiche
current_state = indicatori realtime ricalcolati
BUY = current_state soddisfa contract
```

Difformita':

DocBrown produce advice PAPER_ELIGIBLE che ACDC trasforma in WATCH, ma molti advice non hanno trigger realtime vicino
alla soglia di acquisto. La RUN 118 dimostra che il sistema ha osservato advice non comprabili per 2400 decisioni.

Decisione A1:

Introdurre due stati distinti:

```text
PAPER_WATCH_ELIGIBLE = storico valido, osservabile, ma trigger realtime non ancora presente.
PAPER_BUY_ELIGIBLE = storico valido e trigger realtime coerente con setup presente o appena confermato.
```

Solo `PAPER_BUY_ELIGIBLE` puo' diventare BUY quando passa il ContextGate.

### D7 - Diagnostica BUY Insufficiente

Reason attuale:

```text
WATCH_WAITING_BUY_CONTRACT
```

Questo reason e' troppo generico.

Decisione A1:

Ogni decisione HOLD deve esporre almeno:

```text
buy_trigger_fail_reason
buy_trigger_failed_keys
buy_trigger_failed_count
buy_context_fail_reason
buy_context_failed_keys
buy_context_failed_count
```

Reason granulari minime:

```text
WATCH_WAITING_REENTRY_LOWER_BREACH
WATCH_WAITING_REENTRY_RECOVERY
WATCH_REENTRY_OVEREXTENDED
WATCH_REENTRY_MIDDLE_SLOPE_BLOCKED
WATCH_REENTRY_AGE_EXPIRED
WATCH_WAITING_BREAKOUT_UPPER_BREACH
WATCH_WAITING_BREAKOUT_PERCENT_B
WATCH_WAITING_BREAKOUT_EXPANSION
WATCH_BREAKOUT_MIDDLE_SLOPE_BLOCKED
WATCH_CONTEXT_REGIME_BLOCKED
WATCH_CONTEXT_MOMENTUM_BLOCKED
WATCH_CONTEXT_VOLUME_BLOCKED
WATCH_CONTEXT_RISK_BLOCKED
```

## Nuovo Modello Scientifico A1

### Reentry

Scopo:

```text
comprare recupero controllato da zona bassa Bollinger, non inseguire estensione verso upper band.
```

Trigger minimo A1:

```text
latest_lower_breach_at presente nel lookback evento
AND current_percent_b >= 0
AND current_percent_b <= reentry_max_percent_b
AND current_percent_b <= 1
AND reentry_age_seconds <= max_reentry_age_seconds
```

Classificazione non bloccante:

```text
current_percent_b < reentry_min_percent_b => WATCH_WAITING_REENTRY_RECOVERY
reentry_min_percent_b <= current_percent_b <= reentry_max_percent_b => REENTRY_ZONE_OK
current_percent_b > reentry_max_percent_b => WATCH_REENTRY_OVEREXTENDED
current_percent_b > 1 => BLOCK_REENTRY_ABOVE_UPPER_BAND
```

ContextGate reentry:

```text
not REGIME_CHAOS
not REGIME_TREND_DOWN
abs(ema50_slope) <= contract_max_ema50_slope_abs
rsi14 <= contract_max_oversold_recovery_rsi
volume_ratio_1m_20m <= contract_max_reentry_volume_spike_ratio
atr_pct <= contract_max_range_atr_pct
```

Nota:

`REGIME_RANGE` resta preferito, ma non deve cancellare automaticamente un reentry se gli altri vincoli mostrano
recupero controllato e il regime non e' chaos/trend_down. A1 deve decidere se `REGIME_RANGE` e' hard gate o quality
gate solo dopo backtest/diagnostica comparativa sui campioni gia' registrati.

### Breakout

Scopo:

```text
comprare rottura direzionale dopo compressione/espansione.
```

Trigger minimo A1:

```text
current_percent_b >= contract_min_breakout_percent_b
AND latest_upper_breach_at presente o current_percent_b >= 1
AND bandwidth_delta > 0
```

ContextGate breakout:

```text
not REGIME_CHAOS
not REGIME_TREND_DOWN
EMA9 > EMA21
close > EMA50
volume_ratio_1m_20m >= contract_min_volume_ratio
contract_min_breakout_rsi <= rsi14 <= contract_max_breakout_rsi
atr_pct <= contract_max_atr_pct
```

Squeeze:

```text
squeeze_precondition = bandwidth_percentile low before breakout
breakout_confirmation = upper breach / percent_b >= 1 after squeeze
```

Non e' corretto richiedere che ogni candela in WATCH sia contemporaneamente squeeze e breakout; squeeze e breakout sono
fasi sequenziali.

## Interventi Modulo Per Modulo

### hft-common

Interventi:

1. Aggiungere costanti per reason granulari A1.
2. Aggiungere metadata:
   - `buy_trigger_fail_reason`;
   - `buy_trigger_failed_keys`;
   - `buy_trigger_failed_count`;
   - `buy_context_fail_reason`;
   - `buy_context_failed_keys`;
   - `buy_context_failed_count`;
   - `latest_lower_breach_at_epoch_seconds`;
   - `latest_upper_breach_at_epoch_seconds`;
   - `reentry_zone_state`;
   - `breakout_zone_state`.
3. Aggiungere classificazione evidenza:
   - `NEGATIVE_OPERATIONAL_SIGNAL`;
   - `NO_BUY_TRIGGER_WINDOW`.

Perche':

Senza reason granulari, una run senza trade non e' analizzabile scientificamente.

### influxer

Interventi:

1. Nessun cambio strategico alla fonte dati A0.
2. Confermare che bucket `binance` conserva OHLCV 1m con `_time` = open time Binance della candela chiusa.
3. Mantenere `synthetic_backfill` solo su microbar replay/backfill.

Perche':

Il problema RUN 118 non e' la scrittura dati. E' la semantica trigger/contract.

### docbrown

Interventi:

1. Separare promotion storica da buy eligibility runtime:
   - `PAPER_WATCH_ELIGIBLE`;
   - `PAPER_BUY_ELIGIBLE`.
2. Per breakout, non promuovere a `PAPER_BUY_ELIGIBLE` se live-score non mostra `percent_b >= 1` o upper breach
   recente.
3. Per reentry, non usare `min_reentry_percent_b` come hard lower bound del trigger; usarlo come stato zona.
4. Pubblicare `latest_lower_breach_at`, `latest_upper_breach_at`, `reentry_zone_state`, `breakout_zone_state`.
5. Calcolare breach bar-by-bar con bande contemporanee.
6. Esportare nel contract se una soglia e':
   - `HARD_GATE`;
   - `QUALITY_GATE`;
   - `DIAGNOSTIC_ONLY`.

Perche':

DocBrown deve dire se un advice e' storicamente interessante o immediatamente comprabile. RUN 118 dimostra che oggi
questa distinzione manca.

### acdc

Interventi:

1. Correggere `InfluxSnapshotService.addBollingerFeatures`:
   - calcolare bande per ogni barra nel lookback evento;
   - derivare lower/upper breach contemporanei;
   - derivare latest breach timestamp e age.
2. Rifattorizzare `PreBuyWatchService.triggerAudit`:
   - trigger minimo setup-specifico;
   - failure keys granulari;
   - non usare `min_reentry_percent_b` come hard fail.
3. Rifattorizzare `ContextGateAudit`:
   - conferme di qualita' separate dal trigger Bollinger;
   - hard gate dichiarati dal contract;
   - quality gate tracciati anche quando non bloccano.
4. Cambiare reason da generico a granulare.
5. Persistire feature di audit nelle decisioni e nelle watch.
6. Non cambiare SELL strategica: resta 1m chiusa.

Perche':

ACDC e' il punto in cui RUN 118 si blocca. La correzione deve rendere il blocco leggibile e riportare il BUY alla
semantica Bollinger corretta.

### kenshiro

Interventi:

1. Esporre in `/management/runs/{executionId}`:
   - conteggio per `buy_trigger_fail_reason`;
   - conteggio per `buy_context_fail_reason`;
   - matrice setup x fail reason.
2. Esporre in `/management/state`:
   - `last_run_no_buy_reason`;
   - `last_run_trigger_fail_distribution`;
   - `last_run_context_fail_distribution`.
3. Se una run termina con 0 BUY e trigger full pass = 0:
   - classificare `NEGATIVE_OPERATIONAL_SIGNAL`;
   - non classificarla solo `INCONCLUSIVE`.

Perche':

RUN 118 non e' solo inconclusiva: e' un segnale operativo negativo sul BUY trigger.

### hft-fe

Interventi:

1. In `/management`, mostrare:
   - top trigger blockers;
   - top context blockers;
   - setup interessato;
   - numero decisioni.
2. In `/trades`, per WATCH:
   - mostrare `buy_trigger_fail_reason`;
   - mostrare failed keys;
   - mostrare `%B`, min/max, lower/upper breach, age, slope.
3. Evidenziare `PAPER_WATCH_ELIGIBLE` vs `PAPER_BUY_ELIGIBLE`.

Perche':

Il FE deve impedire che un generico `WATCH_WAITING_BUY_CONTRACT` nasconda la causa reale.

### Script E Diagnostiche

Interventi:

1. Aggiungere script diagnostico `diagnose-a1-buy-blockers.sh` oppure action `/management` equivalente.
2. Output minimo:
   - execution id;
   - setup;
   - trigger fail distribution;
   - context fail distribution;
   - top symbols;
   - A0 metadata;
   - classification.
3. Lo script resta `DIAGNOSTIC_ONLY` se non passa da `/management`.

Perche':

La diagnosi A1 deve essere ripetibile senza query manuali ad hoc.

## Exit Criteria A1

A1 e' implementabile solo se il documento resta rispettato in ogni modulo e se i conflitti aperti sono risolti prima
del codice.

Conflitti aperti da risolvere prima dell'implementazione:

- `REGIME_RANGE` reentry: il documento scientifico iniziale lo elenca come hard gate, A1 lo riclassifica come
  preferenza/quality gate finche' non e' provato che il hard gate non sterilizzi il BUY. Decisione A1 vincolante:
  durante la prima implementazione `REGIME_RANGE` non deve essere hard gate se `REGIME_CHAOS=0`,
  `REGIME_TREND_DOWN=0` e gli altri vincoli di rischio/momentum/volume passano.
- `PAPER_ELIGIBLE` legacy: resta ammesso solo come alias transitorio per advice osservabili. Il nuovo runtime deve
  esporre separatamente `PAPER_WATCH_ELIGIBLE` e `PAPER_BUY_ELIGIBLE`; la BUY puo' partire solo dal secondo stato.

A1 e' verificato solo quando:

1. RUN diagnostica su dati esistenti produce distribuzione fail reason granulare per RUN 118.
2. DocBrown distingue `PAPER_WATCH_ELIGIBLE` da `PAPER_BUY_ELIGIBLE`.
3. ACDC calcola breach bar-by-bar con bande contemporanee.
4. Reentry non blocca piu' per `percent_b < min_reentry_percent_b`; classifica invece `WATCH_WAITING_REENTRY_RECOVERY`.
5. Breakout non viene promosso a buy-eligible se `percent_b < min_breakout_percent_b` su live-score.
6. `/management` mostra per ogni run senza BUY:
   - motivo dominante;
   - setup dominante;
   - fail keys dominanti.
7. `/trades` mostra per ogni WATCH:
   - trigger state;
   - context state;
   - A0 decision metadata;
   - replay metadata separati.
8. Nessuna RUN PAPER nuova viene usata come evidenza strategica se non contiene questi metadata.

## Classificazione RUN 118 Aggiornata

La classificazione precedente resta valida per A0 readiness:

```text
VALID_STRATEGIC_EVIDENCE per A0 provenance/readiness.
```

La classificazione BUY deve essere corretta:

```text
NEGATIVE_OPERATIONAL_SIGNAL per BUY trigger.
```

La classificazione finanziaria resta:

```text
INCONCLUSIVE per PnL, perche' non ci sono BUY/SELL.
```

Quindi RUN 118 non puo' essere usata per dire che la strategia Bollinger e' profittevole o non profittevole. Puo'
essere usata per dire con certezza che il runtime A0+A0.1 attuale non genera BUY perche' il TriggerAudit non passa.

## Ordine Di Implementazione

1. Aggiungere diagnostica fail reason senza cambiare decisioni.
2. Eseguire diagnostica RUN 118 e confermare i contatori nel FE/management.
3. Correggere calcolo breach bar-by-bar in DocBrown e ACDC.
4. Separare `PAPER_WATCH_ELIGIBLE` e `PAPER_BUY_ELIGIBLE`.
5. Modificare trigger reentry per non bloccare su `%B < min`.
6. Modificare promotion breakout per non generare buy eligibility senza breakout live.
7. Build/deploy cross-repo.
8. Solo dopo, nuova PAPER via `/management`.

## Regola Finale

Non si deve cercare "piu' trade" abbassando soglie a caso.

Si deve ottenere:

```text
trade quando il segnale Bollinger setup-specifico esiste su 1m chiusa;
nessun trade quando il segnale non esiste;
diagnostica certa quando non si compra.
```

## Esito Implementazione 2026-07-04

A1 e' stato implementato e deployato su `docbrown`, `acdc`, `kenshiro`, `hft-fe` con costanti condivise in
`hft-common`.

Implementato:

- breach Bollinger bar-by-bar con bande contemporanee;
- `latest_lower_breach_at_epoch_seconds`, `latest_upper_breach_at_epoch_seconds`, `reentry_zone_state`,
  `breakout_zone_state`;
- separazione `bb_advice_paper_watch_eligible` / `bb_advice_paper_buy_eligible`;
- `bb_advice_freshness_contract_pass` separato da `bb_advice_economic_safe_pass`;
- reentry `%B < min` come `WATCH_WAITING_REENTRY_RECOVERY`;
- breakout buy-eligible solo con trigger live coerente;
- reason granulari WATCH/BUY e codici numerici di audit;
- `/management/runs/{executionId}.a1BuyDiagnostics`;
- tab A1 in `/management` e dettagli A1 in `/trades`;
- script `acdc/scripts/diagnose-a1-buy-blockers.sh` `DIAGNOSTIC_ONLY`.

Verifica PAPER:

- RUN 119 ha individuato un residuo gate comune (`bb_advice_freshness_contract_pass=0`) e ha portato alla correzione
  della distinzione tra contract fresco e target economico positivo;
- RUN 120 dopo correzione: `720` ENTRY, `0` BUY, `0` posizioni, blocker granulari dominanti
  `WATCH_REENTRY_OVEREXTENDED=295` e `WATCH_WAITING_BREAKOUT_PERCENT_B=286`;
- RUN 120 e' `VALID_STRATEGIC_EVIDENCE` per A0/A1, `NEGATIVE_OPERATIONAL_SIGNAL` per la finestra BUY,
  `INCONCLUSIVE` finanziaria.
