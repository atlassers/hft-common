# Strategic Bollinger Context V1 REM Charter

Data: 2026-06-30.

## Status

Questo documento e' il charter strategico vincolante del ciclo REM.

Il vincolo operativo primario e' ora:

```text
hft-common/doc/archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md
```

`archived/BOLLINGER_CONTEXT_V1_PLAN.md` resta baseline tecnica consolidata del ciclo Context V1. Non e' piu' il charter
strategico primario.

`archived/BOLLINGER_ONLY_PLAN.md` resta baseline storica consolidata. Non e' piu' il piano strategico primario.

## Gerarchia Documentale

1. `STRATEGIC_REM_RECOVERY_PLAN.md`: charter sintetico e gerarchia vincolante.
2. `archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`: charter strategico operativo e mappa degli interventi.
3. `archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`: base scientifica del processo e delle formule.
4. `archived/BOLLINGER_CONTEXT_V1_PLAN.md`: baseline tecnica Context V1 precedente.
5. `CURRENT_CONTEXT.md`: snapshot corrente cross-modulo.
6. `STRATEGIC_REM_HANDOFF.md`: manuale operativo stabile.
7. `archived/BOLLINGER_ONLY_PLAN.md`: baseline storica e riferimento di regressione.

Se i documenti confliggono, prevale questo charter; poi
`archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md`; poi
`archived/BOLLINGER_CONTEXT_V1_SCIENTIFIC_PROCESS.md`; poi
`archived/BOLLINGER_CONTEXT_V1_PLAN.md`.

## Processo Vincolante

La pipeline resta:

```text
ML -> live-score -> WATCH -> BUY -> SELL -> forensics
```

Cambiano le definizioni decisionali:

- ML produce candidate/advice Bollinger con contratto context completo.
- Live-score produce un set live speculare, senza modificare semanticamente il contratto storico.
- WATCH osserva il mercato e compra solo quando trigger Bollinger e gate Context V1 passano.
- BUY esegue solo PAPER.
- SELL resta Bollinger-only nella fase 1, per isolare l'effetto dei nuovi gate di ingresso.
- Forensics distingue setup, trigger, regime, context gate, ingresso e uscita.

## Regole Sacre

Le regole vincolanti sono centralizzate in:

```text
hft-common/doc/RULES.md
```

## Setup Ammessi

Sono ammessi solo:

- `BB_REENTRY_MEAN_REVERSION_LONG`
- `BB_SQUEEZE_BREAKOUT_LONG`

Trigger ammessi:

- `BB_REENTRY_CONFIRMED`
- `BB_UPPER_BREAKOUT_CONFIRMED`

Regimi ammessi dal contratto Context V1:

- `REGIME_RANGE`
- `REGIME_SQUEEZE`
- `REGIME_EXPANSION`
- `REGIME_TREND_UP`
- `REGIME_TREND_DOWN`
- `REGIME_CHAOS`

## Consiglio Scientifico Permanente

Il Consiglio resta attivo con tre ruoli:

- Saggio ascoltatore: continuita' e lettura prudente dei dati.
- Scienziato severo: rigore, contratto completo, evidenza non contaminata.
- Mediano pragmatico: implementazione misurabile e run controllate.

Decisione unica del Consiglio:

`archived/BOLLINGER_CONTEXT_V1_AS_IS_INTERVENTION_MAP.md` e' il vincolo strategico charter. Va implementato come estensione
disciplinata del runtime Bollinger-only: nessuna riscrittura della pipeline, nessun ramo operativo parallelo, nessun
ritorno a nomi `ml_*` o contratti legacy.

## Stato Strategico

`BOLLINGER_CONTEXT_V1_CHARTER_ACTIVE_IMPLEMENTATION_IN_PROGRESS`.

Aggiornamento Consiglio 2026-07-04:

```text
A0 - Allineamento 1m Decisionale
```

Prima di nuove RUN PAPER, DocBrown e ACDC devono usare la stessa base decisionale a candele 1m chiuse per indicatori,
contract, WATCH e BUY. Microbar 5s resta solo replay/diagnostica/timing/gap detection. Le run prodotte prima della
chiusura A0 non sono nuova evidenza strategica valida.

La chiusura A0 richiede inoltre:

- `1m_alignment_ready=true` esposto da `/management/state`;
- decision snapshot con bucket, interval, candle state, feature window, candle count, max gap e staleness;
- lookback sufficiente per EMA50 e volume ratio 1m/20m;
- divieto di ricostruire la fonte decisionale aggregando realtime o microbar.

Exit criteria tecnici della fase corrente:

- costanti, enum e reason Context V1 condivise;
- DocBrown pubblica advice con setup, trigger, regime e context completo;
- ACDC applica ContextGateAudit dopo il trigger audit Bollinger e prima della BUY;
- `policyJson` congela i valori `entry_*` usati per il BUY;
- Kenshiro espone strategy family, setup/regime e diagnostica context su `/management`;
- hft-fe mostra family, regime, reason context e confronto diagnostico;
- build, deploy e PAPER validation completate sui moduli toccati.

La valutazione finanziaria resta PAPER-only e separata per setup/regime. Context V1 prosegue solo se migliora le
metriche indicate nel piano vincolante.
