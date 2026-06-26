# Session 94 - Fast Universe Scheduler Result

Data: 2026-06-20.

## Scopo

Valutare un `FastUniverseScheduler` shadow come alternativa al triage/drop:

- nessun simbolo escluso definitivamente;
- scheduling parziale per ridurre tempo ciclo;
- full audit periodico;
- misura dei simboli utili differiti.

## Implementazione

DocBrown espone:

`POST /docbrown/rem/blank-candidates/{profileKey}/universe-scheduler/shadow`

Commit:

- `0b7a53a` - `Add shadow universe scheduler endpoint`.

Il ranking e' outcome-free:

- tick count;
- distinct price points;
- recent range netto;
- sample availability.

MFE/end-return sono usati solo per valutare shadow outcome, non per schedulare.

## Test

### Smoke 96/288

- total symbols: `288`;
- scheduled: `96`;
- deferred: `192`;
- estimated reduction: `0.6666666666666666`;
- useful symbols: `119`;
- scheduled useful: `66`;
- deferred useful: `53`;
- useful deferral rate: `0.44537815126050423`;
- verdict: `WARN_USEFUL_DEFERRED_REQUIRES_AUDIT`.

### Multi-Finestra 192/288

Quattro finestre:

- deferred useful: `5`, `13`, `7`, `7`;
- deferred executable MFE: `5`, `12`, `4`, `4`;
- reduction: `0.3333333333333333`;
- verdict: `WARN_USEFUL_DEFERRED_REQUIRES_AUDIT`.

### Multi-Finestra 240/288

Quattro finestre:

- deferred useful: `0`, `2`, `2`, `0`;
- deferred executable MFE: `0`, `1`, `1`, `0`;
- reduction: `0.16666666666666666`;
- verdict: non robusto, perche' differisce ancora opportunita' utili.

### Audit Cycle

- cycle index: `4`;
- audit every cycles: `4`;
- scheduled: `288/288`;
- deferred useful: `0`;
- verdict: `PASS_AUDIT_FULL_SCAN`.

## Interpretazione

Il concetto scheduler e' piu' corretto del drop filter, ma il ranking top-N strutturale non e' ancora validato:

- riduce tempo stimato;
- non perde simboli in modo permanente grazie all'audit;
- pero' differisce opportunita' utili nei cicli non-audit.

Questo puo' essere accettabile solo se viene definito e validato uno SLA di staleness:

- entro quanti cicli un simbolo differito deve essere analizzato;
- quanto decade un advice se arriva con quel ritardo;
- quanti useful deferred diventano realmente missed dopo latenza.

Questa validazione non e' ancora stata fatta.

## Decisione Charter

Charter non aggiornato come vincolo operativo.

Motivo: il risultato non e' ancora OK. Il full audit funziona, ma lo scheduling priority-only differisce opportunita' utili in finestre non-audit.

## Prossimo Step Ammesso

Implementare e validare uno scheduler round-robin con SLA:

- `HOT`: sempre;
- `WARM`: ogni 2 cicli;
- `COLD`: ogni 3-4 cicli;
- full audit ogni N cicli;
- misurare `max_staleness_seconds`;
- misurare se i deferred useful restano utili al ciclo successivo.

Verdetto: `FAIL_NOT_VALIDATED_FOR_OPERATIONAL_SCHEDULING`, `PASS_DIAGNOSTIC_ENDPOINT_AVAILABLE`.
