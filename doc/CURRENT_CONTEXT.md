# Current Context

Ultimo aggiornamento: 2026-06-30 CEST.

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
- implementazione delle costanti/enum condivise;
- pubblicazione feature context da DocBrown;
- gate Context V1 in ACDC;
- diagnostica `/management` e UI.

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
- `influxer`: nessun cambio obbligatorio fase 1; garantisce OHLCV/microbar.

## Prossimo Step Operativo

1. Eseguire i blocchi A-D del charter AS-IS.
2. Verificare build/test cross-repo.
3. Deployare ogni modulo toccato.
4. Fare check del Consiglio contro `archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md` e
   `archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`.
5. Avviare PAPER solo dopo stato `/management` pulito e contract completo.
