# Session 95 - Round-Robin SLA Scheduler Result

Data: 2026-06-20.

## Scopo

Integrare e verificare una strategia di scheduling REM che riduca il carico ML senza scartare definitivamente simboli.

## Implementazione

DocBrown commit:

- `c32cd98` - aggiunge `ROUND_ROBIN_SLA` a `/docbrown/rem/blank-candidates/{profileKey}/universe-scheduler/shadow`;
- `d6a88bf` - rende deterministico l'universo storico ordinando i simboli prima del `limit`.
- `6dc8c49` - applica `ROUND_ROBIN_SLA` a `/docbrown/rem/blank-candidates/{profileKey}/rolling-validation` tramite `universeMode`.

Parametri validati:

- `scheduleMode=ROUND_ROBIN_SLA`;
- `symbolLimit=288`;
- `hotSymbolLimit=96`;
- `warmCadenceCycles=2`;
- `coldCadenceCycles=4`;
- `auditEveryCycles=4`;
- `cycleIndex=1..4`.

## Risultati

Smoke:

- useful: `140`;
- eventually scheduled useful entro 4 cicli: `140`;
- missed within SLA: `0`;
- riduzione media: `0.3125`.

Multi-finestra valida:

| Window | Useful | Scheduled entro SLA | Missed SLA | Avg reduction |
| --- | ---: | ---: | ---: | ---: |
| `105_65` | `133` | `133` | `0` | `0.3125` |
| `85_45` | `133` | `133` | `0` | `0.3125` |
| `65_25` | `132` | `132` | `0` | `0.3125` |
| `75_35` | `140` | `140` | `0` | `0.3125` |
| `55_15` | `83` | `83` | `0` | `0.3125` |

Finestra `125_85`: inconclusiva, useful `0`, esclusa dall'acceptance.

## Interpretazione

Il ranking top-N sessione 94 non era sufficiente, perche' differiva utili senza garanzia.

`ROUND_ROBIN_SLA` risolve il problema corretto:

- riduce il carico medio del ciclo;
- non elimina simboli;
- garantisce recupero entro SLA;
- audit full scan schedula `288/288`.

Questo non dimostra che REM sia profittevole. Dimostra solo che possiamo ridurre latenza/costo ML senza creare falsi negativi permanenti entro lo SLA testato.

## Charter

Aggiornati:

- `doc/STRATEGIC_REM_RECOVERY_PLAN.md`;
- `hft/diary/strategy-reversal-event-mining-charter.md`.

Vincolo aggiunto:

- scheduling ML ammesso solo come ottimizzazione computazionale;
- nessun DROP definitivo;
- nessun effetto diretto su BUY/SELL;
- full audit periodico obbligatorio;
- uso operativo consentito solo con `missedWithinSla=0` su validazione shadow multi-finestra.

## Rolling Integration

Smoke endpoint-driven su rolling validation:

- ciclo `1`, non-audit:
  - `universeMode=ROUND_ROBIN_SLA`;
  - `persistedRows=1344`;
  - status `FAIL_SELECTION_BIAS` per il modello, ma scheduler operativo.
- ciclo `4`, audit:
  - full scan;
  - `persistedRows=2304`.

Riduzione effettiva del ciclo non-audit rispetto all'audit: circa `41.7%`.

Nota: `FAIL_SELECTION_BIAS` riguarda il modello candidato trovato nella finestra, non il funzionamento dello scheduler. Lo scheduler ha ridotto il carico nel rolling reale e l'audit ha ripristinato il full scan.

## Verdict

`PASS_ROUND_ROBIN_SLA_VALIDATED_FOR_ML_SCHEDULING`.

Prossimo step: usare `universeMode=ROUND_ROBIN_SLA` nelle RUN esplorative ML, mantenendo audit e report `missedWithinSla`.
