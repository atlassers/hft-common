# Session 101 - Baseline 98 Forensics Checklist

Data: 2026-06-20.

## Obiettivo

Eseguire il forensics richiesto dallo scienziato severo sulla validation run della sessione `100`, execution `11`, prima di passare al prossimo step.

Regola di metodo:

- nessun tuning;
- nessuna modifica soglie;
- nessun aggiornamento charter;
- classificazione causale solo sui dati osservati;
- prossimo step ammesso solo dopo distinzione tra problema advice, problema freschezza, problema gate e problema SELL.

Artifact analizzati:

`/tmp/session100-baseline98-validation-run`

## Checklist Livello 1

- [x] L1.1 Verificare artifact disponibili.
- [x] L1.2 Rileggere scoring execution `11`.
- [x] L1.3 Rileggere sell-capture execution `11`.
- [x] L1.4 Rileggere counterfactual blocked advice.
- [x] L1.5 Classificare `BANKUSDC`.
- [x] L1.6 Classificare `VANRYUSDC`.
- [x] L1.7 Classificare `SAPIENUSDC`.
- [x] L1.8 Separare causalmente advice, freshness, gate e SELL.
- [x] L1.9 Scrivere verdetto del Consiglio.
- [x] L1.10 Definire prossimo step senza allontanarsi dal charter.
- [x] L1.11 Commit/push documentazione.

## Checklist Livello 2

### L2.1 Integrity

- [x] Execution id unico: `11`.
- [x] PAPER completata.
- [x] Reserved finale: `0`.
- [x] Trade chiusi: `3`.
- [x] Endpoint scoring disponibile.
- [x] Endpoint sell-capture disponibile.
- [x] Endpoint counterfactual live revalidation disponibile.
- [x] Nessun dato H2 usato.

### L2.2 Metriche Di Sessione

- [x] Net profit quote: `+0.13874403358414`.
- [x] Wins/losses: `2/1`.
- [x] Loss-cap exits: `0`.
- [x] Timeout exits: `1`.
- [x] Dynamic trailing exits: `2`.
- [x] Avg capture ratio: `0.37968667854345717`.
- [x] Avg max net return: `0.004987766860129654`.
- [x] Avg net return: `0.001849920451074229`.

### L2.3 Trade Forensics

- [x] `BANKUSDC`: zero-MFE, timeout, advice fresco, entry drift zero.
- [x] `VANRYUSDC`: target hit, trailing armed, capture bassa.
- [x] `SAPIENUSDC`: target hit, trailing armed, capture buona.
- [x] `AXSUSDC`: blocked advice che sarebbe stata opportunity.
- [x] `XAIUSDC`: blocked advice correttamente esclusa.

## Forensics Causale

### BANKUSDC

Classificazione: `ZERO_MFE_BAD_ADVICE`.

Dati:

- advice age: `11s`;
- entry drift: `0`;
- freshness contract: `true`;
- max net return: `0`;
- safe net return: `0.001046153846153846`;
- exit: `EXIT_ML_ADVICE_TIMEOUT`;
- hold: `905s`;
- trailing armed: `false`;
- loss-cap exits: `0`.

Razionale:

`BANKUSDC` non supporta la tesi del BUY tardivo. Il BUY e' avvenuto con advice fresca e senza drift d'ingresso, ma dopo l'apertura non c'e' stato alcun MFE utile. Non e' un problema primario di SELL: la SELL non poteva catturare profitto perche' il trade non e' mai entrato in profitto netto.

Conclusione scientifica:

il difetto e' a monte, nel riconoscimento del segnale o nel ranking/admission dell'advice. Questa e' la ragione per cui la sessione `100` non puo' diventare `PASS_BASELINE`.

### VANRYUSDC

Classificazione: `GOOD_ADVICE_LOW_CAPTURE`.

Dati:

- advice age: `15s`;
- entry drift: `0`;
- freshness contract: `true`;
- max net return: `0.009944565217391304`;
- net return: `0.003700815217391304`;
- capture ratio: `0.3721444966663023`;
- time to MFE: `384s`;
- time from MFE to SELL: `39s`;
- exit: `EXIT_ML_ADVICE_DYNAMIC_TRAILING`;
- trailing armed: `true`.

Razionale:

Il segnale era economicamente buono: ha superato nettamente il target e ha prodotto MFE quasi pari a `0.995%` netto. La SELL dinamica ha funzionato nel senso stretto: trailing armato ed exit dinamica avvenuta. Il problema e' la qualita' di cattura, non l'esistenza del trailing.

Conclusione scientifica:

`VANRYUSDC` non giustifica nuove guardie BUY. Giustifica un controllo successivo sulla parametrizzazione del trailing/capture, ma solo dopo altra validazione, perche' il trade e' profittevole.

### SAPIENUSDC

Classificazione: `GOOD_FLOW`.

Dati:

- advice age: `9s`;
- entry drift: `0`;
- freshness contract: `true`;
- max net return: `0.005018735362997658`;
- net return: `0.003848946135831382`;
- capture ratio: `0.7669155389640692`;
- time to MFE: `623s`;
- time from MFE to SELL: `93s`;
- exit: `EXIT_ML_ADVICE_DYNAMIC_TRAILING`;
- trailing armed: `true`.

Razionale:

Questo e' il trade piu' pulito della run: advice fresca, nessun drift, target raggiunto, trailing armato, capture accettabile e PnL positivo.

Conclusione scientifica:

la pipeline baseline `98` non e' rotta in modo assoluto. E' instabile: produce almeno un buon flusso completo, ma non filtra ancora con sufficiente affidabilita' gli zero-MFE.

## Counterfactual Gate

Dati:

- blocked advice: `2`;
- `GOOD_BLOCK`: `1`, `XAIUSDC`;
- `BAD_BLOCK`: `1`, `AXSUSDC`;
- ambiguous: `0`.

Interpretazione:

Il gate ha bloccato una opportunity (`AXSUSDC`, max net return `0.012453617021276595`) e ha bloccato correttamente uno zero-MFE/non-positive (`XAIUSDC`). Il campione e' troppo piccolo per rimuovere feature dal live gate.

Decisione:

nessuna rimozione feature-by-feature autorizzata.

## Verdetto Del Consiglio

Il saggio ascoltatore direbbe:

la baseline `98` resta viva perche' due trade su tre sono profittevoli, non ci sono loss-cap, la freschezza e' rispettata, e almeno un flusso (`SAPIENUSDC`) e' tecnicamente sano.

Lo scienziato severo direbbe:

non e' una validazione. `BANKUSDC` e' una falsa ammissione netta: advice fresco, drift zero, MFE zero. Questo invalida qualsiasi conclusione ottimistica sulla qualita' del filtro BUY.

Il mediano direbbe:

procedere, ma senza cambiare strada: ripetere la baseline `98` con forensics obbligatorio, migliorando prima la diagnostica operativa se serve, e dichiarare `PASS_BASELINE` solo su evidenza ripetuta.

## Decisione Operativa

Stato: `BASELINE_98_CANDIDATE_NOT_VALIDATED`.

Non abbiamo finito. La sessione `100` e' positiva ma non abbastanza robusta.

Prossimo step consentito:

1. mantenere invariata la baseline `98`;
2. non toccare BUY guard, live gate, ranking o SELL;
3. eseguire una nuova validazione comparabile;
4. raccogliere obbligatoriamente scoring, sell-capture, counterfactual e timing round-robin -> advice -> BUY -> SELL;
5. promuovere a charter solo se la nuova run conferma PnL positivo, zero-MFE non dominante, capture non degradata e counterfactual non sfavorevole.

Refinement ammesso prima della prossima run:

- solo diagnostica endpoint-driven per osservare posizioni aperte e stato trailing durante la run;
- nessun filtro nuovo;
- nessuna soglia nuova;
- nessuna modifica di modello.

## Stato Realtime

- Stato corrente: `FORENSICS_DONE`.
- Baseline: `98`.
- Verdict execution `11`: `INCONCLUSIVE_POSITIVE_NOT_PASS`.
- Charter update: non autorizzato.
- Prossima azione: commit/push del forensics, poi nuova validazione o diagnostica endpoint-driven minimale.
