# Session 93 - Fast Universe Triage Plan And Checklist

Data: 2026-06-20.

## Obiettivo

Ridurre il costo computazionale del ciclo REM su universo ampio senza perdere opportunita' reversal buone.

Il componente proposto non e' un modello predittivo BUY. E' un filtro conservativo di esclusione: deve scartare solo simboli probabilmente non tradabili o incapaci di produrre MFE netto eseguibile nella finestra recente.

## Piano Ultra-Dettagliato

### 1. Definizione Scientifica

Ipotesi causale:

- il ciclo ML su troppi simboli aumenta la latenza advice -> BUY;
- se l'advice decade rapidamente, la latenza trasforma segnali teoricamente validi in BUY tardive o a MFE zero;
- un triage conservativo puo' ridurre simboli e tempo ciclo senza modificare la logica REM, se mantiene alta recall sugli eventi utili.

Metrica primaria:

- `missed_useful_symbols = 0` nella prima validazione shadow.

Metriche secondarie:

- `reduction_ratio` significativo;
- `missed_good_flow = 0`;
- `missed_profitable_symbol = 0`;
- `missed_executable_mfe_symbol = 0`;
- `estimated_cycle_time_saved_ratio > 0`;
- nessun peggioramento stimato di net expectancy sul set tenuto.

Definizione di simbolo utile:

- almeno una osservazione con `max_net_return >= min_executable_entry_edge`;
- oppure almeno una osservazione con `end_net_return > 0`;
- oppure classificazione storica/forensics `GOOD_FLOW`, quando disponibile.

Output ammessi:

- `KEEP`;
- `DROP_UNTRADABLE`;
- `UNKNOWN_KEEP`.

Regola fondamentale:

- in caso di dubbio, `KEEP`.

Metriche ammesse per DROP:

- tick count insufficiente;
- distinct price points insufficiente;
- quote volume recente nullo/insufficiente;
- range netto recente inferiore a costi + edge;
- executable MFE rate storico recente troppo basso;
- zero-MFE rate troppo alto;
- campione insufficiente, ma solo come `UNKNOWN_KEEP`, non come DROP nella prima versione.

Metriche vietate:

- trend positivo;
- momentum bullish;
- performance recente positiva come requisito;
- filtri che penalizzano una caduta solo perche' e' una caduta;
- regole simbolo-specifiche.

### 2. Implementazione Shadow

Implementare in DocBrown un endpoint:

`POST /rem/blank-candidates/{profileKey}/universe-triage/shadow`

Input:

- `from`, `to`;
- `horizonSeconds`;
- `featureWindowMinutes`;
- `cadenceSeconds`;
- `symbolLimit`;
- `perSymbolLimit`;
- soglie triage opzionali;
- `minExecutableEntryEdge`.

Output:

- simboli totali;
- simboli KEEP/DROP/UNKNOWN;
- reduction ratio;
- utile tra KEEP/DROP;
- falsi negativi;
- stima del tempo risparmiato;
- dettaglio per simbolo con reason.

### 3. Validazione Offline/Shadow

Primo test:

- finestra recente con universe ampio;
- nessun effetto su PAPER/REAL;
- confronto tra decisione triage e outcome osservato nella stessa finestra.

Passa solo se:

- `missedUsefulSymbols = 0`;
- `missedExecutableMfeSymbols = 0`;
- riduzione non nulla;
- report riproducibile via endpoint.

Se non passa:

- non si aggiorna il charter come regola attiva;
- si registra il fallimento;
- si torna a calibrare solo la metrica di esclusione, senza enforcement.

### 4. Shadow Live

Solo dopo un primo pass offline:

- il ciclo rolling continua su universo completo;
- in parallelo si registra cosa avrebbe scartato il triage;
- se un simbolo scartato produce opportunity, il triage resta shadow.

### 5. Enforcement Con Audit

Solo dopo shadow live validato:

- introdurre `universeMode=TRIAGE_ENFORCED`;
- ogni N cicli eseguire full scan di audit;
- fallback automatico a SHADOW se appare un falso negativo utile.

## Checklist Livello 1

- [x] L1.1 Scrivere piano dettagliato e criteri di accettazione.
- [x] L1.2 Implementare endpoint shadow DocBrown.
- [x] L1.3 Build DocBrown.
- [x] L1.4 Commit/push DocBrown.
- [x] L1.5 Deploy Docker DocBrown.
- [x] L1.6 Eseguire primo test shadow endpoint-driven.
- [x] L1.7 Analizzare falsi negativi e riduzione.
- [x] L1.8 Aggiornare charter solo se validato: `NOT_DONE_NOT_VALIDATED`.
- [x] L1.9 Completare implementazione enforced solo se validato: `NOT_DONE_NOT_VALIDATED`.
- [ ] L1.10 Report finale e commit/push documentazione.

## Checklist Livello 2

### L2.1 Piano

- [x] Definire ipotesi causale.
- [x] Definire metrica primaria.
- [x] Definire metriche secondarie.
- [x] Definire output ammessi.
- [x] Definire metriche vietate.

### L2.2 Endpoint Shadow

- [x] Creare DTO request/response.
- [x] Calcolare metriche per simbolo.
- [x] Classificare `KEEP`, `DROP_UNTRADABLE`, `UNKNOWN_KEEP`.
- [x] Calcolare falsi negativi.
- [x] Calcolare reduction ratio.
- [x] Esporre endpoint REST.

### L2.3 Test

- [x] Test endpoint su finestra recente.
- [x] Salvare payload request/response.
- [x] Verificare `missedUsefulSymbols`.
- [x] Verificare `missedExecutableMfeSymbols`.
- [x] Verificare riduzione.
- [x] Decidere PASS/FAIL.

### L2.4 Charter

- [ ] Se PASS, aggiungere vincolo triage al charter.
- [x] Se FAIL, documentare esplicitamente che non e' validato.
- [x] Non introdurre enforcement se FAIL.

## Stato Realtime

- Stato corrente: `NOT_VALIDATED_FOR_ENFORCEMENT`.
- Ultimo aggiornamento: endpoint shadow implementato/deployato; test outcome-free multi-finestra completati; criteri riduttivi con falsi negativi, quindi charter non aggiornato come vincolo operativo.

## Risultati Test

Commits DocBrown:

- `35a5811` - endpoint shadow universe triage.
- `2815539` - rispetto effettivo di `symbolLimit`.
- `97842a1` - decisioni triage rese outcome-free.
- `2d1a100` - default conservativi per evitare drop accidentali.

Esito scientifico:

- la prima versione era metodologicamente errata perche' usava outcome futuro per tenere simboli utili;
- dopo correzione outcome-free, i criteri riduttivi `tick/distinct/range` non passano robustamente;
- `minTick=8`, `distinct>=2`, `range>=0.0005` produce falsi negativi pesanti in una finestra;
- `minTick=2`, `distinct>=2`, `range>=0.0005` produce ancora falsi negativi;
- `minTick=1`, `distinct>=1`, `range=0` non perde opportunita', ma non riduce l'universo;
- `distinct>=2`, `range=0` riduce 8-26%, ma fallisce in una finestra con `missedUsefulSymbols=6`.

Verdetto: `FAIL_NOT_VALIDATED_FOR_ENFORCEMENT`.

Il triage e' disponibile come diagnostica shadow, ma non puo' ancora essere inserito nel charter come filtro operativo/enforced.
