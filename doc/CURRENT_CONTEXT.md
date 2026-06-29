# Current Context

Ultimo aggiornamento: 2026-06-29 17:20 CEST.

Snapshot operativo corrente del workspace `/home/mbc/Documenti/ws/java/hft`.

## Gerarchia Documentale

1. `hft-common/doc/STRATEGIC_REM_RECOVERY_PLAN.md`
2. `hft-common/doc/BOLLINGER_ONLY_PLAN.md`
3. `hft-common/doc/CURRENT_CONTEXT.md`
4. `hft-common/doc/STRATEGIC_REM_HANDOFF.md`

Se i documenti confliggono, prevale il piano strategico.

## Vincoli Correnti

- REAL vietata.
- PAPER solo da `/management`.
- Il contratto operativo e' solo `bb_*`.
- La sorgente advice runtime e' `hft.acdc_live_bb_advice`.
- WATCH compra solo se il trigger Bollinger setup-specifico e' vero.
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

## Stato Implementativo

Completato e pushato:

- rename runtime contract `LiveBbAdvice` / `acdc_live_bb_advice`;
- readiness `BB_READY`;
- guard/operator SELL con prefisso `BB_ADVICE_*`;
- namespace config operativo `bb.*`;
- rimozione del ramo operativo parallelo dal management;
- pulizia dei residui management in Kenshiro;
- rimozione del laboratorio Python DocBrown non usato dal runtime Quarkus;
- documentazione root FE/Kenshiro riallineata al ciclo Bollinger-only.

## Stato Live Verificato

Da FE `/management`:

```text
globalStatus = BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE
bbReady = false
paperRunning = false
openPositions = 0
activeAdvice = 0
paperEligibleContractActiveAdvice = 0
promotionMode = BOLLINGER_ONLY
```

Interpretazione: runtime pulito, nessuna PAPER attiva, nessuna posizione aperta, nessuna advice fresca da consumare.

## Moduli

- `hft-common`: contratti, enum, costanti e piano strategico.
- `docbrown`: produzione candidate/advice Bollinger.
- `acdc`: WATCH, BUY, SELL, forensics PAPER.
- `kenshiro`: orchestrazione `/management`.
- `hft-fe`: cockpit `/management`.

## Verifiche Recenti

- `hft-common`: `mvn -q -DskipTests install`
- `acdc`: `mvn -q -DskipTests package`
- `docbrown`: `mvn -q -DskipTests package`
- `kenshiro`: `mvn -q test`, `mvn -q -DskipTests package`
- `hft-fe`: `npm run check`, `npm run build`

## Prossimo Step Operativo

1. Verificare scan residui operativi.
2. Buildare i moduli toccati.
3. Deployare i container toccati.
4. Da `/management`, avviare `AUTO_BOLLINGER_START` solo dopo conferma runtime pulito.
5. Misurare nuova PAPER con forensics.
