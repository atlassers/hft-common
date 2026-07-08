# Bollinger Context V1 Operational Handoff

Data: 2026-07-04.

## Scopo

Manuale operativo compatto per il ciclo `BOLLINGER_CONTEXT_V1`.

Il charter strategico operativo primario e':

```text
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md
```

La base scientifica di riferimento e':

```text
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md
```

Il piano strategia target corrente da implementare e':

```text
hft-common/doc/archived/REALTIME_BB_ADX_NATIVE_F4E954_STRATEGY_INTEGRATION_PLAN.md
```

## Vincoli

- REAL vietata.
- Validazione operativa solo su MySQL e container deployati.
- `/management` e' l'interfaccia primaria.
- La tabella advice runtime resta `hft.acdc_live_bb_advice`.
- Bollinger resta obbligatorio: ogni advice deve avere `bb_setup`, `bb_trigger` e contratto `bb_*`.
- A2 corregge il blocco A0/A1: Bollinger non impone 1m. La readiness valida richiede cadence decisionale dichiarata,
  coerente e non sintetica.
- Profilo operativo corrente A3: `bb.decision.interval_seconds=20`, bucket decisionale `binance-microbar`, synthetic
  backfill vietato.
- Dal 2026-07-08 Influxer deve leggere la cadence decisionale da MySQL (`bb.decision.interval_seconds`, fallback
  `rt.decision.interval_seconds`). Per cadence sub-minute usa klines Binance native `1s` e aggrega OHLCV in barre
  decisionali chiuse sul bucket `binance-microbar`; non deve interpolare candele `1m` in microbar decisionali.
- Context V1 richiede feature esplicite di regime, trend, momentum, volume e risk.
- Nel profilo A3 range reentry, `rt.entry.range.min_volume_ratio=0.50` blocca barre 20s quasi morte; non e' una
  conferma breakout e non sostituisce Bollinger.
- Il prossimo profilo target e' `REALTIME_BB_ADX_NATIVE_F4E954`, hash Melo `f4e954f0b552ce864535fc2b`: i suoi valori
  devono essere centralizzati in `hft-common`, e i gate legacy non inclusi nel profilo non devono agire nel path
  strategico.
- WATCH apre BUY solo se passano trigger Bollinger e gate Context V1.
- SELL A3 resta Bollinger setup-specifica: range cattura middle/upper band; breakout protegge profitto quando una
  posizione gia' aperta ha MFE minimo positivo e rientra sotto la upper band con `net_return > 0`.
- SELL strategica usa la stessa cadence decisionale del BUY per invalidazioni, target, trailing e timeout.
- Loss cap quote-aware puo' usare prezzo eseguibile intraminuto solo come protezione economica meccanica, dichiarata e
  auditata separatamente.
- Le notifiche Telegram BUY/SELL sono idempotenti per posizione tramite `acdc_paper_position.buy_notified_at` e
  `acdc_paper_position.sell_notified_at`; una posizione non deve mai generare piu' di una notifica BUY e una notifica
  SELL.
- La finestra WATCH autorizza osservazione, non e' una condizione BUY.
- BUY e WATCH non hanno cap numerici concorrenti; il limite effettivo e' budget/exchange sizing al momento della BUY.
- Dal Consiglio 2026-07-04, prima di nuove RUN PAPER e' vincolante il blocco cadence/A2:
  - indicatori, contract, WATCH, BUY e SELL strategica sulla stessa cadence dichiarata;
  - microbar 5s decisionale ammessa solo nel profilo A2 e solo se reale/non synthetic;
  - `binance-realtime` non decisionale;
  - `binance-microbar` decisionale solo se `bb.decision.interval_seconds=5` e `decision_synthetic_backfill=0`;
  - il profilo 1m, quando dichiarato, non puo' essere ricostruito aggregando realtime o microbar;
  - decision snapshot deve includere candle count, max gap e staleness;
  - `1m_alignment_ready` resta alias storico del readiness cadence ed e' distinto da `bbReady`;
  - ogni blocker cadence/A0 deve essere visibile da `/management/state`;
  - no PAPER finche' il readiness cadence esposto come `oneMinuteAlignmentReady`/`1m_alignment_ready` non e' vero.

## Processo

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

## Setup, Trigger E Regimi

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

## Endpoint Primari FE

```bash
curl -sS 'http://localhost:5173/backoffice/management/state?profileKey=REM_CURRENT' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs?limit=20' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/runs/{executionId}' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades/executions?date=YYYY-MM-DD&limit=100' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades?profileKey=REM_CURRENT&limit=50' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades?profileKey=REM_CURRENT&executionId={executionId}&limit=300' | jq '.'
curl -sS 'http://localhost:5173/backoffice/management/trades/{executionId}/{symbol}' | jq '.'
curl -sS -X POST -H 'Content-Type: application/json' \
  'http://localhost:5173/backoffice/management/actions/REFRESH_DIAGNOSTICS' \
  -d '{"profileKey":"REM_CURRENT","payload":{}}' | jq '.'
```

## Action Approvate

Operative gia' esistenti:

- `AUTO_BOLLINGER_START`
- `AUTO_BOLLINGER_STOP`
- `REFRESH_DIAGNOSTICS`
- `UNIVERSE_PREFILTER`
- `RUN_RESEARCH`
- `RESEARCH_STATUS`
- `LIVE_SCORE`
- `ROLLING_VALIDATION`
- `ROLLING_SELECTION_ATTRIBUTION_AUDIT`
- `ROLLING_PROMOTION`
- `PAPER_BOLLINGER_START`
- `PAPER_STOP_BUY`
- `PAPER_STOP`
- `SAVE_MANAGEMENT_CONFIG`

Da introdurre solo quando DocBrown e ACDC sono compatibili:

- `APPLY_BOLLINGER_ONLY`
- `APPLY_BOLLINGER_CONTEXT_V1`

## Diagnostica Standard

1. Leggere `/management/state`.
2. Verificare strategy family, `bbReady`, blocker, advice attive, PAPER running e posizioni aperte.
3. Verificare blocco cadence/A2:
   - `/management/state` espone readiness cadence true tramite `oneMinuteAlignmentReady=true` e alias storico
     `1m_alignment_ready=true`;
   - `/management/state` espone blocker A0/cadence specifici se il readiness e' false;
   - leggere da DB `bb.decision.interval_seconds` e verificare che DocBrown, ACDC WATCH/BUY, SELL strategica e
     forensics espongano lo stesso interval;
   - profilo corrente approvato A3: ACDC RT decision source bucket = `binance-microbar`,
     `decision_interval_seconds=20`, `decision_synthetic_backfill=0`;
  - la barra 20s e' aggregata da klines Binance native `1s`; non deve cadere su `binance` 1m e non deve derivare da
    interpolazione di una candela piu' larga;
   - profilo alternativo ammesso: decision source bucket = `binance`, `decision_interval_seconds=60`;
   - candle state = `CLOSED`;
   - decision candle count sufficiente per BB20, EMA50 e volume ratio sulla cadence dichiarata;
   - decision max gap entro soglia approvata;
   - decision staleness entro soglia approvata;
   - `binance-realtime` assente dal path BUY;
   - `binance-microbar` assente dal path indicatori/BUY/SELL strategica solo se il profilo dichiarato e' 1m;
   - SELL decision source bucket = source bucket dichiarato per BUY;
   - SELL decision interval = interval dichiarato per BUY;
   - SELL decision candle state = `CLOSED`;
   - eventuale SELL execution interval 5s separato dalla reason strategica;
  - replay espone `source_bucket`, `interval_seconds`, `candle_count`, `max_gap_seconds`, `synthetic_backfill`.
  - Influxer deve loggare avanzamento refill short-retention ogni 25 simboli e loggare source interval/cadence nella
    sottoscrizione websocket, per esempio `Subscribed ... Binance 1s kline streams ... for 20s decision bars`.
4. Verificare script e diagnostiche:
   - ogni script operativo deve stampare `DIAGNOSTIC_ONLY` se non passa da `/management`;
   - ogni script che legge bucket deve stampare bucket, interval, candle state, max gap e synthetic flag;
   - `acdc/scripts/populate-rt-parameter-verification-matrix.sh` e' `DIAGNOSTIC_ONLY`: non avvia PAPER, genera e
     materializza su MySQL `acdc_rt_parameter_verification_profile` la matrice fattoriale completa
     `REALTIME_BB_ADX_V1` da `6,220,800` profili `PENDING_REPLAY`; usa staging table e swap finale per evitare righe
     parziali visibili;
   - `acdc/scripts/acdc-run-rem-ml.sh` e alias `run-docbrown-research.sh` sono diagnostici DocBrown-only: stampano
     `DIAGNOSTIC_ONLY`, endpoint, source bucket atteso del profilo corrente (`binance-microbar`/`5s` per A2 corrente,
     oppure `binance`/`60s` se il profilo 1m e' dichiarato), `candle_state=CLOSED`, timestamp semantics e
     `synthetic_backfill=false`; promotion e PAPER restano solo da `/management`;
   - nessuno script puo' avviare PAPER direttamente.
5. Se `/management/state` non espone ancora `1m_alignment_ready`, trattarlo come `false`.
6. Verificare count per setup/regime e readiness context.
7. Se la cadence decisionale dichiarata non e' pronta/coerente, fermarsi: nessuna nuova sequenza PAPER.
8. Se A2 non e' implementato/verificato, fermarsi: nessuna nuova PAPER come evidenza strategica.
9. Solo dopo A2 implementato/verificato, se non ci sono PAPER o posizioni aperte, usare l'action approvata per generare
   una nuova sequenza PAPER.
10. Dopo run PAPER, leggere `/management/runs/{executionId}`.
11. Attribuire ogni BUY/SELL a setup, trigger, regime, gate context, reason e PnL.
12. Classificare ogni report come `VALID_STRATEGIC_EVIDENCE`, `NEGATIVE_OPERATIONAL_SIGNAL`, `DIAGNOSTIC_ONLY`,
   `INCONCLUSIVE` o `INVALID_STRATEGIC_EVIDENCE`.
13. Separare sempre le metriche di breakout e reentry.
14. Per analisi visiva usare `/trades`: selezione data, execution del giorno, simboli per execution, filtri fase
   WATCH/BUY/SELL, replay candle persistito e replay live Influx con refresh 1s.

## Stato A0 Verificato

Ultima verifica Consiglio: 2026-07-04.

- A0 implementato, buildato e deployato su `hft-common`, `influxer`, `docbrown`, `acdc`, `kenshiro`, `hft-fe`.
- `/management/state` deve essere letto via FE proxy:
  `http://localhost:5173/backoffice/management/state?profileKey=REM_CURRENT`.
- Stato verificato: `BB_READY`, `oneMinuteAlignmentReady=true`, `1m_alignment_ready=true`, `a0Blockers=[]`,
  `blockers=[]`.
- Advice attive PAPER_ELIGIBLE con decision metadata su `binance` 1m chiusa:
  `decision_source_bucket=binance`, `decision_interval_seconds=60`, `decision_candle_state=CLOSED`,
  `decision_candle_count=89`, `decision_max_gap_seconds=60`, `decision_synthetic_backfill=0`.
- RUN PAPER 118 avviata e fermata solo tramite `/management`.
- RUN PAPER 118: `STOPPED`, 0 posizioni, 0 BUY, 0 SELL, PnL `0`.
- Fonte MySQL operativa RUN 118: 2400 decisioni ENTRY `HOLD`, 2370 `WATCH_WAITING_BUY_CONTRACT`, 30
  `WATCH_OPENED_WAITING_BUY_CONTRACT`.
- Le WATCH della RUN 118 sono state riconciliate a `ABANDONED` con
  `WATCH_ABANDONED_BY_PAPER_TERMINAL_RECONCILIATION`.
- Classificazione RUN 118:
  - `VALID_STRATEGIC_EVIDENCE` per readiness/contratto A0 e governance PAPER;
  - `NEGATIVE_OPERATIONAL_SIGNAL` per BUY trigger, perche' `TriggerAudit` non passa mai;
  - `INCONCLUSIVE` per performance finanziaria per assenza di BUY/SELL.
- Replay detail espone `source_bucket`, `interval_seconds` effettivo, `candle_count`, `max_gap_seconds`,
  `synthetic_backfill`; microbar replay resta diagnostica/timing e non fonte strategica.

## Stato A1 Progettato

Documento vincolante:

```text
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_A1_LITERATURE_ALIGNMENT_PLAN.md
```

Prima di nuove PAPER, implementare e verificare:

- diagnostica fail reason granulare per WATCH/BUY;
- breach Bollinger calcolati bar-by-bar con bande contemporanee;
- `PAPER_WATCH_ELIGIBLE` separato da `PAPER_BUY_ELIGIBLE`;
- reentry `%B < min` trattato come `WATCH_WAITING_REENTRY_RECOVERY`, non hard fail;
- breakout buy-eligible solo con breakout live coerente;
- `/management` e `/trades` devono esporre distribuzione blocker BUY.

Evidenza RUN 118 da ricordare:

- MySQL: 2400 decisioni ENTRY, 0 BUY, 0 posizioni;
- 2370 `WATCH_WAITING_BUY_CONTRACT`;
- 30 `WATCH_OPENED_WAITING_BUY_CONTRACT`;
- reentry: 1680 decisioni, 0 full pass;
- breakout: 720 decisioni, 0 full pass.

## Stato A1 Implementato

Ultima verifica Consiglio: 2026-07-04.

- A1 implementato, buildato e deployato su `hft-common`, `docbrown`, `acdc`, `kenshiro`, `hft-fe`.
- ACDC e DocBrown calcolano breach bar-by-bar con bande contemporanee e mantengono A0 1m chiusa.
- `PAPER_ELIGIBLE` DB resta alias transitorio; il contract espone:
  - `bb_advice_paper_watch_eligible`;
  - `bb_advice_paper_buy_eligible`.
- ACDC separa trigger BUY e context gate:
  - reentry `%B < min` e' attesa `WATCH_WAITING_REENTRY_RECOVERY`, non hard fail;
  - reentry over max o sopra upper resta `WATCH_REENTRY_OVEREXTENDED`;
  - breakout richiede breakout live coerente, non squeeze e breakout simultanei;
  - `REGIME_RANGE` reentry non e' hard gate se non ci sono chaos/trend_down e gli altri gate passano.
- `/management/runs/{executionId}` espone `a1BuyDiagnostics`.
- `/management` mostra tab A1 nel dettaglio run; `/trades` mostra eligible WATCH/BUY, reason code e breach metadata.
- Script diagnostico:

```bash
cd /home/mbc/Documenti/ws/java/hft/acdc
MYSQL_PASSWORD='<password>' scripts/diagnose-a1-buy-blockers.sh <execution_id>
```

Lo script e' `DIAGNOSTIC_ONLY`; non avvia PAPER.

RUN A1:

- RUN 119: micro-run diagnostica dopo primo deploy A1; ha esposto `bb_advice_freshness_contract_pass=0` come gate
  comune residuo. Non usarla come evidenza finanziaria.
- RUN 120: PAPER avviata e fermata solo da `/management`, `STOPPED`, `entryDecisions=720`, `acceptedBuys=0`,
  `positions=0`, `openPositions=0`.
- RUN 120 trigger top:
  `WATCH_REENTRY_OVEREXTENDED=295`, `WATCH_WAITING_BREAKOUT_PERCENT_B=286`,
  `WATCH_REENTRY_AGE_EXPIRED=24`, `WATCH_WAITING_REENTRY_LOWER_BREACH=24`.
- RUN 120 classificazione:
  - `VALID_STRATEGIC_EVIDENCE` per readiness A0 e metadata A1;
  - `NEGATIVE_OPERATIONAL_SIGNAL` per finestra senza BUY ma con blocker granulari;
  - `INCONCLUSIVE` finanziaria.

## Stato A2.1 Middle Slope

Ultima verifica Consiglio: 2026-07-04.

- A2 stabilisce che `middle_slope` non e' hard-blocker Bollinger se non dichiarato da contract validato.
- Fix implementato in DocBrown: `BlankRemCandidateService` non richiede piu' `bb_middle_slope >= 0` per classificare
  una candidata `BB_SQUEEZE_BREAKOUT_LONG`.
- La selection breakout DocBrown usa ora la definizione A2: upper breach, `bb_bandwidth_delta > 0` e
  `bb_expansion > 0`. La soglia `%B >= 1` resta applicata dal trigger runtime WATCH/BUY.
- ACDC `PreBuyWatchService` blocca `middle_slope` solo se una soglia `bb_min_middle_slope`/`live_bb_min_middle_slope`
  e' presente nel contract; sulle advice verificate non e' presente.
- Batch fallito riesaminato:
  `management-rolling-20260704T154310Z` aveva `breakout_old=863` e `breakout_a2=1552`.
- Validazione post-deploy:
  `management-rolling-20260704T180407Z`, `PASS_CANDIDATE`, `PROMOTED=30`, RUN PAPER `122` avviata da `/management`.
- RUN 122 ha ripristinato il ciclo operativo BUY/SELL: `positions=5`, `closedPositions=5`, `openPositions=0`,
  stato finale `COMPLETED`.
- Notifiche Telegram BUY/SELL idempotenti confermate: RUN 122 `buy_notified=5`, `sell_notified=5`.
- PnL RUN 122: `net_profit_quote=-0.415733138049780000`; evidenza tecnica A2.1 valida, evidenza finanziaria negativa.
- Punto residuo: analizzare `A0_DECISION_GAP_TOO_WIDE` in `a1BuyDiagnostics` dopo la chiusura o lo stop governato
  della RUN 122.

## Stato SELL Setup-Specifica

Ultima verifica Consiglio: 2026-07-04.

- La letteratura Bollinger non autorizza una SELL sul semplice tag della upper band: i tag sono posizione relativa, non
  segnali autonomi.
- Il documento scientifico richiede pero' che SELL esca per `target/loss/invalidation`.
- Il runtime attuale copre `EXIT_BB_TAKE_PROFIT`, `EXIT_BB_DYNAMIC_TRAILING`, `EXIT_BB_LOSS_CAP` e `EXIT_BB_TIMEOUT`,
  ma non copre ancora invalidation/capture setup-specifica.
- RUN 122 ha dimostrato il buco: 5/5 uscite sono state `EXIT_BB_LOSS_CAP`, con `max_net_return=0` e diversi target
  operativi nulli.
- Prima della prossima PAPER finanziaria implementare le regole documentate in
  `archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`:
  - `EXIT_BB_REENTRY_CAPTURE`;
  - `EXIT_BB_REENTRY_FAILED`;
  - `EXIT_BB_BREAKOUT_FAILED`;
  - `EXIT_BB_BREAKOUT_PROTECT`.
- Dettaglio vincolante:
  - reentry capture usa middle/SMA20 come primo target di mean-reversion e area `%B >= 0.80` come capture alta;
  - `net_return` e' gia' al netto delle fee, quindi il floor Bollinger/economico minimo e' `net_return >= 0` salvo
    buffer positivo dichiarato separatamente;
  - breakout protect usa Chandelier/ATR come trailing primario; `%B < 0.80` non e' trigger isolato;
  - breakout failed copre rientro sotto upper solo se una conferma del breakout viene meno.
- Queste regole non sono nuovi blocker BUY: agiscono solo su posizioni aperte e devono ridurre loss-cap tardivi,
  preservando la possibilita' di micro-loss compensate da win nette piu' frequenti o piu' grandi.

## Stato A2.2 BUY Economic Feasibility

Ultima verifica Consiglio: 2026-07-04.

Regola runtime vincolante:

```text
economic_buy_eligible =
  safe_net_return >= min_executable_entry_edge
  OR q10_positive_max_net_return >= entry_friction_net_return + min_executable_entry_edge

bb_advice_paper_buy_eligible =
  trigger Bollinger setup-specifico
  AND context gate
  AND economic_buy_eligible
```

Vincoli:

- WATCH resta osservazione e non deve essere bloccata dal gate economico.
- BUY deve fallire chiusa con `WATCH_ECONOMIC_EDGE_BLOCKED` se il trigger/context passano ma l'edge economico non passa.
- SELL non deve compensare ingressi che il contract economico dichiara sotto costo.
- `bb_advice_economic_safe_pass` deve essere visibile in advice, decision snapshot e policy della posizione.

Verifica operativa:

- RUN 125 ha dimostrato il bug residuo: BTC/EUR sono entrate con `bb_advice_economic_safe_pass=0` perche'
  `PreBuyWatchService.watchDecision(...)` riscriveva `bb_advice_paper_buy_eligible` con il solo trigger.
- Fix implementato: `PreBuyWatchService` ora richiede trigger, context e economic gate prima di restituire
  `DecisionAction.BUY`.
- RUN 126 dopo redeploy:
  - `positions=1`, `openPositions=0`, PnL `+0.053202478245000000`;
  - BUY/SELL notifiche idempotenti: `buy_notified=1`, `sell_notified=1`;
  - entry `EPICUSDC` con `bb_advice_economic_safe_pass=1`, target positivo e target-zero disabilitato `0`;
  - exit `EXIT_BB_REENTRY_CAPTURE`;
  - blocker economico misurato: `WATCH_ECONOMIC_EDGE_BLOCKED=47`.

## Stato Context Literature Alignment

Ultima verifica Consiglio: 2026-07-05.

- Bollinger resta trigger primario: lower/upper breach, `%B`, BandWidth delta e setup dichiarato.
- EMA, RSI, volume ratio, ATR/risk e regime restano filtri Context V1 hard-gate, ma devono essere soglie dichiarate e
  centralizzate.
- Valori runtime attesi:
  - `bb.context.breakout.min_rsi=50`;
  - `bb.context.breakout.max_rsi=75`;
  - `bb.context.breakout.min_volume_ratio=1.30`;
  - `bb.context.reentry.max_rsi=62`;
  - `bb.context.reentry.max_percent_b=0.80`.
- Feature attese in DocBrown e ACDC:
  - `rsi_breakout_ok = 1` solo se `50 <= rsi14 <= 75`;
  - `rsi_oversold_recovery = 1` solo se `rsi14 <= 62`;
  - `volume_confirmation = 1` solo se `volume_ratio >= 1.30`;
  - `bb_squeeze = 1` solo se `bb_bandwidth_percentile <= 0.20`.

## Stato A3 20s RT

Ultima verifica Consiglio: 2026-07-05.

- Profilo operativo: `rt.decision.interval_seconds=20`, `bb.decision.interval_seconds=20`,
  `decision_source_bucket=binance-microbar`, `decision_synthetic_backfill=0`.
- ACDC V106 applicata su MySQL: `rt.exit.breakout.protect_percent_b=1.0`,
  `rt.entry.range.min_volume_ratio=0.50`.
- RUN 137: `COMPLETED`, 2 posizioni chiuse, 0 aperte, netto `+0.170766512778600000`.
- SELL reason RUN 137: 2/2 `RT_EXIT_BREAKOUT_UPPER_BAND_PROFIT`.
- Notifiche RUN 137: 2 BUY e 2 SELL, idempotenti.
- Query runtime config:

```bash
docker exec mysql_container mysql -u hft_user -p'<password>' hft -e "
select config_key, config_value, value_type, status
from acdc_shared_runtime_config
where config_key in (
  'bb.context.breakout.min_rsi',
  'bb.context.breakout.max_rsi',
  'bb.context.breakout.min_volume_ratio',
  'bb.context.reentry.max_rsi',
  'bb.context.reentry.max_percent_b'
)
order by config_key;"
```

Query diagnostica approvata:

```bash
docker exec mysql_container mysql -u hft_user -p'<password>' hft -e "
select symbol,status,buy_price,sell_price,net_profit_quote,
       json_extract(policy_json,'$.bb_advice_economic_safe_pass') economic,
       json_extract(policy_json,'$.bb_advice_paper_buy_eligible') buy_eligible,
       json_extract(policy_json,'$.bb_target_net_return') target,
       json_extract(policy_json,'$.q10_positive_max_net_return') q10,
       json_extract(policy_json,'$.entry_friction_net_return') friction,
       json_extract(policy_json,'$.min_executable_entry_edge') min_edge,
       json_extract(policy_json,'$.sell_target_zero_take_profit_disabled') target_zero
from acdc_paper_position
where execution_id=<execution_id>
order by id;
select phase, accepted, reason, count(*) c
from acdc_paper_decision
where execution_id=<execution_id>
group by phase, accepted, reason
order by phase, accepted desc, c desc;"
```

## Build

Ordine consigliato:

```bash
cd hft-common && mvn -q -DskipTests install
cd ../docbrown && mvn -q -DskipTests package
cd ../acdc && mvn -q -DskipTests package
cd ../kenshiro && mvn -q test && mvn -q -DskipTests package
cd ../hft-fe && npm run check && npm run build
cd ../acdc && python3 scripts/check-script-contracts.py
```

## Deploy

Usare i `docker-compose.yml` del modulo o del runtime VPN gia' presenti nel workspace.

Regola: ogni gruppo di codice deployabile va verificato nel container prima di usare la run come evidenza operativa.

## Git

Dopo ogni gruppo coerente:

1. `git status`
2. build/test pertinente
3. commit `MS<n>: <message>`
4. push
