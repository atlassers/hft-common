# Bollinger Context V1 Plan

Data: 2026-06-30.

## Scopo

Questo piano e' il candidato successivo a `BOLLINGER_ONLY_V2`.

Il runtime corrente resta:

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

`BOLLINGER_CONTEXT_V1` non sostituisce la pipeline. Estende il contratto operativo gia' stabilizzato in
Bollinger-only:

- PAPER-only da `/management`;
- tabella runtime `hft.acdc_live_bb_advice`;
- advice setup-specifiche con `bb_setup` e `bb_trigger`;
- WATCH fail-closed su contratto incompleto;
- BUY senza cap numerici concorrenti, limitata solo da budget/sizing exchange;
- SELL e forensics leggibili per setup, trigger, ingresso e uscita;
- nessun ramo SHADOW o REAL operativo.

La differenza e' decisionale: Bollinger resta il centro del segnale, ma trend, momentum, volume, volatilita',
regime e liquidita' diventano feature contrattuali esplicite.

Questo piano richiede conferma esplicita prima dell'implementazione perche' modifica il vincolo attuale
`BOLLINGER_ONLY`.

## Stato Di Partenza Consolidato

Completato in Bollinger-only:

- runtime contract rinominato a `LiveBbAdvice` / `acdc_live_bb_advice`;
- readiness `BB_READY`;
- namespace operativo `bb.*`;
- setup:
  - `BB_REENTRY_MEAN_REVERSION_LONG`;
  - `BB_SQUEEZE_BREAKOUT_LONG`;
- trigger:
  - `BB_REENTRY_CONFIRMED`;
  - `BB_UPPER_BREAKOUT_CONFIRMED`;
- `PreBuyWatchService` dispatcha per setup e compra solo se il trigger setup-specifico passa;
- `policyJson` conserva `entry_bb_setup`, `entry_bb_trigger` e contratto `bb_*`;
- SELL usa target/loss/no-MFE/trailing derivati dal contratto Bollinger;
- `/management` e' il cockpit operativo primario;
- build/deploy dei moduli toccati completati;
- REAL vietata.

Questo piano deve riusare quel lavoro, non reintrodurre un contratto parallelo.

## Evidenza Operativa Recente

### RUN Brevi 79-81

Tre RUN PAPER brevi hanno prodotto:

- 300 decisioni;
- 0 BUY;
- motivi dominanti:
  - `WATCH_WAITING_BUY_CONTRACT`;
  - `WATCH_EXPIRED`;
  - `PAPER_BUY_STOPPED` nella RUN fermata subito.

Diagnostica Context V1 su candidati vicini al trigger:

- 69 candidati focus;
- 4 pass diagnostici;
- nessuna evidenza conclusiva per miglioramento perche' non c'erano trade reali.

### Finestra PAPER 30m RUN 82-91

La finestra di circa 30 minuti ha prodotto 10 execution automatiche (`82`-`91`) e 9 trade reali:

- WIN: 3;
- LOSS: 6;
- PnL netto: `-0.5464600973`;
- `BB_SQUEEZE_BREAKOUT_LONG`: 2 trade, 0 WIN, 2 LOSS, netto `-0.0823747351`;
- `BB_REENTRY_MEAN_REVERSION_LONG`: 7 trade, 3 WIN, 4 LOSS, netto `-0.4640853622`.

Motivi delle WIN:

- tutte le WIN sono reentry;
- vincono quando generano MFE rapidamente e `EXIT_BB_DYNAMIC_TRAILING` conserva parte del movimento.

Motivi delle LOSS:

- prevalgono zero-MFE e loss-cap;
- breakout senza trend confermato produce falsi segnali;
- reentry in contesto non abbastanza range o gia' deteriorato compra movimenti senza recupero.

Forensics:

- utili per attribuzione;
- spesso `INCONCLUSIVE_GRANULARITY` per gap microbar 41-60s;
- la granularita' va migliorata prima di usare post-exit come prova forte di SELL second-level.

Diagnostica Context V1 sugli ingressi reali:

- trade attuali: 9;
- trade che passerebbero i gate diagnostici Context V1: 2;
- PnL effettivo: `-0.5464600973`;
- PnL dei soli trade tenuti dal filtro diagnostico: `-0.1464585003`;
- trade bloccati: 7, PnL aggregato `-0.4000015969`.

Conclusione: Context V1 avrebbe migliorato il risultato numerico della finestra, ma sarebbe rimasto negativo e avrebbe
scartato anche alcune WIN. Il piano quindi va corretto: non basta aggiungere EMA/RSI/volume come gate semplici; serve
un contratto di regime piu' severo, soprattutto per reentry.

## Decisione Del Consiglio

Saggio ascoltatore:

- il lavoro Bollinger-only e' una base buona: non va buttato;
- Context V1 deve essere un'estensione disciplinata, non una riscrittura.

Scienziato severo:

- i nuovi indicatori non possono essere filtri opachi;
- ogni campo deve dichiarare fonte, timeframe, ruolo e soglia candidate-specific;
- il filtro diagnostico ha ridotto perdita ma non ha prodotto expectancy positiva;
- e' obbligatorio separare breakout e reentry nelle metriche.

Mediano pragmatico:

- implementare prima un V1 minimo;
- usare solo OHLCV 1m gia' disponibile e microbar come supporto forensics;
- non introdurre order-flow/spread come gate finche' la fonte non e' affidabile.

Decisione unica:

`BOLLINGER_CONTEXT_V1` resta candidato, ma corretto cosi':

1. riusa i setup runtime gia' implementati;
2. non introduce `BB_RANGE_REVERSION_LONG` come nuovo nome operativo nella prima fase;
3. estende `BB_REENTRY_MEAN_REVERSION_LONG` con regime/range/context gate;
4. estende `BB_SQUEEZE_BREAKOUT_LONG` con trend/momentum/volume gate;
5. mantiene SELL esistente nella fase 1;
6. valuta il miglioramento solo per setup e solo dopo PAPER con trade reali.

## Audit AS-IS

Il piano e' stato ricontrollato contro lo stato corrente dei moduli.

### hft-common

AS-IS:

- esistono `BollingerSetupType` e `BollingerTriggerType`;
- esistono costanti `BOLLINGER_ONLY` e `BOLLINGER_ONLY_V2`;
- esistono le key Bollinger `bb_*`, `history_bb_*`, `live_bb_*`, `entry_bb_*`;
- non esiste ancora una family runtime `BOLLINGER_CONTEXT_V1`;
- non esistono ancora enum/costanti condivise per regime, trend, momentum, volume e risk.

Implicazione:

- Context V1 deve estendere `RemConstants` e gli enum esistenti senza duplicare setup/trigger;
- non deve introdurre una seconda gerarchia contrattuale parallela.

### docbrown

AS-IS:

- `InfluxSnapshotService` calcola gia' le feature Bollinger operative;
- `BlankRemCandidateService` seleziona gia' reentry e squeeze/breakout separatamente;
- `RollingPaperPromotion` produce advice setup-specifiche e contratto Bollinger;
- non sono ancora calcolati EMA/RSI/ATR/volume ratio/regime nel contratto live.

Implicazione:

- Context V1 deve aggiungere un layer context sopra la selection Bollinger esistente;
- non deve ricreare il motore candidate da zero.

### acdc

AS-IS:

- `PreBuyWatchService` ha gia' dispatch per `BB_REENTRY_MEAN_REVERSION_LONG` e `BB_SQUEEZE_BREAKOUT_LONG`;
- il trigger audit controlla common pass, reentry contract e breakout contract;
- le reason WATCH sono ancora generiche (`WATCH_WAITING_BUY_CONTRACT`, `WATCH_EXPIRED`, ecc.);
- `PaperRunService` scrive `policyJson` con setup/trigger e contratto Bollinger;
- SELL e post-sell forensics sono gia' legati al contratto Bollinger.

Implicazione:

- Context V1 deve innestarsi dopo il trigger audit Bollinger e prima della BUY;
- le nuove reason context devono essere enum/costanti, non stringhe locali;
- SELL fase 1 deve restare invariato per isolare l'effetto dei nuovi gate di ingresso.

### kenshiro / hft-fe

AS-IS:

- l'automazione management salva `mode=BOLLINGER_ONLY_V2`;
- `/management` espone e orchestra solo Bollinger-only;
- `REAL_RUN` e' bloccata;
- non esistono ancora action/config runtime per scegliere Context V1;
- FE mostra il cockpit `/management`, ma non ha sezioni regime/context.

Implicazione:

- Context V1 deve essere introdotto come family selezionabile solo dopo approvazione;
- fino ad allora `/management` deve continuare a mostrare chiaramente `BOLLINGER_ONLY_V2`.

### influxer / InfluxDB

AS-IS:

- OHLCV 1m su bucket realtime e' sufficiente per EMA/RSI/ATR/volume ratio;
- microbar e' utile per forensics, ma la RUN 82-91 ha mostrato gap 41-60s in molti post-exit.

Implicazione:

- la fase 1 puo' usare OHLCV 1m per i gate Context;
- la forensics second-level non deve essere usata come prova forte finche' i gap microbar restano larghi.

## Setup Ammessi

### BB_SQUEEZE_BREAKOUT_LONG

Obiettivo:

- comprare espansione di volatilita' dopo compressione solo se il contesto conferma movimento long.

ML identifica:

- squeeze recente o bandwidth percentile basso;
- espansione iniziale;
- rottura superiore;
- trend non contrario;
- volume e momentum compatibili.

WATCH compra solo se:

```text
setup = BB_SQUEEZE_BREAKOUT_LONG
AND trigger = BB_UPPER_BREAKOUT_CONFIRMED
AND market_regime IN (REGIME_SQUEEZE, REGIME_EXPANSION, REGIME_TREND_UP)
AND current bb_upper_breach=1
AND current bb_percent_b >= contract_min_breakout_percent_b
AND current bb_bandwidth_delta > 0
AND current bb_expansion=1
AND current bb_middle_slope >= contract_min_middle_slope
AND ema9 > ema21
AND close_1m > ema50
AND rsi14 >= contract_min_breakout_rsi
AND rsi14 <= contract_max_breakout_rsi
AND volume_ratio_1m_20m >= contract_min_volume_ratio
AND liquidity contract pass, if reliable source exists
```

Correzione da evidenza RUN 82-91:

- breakout con `bb_upper_breach` e `bb_bandwidth_delta > 0` ma `ema9 <= ema21` o `close <= ema50` va bloccato;
- il volume ratio da solo non basta;
- zero-MFE breakout deve diventare metrica primaria.

### BB_REENTRY_MEAN_REVERSION_LONG

Obiettivo:

- comprare rientro dentro le bande solo in regime laterale controllato.

ML identifica:

- banda inferiore violata;
- rientro confermato;
- assenza di trend down o trend troppo ripido;
- recupero non gia' esausto;
- volatilita' controllata.

WATCH compra solo se:

```text
setup = BB_REENTRY_MEAN_REVERSION_LONG
AND trigger = BB_REENTRY_CONFIRMED
AND market_regime = REGIME_RANGE
AND bb_lower_breach=1
AND bb_reentry_confirmed=1
AND bb_percent_b >= contract_min_reentry_percent_b
AND bb_percent_b <= contract_max_reentry_percent_b
AND abs(bb_middle_slope) <= contract_max_middle_slope_abs
AND abs(ema50_slope) <= contract_max_ema50_slope_abs
AND rsi14 <= contract_max_oversold_recovery_rsi
AND volume_ratio_1m_20m <= contract_max_reentry_volume_spike_ratio
AND atr_pct <= contract_max_atr_pct
AND liquidity contract pass, if reliable source exists
```

Correzione da evidenza RUN 82-91:

- reentry con RSI troppo alto o slope eccessiva va bloccato;
- reentry in spike di volume non e' automaticamente bullish;
- il range regime deve essere esplicito, non inferito solo da `bb_reentry_confirmed`;
- il setup produce WIN, ma anche le LOSS piu' pesanti: serve contratto piu' severo.

## Feature Del Contratto

Ogni feature operativa nuova deve essere pubblicata in blocchi coerenti:

- `history_*`: valutazione research/promotion;
- `live_*`: snapshot usato da live-score/WATCH;
- `entry_*`: snapshot immutabile al BUY;
- `exit_*`: opzionale, snapshot SELL/forensics.

Le feature canoniche runtime devono essere valorizzate da `live_*`; `entry_*` va scritto nel `policyJson`.

### Bollinger

- `bb_setup`
- `bb_trigger`
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

Prima classificazione ammessa:

```text
REGIME_RANGE:
  abs(ema50_slope) <= contract_max_ema50_slope_abs
  AND bb_bandwidth_percentile <= contract_max_range_bandwidth_percentile
  AND atr_pct <= contract_max_range_atr_pct

REGIME_EXPANSION:
  bb_bandwidth_delta > 0
  AND bb_expansion=1

REGIME_TREND_UP:
  ema9 > ema21
  AND close_1m > ema50

REGIME_TREND_DOWN:
  ema9 < ema21
  AND close_1m < ema50

REGIME_CHAOS:
  atr_pct > contract_max_chaos_atr_pct
  OR volume_ratio_1m_20m > contract_max_chaos_volume_ratio
```

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
- `volume_spike_risk`
- `volume_score`

### Risk / Liquidity

- `atr14`
- `atr_pct`
- `spread_pct`, se disponibile;
- `liquidity_score`;
- `risk_score`;
- `max_spread_pct`;
- `min_quote_volume_5m`, se disponibile.

Regola: se spread/liquidita' non hanno fonte affidabile, marcare `UNKNOWN` e non usarli come gate hard.

## Interventi Per Modulo

### hft-common

Interventi:

- aggiungere costante condivisa `BOLLINGER_CONTEXT_V1`;
- se viene introdotto un enum strategy family, deve mappare le costanti esistenti:
  - `BOLLINGER_ONLY`;
  - `BOLLINGER_ONLY_V2`;
  - `BOLLINGER_CONTEXT_V1`.
- riusare i setup gia' operativi:
  - `BB_SQUEEZE_BREAKOUT_LONG`;
  - `BB_REENTRY_MEAN_REVERSION_LONG`.
- riusare i trigger gia' operativi:
  - `BB_UPPER_BREAKOUT_CONFIRMED`;
  - `BB_REENTRY_CONFIRMED`.
- aggiungere enum `RemMarketRegime`:
  - `REGIME_TREND_UP`;
  - `REGIME_TREND_DOWN`;
  - `REGIME_RANGE`;
  - `REGIME_SQUEEZE`;
  - `REGIME_EXPANSION`;
  - `REGIME_CHAOS`.
- aggiungere costanti JSON in `RemConstants` per tutte le nuove feature context.
- aggiungere prefissi coerenti:
  - `history_market_regime`, `live_market_regime`, `entry_market_regime`;
  - `history_ema9`, `live_ema9`, `entry_ema9`, ecc.;
  - `history_rsi14`, `live_rsi14`, `entry_rsi14`;
  - `history_volume_ratio_1m_20m`, `live_volume_ratio_1m_20m`, `entry_volume_ratio_1m_20m`;
  - `history_atr14`, `live_atr14`, `entry_atr14`.
- aggiungere reason condivise per WATCH context:
  - `WATCH_WAITING_BB_BREAKOUT_CONTEXT`;
  - `WATCH_WAITING_BB_REENTRY_CONTEXT`;
  - `WATCH_CONTEXT_CONTRACT_INCOMPLETE`;
  - `WATCH_REGIME_BLOCKED`;
  - `WATCH_TREND_BLOCKED`;
  - `WATCH_MOMENTUM_BLOCKED`;
  - `WATCH_VOLUME_BLOCKED`;
  - `WATCH_LIQUIDITY_BLOCKED`.

Note:

- niente stringhe operative nei consumer;
- nessun ritorno a nomi `ml_*` o contratti runtime legacy;
- nessun payload operativo nuovo deve emettere `reversal_*`.
- ogni key nuova deve avere una sola fonte canonica in `RemConstants`, poi shim locale nei moduli se serve.

### docbrown

Interventi:

- `InfluxSnapshotService`:
  - calcolare EMA 9/21/50 su 1m;
  - calcolare RSI 14;
  - calcolare volume ratio 1m/20m;
  - calcolare ATR 14 e `atr_pct`;
  - mantenere feature Bollinger gia' introdotte: percentile, delta, middle slope.
- candidate generation:
  - riusare candidate setup-specifiche Bollinger-only;
  - aggiungere classificazione regime;
  - calcolare score per setup e regime;
  - non promuovere candidate senza contratto context completo.
- `RollingPaperPromotion`:
  - pubblicare `history_*` e `live_*` per Bollinger, regime, trend, momentum, volume, risk;
  - includere soglie candidate-specific:
    - RSI min/max per breakout;
    - RSI max per reentry;
    - min volume ratio breakout;
    - max volume spike reentry;
    - max EMA50 slope;
    - max ATR pct;
    - target/loss/timeout.

Sequenza consigliata:

1. aggiungere calcolo indicatori puramente diagnostico nel report research, senza promotion;
2. aggiungere `market_regime` e score context nei candidate;
3. pubblicare `history_*` context nelle advice;
4. solo dopo test, pubblicare `live_*` context e renderlo requisito di promotion;
5. solo dopo deploy ACDC compatibile, rendere `BOLLINGER_CONTEXT_V1` selezionabile.

Vincoli AS-IS:

- la selection esistente `PromotionSelection` resta il punto di innesto;
- `BollingerContract` va esteso o affiancato da `ContextContract`, non sostituito al primo step;
- le soglie context devono derivare dai campioni candidate-specific, non da global defaults opachi;
- se un indicatore non ha abbastanza barre, la candidate non e' promotable in Context V1.

Test:

- calcolo EMA/RSI/ATR/volume ratio;
- regime range/trend/expansion/chaos;
- advice senza setup, trigger o regime non promotable;
- payload nuovi senza `reversal_*`.

### acdc

Interventi:

- `OutcomeQualityModelService`:
  - consumare e propagare `history_*` e `live_*`;
  - valorizzare i campi canonici runtime solo da `live_*`;
  - scrivere `entry_*` nel `policyJson`.
- `PreBuyWatchService`:
  - mantenere dispatch per setup gia' implementato;
  - aggiungere gate context dopo il trigger Bollinger setup-specifico;
  - fail-closed se setup/regime/trigger/context manca;
  - non reintrodurre cap numerici su WATCH o BUY;
  - reason distinte:
    - `WATCH_WAITING_BB_BREAKOUT_CONTEXT`;
    - `WATCH_WAITING_BB_REENTRY_CONTEXT`;
    - `WATCH_CONTEXT_CONTRACT_INCOMPLETE`;
    - `WATCH_REGIME_BLOCKED`;
    - `WATCH_TREND_BLOCKED`;
    - `WATCH_MOMENTUM_BLOCKED`;
    - `WATCH_VOLUME_BLOCKED`;
    - `WATCH_LIQUIDITY_BLOCKED`.
- `PaperRunService`:
  - salvare setup, trigger, regime e context gates nelle decisioni;
  - includere nuove feature in `policyJson`;
  - esporre Context V1 pass/fail in forensics.
- SELL:
  - fase 1: mantenere SELL Bollinger-only esistente;
  - fase 2: aggiungere SELL setup-specific solo dopo campione PAPER:
    - breakout: trailing ATR o middle-band failure;
    - reentry: target middle band e invalidazione range.

Sequenza consigliata:

1. estendere `BbAdviceFeatures` / mapper contract per portare `history_*`, `live_*`, `entry_*` context;
2. aggiungere un `ContextGateAudit` separato dal `TriggerAudit` Bollinger;
3. nel WATCH, eseguire:

```text
common pass -> setup trigger audit -> context gate audit -> BUY
```

4. se il context fallisce, mantenere la watch aperta finche' non scade, come oggi accade per il trigger Bollinger;
5. scrivere in decisione:
   - `context_checked`;
   - `context_failed`;
   - `context_passed`;
   - reason specifica del primo blocco dominante;
6. nel `policyJson`, congelare i valori `entry_*` usati per il BUY;
7. lasciare SELL invariato nella prima PAPER Context V1.

Vincoli AS-IS:

- `bb_buy_contract_pass` puo' restare diagnostico/derivato, ma non deve bypassare il context gate;
- le reason generiche esistenti restano per Bollinger-only;
- le reason context devono comparire solo quando strategy family e' `BOLLINGER_CONTEXT_V1`;
- se strategy family e' `BOLLINGER_ONLY_V2`, il comportamento deve restare byte-for-byte equivalente salvo logging.

Test:

- fail-closed su contratto context incompleto;
- breakout compra solo con trend/momentum/volume coerenti;
- reentry compra solo in range;
- no BUY se regime `REGIME_TREND_DOWN` o `REGIME_CHAOS`;
- BUY continua a rispettare solo budget/sizing, non cap arbitrari.
- regressione: Bollinger-only continua a comprare con gli stessi trigger setup-specifici di oggi.

### kenshiro

Interventi:

- `/management/state`:
  - esporre strategy family;
  - esporre count per setup/regime;
  - esporre latest setup/regime selected;
  - mostrare zero-MFE/loss-cap per setup;
  - mostrare context pass/fail summary.
- Actions:
  - mantenere orchestrazione PAPER-only;
  - aggiungere action/config solo dopo approvazione:
    - `APPLY_BOLLINGER_ONLY`;
    - `APPLY_BOLLINGER_CONTEXT_V1`.
- Diagnostics:
  - attribution per setup/regime;
  - expectancy, win/loss, MFE, zero-MFE, loss-cap;
  - confronto "actual vs Context V1 filtered" per run PAPER.

Sequenza consigliata:

1. aggiungere lettura config strategy family, default `BOLLINGER_ONLY_V2`;
2. mostrare la family corrente in `/management/state`;
3. aggiungere sezioni diagnostiche context senza cambiare orchestrazione;
4. aggiungere `APPLY_BOLLINGER_CONTEXT_V1` solo quando DocBrown e ACDC sono deployati con contract compatibile;
5. bloccare `PAPER_BOLLINGER_START` se family Context V1 e advice senza regime/context completo;
6. mantenere `AUTO_BOLLINGER_START` come nome action, oppure introdurre alias UI solo dopo aver evitato confusione operativa.

Vincoli AS-IS:

- oggi `autoAutomationStart` persiste `mode=BOLLINGER_ONLY_V2`;
- nessuna action Context esiste ancora;
- la prima implementazione deve essere backward-compatible con le RUN Bollinger-only.

Test:

- state summary include family/setup/regime;
- PAPER resta bloccata se readiness non passa;
- REAL resta bloccata.

### hft-fe

Interventi:

- `/management`:
  - mostrare strategy family;
  - mostrare setup, trigger e regime su advice;
  - mostrare WATCH reason context-specific;
  - aggiungere sezioni compatte:
    - setup distribution;
    - regime distribution;
    - latest PAPER by setup/regime;
    - zero-MFE/loss-cap by setup/regime;
    - Context V1 diagnostic comparison.
- Nessuna nuova pagina.

Sequenza consigliata:

1. estendere i tipi management con family/regime/context opzionali;
2. visualizzare i nuovi campi solo se presenti;
3. mantenere le label Bollinger-only esistenti quando family non e' Context;
4. aggiungere pannello diagnostico compatto, non una nuova landing/page;
5. impedire azioni Context se backend non espone action abilitate.

Test:

- `npm run check`;
- `npm run build`.

### influxer

Interventi:

- nessun cambio obbligatorio per fase 1 se OHLCV 1m e microbar sono disponibili;
- verificare copertura OHLCV 1m su `binance-realtime`;
- migliorare continuita' microbar se si vuole usare forensics second-level come prova SELL;
- non usare spread/order-flow come gate finche' non c'e' fonte affidabile.

Test:

- query OHLCV 1m per almeno 2h;
- verifica gap massimi per simboli USDC;
- report gap microbar per trade con forensics inconclusive.

### DB / Migration

ACDC:

- aggiungere config `rem.ml.strategy.family=BOLLINGER_CONTEXT_V1` solo se piano approvato;
- aggiungere config candidate-specific default per:
  - min/max RSI breakout;
  - max RSI reentry;
  - min volume ratio breakout;
  - max volume spike reentry;
  - max EMA50 slope;
  - max ATR pct;
  - ATR multiplier opzionale per SELL fase 2.
- non creare nuove tabelle al primo step se JSON contract basta.

DocBrown:

- nessuna tabella nuova obbligatoria;
- valutare indice/query solo se diagnostiche setup/regime diventano lente.

Ordine migration/config:

1. aggiungere solo chiavi config e costanti;
2. usare JSON advice/policy per feature context;
3. creare colonne dedicate solo se `/management` o report diventano lenti;
4. non modificare le migration di purge legacy per introdurre Context V1;
5. non cambiare checksum Flyway gia' deployati salvo procedura esplicita.

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

Score reentry indicativo:

```text
score =
  0.25 * range_regime_score
+ 0.25 * bollinger_reentry_score
+ 0.15 * oversold_recovery_score
+ 0.15 * controlled_volatility_score
+ 0.10 * liquidity_score
- 0.25 * trend_down_risk
- 0.20 * chaos_risk
- 0.15 * volume_spike_risk
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
- advice con setup, trigger e regime completi;
- `policyJson` con `entry_*` context completo;
- management mostra context pass/fail.

Fase 2 - PAPER:

- PAPER solo da `/management`;
- almeno 5 run con trade reali per setup prima di giudicare;
- non aggregare breakout e reentry nella stessa metrica;
- ogni run deve produrre report actual vs Context V1 filtered;
- ogni loss-cap e zero-MFE deve indicare setup, regime e gate context.

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
- context pass rate;
- performance per setup;
- performance per regime;
- performance per simbolo.

## Exit Criteria

`BOLLINGER_CONTEXT_V1` e' candidato a proseguire solo se:

- almeno un setup riduce zero-MFE rispetto a Bollinger-only V2;
- almeno un setup produce MFE positivo ripetuto;
- loss-cap rate non domina;
- expectancy per setup e' non negativa su campione minimo;
- Context V1 filtered migliora PnL e non scarta in modo sistematico le WIN migliori;
- forensics complete distinguono setup, trigger, regime e gate context;
- runtime finale resta pulito.

## Rischi

- Overfitting su troppi indicatori.
- Troppe soglie candidate-specific poco leggibili.
- Riduzione eccessiva del numero di trade.
- Dati spread/order-flow non disponibili o non affidabili.
- Confondere reentry valido con coltello che cade.
- Scartare WIN buone per soglie RSI/slope troppo rigide.
- Usare forensics post-exit non conclusiva come prova forte quando microbar ha gap larghi.

## Decisione Richiesta

Scegliere questo piano se si accetta di cambiare il vincolo strategico attuale da:

```text
solo Bollinger
```

a:

```text
Bollinger come segnale centrale + contesto regime/trend/momentum/volume/risk
```

Questo piano e' piu' promettente del Bollinger-only puro secondo la RUN 82-91, ma non e' ancora validato:
riduce la perdita diagnostica, resta negativo e richiede una implementazione disciplinata del regime.
