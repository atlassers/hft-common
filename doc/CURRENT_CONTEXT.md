# Current Context

Ultimo aggiornamento: 2026-07-04 CEST.

Snapshot operativo corrente del workspace `/home/mbc/Documenti/ws/java/hft`.

## Gerarchia Documentale

1. `hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`
2. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`
3. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`
4. `hft-common/doc/archived/BOLLINGER_CONTEXT_V1_PLAN.md`
5. `hft-common/doc/CURRENT_CONTEXT.md`
6. `hft-common/doc/STRATEGIC_REM_HANDOFF.md`
7. `hft-common/doc/archived/BOLLINGER_ONLY_PLAN.md`

Se i documenti confliggono, prevale il charter; poi
`archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`; poi
`archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`; poi `archived/BOLLINGER_CONTEXT_V1_PLAN.md`.

## Vincoli Correnti

- REAL vietata.
- PAPER solo da `/management`.
- La sorgente advice runtime resta `hft.acdc_live_bb_advice`.
- Bollinger resta il segnale centrale: setup e trigger sono obbligatori.
- Context V1 aggiunge regime, trend, momentum, volume e risk come feature contrattuali esplicite.
- WATCH compra solo se passano trigger Bollinger setup-specifico e gate Context V1.
- SELL fase 1 resta invariato rispetto a Bollinger-only, per isolare l'effetto dei gate di ingresso.
- SELL strategica ragiona su candele 1m chiuse; microbar 5s non genera invalidazioni/target/trailing strategici.
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

- implementazione del charter strategico `archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`;
- allineamento decisionale 1m end-to-end introdotto dal Consiglio il 2026-07-04;
- implementazione delle costanti/enum condivise;
- pubblicazione feature context da DocBrown;
- gate Context V1 in ACDC;
- diagnostica `/management` e UI.

Blocco corrente vincolante:

```text
A0 - Allineamento 1m Decisionale
```

Policy corrente:

- indicatori, contract, WATCH, BUY e SELL strategica devono usare candele 1m chiuse;
- `binance` e' il bucket decisionale target;
- `binance-realtime` e' diagnostica/UI e non puo' autorizzare BUY;
- `binance-microbar` e' replay/forensics/gap/timing/execution observation e non puo' alimentare indicatori strategici;
- SELL deve esporre separatamente metadati di decisione 1m e metadati di esecuzione/replay 5s;
- la fonte decisionale 1m non puo' essere ricostruita aggregando realtime o microbar;
- il decision snapshot deve esporre bucket, interval, candle state, feature window, candle count, max gap e staleness;
- il lookback decisionale deve coprire EMA50 e volume ratio 1m/20m; una finestra da 15m e' insufficiente;
- microbar synthetic da backfill 1m espanso a 5s devono essere marcate e non usate come evidenza di micro timing;
- nessuna nuova RUN PAPER prima di `1m_alignment_ready = true`.

AS-IS codice verificato:

- DocBrown `InfluxSnapshotService` usa ancora `microbarBucketName()` per storico/live feature;
- ACDC `InfluxSnapshotService` usa ancora `microbarBucketName()` per historical/current snapshot e preferisce microbar
  nel replay source fallback;
- `acdc_shared_runtime_config` descrive ancora microbar come condivisa da trading e ML;
- hft-fe contiene superfici legacy con selettore REAL, mentre Kenshiro blocca `REAL_RUN`.
- non esiste ancora un owner/runtime contract esplicito per `1m_alignment_ready`.

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

1. Eseguire il blocco A0 del charter AS-IS: audit e progetto/implementazione dell'allineamento 1m decisionale.
2. Non avviare PAPER finche' DocBrown e ACDC non usano la stessa base 1m chiusa per contract/current state.
3. Definire ed esporre `1m_alignment_ready` con blocker specifici in `/management/state`.
4. Definire soglie operative per `decision_max_gap_seconds` e `decision_staleness_seconds`.
5. Verificare build/test cross-repo dei moduli toccati.
6. Deployare ogni modulo toccato prima di validazione operativa.
7. Fare check del Consiglio contro `archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md` e
   `archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`.
8. Avviare PAPER solo dopo `1m_alignment_ready = true`, stato `/management` pulito e contract completo.
