# Session 95 - Round-Robin SLA Scheduler Checklist

Data: 2026-06-20.

## Obiettivo

Trovare una quadra entro la finestra operativa disponibile: integrare e verificare uno scheduler REM che riduca il carico ML senza escludere simboli e senza dipendere da un ranking top-N puro.

## Ipotesi

Il problema del `FastUniverseScheduler` sessione 94 non e' il concetto di scheduling, ma il ranking top-N:

- top-N riduce carico;
- pero' differisce simboli utili in modo non controllato;
- serve una garanzia di staleness massima: ogni simbolo deve essere analizzato entro N cicli.

## Strategia Candidata

`ROUND_ROBIN_SLA`:

- HOT: sempre schedulati;
- WARM: schedulati a rotazione ogni `warmCadenceCycles`;
- COLD: schedulati a rotazione ogni `coldCadenceCycles`;
- audit: ogni `auditEveryCycles`, full scan 288/288.

Non e' una regola trading. E' una strategia computazionale del ciclo ML.

## Criteri Di Accettazione

- Build OK.
- Deploy Docker OK.
- Endpoint shadow funziona.
- Multi-cycle coverage: `eventuallyScheduledUsefulSymbols == usefulSymbols` entro SLA.
- Full audit passa.
- Riduzione media per ciclo > 0.
- Se passa, aggiornare charter come ottimizzazione shadow/ML, non come filtro BUY.

## Checklist Livello 1

- [x] L1.1 Creare checklist realtime.
- [x] L1.2 Implementare `ROUND_ROBIN_SLA` in DocBrown.
- [x] L1.3 Build DocBrown.
- [x] L1.4 Commit/push DocBrown.
- [x] L1.5 Deploy Docker DocBrown.
- [x] L1.6 Test smoke endpoint.
- [x] L1.7 Test multi-cycle SLA.
- [x] L1.8 Test multi-finestra.
- [x] L1.9 Aggiornare charter se validato.
- [ ] L1.10 Report finale e commit/push docs.

## Checklist Livello 2

### L2.1 Implementazione

- [x] Aggiungere `scheduleMode`.
- [x] Aggiungere `hotSymbolLimit`.
- [x] Aggiungere `warmCadenceCycles`.
- [x] Aggiungere `coldCadenceCycles`.
- [x] Mantenere `TOP_N` come fallback diagnostico.
- [x] Calcolare schedule reason SLA.

### L2.2 Test

- [x] Smoke `ROUND_ROBIN_SLA`.
- [x] Cicli 1..4 stessa finestra.
- [x] Verifica union scheduled useful.
- [x] Verifica audit cycle.
- [x] Multi-finestra.

### L2.3 Charter

- [x] Se PASS, aggiornare charter.
- [ ] Se FAIL, non aggiornare charter.
- [x] Documentare verdict.

## Stato Realtime

- Stato corrente: `PASS_ROUND_ROBIN_SLA_VALIDATED_FOR_ML_SCHEDULING`.
- Ultimo aggiornamento: validazione multi-finestra completata, charter aggiornato come ottimizzazione ML/scheduler.

## Risultati

Commits DocBrown:

- `c32cd98` - `ROUND_ROBIN_SLA`.
- `d6a88bf` - universo simboli deterministico.
- `6dc8c49` - integrazione `ROUND_ROBIN_SLA` in rolling validation tramite `universeMode`.

Validazione SLA:

- configurazione: `HOT=96`, `WARM cadence=2`, `COLD cadence=4`, audit ogni `4` cicli;
- riduzione media su cicli 1..4: `0.3125`;
- finestra smoke: useful `140`, eventually scheduled useful `140`, missed within SLA `0`;
- finestre valide:
  - `105_65`: useful `133`, scheduled entro SLA `133`, missed `0`;
  - `85_45`: useful `133`, scheduled entro SLA `133`, missed `0`;
  - `65_25`: useful `132`, scheduled entro SLA `132`, missed `0`;
  - `75_35`: useful `140`, scheduled entro SLA `140`, missed `0`;
  - `55_15`: useful `83`, scheduled entro SLA `83`, missed `0`.
- finestra `125_85`: inconclusiva per assenza di useful/outcome, esclusa dall'acceptance.

Verdetto: `PASS_ROUND_ROBIN_SLA_VALIDATED_FOR_ML_SCHEDULING`.

Limite: validato come scheduling del ciclo ML, non come regola trading e non come filtro BUY.

## Rolling Integration Smoke

Endpoint:

`POST /docbrown/rem/blank-candidates/REM_CURRENT/rolling-validation`

Parametri:

- `universeMode=ROUND_ROBIN_SLA`;
- `hotSymbolLimit=96`;
- `warmCadenceCycles=2`;
- `coldCadenceCycles=4`;
- `auditEveryCycles=4`;
- `symbolLimit=288`;
- `perSymbolLimit=2`.

Risultato:

- ciclo `1` non-audit: `persistedRows=1344`;
- ciclo `4` audit: `persistedRows=2304`;
- riduzione effettiva rows non-audit vs audit: `41.7%`;
- entrambi endpoint-driven, Docker-deployed.
