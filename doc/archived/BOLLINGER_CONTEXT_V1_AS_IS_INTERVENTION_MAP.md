# Bollinger Context V1 AS-IS Intervention Map

Data: 2026-07-03.

Documento charter strategico operativo.

Dal 2026-07-03 questo documento sostituisce `BOLLINGER_CONTEXT_V1_PLAN.md` come vincolo strategico primario. La base
scientifica obbligatoria e':

```text
hft-common/doc/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md
```

`BOLLINGER_CONTEXT_V1_PLAN.md` resta baseline tecnica precedente e riferimento di regressione.

## Decisione Operativa

Non reintrodurre MFE come condizione SELL.

Il problema osservato nella RUN PAPER `111` non e' che manca MFE in SELL. Il problema e' che alcuni BUY passano con
contesto debole o semanticamente ambiguo rispetto al setup dichiarato:

- reentry che entra gia' esteso verso/sopra upper band;
- reentry con RSI gia' carico;
- trade su mercato piatto o con ATR insufficiente rispetto alle fee;
- breakout/reentry non separati abbastanza nella costruzione del contratto;
- loss cap percentuale troppo largo rispetto al costo economico accettabile per trade da circa 25 USDC.

L'intervento deve quindi rafforzare:

```text
storico -> contratto setup-specifico -> WATCH realtime -> BUY -> SELL invalidation/cost-aware
```

Il ML/DocBrown puo' dedurre soglie dal passato. Il WATCH ACDC deve decidere usando solo stato vivo ricalcolato.

## Revisione Del Consiglio 2026-07-03

Rilettura dopo prima stesura:

- il documento era coerente sul runtime `WATCH -> BUY -> SELL`, ma incompleto sul layer `ML`;
- mancava la mappa esplicita degli script che invocano DocBrown;
- mancava una separazione chiara tra workflow ML Quarkus, endpoint `/management` e wrapper shell;
- la sezione script era troppo generica e poteva lasciare credere che gli script possano diventare un ramo operativo
  parallelo.

Correzione vincolante:

```text
ML = workflow DocBrown Quarkus + orchestrazione /management.
Gli script shell possono essere solo wrapper diagnostici/manuali, mai un canale decisionale parallelo.
```

Lo script attuale:

```text
acdc/scripts/acdc-run-rem-ml.sh
```

invoca direttamente:

```text
POST ${DOCBROWN_BASE_URL}/rem/research/${PROFILE_KEY}/run
```

e salva il JSON in:

```text
acdc/target/rem-ml/latest.json
```

Questo e' utile come diagnostica, ma non puo':

- promuovere advice;
- avviare PAPER;
- saltare `/management`;
- scrivere contratti runtime;
- diventare requisito per BUY.

## Revisione Del Consiglio 2026-07-04 - Allineamento Decisionale 1m

Rilettura dopo RUN PAPER globalmente positive su campione cumulato e dopo decisione di non passare ora a
`5s/20s/30s/40s` come base strategica.

La domanda non e' piu':

```text
usiamo 1m o 5s?
```

La decisione architetturale e':

```text
indicatori / contract / WATCH / BUY = 1m
microbar 5s = replay, diagnostica, timing, gap detection
```

### Posizione Del Consiglio

Saggio ascoltatore:

- consolidare la base 1m prima di introdurre nuove cadenze;
- le ultime evidenze PAPER non giustificano una migrazione strategica immediata a frequenze piu' basse;
- il rischio maggiore ora e' rendere il risultato non interpretabile mescolando basi temporali.

Scienziato severo:

- un contratto calcolato su 1m e un WATCH calcolato su 5s non sono lo stesso esperimento;
- `%B`, RSI, EMA, ATR, regime e `volume_ratio_1m_20m` devono avere la stessa semantica temporale in DocBrown e ACDC;
- la candela 1m corrente non chiusa puo' cambiare valore e non deve essere trattata come equivalente storico della
  candela chiusa.

Mediano pragmatico:

- non serve riscrivere la pipeline;
- serve separare esplicitamente le API e i payload:
  - decisionale 1m;
  - replay microbar;
  - diagnostica realtime;
- se la base dichiarata non e' disponibile o non combacia, WATCH deve fallire chiusa.

Decisione unica:

```text
Decision interval: 60s.
Decision bucket: binance, candele 1m chiuse.
Decision timestamp: open time della candela Binance 1m chiusa, persistito in Influx come _time.
Decision freshness: l'ultima candela usabile e' l'ultima candela chiusa disponibile.
Current 1m realtime bucket: non decisionale per contract/WATCH; ammesso solo per stato UI o diagnostica esplicita.
Microbar bucket: non decisionale per indicatori strategici; ammesso per replay, gap detection, timing e forensics.
Synthetic microbar backfill: mai equivalente a microbar live; deve essere marcato come synthetic_backfill.
```

Fino a chiusura dell'allineamento 1m end-to-end, non avviare nuove RUN PAPER.

## Vincoli Da Non Rompere

- REAL vietata.
- PAPER solo da `/management`.
- La tabella runtime resta `hft.acdc_live_bb_advice`.
- Setup ammessi:
  - `BB_REENTRY_MEAN_REVERSION_LONG`;
  - `BB_SQUEEZE_BREAKOUT_LONG`.
- Trigger ammessi:
  - `BB_REENTRY_CONFIRMED`;
  - `BB_UPPER_BREAKOUT_CONFIRMED`.
- `WATCH` non puo' usare tempo ML, promotion age, watch timeout o max buy age come condizione BUY.
- `WATCH` deve fallire chiusa se manca contratto o current state richiesto.
- `WATCH` e DocBrown devono usare la stessa base dati decisionale 1m chiusa per indicatori, contract e BUY.
- `binance-realtime` non puo' alimentare BUY: rappresenta la candela corrente non chiusa e resta diagnostico/UI.
- `binance-microbar` non puo' alimentare indicatori strategici Context V1: resta replay/diagnostica/timing/gap.
- microbar sintetiche da backfill 1m espanso a 5s devono essere marcate e non possono diventare evidenza di micro
  timing.
- Nessun ritorno a nomi o logiche `ml_*` legacy.
- Nessuna soglia DB `rem_*` legacy puo' diventare guardia di trading.
- Le stringhe operative devono restare in costanti/enum.

## AS-IS Sintetico

### hft-common

Gia' presenti:

- `RemConstants.BOLLINGER_CONTEXT_V1`.
- `RemStrategyFamily.BOLLINGER_CONTEXT_V1`.
- `RemMarketRegime`.
- Costanti Bollinger, context, `history_*`, `live_*`, `entry_*`.
- Reason WATCH context:
  - `WATCH_WAITING_BB_BREAKOUT_CONTEXT`;
  - `WATCH_WAITING_BB_REENTRY_CONTEXT`;
  - `WATCH_CONTEXT_CONTRACT_INCOMPLETE`;
  - `WATCH_REGIME_BLOCKED`;
  - `WATCH_TREND_BLOCKED`;
  - `WATCH_MOMENTUM_BLOCKED`;
  - `WATCH_VOLUME_BLOCKED`;
  - `WATCH_LIQUIDITY_BLOCKED`.

Gap:

- Mancano costanti piu' esplicite per i nuovi limiti proposti:
  - `contract_min_reentry_percent_b`;
  - `contract_max_reentry_percent_b`;
  - eventuale `contract_max_reentry_rsi`;
  - `contract_max_reentry_overextension_atr`;
  - `contract_min_breakout_volume_ratio`;
  - eventuale `contract_max_breakout_extension_atr`;
  - `contract_max_net_loss_quote`.
- Alcune costanti esistenti sono sufficienti ma semanticamente generiche. Va deciso se riusarle o introdurre nomi piu'
  specifici.

### docbrown

Gia' presenti:

- `docbrown/src/main/java/it/mbc/hft/docbrown/rem/service/InfluxSnapshotService.java`
  - calcola Bollinger, EMA9/21/50, RSI14, volume ratio, ATR14, `atr_pct`, regime flags.
  - usa soglie hard-coded per classificazione primaria.
- `docbrown/src/main/java/it/mbc/hft/docbrown/rem/service/BlankRemCandidateService.java`
  - separa reentry e breakout in `promotionSelection`.
  - costruisce `BollingerContract`.
  - costruisce `ContextContract`.
  - pubblica blocchi `history_*` e `live_*`.

Gap:

- Le soglie Context sono spesso percentile-based sui candidati selezionati. Questo puo' trasferire nel contratto la
  debolezza del campione invece di correggerla.
- Reentry e breakout sono separati, ma non abbastanza severi sul significato operativo:
  - reentry deve evitare overextension sopra middle/upper;
  - breakout deve richiedere volume/trend/espansione piu' forti.
- La classificazione regime contiene soglie statiche:
  - `ema50_slope <= 0.0015`;
  - bandwidth percentile `<= 0.70`;
  - `atr_pct <= 0.015`;
  - chaos `atr_pct > 0.025` o volume ratio `> 3.00`.
  Queste soglie devono diventare o candidate-specific, o dichiarate come default con audit.

### ML / Research / Script Layer

AS-IS:

- non esiste piu' `docbrown/python`; il laboratorio Python storico e' stato rimosso e non va ripristinato;
- il processo ML operativo e' nel runtime Quarkus DocBrown;
- endpoint DocBrown rilevanti:
  - `POST /rem/research/{profileKey}/run`;
  - `GET /rem/research/{profileKey}/status`;
  - `POST /rem/blank-candidates/{profileKey}/generate`;
  - `POST /rem/blank-candidates/{profileKey}/rolling-validation`;
  - `POST /rem/blank-candidates/{profileKey}/universe-triage`;
  - `POST /rem/blank-candidates/{profileKey}/universe-scheduler`;
  - `POST /rem/blank-candidates/{profileKey}/rolling-paper-promotion`;
  - `POST /rem/live-advice/{profileKey}/score`;
- endpoint Kenshiro `/management` rilevanti:
  - `RUN_RESEARCH`;
  - `RESEARCH_STATUS`;
  - `UNIVERSE_PREFILTER`;
  - `LIVE_SCORE`;
  - `ROLLING_VALIDATION`;
  - `ROLLING_PROMOTION`;
  - `AUTO_BOLLINGER_START`;
- script esistenti:
  - `acdc/scripts/acdc-run-rem-ml.sh`;
  - `acdc/scripts/lib/rem_contracts.sh`;
  - `acdc/scripts/check-script-contracts.py`.

Gap:

- `acdc-run-rem-ml.sh` chiama DocBrown direttamente e non passa da Kenshiro `/management`;
- il nome dello script contiene ancora `ml`, ma oggi il contratto strategico e' Bollinger Context V1;
- lo script salva solo l'output research, senza legare in modo esplicito:
  - source generation;
  - rolling validation;
  - live-score;
  - promotion;
  - readiness `/management`;
- il documento precedente non indicava se questo script fosse operativo, diagnostico o legacy.

Decisione:

- lo script resta ammesso solo come diagnostica/manual run research;
- l'orchestrazione operativa PAPER deve passare da `/management`;
- se lo script resta, va rinominato o affiancato con un nome coerente, ad esempio:
  - `run-bollinger-context-research.sh`;
  - `run-docbrown-research.sh`;
- se viene mantenuto, deve stampare chiaramente:
  - `DIAGNOSTIC_ONLY`;
  - profile;
  - endpoint invocato;
  - output file;
  - avviso che non promuove e non avvia PAPER;
- un eventuale script end-to-end puo' invocare solo endpoint `/management`, non direttamente DocBrown, salvo
  diagnostica esplicita.

### acdc

Gia' presenti:

- `acdc/src/main/java/it/mbc/hft/acdc/service/InfluxSnapshotService.java`
  - ricalcola feature live e non usa DocBrown come verita' del BUY.
- `acdc/src/main/java/it/mbc/hft/acdc/service/PreBuyWatchService.java`
  - ha `TriggerAudit` setup-specifico.
  - ha `ContextGateAudit`.
  - valuta reentry e breakout separatamente.
  - fallisce chiusa su numeri context mancanti.
- `acdc/src/main/java/it/mbc/hft/acdc/service/BbAdviceFeatures.java`
  - porta advice/policy feature in exit features.
- `acdc/src/main/java/it/mbc/hft/acdc/service/PaperRunService.java`
  - congela `policyJson`, calcola PnL e decisioni.
- `acdc/src/main/java/it/mbc/hft/acdc/service/GuardEvaluator.java`
  - gestisce SELL guards, incluso `BB_ADVICE_LOSS_CAP_EXIT`.

Gap:

- `reentryTriggerAudit` accetta il range `%B` contrattuale corrente, ma il contratto puo' essere troppo largo.
- `reentryContextAudit` blocca RSI alto, volume spike, ATR e bandwidth, ma non controlla esplicitamente:
  - prezzo sopra middle/upper come overextension reentry;
  - distanza prezzo-upper/middle normalizzata ATR;
  - coerenza "recupero ma non gia' esausto".
- `breakoutContextAudit` controlla trend, RSI, volume, ATR, ma non controlla esplicitamente:
  - estensione massima oltre upper band;
  - forza volume minima setup-specifica piu' severa;
  - fallimento immediato di breakout quando rientra sotto upper/middle.
- `BB_ADVICE_LOSS_CAP_EXIT` usa `bb_loss_cap_net_return`. Nella RUN `111`, valori circa `-0.008` permettono perdite
  tra circa `0.12` e `0.25` USDC su trade da circa `25` USDC. Questo e' troppo largo rispetto alla tolleranza indicata.

### kenshiro

Gia' presenti:

- `kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice/ManagementService.java`
  - espone `/management/state`.
  - espone `/management/trades`.
  - ha family/config `bb.strategy.family`.
  - ha action `APPLY_BOLLINGER_ONLY` e `APPLY_BOLLINGER_CONTEXT_V1`.
  - usa wording Context V1 nell'orchestrazione.

Gap:

- La diagnostica deve rendere visibile per ogni run:
  - setup;
  - regime;
  - gate context dominante;
  - loss cap in quote;
  - `actual vs stricter WATCH proposal`.
- Le run devono essere leggibili per setup senza query manuali.

### hft-fe

Gia' presenti:

- `/management` mostra family e stato.
- `/trades` mostra replay e decisioni.
- Fix recente: PnL non viene piu' confuso con prezzo SELL.

Gap:

- Il grafico/replay puo' ancora essere troppo rado se ACDC riceve punti Influx aggregati a 1 minuto.
- Il grafico/replay non deve far credere che la base decisionale sia 5s quando il BUY e' 1m.
- Serve rendere visibili i gate context e le soglie del contratto intorno a WATCH/BUY/SELL:
  - `%B`;
  - RSI;
  - ATR%;
  - volume ratio;
  - regime;
  - reason dominante.

### influxer / InfluxDB

Gia' presenti:

- `influxer/src/main/java/it/mbc/hft/influxer/xchange/stream/BinanceWebSocketClient.java`
  - costruisce microbar da 5s.
- `influxer/src/main/java/it/mbc/hft/influxer/influx/ShortRetentionBucketBackfillJob.java`
  - fa backfill short-retention.
- `acdc/src/main/java/it/mbc/hft/acdc/service/InfluxSnapshotService.java`
  - legge microbar con `aggregateWindow(every: %ss, fn: last)`.
- `acdc/src/main/java/it/mbc/hft/acdc/service/TradeReplayLiveService.java`
  - usa Influx per replay live.

Gap:

- Nella RUN `111`, TLMUSDC ha mostrato replay effettivo a passo circa 1 minuto, non 5 secondi.
- Prima di usare il replay come prova forte di micro timing SELL, serve una diagnostica gap per simbolo/run.
- Per WATCH/BUY e indicatori, la fonte 1m/microbar deve essere coerente tra DocBrown e ACDC. Non possiamo avere
  contratto su un campionamento e decisione live su un altro senza audit.
- `binance`, `binance-realtime` e `binance-microbar` devono avere ruoli non sovrapponibili:
  - `binance`: candele 1m chiuse decisionale;
  - `binance-realtime`: candela 1m corrente non decisionale;
  - `binance-microbar`: replay/forensics, non contract.
- Il backfill short-retention puo' produrre microbar sintetiche da candele 1m; queste non sono microbar live.

## Strategia Di Intervento

### Principio 1: Setup Prima, Indicatori Dopo

Gli indicatori non devono diventare filtri globali ciechi.

Ogni indicatore deve essere interpretato diversamente per setup:

- reentry: recupero controllato da lower band verso middle, senza estensione gia' esausta;
- breakout: espansione oltre upper band solo con trend, volume e regime coerenti.

### Principio 2: Storico Deduce Soglie, WATCH Decide

DocBrown deve produrre:

- soglie candidate-specific;
- regime ammesso;
- limiti di rischio;
- expected setup semantics.

ACDC WATCH deve ricalcolare:

- prezzo;
- Bollinger live;
- EMA live;
- RSI live;
- ATR live;
- volume ratio live;
- regime live.

Il BUY avviene solo se lo stato live soddisfa il contratto.

### Principio 3: SELL Senza MFE

SELL non deve usare MFE come condizione di uscita.

SELL deve usare:

- invalidazione setup-specifica Bollinger/context;
- loss cap quote-aware;
- target/trailing coerente col setup;
- timeout solo se contrattuale e non in conflitto col charter.

### Principio 4: Una Sola Base Temporale Decisionale

La base temporale decisionale del ciclo Context V1 e' 1 minuto.

Contratto dati vincolante:

```text
source_bucket = binance
interval_seconds = 60
candle_state = CLOSED
timestamp_semantics = BINANCE_OPEN_TIME
```

Regole:

1. DocBrown calcola storico, candidate, contract, rolling validation, promotion e live-score su candele 1m chiuse.
2. ACDC WATCH ricalcola il current state sulla stessa base 1m chiusa.
3. BUY congela in `policyJson` e negli `entry_*` valori calcolati dalla stessa base 1m.
4. `binance-realtime` puo' essere usato solo per UI/stato live diagnostico, non per autorizzare BUY.
5. `binance-microbar` puo' essere usato solo per replay, post-sell forensics, gap detection e timing.
6. Se il dato replay e' 1m o sintetico, non puo' spiegare decisioni a precisione 5s.
7. Se il contratto non dichiara la granularita' o se ACDC osserva una granularita' diversa, WATCH deve fallire chiusa.

## Mappa Interventi Per Modulo

### 1. hft-common

File:

- `hft-common/src/main/java/it/mbc/hft/common/rem/constants/RemConstants.java`
- `hft-common/src/main/java/it/mbc/hft/common/rem/enums/RemMarketRegime.java`
- `hft-common/src/main/java/it/mbc/hft/common/rem/enums/RemStrategyFamily.java`

Interventi:

1. Verificare che tutte le key nuove siano gia' presenti.
2. Aggiungere solo se mancanti:
   - `CONTRACT_MIN_REENTRY_PERCENT_B`;
   - `CONTRACT_MAX_REENTRY_PERCENT_B`;
   - `LIVE_CONTRACT_MIN_REENTRY_PERCENT_B`;
   - `LIVE_CONTRACT_MAX_REENTRY_PERCENT_B`;
   - `ENTRY_CONTRACT_MIN_REENTRY_PERCENT_B`;
   - `ENTRY_CONTRACT_MAX_REENTRY_PERCENT_B`;
   - `CONTRACT_MAX_NET_LOSS_QUOTE`;
   - `LIVE_CONTRACT_MAX_NET_LOSS_QUOTE`;
   - `ENTRY_CONTRACT_MAX_NET_LOSS_QUOTE`;
   - `DECISION_SOURCE_BUCKET`;
   - `DECISION_INTERVAL_SECONDS`;
   - `DECISION_CANDLE_STATE`;
   - `DECISION_FEATURE_WINDOW_MINUTES`;
   - `ENTRY_DECISION_SOURCE_BUCKET`;
   - `ENTRY_DECISION_INTERVAL_SECONDS`;
   - `ENTRY_DECISION_CANDLE_STATE`;
   - `WATCH_DECISION_GRANULARITY_MISMATCH`;
   - `WATCH_REENTRY_OVEREXTENDED`;
   - `WATCH_BREAKOUT_WEAK_CONFIRMATION`;
   - `EXIT_BB_CONTEXT_INVALIDATED`, solo se verra' implementata SELL fase 2.
3. Non aggiungere nuovi setup operativi.
4. Non rinominare costanti esistenti.

Test:

- build `mvn -q -DskipTests install`.
- eventuale test enum/costanti se gia' esiste pattern.

### 2. docbrown

File:

- `docbrown/src/main/java/it/mbc/hft/docbrown/rem/service/InfluxSnapshotService.java`
- `docbrown/src/main/java/it/mbc/hft/docbrown/rem/service/BlankRemCandidateService.java`
- `docbrown/src/test/java/it/mbc/hft/docbrown/rem/service/BlankRemCandidateServiceTest.java`

Interventi in `InfluxSnapshotService`:

1. Rendere esplicita la base temporale degli indicatori:
   - Bollinger: 20 periodi;
   - EMA: 9/21/50;
   - RSI: 14;
   - ATR: 14;
   - volume ratio: 1m/20m o microbar equivalente dichiarato.
2. Per il percorso operativo Context V1, rimuovere l'equivoco "microbar equivalente": la base ammessa e' 1m chiusa.
3. Separare i metodi per ruolo:
   - lettura decisionale 1m chiusa;
   - lettura replay microbar;
   - lettura diagnostica realtime.
4. Dichiarare nel contract/advice:
   - `decision_source_bucket`;
   - `decision_interval_seconds`;
   - `decision_candle_state`;
   - `decision_feature_window_minutes`.
5. Estrarre soglie regime hard-coded in costanti private nominate, oppure config condivisa se gia' pattern.
6. Aggiungere audit feature per granularita':
   - numero barre disponibili;
   - feature window seconds/minutes;
   - max gap 1m decisionale;
   - source bucket decisionale.
7. Live-score non deve promuovere advice se:
   - manca la base 1m chiusa;
   - `interval_seconds != 60`;
   - il contratto non dichiara la granularita';
   - la sorgente e' `binance-realtime` o `binance-microbar`.

Interventi in `BlankRemCandidateService`:

1. Rafforzare `bollingerContract(PromotionSelection selection)` per reentry:
   - il contratto deve avere min e max `%B` semanticamente validi;
   - cap massimo reentry consigliato: non permettere `%B > 0.80`;
   - preferibile area iniziale: `0.20 <= %B <= 0.65`, poi tarata per simbolo.
2. Rafforzare `contextContract(PromotionSelection selection)` per reentry:
   - RSI max non deve derivare solo dal p90 se il p90 e' alto;
   - introdurre cap statico auditabile, es. `min(candidateP90, 62)` o config equivalente;
   - ATR% max deve essere simbolo-specifico ma con cap anti-chaos;
   - volume spike ratio max deve bloccare spike caotici.
3. Rafforzare breakout:
   - volume ratio minimo deve essere piu' severo del valore medio se il campione e' debole;
   - RSI deve restare in fascia breakout, es. `50 <= RSI <= 70/75`, non `> 78`;
   - trend deve richiedere `EMA9 > EMA21` e `close > EMA50`;
   - BandWidth deve mostrare espansione, non solo upper breach.
4. Separare metriche e report per setup:
   - reentry rows;
   - breakout rows;
   - rejected by context.
5. Non usare MFE come soglia SELL. Puo' restare metrica forensics.

Test:

- reentry candidate con `%B > 1` non produce contract compatibile con BUY;
- reentry candidate con RSI alto viene esclusa o produce contract bloccante;
- breakout senza volume/trend non viene promosso Context V1;
- candidate senza barre sufficienti non e' promotable;
- candidate con metadata decisionale mancante non e' promotable;
- payload non contiene `reversal_*`.

### 2B. ML / Script Orchestration

File:

- `acdc/scripts/acdc-run-rem-ml.sh`
- `acdc/scripts/lib/rem_contracts.sh`
- `acdc/scripts/check-script-contracts.py`
- eventuale nuovo script in `acdc/scripts` o `hft-common/scripts`.

Interventi:

1. Classificare `acdc-run-rem-ml.sh` come `DIAGNOSTIC_ONLY`.
2. Rinominare o creare alias coerente:
   - preferito: `run-docbrown-research.sh`;
   - alternativa: mantenere il nome attuale ma aggiungere banner e documentazione.
3. Vietare allo script di:
   - invocare promotion;
   - invocare PAPER;
   - scrivere DB runtime;
   - bypassare `/management` per qualunque passaggio operativo.
4. Aggiungere output JSON/metadata minimo:
   - `profileKey`;
   - `docbrownBaseUrl`;
   - `endpoint`;
   - `runStartedAt`;
   - `outputFile`;
   - `diagnosticOnly=true`.
5. Se serve uno script operativo end-to-end, deve chiamarsi diversamente e usare solo:
   - `POST /backoffice/management/actions/RUN_RESEARCH`;
   - `POST /backoffice/management/actions/LIVE_SCORE`;
   - `POST /backoffice/management/actions/ROLLING_VALIDATION`;
   - `POST /backoffice/management/actions/ROLLING_PROMOTION`;
   - `POST /backoffice/management/actions/PAPER_BOLLINGER_START` o `AUTO_BOLLINGER_START`,
     solo quando lo stato e' pronto.
6. Aggiornare `check-script-contracts.py` per coprire eventuali nuove stringhe operative introdotte negli script.

Test:

- `python3 acdc/scripts/check-script-contracts.py`;
- dry run dello script diagnostico con endpoint non distruttivo o ambiente locale;
- verifica che nessuno script contenga `REAL` hard-coded fuori da `rem_contracts.sh`.

### 3. acdc

File:

- `acdc/src/main/java/it/mbc/hft/acdc/service/InfluxSnapshotService.java`
- `acdc/src/main/java/it/mbc/hft/acdc/service/PreBuyWatchService.java`
- `acdc/src/main/java/it/mbc/hft/acdc/service/BbAdviceFeatures.java`
- `acdc/src/main/java/it/mbc/hft/acdc/service/PaperRunService.java`
- `acdc/src/main/java/it/mbc/hft/acdc/service/GuardEvaluator.java`
- `acdc/src/main/java/it/mbc/hft/acdc/service/TradeReplayLiveService.java`
- `acdc/src/test/java/it/mbc/hft/acdc/service/InfluxSnapshotServiceTest.java`
- `acdc/src/test/java/it/mbc/hft/acdc/service/GuardEvaluatorTest.java`
- nuovo test consigliato: `PreBuyWatchServiceContextGateTest`.

Interventi in `InfluxSnapshotService`:

1. Allineare formule, periodi e base dati a DocBrown.
2. Separare esplicitamente:
   - snapshot decisionale 1m chiuso;
   - replay microbar;
   - diagnostica realtime.
3. Impedire che `binance-microbar` alimenti WATCH/BUY.
4. Impedire che `binance-realtime` alimenti WATCH/BUY.
5. Esporre bar count/gap diagnostics per replay e forensics.
6. Evitare che `putEmptyContext` produca falsi pass. Mancanza feature deve fallire chiusa in WATCH.

Interventi in `PreBuyWatchService`:

1. Prima di `triggerAudit` e `contextGateAudit`, validare metadata decisionale:
   - source bucket ammesso;
   - interval seconds = 60;
   - candle state = `CLOSED`;
   - feature window coerente con contract;
   - max gap decisionale compatibile con 1m.
2. In `reentryTriggerAudit`:
   - mantenere lower breach e reentry confirmed;
   - aggiungere controllo hard anti-overextension se disponibile:
     - `%B <= contract_max_reentry_percent_b`;
     - fallback massimo non oltre `0.80` se autorizzato;
     - bloccare `%B > 1` come `WATCH_REENTRY_OVEREXTENDED` o `WATCH_MOMENTUM_BLOCKED`.
3. In `reentryContextAudit`:
   - mantenere regime range, no chaos, no trend down;
   - mantenere `ema50_slope`, RSI, volume ratio, ATR%, bandwidth;
   - aggiungere controllo esplicito di prezzo non sopra upper/middle oltre soglia;
   - reason dominante specifica, non generica.
4. In `breakoutContextAudit`:
   - mantenere squeeze/expansion/trend up;
   - mantenere EMA9>EMA21 e close>EMA50;
   - rendere volume ratio minimo obbligatorio e setup-specifico;
   - aggiungere eventuale max extension ATR per evitare comprare breakout gia' troppo lontani.
5. In `watchDecision`:
   - assicurare `entry_*` completo al BUY;
   - aggiungere soglie contratto effettivamente usate, non solo feature osservate.
6. Non reintrodurre `BB_ADVICE_NO_MFE_DECAY_EXIT`.

Interventi in `GuardEvaluator` / SELL:

1. Lasciare MFE fuori dalla SELL.
2. Rendere `BB_ADVICE_LOSS_CAP_EXIT` quote-aware:
   - trigger se `net_loss_quote >= contract_max_net_loss_quote`;
   - mantenere anche il net return loss cap come guardia secondaria.
3. Preparare ma non attivare SELL fase 2:
   - reentry invalidation: price torna sotto lower, `%B < 0`, RSI debole, close < middle;
   - breakout invalidation: rientro sotto upper con volume/expansion decaduti.

Interventi in `TradeReplayLiveService`:

1. Esporre:
   - `source_bucket`;
   - `interval_seconds`;
   - `candle_count`;
   - `max_gap_seconds`;
   - `synthetic_backfill`.
2. Se i dati tornano a passo 1m, il FE deve saperlo.
3. Se i dati sono synthetic backfill, il FE deve saperlo.
4. Non usare replay rado o sintetico come prova scientifica di micro timing.

Test:

- reentry `%B > 1` bloccato.
- reentry RSI alto bloccato.
- breakout senza volume bloccato.
- breakout con trend down bloccato.
- missing context -> `WATCH_CONTEXT_CONTRACT_INCOMPLETE`.
- mismatch base decisionale -> `WATCH_DECISION_GRANULARITY_MISMATCH`.
- loss quote cap chiude prima di perdite `> 0.08 USDC` se autorizzato.
- no-MFE SELL resta disabilitato.

### 4. kenshiro

File:

- `kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice/ManagementService.java`
- `kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice/ManagementString.java`
- `kenshiro/src/main/java/it/mbc/hft/kenshiro/backoffice/BackofficeDtos.java`
- eventuali test backoffice se presenti.

Interventi:

1. `/management/state` deve esporre:
   - strategy family;
   - setup distribution;
   - regime distribution;
   - context pass/fail;
   - dominant block reason;
   - loss cap quote summary.
2. `/management/trades/{executionId}/{symbol}` deve rendere facile leggere:
   - contract values;
   - live current_state;
   - entry values;
   - exit values;
   - source cadence/replay metadata.
3. I metadata replay obbligatori sono:
   - `source_bucket`;
   - `interval_seconds`;
   - `candle_count`;
   - `max_gap_seconds`;
   - `synthetic_backfill`.
4. Aggiungere diagnostica "stricter proposal" senza cambiare runtime:
   - quanti BUY della run sarebbero bloccati dalle nuove soglie;
   - PnL dei trade mantenuti/scartati;
   - motivi di blocco.
5. `APPLY_BOLLINGER_CONTEXT_V1` resta disponibile solo se readiness context completa.

Test:

- state contiene campi opzionali senza rompere FE.
- REAL resta bloccata.
- PAPER non parte se Context V1 richiesto ma advice incompleta.

### 5. hft-fe

File:

- `hft-fe/src/routes/management/+page.svelte`
- `hft-fe/src/routes/trades/+page.svelte`
- `hft-fe/src/lib/types/management.ts`
- `hft-fe/src/lib/api/management.ts`
- `hft-fe/src/lib/contracts/rem.ts`

Interventi:

1. `/management`:
   - mostrare setup/regime distribution;
   - mostrare context pass/fail e dominant reason;
   - mostrare loss cap quote e non solo percentuale;
   - distinguere chiaramente price, net return, net PnL.
2. `/trades`:
   - overlay contract/current/entry/exit;
   - mostrare cadenza dati Influx (`5s`, `60s`, gap);
   - mostrare `source_bucket`, `interval_seconds`, `candle_count`, `max_gap_seconds`, `synthetic_backfill`;
   - mostrare chiaramente se il replay e':
     - `5s live`;
     - `60s`;
     - `synthetic`;
   - mostrare marker BUY/SELL con tooltip:
     - `%B`;
     - RSI;
     - ATR%;
     - volume ratio;
     - regime;
     - guard/reason.
3. Non creare nuova pagina.
4. Nessuna UI descrittiva lunga: pannelli operativi densi e leggibili.

Test:

- `npm run check`.
- `npm run build`.
- verifica manuale `/management` e `/trades`.

### 6. influxer

File:

- `influxer/src/main/java/it/mbc/hft/influxer/xchange/stream/BinanceWebSocketClient.java`
- `influxer/src/main/java/it/mbc/hft/influxer/influx/InfluxDBService.java`
- `influxer/src/main/java/it/mbc/hft/influxer/influx/ShortRetentionBucketBackfillJob.java`
- `influxer/src/main/resources/application.yml`

Interventi:

1. Verificare e documentare i ruoli bucket:
   - `binance`: candele 1m chiuse, fonte decisionale;
   - `binance-realtime`: candela 1m corrente, diagnostica/UI;
   - `binance-microbar`: microbar 5s, replay/forensics.
2. Verificare che `binance` contenga candele 1m chiuse sufficienti per DocBrown e ACDC.
3. Verificare che `binance-microbar` contenga davvero punti 5s e non solo 1m.
4. Aggiungere diagnostica gap:
   - per simbolo;
   - per bucket;
   - per finestra trade.
5. Se il backfill usa candele 1m espanse a 5s, marcarle come synthetic/backfill; non confonderle con microbar live.
6. Non introdurre spread/order-flow finche' la fonte non e' affidabile.

Test:

- test `ShortRetentionBucketBackfillJob.toMicrobars`.
- endpoint/diagnostica gap se implementata.
- verifica runtime bucket `binance-microbar`.

### 7. Script / Diagnostics

Posizione consigliata:

- `hft-common/doc/acdc/session/...` per report manuali;
- oppure script dedicato in `acdc/scripts` o `hft-common/scripts` se esiste pattern locale.

Interventi:

1. Script ML/research:
   - non deve essere operativo di trading;
   - deve essere `DIAGNOSTIC_ONLY`;
   - deve invocare DocBrown direttamente solo per research/status;
   - deve demandare promotion/PAPER a `/management`.
2. Script forensics execution:
   - input `executionId`;
   - output per symbol:
     - setup;
     - regime;
     - `%B` BUY/SELL;
     - RSI BUY/SELL;
     - ATR%;
     - volume ratio;
     - reason BUY/SELL;
     - loss quote;
     - proposed block reason.
3. Script gap replay:
   - input `executionId` + symbol;
   - confronta expected interval vs actual bucket times.
4. Script actual-vs-proposed:
   - non cambia DB;
   - simula soglie nuove su decisioni/candles persistiti.
5. Ogni script che legge bucket deve stampare:
   - `DIAGNOSTIC_ONLY`;
   - `source_bucket`;
   - `interval_seconds`;
   - `candle_count`;
   - `max_gap_seconds`;
   - `synthetic_backfill`.

## Sequenza Di Implementazione Proposta

### Blocco A0 - Allineamento 1m Decisionale

Obiettivo: rendere coerente la base temporale prima di nuove RUN.

Moduli:

- hft-common;
- influxer;
- docbrown;
- acdc;
- kenshiro;
- hft-fe;
- script/diagnostiche.

Deliverable:

- contract/advice dichiara source bucket, interval, candle state e feature window.
- DocBrown calcola contract e live-score su candele 1m chiuse.
- ACDC WATCH calcola current state su candele 1m chiuse.
- ACDC fallisce chiusa su mismatch di granularita'.
- `/trades` mostra source bucket, interval seconds, candle count, max gap seconds e synthetic backfill.
- microbar 5s resta confinata a replay/diagnostica/timing/gap detection.

Validazione:

- audit statico dei lettori Influx.
- test mirati DocBrown/ACDC.
- build FE/Kenshiro se cambia payload/UI.
- nessuna PAPER RUN finche' `1m_alignment_ready` non e' vero.

### Blocco A - Diagnostica Zero-Risk

Obiettivo: capire senza cambiare trading.

Moduli:

- Kenshiro;
- FE;
- eventuale script.

Deliverable:

- `/management` mostra metriche setup/regime/context.
- `/trades` mostra cadenza dati e contract/current/entry/exit.
- report actual-vs-proposed su RUN `111`.
- classificazione script ML/research: diagnostico o rimosso dal percorso operativo.

Validazione:

- build FE/Kenshiro.
- nessuna RUN live necessaria.

### Blocco B - Contract Tightening In DocBrown

Obiettivo: il contratto impedisce BUY semanticamente sbagliati.

Moduli:

- hft-common se mancano costanti;
- docbrown.

Deliverable:

- reentry non puo' generare contratto che consenta `%B > 0.80` o RSI alto senza audit;
- breakout richiede volume/trend/espansione.

Validazione:

- test DocBrown.
- payload advice completo.
- no `reversal_*`.

### Blocco C - WATCH Runtime Enforcement

Obiettivo: ACDC applica il contratto live in modo fail-closed.

Moduli:

- acdc.

Deliverable:

- `PreBuyWatchService` blocca reentry overextended;
- breakout debole bloccato;
- missing context bloccato;
- no-MFE non reintrodotto.

Validazione:

- targeted tests.
- package.
- deploy ACDC.

### Blocco D - Loss Cap Economico

Obiettivo: evitare loss quote non accettabili senza MFE.

Moduli:

- hft-common se manca key;
- docbrown per contract max loss quote;
- acdc per guard quote-aware;
- Kenshiro/FE per mostrare soglia.

Deliverable:

- perdita netta massima per trade leggibile in quote;
- fallback percentuale resta seconda guardia.

Validazione:

- `GuardEvaluatorTest`;
- PAPER breve solo dopo deploy.

### Blocco E - PAPER Validation

Obiettivo: validare senza sovrascrivere evidenza.

Regole:

- PAPER solo da `/management`.
- almeno run con trade reali per setup.
- separare reentry/breakout.
- niente REAL.
- ogni loss deve avere attribution:
  - setup;
  - regime;
  - trigger;
  - context;
  - exit.

## Criteri Di Accettazione

Prima dell'implementazione:

- il Consiglio deve approvare questa mappa o modificarla.

Dopo implementazione:

- allineamento 1m dichiarato e verificato end-to-end;
- DocBrown e WATCH usano la stessa base 1m chiusa;
- `binance-realtime` e `binance-microbar` non alimentano BUY;
- no-MFE SELL resta disabilitato;
- reentry overextended non compra;
- breakout senza volume/trend non compra;
- loss quote massimo e' visibile e applicato;
- `/trades` indica chiaramente se il replay e' 5s reale, 1m o backfill sintetico;
- ogni decisione BUY ha `entry_*` completo;
- ogni blocco ha reason specifica;
- build/test/deploy completati;
- report PAPER separato per setup.

## Punti Da Chiarire Col Consiglio

1. Soglia hard reentry `%B`:
   - proposta: max `0.80`, preferenza operativa `0.65`.
2. RSI reentry:
   - proposta: max `62`, eventualmente `min(candidateP90, 62)`.
3. RSI breakout:
   - proposta: `50 <= RSI <= 70/75`, evitare `78` salvo evidenza.
4. Loss cap quote:
   - proposta iniziale: `0.06` USDC soft, `0.08` hard per size 25 USDC.
5. Volume breakout:
   - proposta: min `1.3` o `1.5` rispetto a baseline, da calibrare.
6. Uso di ATR:
   - reentry: limite volatilita'/chaos;
   - breakout: limite overextension e trailing fase 2.
7. Dati microbar:
   - decidere se bloccare validazione scientifica fine finche' gap > 5-10s.
8. Allineamento 1m:
   - confermare `binance` come bucket decisionale 1m chiuso;
   - confermare `binance-realtime` come non decisionale;
   - confermare `binance-microbar` come replay/diagnostica;
   - definire soglia max gap 1m ammessa prima di bloccare WATCH.
