# Session 47 - SHADOW V40 Ranking Audit

Data: 2026-06-16.

## Obiettivo

Verificare la nuova selezione BUY dopo `V40__rem_entry_advice_ranking.sql`:

- advice DocBrown fresche;
- ranking BUY outcome-first;
- nessuna BUY con `reversal_ml_score < 0`;
- SELL runtime completa con stop-buy e drenaggio.

## Preparazione

DocBrown avviato localmente su `8083` con MySQL e Influx reali.

Job:

- endpoint: `POST /docbrown/rem/research/REM_CURRENT/run`;
- campioni outcome: `5000`;
- GOOD: `1493`;
- BAD: `3507`;
- firme outcome promosse: `334`;
- regole ML valutate: `365`;
- regole ML promosse: `225`;
- advice attive prima della SHADOW: `195`.

## SHADOW

Execution: `46`.

- avvio via hft-fe wrapper;
- stop-buy immediato;
- status finale: `COMPLETED`;
- posizione aperta: `EURUSDC`;
- BUY: `1.160900000000000000`;
- SELL: `1.161000000000000000`;
- uscita: `EXIT_ML_ADVICE_TIMEOUT`;
- durata advice: `630s`;
- budget finale: `99.952151350171500000`;
- realized: `-0.047848649828500000`;
- MFE/max net return: `0`.

## Advice BUY

Feature principali al BUY:

- `ranking_score=28.380911940931885`;
- `reversal_ml_score=0.087834581246591`;
- `ml_advice_safe_net_return=0.001264392324093817`;
- `ml_advice_max_net_return=0.004739938080495356`;
- `ml_advice_loss_cap_net_return=-0.003791765637371338`;
- `ml_advice_duration_seconds=630`;
- `reversal_ml_profit_probability=0.44616457002036863`;
- `reversal_data_coverage_ratio=0.6312849162011173`;
- `reversal_volume_confirmation=1.4980302123536668`.

## Costi

La trade ha avuto prezzo leggermente favorevole, ma non abbastanza da coprire costi/frizioni:

- gross profit quote: `0.002153501500000000`;
- buy fee quote: `0.024999998913500000`;
- sell fee quote: `0.025002152415000000`;
- net profit quote: `-0.047848649828500000`.

## Reject Principali

- `SHADOW_BUY_STOPPED`: `19303`;
- `REVERSAL_DATA_COVERAGE_LOW`: `330`;
- `REVERSAL_ML_RULE_MISSING`: `62`;
- `REVERSAL_VOLUME_CONFIRMATION_LOW`: `6`.

## Lettura

Il ranking V40 funziona tecnicamente:

- la BUY selezionata aveva score ML positivo;
- i filtri dati hanno escluso segnali scadenti;
- la SELL ha girato ogni pochi secondi;
- la posizione e' stata chiusa al timeout della advice.

Il problema residuo non e' la meccanica SELL di ACDC.
Il problema e' che l'advice ML puo' ancora promuovere movimenti attesi troppo piccoli rispetto ai costi reali.

Decisione scientifica:

- MFE pari a `0` significa che il trade non ha mai raggiunto profitto netto dopo costi;
- il miner deve penalizzare o rifiutare advice con safe/max return troppo vicini alle fee;
- la probabilita' profitto `0.446` e' troppo bassa per consumare budget, anche se lo score ML e' positivo;
- il prossimo tuning deve essere ancora outcome-first: non aggiungere soglie manuali runtime, ma far produrre a DocBrown advice gia' cost-aware con margine minimo netto.
