# Bollinger Context V1 Plan

Data: 2026-06-28.

## Scopo

Questo piano modella lo scenario:

```text
Regime -> Trend -> Bollinger -> Momentum -> Volume -> Trigger -> Risk management
```

Il processo operativo resta:

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

Ma il vincolo strategico cambia: Bollinger non e' piu' l'unica famiglia decisionale. Bollinger resta il centro del
segnale, mentre trend, momentum, volume, volatilita' e liquidita' diventano feature decisionali del contratto.

Questo piano richiede conferma esplicita prima dell'implementazione perche' modifica il vincolo attuale
`Bollinger-only`.

## Diagnosi AS-IS

Le PAPER `37` e `38` mostrano che un solo trigger Bollinger non basta:

- `bb_buy_contract_pass=1` ha autorizzato BUY;
- `SUSDC` ha prodotto MFE netto `0`;
- entrambe le run sono finite in loss-cap;
- nella run `38`, piu' pulita, `entry_drift=0` e il risultato e' comunque negativo.

Conclusione:

- WATCH funziona tecnicamente;
- il contratto Bollinger e' troppo povero per filtrare contesto e qualita' del movimento;
- serve distinguere il setup e validarlo con segnali che misurano cose diverse.

## Decisione Del Consiglio

Saggio ascoltatore:

- passare a un contesto piu' ricco ha senso, ma senza stravolgere la pipeline.

Scienziato severo:

- e' vietato aggiungere indicatori come filtri opachi;
- ogni nuovo campo deve avere semantica, fonte, timeframe e ruolo nel contratto.

Mediano pragmatico:

- non introdurre subito venti indicatori;
- prima versione con set minimo: EMA, RSI, volume ratio, ATR/liquidita'.

Decisione unica:

Implementare `BOLLINGER_CONTEXT_V1` con due setup long separati:

- `BB_SQUEEZE_BREAKOUT_LONG`
- `BB_RANGE_REVERSION_LONG`

Ogni advice deve dichiarare setup, regime, trigger, feature storiche, feature live e risk contract.

## Setup Ammessi

### BB_SQUEEZE_BREAKOUT_LONG

Obiettivo:

- comprare espansione di volatilita' dopo compressione.

ML identifica:

- squeeze recente;
- bandwidth percentile basso;
- espansione iniziale;
- trend non contrario;
- volume e momentum compatibili.

WATCH compra solo se:

```text
setup = BB_SQUEEZE_BREAKOUT_LONG
AND regime IN (REGIME_SQUEEZE, REGIME_EXPANSION, REGIME_TREND_UP)
AND close_1m > bb_upper
AND bb_bandwidth_delta > 0
AND volume_ratio_1m_20m >= contract_min_volume_ratio
AND ema9 > ema21
AND close > ema50
AND rsi14 >= contract_min_rsi
AND rsi14 <= contract_max_rsi
AND liquidity/spread contract pass
```

### BB_RANGE_REVERSION_LONG

Obiettivo:

- comprare rientro dentro le bande solo in regime laterale.

ML identifica:

- range o assenza di trend forte;
- banda inferiore violata;
- rientro o probabilita' di rientro;
- momentum non ancora deteriorato;
- rischio operativo compatibile.

WATCH compra solo se:

```text
setup = BB_RANGE_REVERSION_LONG
AND regime = REGIME_RANGE
AND bb_lower_breach=1
AND bb_reentry_confirmed=1
AND rsi14 <= contract_max_oversold_recovery_rsi
AND abs(ema50_slope) <= contract_max_ema50_slope_abs
AND volume_sell_pressure_not_increasing = true, if available
AND liquidity/spread contract pass
```

## Nuove Famiglie Feature

### Bollinger

- `bb_percent_b`
- `bb_bandwidth`
- `bb_bandwidth_percentile`
- `bb_bandwidth_delta`
- `bb_middle_slope`
- `bb_lower_breach`
- `bb_upper_breach`
- `bb_reentry_confirmed`
- `bb_squeeze`
- `bb_expansion`

### Regime

- `market_regime`
- `regime_score`
- `regime_range`
- `regime_squeeze`
- `regime_expansion`
- `regime_trend_up`
- `regime_trend_down`
- `regime_chaos`

### Trend

- `ema9`
- `ema21`
- `ema50`
- `ema9_gt_ema21`
- `close_gt_ema50`
- `ema50_slope`
- `trend_score`

### Momentum

- `rsi14`
- `rsi_breakout_ok`
- `rsi_oversold_recovery`
- `momentum_score`

### Volume

- `volume_ratio_1m_20m`
- `volume_confirmation`
- `volume_score`

### Risk / Liquidity

- `atr14`
- `atr_pct`
- `spread_pct`, se disponibile;
- `liquidity_score`;
- `risk_score`;
- `max_spread_pct`;
- `min_quote_volume_5m`, se disponibile.

## Interventi Per Modulo

### hft-common

Interventi:

- aggiungere enum `RemStrategyFamily` o estendere equivalente:
  - `BOLLINGER_ONLY`
  - `BOLLINGER_CONTEXT_V1`
- aggiungere enum `RemSetupType`:
  - `BB_SQUEEZE_BREAKOUT_LONG`
  - `BB_RANGE_REVERSION_LONG`
- aggiungere enum `RemMarketRegime`:
  - `REGIME_TREND_UP`
  - `REGIME_TREND_DOWN`
  - `REGIME_RANGE`
  - `REGIME_SQUEEZE`
  - `REGIME_EXPANSION`
  - `REGIME_CHAOS`
- aggiungere enum `RemEntryTriggerType`:
  - `BB_BREAKOUT_CONFIRMED`
  - `BB_REENTRY_CONFIRMED`
- aggiungere costanti JSON in `RemConstants` per tutte le nuove feature.

Note:

- niente stringhe operative nei moduli consumer;
- ogni nuova key `history_*`, `live_*`, `entry_*`, `exit_*` deve avere costante condivisa o shim locale verso common.

### docbrown

Interventi:

- `InfluxSnapshotService`:
  - calcolare EMA 9/21/50 su 1m;
  - calcolare RSI 14;
  - calcolare volume ratio 1m/20m;
  - calcolare ATR 14 o proxy su OHLC disponibile;
  - calcolare feature Bollinger estese: percentile, delta, middle slope.
- `BlankRemCandidateService`:
  - separare candidate per setup;
  - classificare regime per ogni riga;
  - calcolare score per setup;
  - salvare `setup`, `market_regime`, `entry_trigger`;
  - promuovere solo candidate con contratto completo.
- `RollingPaperPromotion`:
  - produrre advice setup-specific;
  - pubblicare blocchi `history_*` e `live_*` per Bollinger, regime, trend, momentum, volume e risk;
  - includere soglie candidate-specific nel contratto:
    - min/max RSI;
    - min volume ratio;
    - max spread;
    - ATR/loss/target;
    - trigger setup-specifico.

Test:

- calcolo EMA/RSI/ATR/volume ratio;
- setup breakout distinto da setup reversion;
- advice senza setup o regime deve essere non promotable;
- payload nuovi senza `reversal_*`.

### acdc

Interventi:

- `OutcomeQualityModelService`:
  - consumare e propagare le nuove feature `history_*` e `live_*`;
  - valorizzare campi canonici runtime solo da `live_*`;
  - scrivere `entry_*` al BUY.
- `PreBuyWatchService`:
  - introdurre dispatch per `setup`;
  - implementare trigger breakout;
  - implementare trigger range reversion;
  - fail-closed se setup/regime/trigger manca;
  - reason distinte:
    - `WATCH_WAITING_BB_BREAKOUT_CONTEXT`
    - `WATCH_WAITING_BB_REENTRY_CONTEXT`
    - `WATCH_CONTEXT_CONTRACT_INCOMPLETE`
    - `WATCH_REGIME_BLOCKED`
    - `WATCH_LIQUIDITY_BLOCKED`
- `PaperRunService`:
  - salvare setup e regime nelle decisioni/posizioni;
  - includere nuove feature in `policy_json`.
- SELL:
  - fase 1: mantenere loss-cap/target/no-MFE/trailing esistenti, ma derivare target/loss dal contratto context;
  - fase 2: aggiungere SELL setup-specific:
    - breakout: trailing ATR/middle band;
    - reversion: target middle band.

Test:

- fail-closed su contratto context incompleto;
- breakout compra solo con trend/momentum/volume coerenti;
- reversion compra solo in range;
- no BUY se regime `REGIME_TREND_DOWN` o `REGIME_CHAOS`.

### kenshiro

Interventi:

- `/management/state`:
  - esporre strategy family;
  - esporre count per setup/regime;
  - mostrare latest setup selected;
  - mostrare zero-MFE/loss-cap per setup.
- Actions:
  - mantenere `AUTO_AB_START` come orchestrazione;
  - non reintrodurre SHADOW;
  - aggiungere eventuale action config per scegliere strategy family solo se approvata:
    - `APPLY_BOLLINGER_ONLY`
    - `APPLY_BOLLINGER_CONTEXT_V1`
- Diagnostics:
  - report attribution per setup/regime;
  - summary `expectancy`, win/loss, MFE, zero-MFE.

Test:

- current state mostra family/setup;
- PAPER resta bloccata se `ML_READY=false`;
- REAL resta bloccata.

### hft-fe

Interventi:

- `/management`:
  - mostrare strategy family;
  - mostrare setup/regime su advice;
  - mostrare WATCH reason context-specific;
  - aggiungere sezioni compatte:
    - setup distribution;
    - latest PAPER by setup;
    - zero-MFE/loss-cap by setup.
- Nessuna nuova pagina.

Test:

- `npm run check`;
- `npm run build`.

### influxer

Interventi:

- nessun cambio obbligatorio per fase 1 se OHLCV 1m e microbar 1m sono gia' disponibili;
- verificare copertura OHLCV su `binance` e `binance-microbar`;
- se spread/liquidita' non disponibili, marcare `spread_pct` come `UNKNOWN` e non usarlo come gate finche' non c'e'
  fonte affidabile.

Test:

- query Influx su OHLCV 1m per almeno 2h;
- verifica gap massimi per simboli USDC.

### DB / Migration

ACDC:

- aggiungere config `rem.ml.strategy.family=BOLLINGER_CONTEXT_V1` solo se piano approvato;
- aggiungere config candidate-specific default per:
  - min volume ratio;
  - RSI bounds;
  - max spread/liquidity se disponibili;
  - ATR multiplier per SELL fase 2.
- non creare nuove tabelle al primo step se JSON contract basta.

DocBrown:

- nessuna tabella nuova obbligatoria;
- valutare indice/query solo se diagnostiche setup/regime diventano lente.

## Scoring Live

Score breakout indicativo:

```text
score =
  0.25 * regime_score
+ 0.20 * bollinger_squeeze_or_expansion_score
+ 0.15 * trend_score
+ 0.15 * volume_score
+ 0.10 * momentum_score
+ 0.10 * liquidity_score
- 0.20 * mean_reversion_risk
- 0.30 * chaos_risk
```

Score reversion indicativo:

```text
score =
  0.25 * range_regime_score
+ 0.25 * bollinger_reentry_score
+ 0.15 * oversold_recovery_score
+ 0.15 * controlled_volatility_score
+ 0.10 * liquidity_score
- 0.25 * trend_down_risk
- 0.20 * chaos_risk
```

I pesi non devono diventare soglie globali nascoste. Devono essere:

- nel contratto;
- tracciati in advice;
- leggibili in forensics.

## Validazione

Fase 0 - Charter:

- utente approva esplicitamente il cambio da `BOLLINGER_ONLY` a `BOLLINGER_CONTEXT_V1`.

Fase 1 - Technical:

- build/deploy di tutti i moduli toccati;
- diagnostics clean;
- payload senza `reversal_*`;
- advice con setup/regime completi.

Fase 2 - PAPER:

- PAPER solo se `ML_READY=true`;
- almeno 5 run per setup prima di giudicare;
- non aggregare breakout e reversion nella stessa metrica.

Metriche:

- expectancy;
- profit factor;
- win rate;
- avg win/loss;
- max drawdown;
- MFE/MAE;
- zero-MFE rate;
- loss-cap rate;
- WATCH expired rate;
- performance per setup;
- performance per regime;
- performance per simbolo.

## Exit Criteria

`BOLLINGER_CONTEXT_V1` e' candidato a proseguire solo se:

- almeno un setup riduce zero-MFE rispetto a PAPER `37`/`38`;
- almeno un setup produce MFE positivo ripetuto;
- loss-cap rate non domina;
- expectancy per setup e' non negativa su campione minimo;
- forensics complete distinguono setup/regime;
- runtime finale resta pulito.

## Rischi

- Overfitting su troppi indicatori.
- Troppe soglie candidate-specific poco leggibili.
- Riduzione eccessiva del numero di trade.
- Dati spread/order-flow non disponibili o non affidabili.
- Confondere setup breakout e setup reversion.

## Decisione Richiesta

Scegliere questo piano se si accetta di cambiare il vincolo strategico attuale e passare da:

```text
solo Bollinger
```

a:

```text
Bollinger come segnale centrale + contesto trend/momentum/volume/risk
```

Questo piano e' piu' promettente dal punto di vista finanziario, ma piu' rischioso dal punto di vista scientifico:
richiede piu' feature, piu' contratti, piu' forensics e maggiore disciplina contro l'overfitting.
