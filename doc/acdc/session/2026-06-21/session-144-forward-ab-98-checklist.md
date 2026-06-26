# Session 144 - Forward A/B 98 Checklist

Data: 2026-06-21.

## Obiettivo

Introdurre una procedura operativa vincolante per cui ogni nuova run SHADOW/PAPER usata come evidenza sulla baseline `98` deve essere una `FORWARD_AB_98`.

La sessione `98` e' la base contrattuale, non un replay di mercato completo: il buffer realtime Influx non conserva piu' quella finestra. La validazione corretta e' quindi forward, su finestre future comuni.

## Posizione Del Consiglio

Saggio ascoltatore:

Non buttare via la `98`: ha prodotto un flusso completo HEIUSDC advice -> BUY -> SELL con PnL positivo e trailing funzionante. Pero' non va mitizzata: va confrontata in avanti con il ciclo corrente.

Scienziato severo:

Una run singola rolling-paper non dimostra piu' nulla sulla `98`, perche' cambia sorgente advice, generation binding, revalidation ed economic safe. Senza bracci A/B simultanei o almeno co-finestrati, l'evidenza e' contaminata.

Mediano pragmatico:

Il primo obiettivo non e' ottimizzare: e' rendere ogni run leggibile. Si parte con A/B forward osservazionale, poi si introduce enforcement applicativo se serve.

Decisione unica:

Ogni run SHADOW/PAPER validativa deve produrre un artifact `FORWARD_AB_98` con braccio A `BASELINE_98_CONTRACT` e braccio B `CURRENT_ROLLING_PIPELINE`. Le run prive di questo artifact sono `TECHNICAL_ONLY`.

## Definizione Dei Bracci

### Braccio A - BASELINE_98_CONTRACT

- `reversal_pre_trough_drop` fuori da `LIVE_REVALIDATION_FEATURES`.
- `reversal_slope_delta` dentro `LIVE_REVALIDATION_FEATURES`.
- Nessun ulteriore allentamento feature-by-feature.
- BUY, SELL, ranking, trailing e fee invariati rispetto al contratto validativo.
- Advice source da marcare come `BASELINE_98_CONTRACT` o equivalente diagnostico.
- Se il braccio A non puo' ancora essere eseguito dal runtime, deve essere almeno calcolato come shadow/counterfactual sullo stesso snapshot set.

### Braccio B - CURRENT_ROLLING_PIPELINE

- Pipeline corrente DocBrown/ACDC.
- `ROLLING_PAPER` con `source_generation_id`.
- `expectedSourceGenerationId` sulla run.
- Live revalidation attiva.
- Economic safe attivo.
- `ML_READY=true` obbligatorio per PAPER.

## Checklist Pre-Run

- [ ] Nessuna REAL.
- [ ] Nessun H2.
- [ ] Container base up: `acdc-vpn`, `docbrown`, `mysql_container`, `influxer`, `influxdb`.
- [ ] Nessuna SHADOW/PAPER running inattesa.
- [ ] Nessuna posizione aperta inattesa.
- [ ] DocBrown research non `RUNNING`.
- [ ] Per PAPER: `GET /diagnostics/acdc/ml-readiness?profileKey=REM_CURRENT` restituisce `ready=true`.
- [ ] Braccio A definito e riproducibile sulla finestra realtime corrente.
- [ ] Braccio B definito e con generation/source espliciti.
- [ ] Artifact directory creata: `/tmp/session144-forward-ab-98-{timestamp}` o successiva.
- [ ] Run label dichiarata: `FORWARD_AB_98`.
- [ ] Durata minima dichiarata prima dello start.
- [ ] Criteri di stop dichiarati prima dello start.

## Checklist Durante La Run

- [ ] Campionare periodicamente stato execution, budget, reserved e open positions.
- [ ] Salvare `live-advice`, `ml-readiness`, `rem/readiness`, `session-guard`.
- [ ] Salvare per entrambi i bracci:
  - advice emessi;
  - advice ammessi;
  - advice bloccati;
  - BUY;
  - SELL;
  - simboli valutati;
  - source/generation/rule key.
- [ ] Non cambiare gate, ranking, SELL o parametri durante la finestra.
- [ ] Se PAPER, usare `stop-buy` prima dello stop finale e lasciare drenare le posizioni.

## Checklist Post-Run

- [ ] `scoring`.
- [ ] `timeline`.
- [ ] `sell-capture`.
- [ ] `post-sell-forensics`.
- [ ] `live-revalidation-counterfactual`.
- [ ] Stato finale execution.
- [ ] Open positions finali `0`.
- [ ] Reserved finale `0`.
- [ ] Classificazione trade:
  - `GOOD_FLOW`;
  - `GOOD_ADVICE_LOW_CAPTURE`;
  - `BAD_SELL_AFTER_MFE`;
  - `ZERO_MFE_BAD_ADVICE`;
  - `LOSS_CAP_ZERO_MFE`;
  - `TIMEOUT_ZERO_MFE`;
  - `GOOD_BLOCK`;
  - `BAD_BLOCK`;
  - `AMBIGUOUS_BLOCK`.
- [ ] Tabella comparativa A/B compilata.
- [ ] Verdetto del Consiglio scritto.
- [ ] Handoff aggiornato.

## Metriche Minime A/B

Per ogni braccio:

- simboli valutati;
- advice totali;
- advice `PAPER_ELIGIBLE`;
- BUY;
- trade chiusi;
- win/loss;
- PnL netto;
- avg/max net return;
- safe hit rate;
- target hit rate;
- capture ratio medio;
- zero-MFE rate;
- loss-cap rate;
- timeout rate;
- dynamic trailing rate;
- blocked advice;
- `GOOD_BLOCK`, `BAD_BLOCK`, `AMBIGUOUS_BLOCK`;
- post-SELL verdict.

## Regole Di Verdetto

`PASS_BASELINE` richiede:

- run pulita;
- confronto A/B disponibile;
- braccio A positivo o almeno superiore al braccio B su PnL/capture/zero-MFE;
- nessuna anomalia SELL dominante;
- counterfactual non sfavorevole;
- forensics non contaminato.

`FAIL_BASELINE` richiede:

- run pulita;
- confronto A/B disponibile;
- braccio A negativo o non migliore del braccio B;
- falsi positivi o SELL failure spiegati;
- nessuna infrastruttura contaminante.

`INCONCLUSIVE` se:

- dati insufficienti;
- nessun trade;
- Influx/microbar gap impediscono lettura;
- A/B non completo;
- ML_READY instabile;
- source/generation incoerenti.

`TECHNICAL_ONLY` se:

- manca il braccio A;
- manca il braccio B;
- la run e' stata lanciata per verificare endpoint, deploy o wiring.

## Strategia Di Introduzione

### Step 1 - Procedura Vincolante

- Aggiornare charter e handoff.
- Da questo momento nessuna run SHADOW/PAPER puo' essere citata come evidenza baseline senza checklist `FORWARD_AB_98`.

### Step 2 - Enforcement Applicativo

- [x] Estendere `RunRequest` con metadati forward A/B.
- [x] Persistire su `acdc_run_execution.metadata_json`:
  - `validationProtocol=FORWARD_AB_98`;
  - `forwardAbGroupId`;
  - `forwardAbArm`;
  - `baselineReferenceSession=98`;
  - `comparisonArm`;
  - `expectedSourceGenerationId`, se PAPER rolling.
- [x] Marcare automaticamente `TECHNICAL_ONLY` le run prive di protocollo A/B, cosi' non possono essere usate come evidenza baseline.
- [x] Rifiutare `FORWARD_AB_98` incompleti con `409`.

Payload minimo braccio A:

```json
{
  "executionMode": "SHADOW",
  "validationProtocol": "FORWARD_AB_98",
  "forwardAbGroupId": "ab98-YYYYMMDDTHHMMSSZ",
  "forwardAbArm": "A_BASELINE_98_CONTRACT",
  "baselineReferenceSession": "98",
  "comparisonArm": "B_CURRENT_ROLLING_PIPELINE"
}
```

Payload minimo braccio B:

```json
{
  "executionMode": "PAPER",
  "validationProtocol": "FORWARD_AB_98",
  "forwardAbGroupId": "ab98-YYYYMMDDTHHMMSSZ",
  "forwardAbArm": "B_CURRENT_ROLLING_PIPELINE",
  "baselineReferenceSession": "98",
  "comparisonArm": "A_BASELINE_98_CONTRACT"
}
```

### Step 3 - Diagnostica A/B

- [x] Aggiungere endpoint diagnostico dedicato:
  - `GET /diagnostics/acdc/forward-ab/98?groupId=...`
- [x] Il report espone readiness strutturale, execution per braccio e metriche aggregate minime da posizioni/decisioni.
- [ ] Il report deve unire anche timeline, sell-capture, counterfactual e post-sell forensics dettagliati per i due bracci.

Nota:

- la prima versione dell'endpoint verifica readiness strutturale del gruppo A/B e la presenza dei due bracci;
- il merge statistico dettagliato resta obbligatorio prima di dichiarare una PAPER promossa.

## Stato

`FORWARD_AB_98_RUNTIME_READY`.
