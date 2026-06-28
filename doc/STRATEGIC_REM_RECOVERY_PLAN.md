# Strategic REM Recovery Plan

Data: 2026-06-15.

## Status

Questo piano e' vincolante.
Non puo' essere modificato, saltato o sostituito senza fermarsi e chiedere conferma all'utente.

## Gerarchia Documentale

Questo documento e' il vincolo strategico.

Scopo:

- definire obiettivo, regole sacre, diagnosi vincolante e decisioni del Consiglio;
- guidare la creazione di piani operativi, checklist e procedure;
- impedire cambi di famiglia strategica, tuning contaminato e run non confrontabili.

Non e' lo scopo di questo documento:

- elencare tutti gli endpoint disponibili;
- mantenere comandi diagnostici dettagliati;
- fare da snapshot corrente dei moduli.
- registrare avanzamenti di sessione o stato live temporaneo.

Gerarchia:

1. `STRATEGIC_REM_RECOVERY_PLAN.md`: vincolo strategico.
2. `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md`: snapshot corrente cross-modulo, regole globali,
   ultimo piano implementato e prossimo TODO.
3. `STRATEGIC_REM_HANDOFF.md`: manuale operativo con endpoint, ordine diagnostico, script e procedure approvate.

Se handoff o current context confliggono con questo piano, prevale questo piano.

Igiene documentale vincolante:

- questo piano contiene solo vincoli strategici, decisioni validate del Consiglio e regole che non dipendono dallo stato
  live di una singola sessione;
- `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md` contiene stato corrente, avanzamenti dell'ultimo piano, esiti
  dell'ultima diagnostica e prossimo TODO/checklist;
- `STRATEGIC_REM_HANDOFF.md` contiene solo manuale operativo stabile: dove leggere lo stato, quali endpoint/proxy usare,
  payload, query, script, ordine diagnostico e procedure approvate;
- uno snapshot live o l'esito di una singola sessione non va registrato nell'handoff salvo che diventi una procedura,
  un endpoint, una query o una regola operativa riutilizzabile.

## Consiglio Scientifico Permanente

Il piano e le sue evoluzioni sono valutati da un Consiglio Scientifico Permanente composto da tre scienziati senior, super esperti di:

- matematica finanziaria;
- scienze statistiche;
- machine learning;
- informatica;
- sistemistica;
- architettura IT.

I tre membri hanno spirito critico indipendente e possono produrre diagnosi, obiezioni e suggerimenti diversi.

Ruoli:

- Saggio ascoltatore: calmo, prudente, orientato alla continuita' del metodo e alla lettura dei dati senza panico.
- Scienziato severo: pungente, rigoroso, intollerante verso scorciatoie, contaminazioni sperimentali e inferenze non dimostrate.
- Mediano pragmatico: posizione intermedia, orientato alla sintesi operativa, alla fattibilita' ingegneristica e alla misurabilita'.

Ogni piano elaborato dal Consiglio deve sintetizzare le tre posizioni in una decisione unica, motivata e operativa. Se i tre membri divergono, la divergenza va esplicitata; la sintesi finale deve spiegare cosa viene accettato, cosa viene rigettato e quali condizioni rendono il piano scientificamente eseguibile.

## Obiettivo

Riportare ACDC sul filone REM outcome-first originale, usando HFT e DocBrown come riferimento storico, ma mantenendo ACDC come runtime generico DB-driven.

L'obiettivo finale di questo piano e' arrivare a stato `PAPER_READY`, cioe':

- ENTRY REM ripristinate su firme candidate-specific promosse, non soglie statiche globali;
- SHADOW/parity run disponibile e leggibile;
- replay lifecycle BUY->SELL disponibile prima della PAPER;
- ML outcome-first eseguito;
- almeno una configurazione/firma promossa o candidata validata pronta per PAPER;
- nessuna REAL RUN avviata.

## Regole Sacre

1. Non usare soglie statiche globali derivate da un counterfactual come sostituto delle firme REM.
2. Non avviare PAPER RUN finche' il replay lifecycle BUY->SELL non e' disponibile.
3. Non avviare PAPER RUN se il ML REM non ha prodotto firme o se la run non e' marcata esplicitamente come esplorativa.
4. Non cambiare famiglia strategica: REM resta outcome-first.
5. Non modificare il piano senza fermarsi e chiedere conferma.
6. HFT resta riferimento storico; nuovi wrapper operativi stanno in ACDC.
7. REAL resta bloccata.
8. L'ottimizzazione dell'universo ML non puo' essere un filtro negativo definitivo. Dal 2026-06-20 e' ammesso solo scheduling computazionale shadow/ML con SLA: HOT sempre, WARM/COLD a rotazione, full audit periodico, nessun simbolo escluso definitivamente e nessun effetto diretto su BUY/SELL.
9. Dal 2026-06-20 la configurazione sessione `98` e' la baseline candidata sperimentale: `reversal_pre_trough_drop` fuori dal live hard gate, `reversal_slope_delta` dentro. Non sono ammessi ulteriori allentamenti feature-by-feature del live hard gate finche' questa baseline non e' stata validata su run ripetute e forensics complete.
10. Dal 2026-06-21 una PAPER validativa non puo' partire se il ciclo ML e l'orchestrazione non sono in stato `ML_READY`. `ML_READY` richiede: training/mining pesante completato senza rollback, regole/advice persistite o stato `NO_SIGNATURES` dichiarato, scoring live leggero verificato, nessuna execution PAPER gia' running, scheduler ACDC coerente con la finestra validativa, e readiness endpoint/diagnostica consultabile prima dello start.
11. Dal 2026-06-21 il training/mining pesante DocBrown non fa parte della finestra PAPER. La finestra PAPER misura trading runtime e SELL/forensics; il mining pesante e' una fase preparatoria separata, osservabile, batchata e con checkpoint persistenti. Se il mining va in timeout/rollback, la run e' `FAILED_PREREQ`, non una validazione della baseline.
12. Dal 2026-06-21 ACDC deve fail-closed su PAPER quando la provenienza delle advice e' ambigua, vecchia o incoerente con il piano di validazione. Advice residue da sessioni precedenti possono essere diagnosticate, ma non devono alimentare una run dichiarata pulita.
13. Dal 2026-06-21 `hft-common/doc/STRATEGIC_REM_HANDOFF.md` e' il manuale operativo aggiornato di ogni nuova analisi REM. Ogni
    nuovo check, endpoint, payload, script, query o procedura diagnostica approvata dal Consiglio deve essere aggiunta a
    quel documento appena diventa vincolante e riutilizzabile. Gli avanzamenti di sessione e gli snapshot live stanno nel
    current context o in checklist/sessioni dedicate, non nell'handoff.
14. Dal 2026-06-21 ogni nuova run SHADOW o PAPER usata come evidenza sulla baseline `98` deve essere una `FORWARD_AB_98`: nella stessa finestra realtime devono essere raccolti e confrontati il braccio A `A_BASELINE_98_CONTRACT` e il braccio B `B_CURRENT_ROLLING_PIPELINE`. Una run SHADOW/PAPER non marcata e documentata come forward A/B e' ammessa solo come test tecnico/infrastrutturale e non puo' promuovere, bocciare o modificare la baseline.
15. Dal 2026-06-26 le soglie economiche che decidono l'eleggibilita' runtime devono essere candidate/advice-specific.
    ACDC non puo' ricostruire un floor globale da `entry_friction + buffer` o da config live runtime per trasformarlo in
    gate PAPER. DocBrown deve pubblicare il contratto economico nell'advice; ACDC lo consuma fail-closed se incoerente,
    ma non inventa soglie fuori dal payload ML.
16. Dal 2026-06-26 il SELL risk-control puo' proteggere un MFE netto positivo gia' osservato senza considerarlo
    allargamento gate. Se un trade rientra sotto break-even dopo MFE positivo, il dynamic trailing puo' chiudere prima
    del timeout; questa regola riduce decay/loss e non autorizza PAPER con `ML_READY=false`, non cambia BUY eligibility
    e non sostituisce evidenza Forward A/B.
17. Dal 2026-06-27 il SELL risk-control puo' chiudere prima del timeout pieno un trade BUY-confirmed che resta a
    zero-MFE e decade sotto break-even. La durata deve arrivare esclusivamente dal contratto ML/advice
    `ml_advice_no_mfe_timeout_seconds`; se manca, ACDC resta fail-closed e non inventa fallback da durata advice,
    ratio, metadata DB o config runtime. Questa regola non e' una soglia di selection, non allarga gate/live/PAPER e
    non sostituisce WATCH o Forward A/B evidence.
18. Dal 2026-06-28 il vincolo strategico runtime diventa Bollinger-only. Il processo resta
    `ML -> live-score -> WATCH -> BUY -> SELL -> forensics`, ma le definizioni decisionali sono solo Bollinger:
    DocBrown identifica advice da `bb_*`, il live-score produce un set live speculare `live_bb_*`, la WATCH compra solo
    quando il trigger corrente `bb_buy_contract_pass=1` e' vero, e la SELL usa target/loss/timeout derivati dal contratto
    Bollinger. Reversal, trough, slope, simbolo, volume e live-revalidation legacy possono restare solo come diagnostica
    storica finche' le entity/tabelle non vengono ritirate, ma non possono selezionare, ordinare, bloccare o confermare
    BUY.

## Diagnosi Vincolante

La deviazione critica e' stata `V14__relax_rem_entry_thresholds_from_counterfactual.sql`.
V14 ha trasformato guardie candidate-specific `FEATURE_*` in soglie statiche `BETWEEN/GTE`.
Questa modifica era esplorativa, non validata, e non puo' essere la base della prossima PAPER RUN.

I "47 simboli profittevoli" erano opportunita' counterfactual basate su massimo favorevole futuro.
Non dimostravano una trade chiusa in profitto con fee, tick, sizing, ranking, latenza e SELL reale.

## Piano Esecutivo

### Fase 1 - Ripristino REM Candidate-Specific

- Aggiungere migration ACDC che marca V14 come esplorativa non-validata.
- Ripristinare le guardie ENTRY REM a operatori `FEATURE_BETWEEN` / `FEATURE_GTE`.
- Conservare fallback statici solo come fallback, non come criterio primario.
- Aggiornare JUnit dedicati.

Exit criteria:

- `REM_CURRENT` usa candidate-specific guards.
- Test configurazione aggiornato.
- Nessuna PAPER avviata.

### Fase 2 - SHADOW/Parity Diagnostics

- Aggiungere diagnostics ACDC per spiegare per ogni candidato:
  - firma/parametro HFT usato;
  - feature live ACDC;
  - guardia che accetta o rifiuta;
  - ranking score;
  - decisione ACDC;
  - motivo di eventuale divergenza rispetto al contratto HFT/DocBrown.
- Esporre endpoint sotto `/diagnostics/acdc/rem/*`.

Exit criteria:

- una SHADOW/parity run reale produce report JSON.

### Fase 3 - Replay Lifecycle BUY->SELL

- Aggiungere valutazione lifecycle completa:
  - entry price;
  - fee;
  - slippage;
  - trailing dinamico;
  - stop loss;
  - max hold;
  - esito finale BUY->SELL.
- Non usare solo max favorable excursion.

Exit criteria:

- il report distingue `would_hit_profit` da `would_close_profitably`.

### Fase 4 - ML Outcome-First

- Eseguire il miner REM DocBrown come motore ML outcome-first.
- Usare dati microbar/Influx reali.
- Generare report firmato in `target/rem-ml`.
- Promuovere solo firme validate o dichiarare `NO_SIGNATURES`.
- Se il ciclo ML su tutto l'universo e' troppo lento, usare `ROUND_ROBIN_SLA` come scheduling computazionale:
  - `HOT` schedulati ogni ciclo;
  - `WARM`/`COLD` schedulati a rotazione entro SLA;
  - full audit 288/288 ogni N cicli;
  - universo simboli deterministico;
  - misurazione obbligatoria di `eventuallyScheduledUsefulSymbols`, `missedWithinSla`, riduzione media e deferred useful per ciclo.
- Questo scheduling non e' una regola di trading, non promuove advice e non puo' scartare simboli definitivamente.

Exit criteria:

- report ML prodotto;
- almeno una firma candidata/promossa, oppure stato fail-closed documentato.
- se usato scheduler, `missedWithinSla=0` su validazione shadow multi-finestra prima di abilitarlo nel ciclo ML operativo.

### Fase 5 - PAPER Readiness

- Verificare che ACDC legga firme candidate-specific.
- Verificare SHADOW/parity con dati reali.
- Verificare replay lifecycle sui candidati.
- Solo se tutto passa, marcare `PAPER_READY`.

Exit criteria:

- ACDC e' pronto per PAPER RUN dal FE.
- La PAPER non viene avviata automaticamente da questo piano.

## Comandi Consentiti

- SHADOW: consentita.
- DRY: consentita.
- ML/replay offline: consentiti.
- PAPER: non consentita finche' non si raggiunge `PAPER_READY`, salvo run di validazione baseline esplicitamente pre-registrate, endpoint-driven, containerizzate, senza REAL, con stop/drain ordinato e report forense post-run.
- REAL: vietata.

## Stato Attuale

`BASELINE_98_CANDIDATE_NOT_VALIDATED`.

La sessione `98` ha prodotto il miglior compromesso recente: una catena completa `HEIUSDC` advice -> BUY -> SELL con net `+0.3230705383788`, exit `EXIT_ML_ADVICE_DYNAMIC_TRAILING` e capture ratio `0.6091855097410218`.

La sessione `99` ha testato l'allentamento successivo (`reversal_slope_delta` fuori dal hard gate) e lo ha rigettato: execution `10`, 6 trade, 3 win, 3 loss, net `-0.04795612367618`, 2 loss-cap zero/low-MFE. Il rollback e' stato eseguito.

Stato attivo vincolante:

- `reversal_pre_trough_drop` resta fuori da `LIVE_REVALIDATION_FEATURES`;
- `reversal_slope_delta` resta dentro `LIVE_REVALIDATION_FEATURES`;
- DocBrown/ACDC devono trattare questa configurazione come `BASELINE_98_CANDIDATE`;
- il prossimo lavoro non e' tuning, ma validazione ripetuta della baseline `98`.

ACDC non deve avviare REAL. PAPER e' ammessa solo come validazione scientifica della baseline `98`, seguendo checklist del Consiglio e producendo report forense completo.

## Decisione Del Consiglio - Forward A/B Obbligatorio

La sessione `98` non e' replayabile come mercato completo perche' il buffer realtime Influx e' temporaneo. La baseline `98` resta quindi una baseline forense e contrattuale, non un dataset storico riproducibile tick-by-tick.

Decisione vincolante:

- ogni nuova SHADOW/PAPER validativa deve confrontare in avanti, sulla stessa finestra realtime, almeno due bracci:
  - `A_BASELINE_98_CONTRACT`: configurazione baseline `98`, cioe' `reversal_pre_trough_drop` fuori dal live hard gate, `reversal_slope_delta` dentro, nessun ulteriore tuning feature-by-feature;
  - `B_CURRENT_ROLLING_PIPELINE`: pipeline corrente con source governance, rolling promotion, live revalidation, economic safe e generation binding;
- il confronto deve misurare non solo PnL, ma anche opportunity set, advice ammessi/bloccati, MFE, safe hit, capture, zero-MFE, loss-cap, timeout, counterfactual dei blocchi e post-SELL forensics;
- senza braccio A e braccio B sulla stessa finestra, la run e' `TECHNICAL_ONLY` o `INCONCLUSIVE`, mai `PASS_BASELINE` o `FAIL_BASELINE`;
- la procedura operativa approvata e' mantenuta in `hft-common/doc/STRATEGIC_REM_HANDOFF.md` e nella checklist `hft-common/doc/acdc/session/2026-06-21/session-144-forward-ab-98-checklist.md`.

Stato conseguente:

`BASELINE_98_CANDIDATE_REQUIRES_FORWARD_AB`.

## Decisione Del Consiglio - ML Readiness Prima Della PAPER

La sessione `105` non e' una bocciatura della baseline `98`: e' una bocciatura dell'orchestrazione pre-run.

Decisione vincolante:

- una run PAPER validativa deve essere preceduta da un check `ML_READY`;
- `acdc_reversal_ml_rule=0`, DocBrown research `RUNNING`, timeout transazionale, rollback o advice residue incoerenti bloccano lo start;
- DocBrown heavy mining deve essere reso completabile e osservabile prima di nuove finestre PAPER;
- la finestra PAPER parte solo dopo scoring live leggero verificato;
- il Consiglio non accetta piu' run "semi-pulite" o contaminate da scheduler/advice residue come evidenza sulla baseline.

Stato conseguente:

`BASELINE_98_CANDIDATE_BLOCKED_BY_ML_ORCHESTRATION`.

Stato operativo corrente dopo l'introduzione del protocollo Forward A/B:

`BASELINE_98_CANDIDATE_REQUIRES_FORWARD_AB`.

Una RUN validativa resta bloccata finche' `ML_READY=false`; lo stato operativo dettagliato e la checklist corrente sono
mantenuti nello snapshot `/home/mbc/Documenti/ws/java/hft/hft-common/doc/CURRENT_CONTEXT.md`, mentre endpoint, payload e procedure
stabili restano in `hft-common/doc/STRATEGIC_REM_HANDOFF.md`.

## Decisione Del Consiglio - Lifecycle Capture Come Ramo Parallelo

Dal 2026-06-23 un candidato rolling puo' esporre un ramo parallelo lifecycle-capture quando l'end-return classico non
passa ma la dinamica BUY->SELL con take-profit prioritario mostra evidenza di cattura safe prima del decay.

Decisione vincolante:

- il ramo lifecycle-capture non sostituisce `PASS_CANDIDATE` e non autorizza PAPER;
- lo status parallelo ammesso e' `PASS_CAPTURE_CANDIDATE_REQUIRES_SHADOW`;
- lo strategic status ammesso e' `LIFECYCLE_CAPTURE_REQUIRES_SHADOW_PREFLIGHT`;
- Kenshiro deve continuare a promuovere verso PAPER solo `PASS_CANDIDATE_REQUIRES_PAPER_PREFLIGHT`;
- un candidato lifecycle-capture puo' alimentare solo SHADOW/research preflight tecnico, con stessi live gate e stessi
  SELL guard del path PAPER;
- ogni passaggio futuro da lifecycle-capture a PAPER richiede evidenza ripetuta, forensics, `ML_READY=true` e Forward A/B
  98, senza REAL.

No-go:

- non trasformare un singolo batch lifecycle-capture in `PAPER_ELIGIBLE`;
- non abbassare Wilson/end-return chiamandolo lifecycle;
- non allargare live revalidation, PAPER gate o SELL;
- non scegliere simboli manualmente.

## Decisione Del Consiglio - REM Pre-BUY Watch

Dal 2026-06-24 il sottoprotocollo corretto e' `REM_PRE_BUY_WATCH_V1`.

Decisione vincolante:

- il watch non e' un ramo SHADOW tecnico post-selezione e non e' una selection alternativa;
- il watch entra solo dopo scan/scoring live, conferma live ed eleggibilita' BUY;
- il watch vive dentro il normale scheduling BUY: un candidato BUY-eligible viene registrato come `WATCHING` e conta
  come BUY pending ai fini dei limiti operativi;
- le condizioni di conferma sono le stesse condizioni del BUY, rivalutate dal normale contratto ENTRY/ML;
- timeout, obbligatorieta' e parametri watch devono arrivare dall'ML/advice; non sono ammessi threshold watch manuali
  paralleli;
- appena il contratto BUY torna vero entro il timeout ML, il runtime autorizza la BUY nello stesso path di apertura
  posizione;
- se il timeout scade o il contratto BUY non e' piu' valido, il watch chiude senza BUY e resta tracciato;
- il runtime SELL resta invariato.

No-go:

- non ripristinare `REVERSAL_WATCH_SHADOW_PREFLIGHT`;
- non introdurre soglie watch statiche in ACDC/Kenshiro/FE;
- non usare il watch per promuovere batch falliti o allargare gate/live/SELL;
- non ignorare watch scaduti, invalidati o confermati-ma-rifiutati dal runtime;
- non avviare PAPER se `ML_READY=false`;
- non avviare REAL.
