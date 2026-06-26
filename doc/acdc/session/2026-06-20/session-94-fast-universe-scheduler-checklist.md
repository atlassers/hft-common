# Session 94 - Fast Universe Scheduler Checklist

Data: 2026-06-20.

## Obiettivo

Sostituire il concetto di `DROP` con scheduling computazionale shadow:

- nessun simbolo viene escluso definitivamente;
- ogni ciclo processa un sottoinsieme prioritario;
- i simboli differiti restano in coda;
- un full audit periodico riprocessa tutti i 288 simboli;
- la validazione misura quante opportunita' utili sarebbero rimaste differite nel ciclo corrente.

## Ipotesi Pre-Registrata

Il problema principale non e' identificare con certezza simboli negativi, ma ridurre `advice_age` e tempo ciclo senza perdere definitivamente reversal utili.

Uno scheduler outcome-free puo' ridurre il carico per giro se:

- ordina i simboli per costo/opportunita' computazionale osservabile;
- non fa DROP definitivo;
- mantiene audit full-universe periodico;
- documenta i simboli utili differiti.

## Criteri Di Accettazione

Per autorizzare aggiornamento del charter:

- endpoint shadow implementato e Docker-deployed;
- test multi-finestra endpoint-driven;
- `permanent_missed_useful_symbols = 0` per definizione del full audit;
- `deferred_useful_symbols` misurato e accettabile solo se recuperabile entro audit SLA;
- `estimated_cycle_reduction_ratio > 0`;
- nessun enforcement automatico senza ulteriore PAPER/shadow audit.

Se i test mostrano troppi simboli utili differiti, il charter non viene aggiornato come vincolo operativo.

## Checklist Livello 1

- [x] L1.1 Scrivere checklist a 2 livelli e ipotesi.
- [x] L1.2 Implementare endpoint shadow scheduler DocBrown.
- [x] L1.3 Build DocBrown.
- [x] L1.4 Commit/push DocBrown.
- [x] L1.5 Deploy Docker DocBrown.
- [x] L1.6 Eseguire test multi-finestra.
- [x] L1.7 Analizzare deferred useful e riduzione stimata.
- [x] L1.8 Aggiornare charter solo se validato: `NOT_DONE_NOT_VALIDATED`.
- [ ] L1.9 Report finale e commit/push documentazione.

## Checklist Livello 2

### L2.1 Modello Scheduler

- [x] Nessun `DROP` definitivo.
- [x] Stati ammessi: `HOT`, `WARM`, `COLD`.
- [x] Decisione outcome-free.
- [x] Full audit periodico obbligatorio.
- [x] Ranking strutturale implementato.
- [x] Report `scheduled` vs `deferred` implementato.

### L2.2 Endpoint

- [x] DTO request/response.
- [x] Endpoint REST.
- [x] Parametri `perCycleSymbolLimit`, `auditEveryCycles`, `cycleIndex`.
- [x] Metriche scheduled/deferred.
- [x] Metriche useful/deferred useful.
- [x] Strategic status.

### L2.3 Test

- [x] Smoke test endpoint.
- [x] Test multi-finestra scenario 96/288.
- [x] Test audit cycle full universe.
- [x] Salvare request/response in `/tmp`.
- [x] Decidere PASS/FAIL.

### L2.4 Charter

- [ ] Se PASS, aggiungere scheduler come ottimizzazione ML, non come regola trading.
- [x] Se FAIL, documentare non validato.
- [x] Non introdurre enforcement se FAIL.

## Stato Realtime

- Stato corrente: `FAIL_NOT_VALIDATED_FOR_OPERATIONAL_SCHEDULING`.
- Ultimo aggiornamento: test multi-finestra completati; `192/288` e `240/288` differiscono opportunita' utili in alcune finestre; audit full scan passa ma non basta per rendere operativo lo scheduler.

## Risultati Test

Endpoint:

`POST /docbrown/rem/blank-candidates/REM_CURRENT/universe-scheduler/shadow`

Commit DocBrown: `0b7a53a`.

Smoke `96/288`:

- reduction stimata: `0.6666666666666666`;
- useful symbols: `119`;
- deferred useful: `53`;
- verdict: `WARN_USEFUL_DEFERRED_REQUIRES_AUDIT`.

Scenario `192/288`, quattro finestre:

- reduction stimata: `0.3333333333333333`;
- deferred useful: `5`, `13`, `7`, `7`;
- verdict: `WARN_USEFUL_DEFERRED_REQUIRES_AUDIT`.

Scenario `240/288`, quattro finestre:

- reduction stimata: `0.16666666666666666`;
- deferred useful: `0`, `2`, `2`, `0`;
- verdict: non robusto, ancora useful deferred in due finestre.

Audit cycle:

- scheduled symbols: `288/288`;
- deferred useful: `0`;
- verdict: `PASS_AUDIT_FULL_SCAN`.

Conclusione:

- lo scheduler shadow e' disponibile come diagnostica;
- il ranking strutturale top-N non e' validato come scheduling operativo;
- non aggiornare il charter come vincolo operativo;
- prossimo refinement ammesso: scheduling round-robin con SLA di staleness, non priority-only.
