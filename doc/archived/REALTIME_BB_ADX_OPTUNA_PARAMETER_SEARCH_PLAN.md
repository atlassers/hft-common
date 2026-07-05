# Realtime BB ADX Optuna Parameter Search Plan

Data: 2026-07-05.

## Stato Del Documento

Questo documento e' il piano tecnico per costruire un laboratorio offline Python che seleziona un sottoinsieme
testabile dei profili `REALTIME_BB_ADX_V1` gia' materializzati in MySQL nella tabella:

```text
acdc_rt_parameter_verification_profile
```

Il laboratorio non rientra nel normale ciclo di trading.

Non avvia PAPER.
Non avvia REAL.
Non modifica il runtime ACDC durante la ricerca.
Non sostituisce la validazione PAPER da `/management`.

Scopo:

```text
Identificare profili parametrici promettenti, generali e riproducibili, da proporre al Consiglio come candidati per
successive verifiche causali e solo dopo per eventuale RUN PAPER.
```

## Fonti E Motivazione Scientifica

Fonti vincolanti per formule trading gia' definite nel piano precedente:

- John Bollinger, Bollinger Band rules:
  https://www.bollingerbands.com/bollinger-band-rules
- StockCharts, Bollinger Bands:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-overlays/bollinger-bands
- StockCharts, Percent B:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/b-indicator
- StockCharts, ADX:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/average-directional-index-adx
- StockCharts, ATR:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/average-true-range-atr
- StockCharts, Chandelier Exit:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-overlays/chandelier-exit

Fonti per ricerca parametrica:

- Bergstra, Bengio, "Random Search for Hyper-Parameter Optimization", JMLR 2012:
  https://jmlr.org/papers/v13/bergstra12a.html
  - random search e' baseline piu' efficiente della grid completa quando pochi iperparametri dominano il risultato.
- Snoek, Larochelle, Adams, "Practical Bayesian Optimization of Machine Learning Algorithms", NeurIPS 2012:
  https://papers.nips.cc/paper/4522-practical-bayesian-optimization-of-machine-learning-algorithms
  - Bayesian optimization e' adatta quando ogni valutazione costa e le valutazioni precedenti informano le successive.
- Deb et al., "A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II", IEEE 2002:
  https://sci2s.ugr.es/sites/default/files/files/Teaching/OtherPostGraduateCourses/Metaheuristicas/Deb_NSGAII.pdf
  - NSGA-II e' riferimento per frontiere Pareto multi-obiettivo.
- Li et al., "Hyperband: A Novel Bandit-Based Approach to Hyperparameter Optimization", JMLR 2017:
  https://jmlr.org/papers/v18/16-558.html
  - allocazione progressiva di risorsa e early stop sono corretti quando la valutazione puo' essere fatta su budget
    crescenti.
- Optuna official documentation, multi-objective and constrained optimization:
  https://optuna.readthedocs.io/en/stable/tutorial/20_recipes/002_multi_objective.html
  - molti obiettivi causano molte soluzioni non dominate; quando possibile alcuni obiettivi vanno modellati come
    vincoli.
- Optuna TPESampler documentation:
  https://optuna.readthedocs.io/en/stable/reference/samplers/generated/optuna.samplers.TPESampler.html
  - TPE supporta multi-objective se selezionato esplicitamente; NSGA-II e' default multi-objective.
- White, "A Reality Check for Data Snooping", Econometrica 2000:
  https://www.ssc.wisc.edu/~bhansen/718/White2000.pdf
  - riusare dati per scegliere regole introduce data snooping; serve controllo out-of-sample.
- Bailey, Lopez de Prado, "The Deflated Sharpe Ratio", Journal of Portfolio Management 2014:
  https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2460551
  - selection bias, overfitting e non normalita' dei ritorni devono essere penalizzati.

Decisione del Consiglio:

```text
Usare Optuna constrained multi-objective optimization come motore primario.
Usare TPE/MOTPE come sampler primario per sample efficiency.
Usare NSGA-II come cross-check evolutivo successivo sulle regioni promettenti.
Non usare reti neurali come motore primario di selezione in questa fase.
Non testare brute-force tutti i profili contro tutto lo storico.
```

## Confini Operativi

Input ammessi:

- MySQL operativo;
- `acdc_rt_parameter_verification_profile`;
- `acdc_paper_decision`;
- `acdc_paper_position`;
- tabelle candle/replay/forensics disponibili;
- feature JSON persistite nelle decisioni;
- candele Influx/MySQL solo quando disponibili e non sintetiche.

Output ammessi:

- tabelle di laboratorio offline;
- report markdown/CSV/JSON;
- profili candidati `CANDIDATE_FOR_REPLAY`;
- profili scartati con motivazione;
- nessuna modifica automatica a config runtime `rt.*`;
- nessuna RUN automatica.

Output vietati:

- avvio PAPER;
- avvio REAL;
- modifica diretta di `rt.strategy.enabled`;
- modifica diretta dei parametri runtime ACDC;
- promozione automatica a profilo operativo.

## Package Python Proposto

Root proposta:

```text
acdc/scripts/rt_parameter_search/
```

Entry point proposta:

```text
acdc/scripts/run-rt-parameter-search.sh
```

Moduli:

```text
rt_parameter_search/
  __init__.py
  config.py
  db.py
  schema.py
  extraction.py
  feature_parser.py
  candle_join.py
  replay.py
  metrics.py
  constraints.py
  objective.py
  optuna_runner.py
  nsga2_crosscheck.py
  walk_forward.py
  overfit_risk.py
  candidate_selection.py
  persistence.py
  reports.py
  cli.py
```

## Schema Tabelle Di Laboratorio

Le tabelle qui sotto sono offline e non operative.

### `acdc_rt_parameter_search_trial`

Una riga per ogni trial Optuna valutato.

Colonne minime:

```text
id
study_name
trial_number
profile_hash
profile_id
sampler_name
fold_id
train_start_at
train_end_at
validation_start_at
validation_end_at
constraint_status
objective_net_profit_quote
objective_loss_rate
objective_avg_loss_abs
objective_avg_win
objective_mfe_zero_rate
objective_capture_ratio
trade_count
win_count
loss_count
status
created_at
updated_at
metrics_json
constraints_json
```

### `acdc_rt_parameter_search_candidate`

Una riga per ogni profilo candidato prodotto dal laboratorio.

Colonne minime:

```text
id
profile_hash
profile_id
selection_stage
pareto_rank
robustness_rank
recommended_action
candidate_status
train_metrics_json
validation_metrics_json
holdout_metrics_json
overfit_risk_json
notes
created_at
updated_at
```

Status ammessi:

```text
REJECTED_BAD_PROFILE
REJECTED_INSUFFICIENT_COVERAGE
REJECTED_OVERFIT_RISK
PARETO_CANDIDATE
CANDIDATE_FOR_CAUSAL_REPLAY
CANDIDATE_FOR_COUNCIL_REVIEW
```

## Fase 0 - Configurazione Riproducibile

### Step 0.1 - Caricamento Config

Classe:

```text
RtParameterSearchConfig
```

File:

```text
rt_parameter_search/config.py
```

Parametri in ingresso attesi:

```text
mysql_dsn
profile_table = acdc_rt_parameter_verification_profile
decision_table = acdc_paper_decision
position_table = acdc_paper_position
min_trade_count
min_candle_coverage_ratio
max_synthetic_ratio = 0
train_windows
validation_windows
holdout_windows
sampler = TPE
trial_budget
random_seed
parallel_jobs
```

Output testato:

```text
SearchConfigLoaded
```

Campi obbligatori output:

```text
config_hash
random_seed
trial_budget
window_count
mysql_schema_checked
```

Logica algoritmo:

1. Leggere YAML/JSON/env.
2. Validare che ogni tabella richiesta esista.
3. Validare che `trial_budget > 0`.
4. Validare che le finestre temporali non si sovrappongano in modo causale errato.
5. Calcolare `config_hash` deterministico.

Test obbligatori:

- config incompleta deve fallire;
- finestre sovrapposte train/holdout devono fallire;
- seed uguale deve produrre stesso `config_hash`;
- assenza MySQL deve fallire senza fallback H2.

Base letteratura:

- Snoek et al.: esperimenti costosi richiedono configurazione riproducibile.
- White/Bailey-Lopez de Prado: non separare train/holdout contamina inferenza.

### Step 0.2 - Registro Studio

Classe:

```text
SearchStudyRegistry
```

File:

```text
rt_parameter_search/persistence.py
```

Parametri in ingresso attesi:

```text
RtParameterSearchConfig
study_name
strategy_family = REALTIME_BB_ADX_V1
```

Output testato:

```text
StudyRegistered
```

Campi obbligatori output:

```text
study_name
study_id
config_hash
created_or_reused
```

Logica algoritmo:

1. Creare o riusare uno studio offline.
2. Bloccare riuso se stesso nome ma config hash diverso.
3. Persistire metadati studio.

Test obbligatori:

- stesso nome/stesso hash riusa studio;
- stesso nome/hash diverso fallisce;
- lo studio non modifica tabelle runtime.

## Fase 1 - Estrazione Dati Storici Causali

### Step 1.1 - Repository MySQL

Classe:

```text
MySqlResearchRepository
```

File:

```text
rt_parameter_search/db.py
```

Parametri in ingresso attesi:

```text
mysql_dsn
read_timeout_seconds
fetch_batch_size
```

Output testato:

```text
RepositoryReady
```

Campi obbligatori output:

```text
schema_version
profile_count
decision_count
position_count
```

Logica algoritmo:

1. Aprire connessione read/write solo per tabelle laboratorio.
2. Leggere conteggi base.
3. Verificare `acdc_rt_parameter_verification_profile.total = 6,220,800`.
4. Non usare H2.

Test obbligatori:

- connessione MySQL valida;
- conteggio profili uguale a matrice;
- permessi scrittura limitati a tabelle laboratorio;
- nessuna query su REAL runtime.

### Step 1.2 - Estrazione Decisioni

Classe:

```text
PaperDecisionExtractor
```

File:

```text
rt_parameter_search/extraction.py
```

Parametri in ingresso attesi:

```text
execution_ids
start_at
end_at
phases = ENTRY, EXIT
actions = BUY, SELL, HOLD, REJECT
strategy_family = REALTIME_BB_ADX_V1
```

Output testato:

```text
DecisionFrame
```

Colonne obbligatorie output:

```text
execution_id
symbol
phase
action
reason
decision_at
price
feature_json
policy_json
position_id
```

Logica algoritmo:

1. Leggere `paper_decision` solo nel range richiesto.
2. Conservare BUY/SELL/HOLD/REJECT.
3. Separare ENTRY da EXIT.
4. Ordinare causalmente per `decision_at`.
5. Escludere decisioni senza feature JSON leggibile.

Test obbligatori:

- BUY senza SELL successiva resta censurato, non inventato;
- EXIT prima di ENTRY fallisce;
- decisioni fuori finestra escluse;
- ordinamento stabile per `execution_id/symbol/decision_at`.

Base letteratura:

- White: la stessa evidenza non va usata informalmente piu' volte senza tracciamento del set di regole.
- Bailey-Lopez de Prado: ogni esperimento deve essere contato nel processo di selezione.

### Step 1.3 - Parsing Feature

Classe:

```text
FeatureJsonParser
```

File:

```text
rt_parameter_search/feature_parser.py
```

Parametri in ingresso attesi:

```text
DecisionFrame
required_features
```

Feature richieste:

```text
bb_setup_code
bb_percent_b
bb_upper_breach
bb_lower_breach
bb_bandwidth_delta
adx14
previous_adx14
plus_di14
minus_di14
rsi14
volume_ratio_1m_20m
atr_pct
last_close_return
decision_source_bucket
decision_interval_seconds
decision_synthetic_backfill
ohlc_wilder_indicators
```

Output testato:

```text
ParsedDecisionFrame
```

Colonne obbligatorie output:

```text
all input columns
parsed numeric features
parse_status
missing_feature_count
```

Logica algoritmo:

1. Decodificare JSON.
2. Convertire numerici a decimal/float controllati.
3. Marcare missing feature.
4. Non imputare feature strategiche mancanti.
5. Scartare o marcare record con `decision_synthetic_backfill != 0`.

Test obbligatori:

- JSON malformato produce `parse_status=INVALID`;
- feature mancante non diventa zero silenzioso;
- synthetic backfill marcato;
- `ohlc_wilder_indicators=0` blocca record da training valido.

Base letteratura:

- Le formule ADX/ATR/Bollinger richiedono feature complete; imputare high/low/close strategici violerebbe la base
  Wilder/StockCharts.

### Step 1.4 - Join Candele

Classe:

```text
CandleAvailabilityJoiner
```

File:

```text
rt_parameter_search/candle_join.py
```

Parametri in ingresso attesi:

```text
ParsedDecisionFrame
candle_source_priority = trade_candle_replay, Influx decision bars
required_interval_seconds = 20
required_source_bucket = binance-microbar
synthetic_backfill_allowed = false
```

Output testato:

```text
CandleQualifiedDecisionFrame
```

Colonne obbligatorie output:

```text
candle_available
candle_count
max_gap_seconds
source_bucket
interval_seconds
synthetic_backfill
ohlc_available
```

Logica algoritmo:

1. Collegare ogni decisione alla barra decisionale chiusa corrispondente.
2. Verificare source bucket.
3. Verificare interval.
4. Verificare synthetic false.
5. Verificare OHLC disponibile.

Test obbligatori:

- decisione senza candela viene esclusa dal dataset ottimizzabile;
- interval diverso da 20s viene escluso;
- source diverso da `binance-microbar` viene escluso;
- synthetic true viene escluso.

## Fase 2 - Ricostruzione Episodi Trade

### Step 2.1 - Costruzione Episodi

Classe:

```text
TradeEpisodeBuilder
```

File:

```text
rt_parameter_search/replay.py
```

Parametri in ingresso attesi:

```text
CandleQualifiedDecisionFrame
positions_table
max_episode_hold_seconds
```

Output testato:

```text
TradeEpisodeFrame
```

Colonne obbligatorie output:

```text
episode_id
execution_id
symbol
setup_type
entry_decision_at
entry_price
exit_decision_at
exit_price
exit_reason
net_return
net_profit_quote
max_net_return
min_net_return
mfe
mae
hold_seconds
decision_count
censored
```

Logica algoritmo:

1. Associare BUY a posizione.
2. Associare SELL successiva alla stessa posizione.
3. Calcolare MFE/MAE su candele disponibili tra entry e exit.
4. Marcare episodi incompleti come `censored`.
5. Non usare candele successive alla SELL per metriche di quel trade.

Test obbligatori:

- BUY senza SELL produce `censored=1`;
- MFE non usa dati dopo exit;
- fee e net return coerenti con `acdc_paper_position`;
- episodi duplicati vengono rifiutati.

Base letteratura:

- Evitare leakage futuro e' requisito minimo di backtest causale.

### Step 2.2 - Generazione Controfattuale Parametrica

Classe:

```text
ParameterProfileEvaluator
```

File:

```text
rt_parameter_search/replay.py
```

Parametri in ingresso attesi:

```text
TradeEpisodeFrame
CandidateDecisionFrame
ParameterProfile
```

`ParameterProfile` contiene:

```text
profile_id
profile_hash
static_min_last_close_return
atr_follow_through_multiplier
max_breakout_percent_b
min_range_volume_ratio
min_breakout_volume_ratio
min_breakout_rsi
max_breakout_rsi
rsi_cap_mode
hot_rsi_min_volume_ratio
static_loss_cap_abs
loss_cap_atr_multiplier
loss_cap_mode
```

Output testato:

```text
ProfileEvaluationResult
```

Campi obbligatori output:

```text
profile_hash
accepted_entry_count
rejected_entry_count
simulated_sell_count
metric_bundle
constraint_bundle
```

Logica algoritmo:

1. Rivalutare ogni decisione ENTRY con i parametri del profilo.
2. Accettare BUY solo se setup e condizioni passano.
3. Per ogni BUY accettato simulare SELL setup-specifica sulle candele successive disponibili.
4. Applicare loss cap e profit capture secondo la stessa logica ACDC documentata.
5. Calcolare metriche finali.

Nota critica:

```text
La simulazione non deve usare decisioni PAPER prodotte da una configurazione diversa come se fossero SELL obbligatorie.
Le decisioni storiche servono come sorgente feature/candele; la SELL del profilo va ricalcolata.
```

Test obbligatori:

- se profilo uguale alla configurazione storica, il replay deve approssimare le uscite osservate;
- un profilo con follow-through impossibile deve generare zero BUY;
- un profilo permissivo deve aumentare accepted BUY rispetto a uno piu' restrittivo a parita' di dati;
- SELL non puo' avvenire prima della prima candela post-entry.

## Fase 3 - Metriche Multi-Obiettivo

### Step 3.1 - Calcolo Metriche Base

Classe:

```text
MetricBundleCalculator
```

File:

```text
rt_parameter_search/metrics.py
```

Parametri in ingresso attesi:

```text
ProfileEvaluationResult
TradeEpisodeFrame
```

Output testato:

```text
MetricBundle
```

Metriche obbligatorie:

```text
trade_count
win_count
loss_count
loss_rate
net_profit_quote
avg_net_return
median_net_return
avg_win
avg_loss_abs
p95_loss_abs
max_loss_abs
profit_factor
mfe_zero_count
mfe_zero_rate
mfe_positive_count
avg_mfe
avg_mae
capture_ratio
exit_reason_distribution
```

Logica algoritmo:

1. Aggregare trade accettati.
2. Calcolare win/loss dopo fee.
3. Calcolare downside separato da upside.
4. Separare MFE-zero da MFE-positive.
5. Separare breakout e range.

Test obbligatori:

- zero trade produce metriche nulle e constraint fail;
- profit factor con loss zero usa valore sentinella controllato;
- loss rate = loss_count / trade_count;
- capture ratio = realized net positive / MFE positivo disponibile, con denominatore protetto.

### Step 3.2 - Metriche Per Le Cinque Ricerche Richieste

Classe:

```text
ResearchObjectiveMapper
```

File:

```text
rt_parameter_search/objective.py
```

Parametri in ingresso attesi:

```text
MetricBundle
SearchMode
```

SearchMode ammessi:

```text
EXCLUDE_DAMAGING
MAXIMIZE_TRADE_PROFIT
REDUCE_LOSS_SELLS
MINIMIZE_RESIDUAL_LOSSES
INCREASE_GAINS
PARETO_BALANCED
```

Output testato:

```text
ObjectiveVector
```

Obiettivi per mode:

```text
EXCLUDE_DAMAGING:
  minimize mfe_zero_rate
  minimize loss_rate
  minimize max_loss_abs
  constraint trade_count >= min_trade_count

MAXIMIZE_TRADE_PROFIT:
  maximize net_profit_quote
  maximize avg_net_return
  maximize profit_factor

REDUCE_LOSS_SELLS:
  minimize loss_count
  minimize loss_rate
  minimize mfe_zero_loss_count

MINIMIZE_RESIDUAL_LOSSES:
  minimize avg_loss_abs
  minimize p95_loss_abs
  minimize max_loss_abs

INCREASE_GAINS:
  maximize avg_win
  maximize capture_ratio
  maximize mfe_positive_count

PARETO_BALANCED:
  maximize net_profit_quote
  minimize loss_rate
  minimize avg_loss_abs
  maximize avg_win
  minimize mfe_zero_rate
```

Logica algoritmo:

1. Non sommare automaticamente obiettivi incompatibili.
2. Esporre vettori separati.
3. Convertire massimizzazioni/minimizzazioni nel formato richiesto da Optuna.
4. Applicare vincoli prima della dominanza.

Test obbligatori:

- ogni SearchMode produce direzioni coerenti;
- `PARETO_BALANCED` produce vettore multi-obiettivo;
- un profilo con zero trade fallisce constraints anche se loss zero.

Base letteratura:

- Optuna docs: con molti obiettivi conviene trasformare requisiti minimi in constraints.
- Deb et al.: frontiera Pareto preserva compromessi, non forza uno score arbitrario.

## Fase 4 - Vincoli E Scarti Deterministici

### Step 4.1 - Scarto Semantico Duplicati/Incoerenze

Classe:

```text
SemanticProfileFilter
```

File:

```text
rt_parameter_search/constraints.py
```

Parametri in ingresso attesi:

```text
ParameterProfile
```

Output testato:

```text
SemanticFilterResult
```

Campi obbligatori output:

```text
profile_hash
passed
semantic_key
reject_reasons
ignored_parameters
```

Logica algoritmo:

1. Se `rsi_cap_mode=OFF`, `max_breakout_rsi` e' semanticamente ignorato.
2. Se `loss_cap_mode=STATIC`, `loss_cap_atr_multiplier` e' semanticamente ignorato.
3. Se `max_breakout_percent_b=INF` e `rsi_cap_mode=CONDITIONAL_EXTENSION`, il bypass RSI perde il cap `%B`.
4. Generare `semantic_key` normalizzato.
5. Tenere un solo rappresentante per duplicati semantici prima dell'ottimizzazione, ma conservare mapping ai profili
   originali.

Output testato:

```text
SemanticProfileSet
```

Test obbligatori:

- profili duplicati semanticamente collassano a una chiave;
- profili collassati mantengono lista `profile_hash`;
- nessuna combinazione viene cancellata senza reason.

### Step 4.2 - Vincoli Di Validita' Minima

Classe:

```text
HardConstraintEvaluator
```

File:

```text
rt_parameter_search/constraints.py
```

Parametri in ingresso attesi:

```text
MetricBundle
RtParameterSearchConfig
```

Output testato:

```text
ConstraintBundle
```

Vincoli minimi:

```text
trade_count >= min_trade_count
candle_coverage_ratio >= min_candle_coverage_ratio
synthetic_ratio == 0
ohlc_wilder_ready_ratio == 1
max_loss_abs <= configured_max_loss_abs
mfe_zero_rate <= configured_max_mfe_zero_rate
```

Logica algoritmo:

1. Applicare vincoli prima degli obiettivi.
2. Se fallisce coverage, profilo non e' confrontabile.
3. Se fallisce synthetic/OHLC, profilo non e' scientificamente valido.
4. Se fallisce max loss, profilo e' operativo-dannoso anche se net profit alto.

Test obbligatori:

- trade count sotto soglia fallisce;
- synthetic ratio > 0 fallisce;
- max loss oltre soglia fallisce;
- constraint pass/fail persistito con reason.

Base letteratura:

- Optuna constrained multi-objective: i requisiti minimi devono essere constraints.
- Bailey-Lopez de Prado: selection bias peggiora se si accettano profili su campioni troppo piccoli.

## Fase 5 - Ottimizzazione Optuna Primaria

### Step 5.1 - Sampler TPE Multi-Objective

Classe:

```text
OptunaTpeParetoOptimizer
```

File:

```text
rt_parameter_search/optuna_runner.py
```

Parametri in ingresso attesi:

```text
SemanticProfileSet
ObjectiveFunction
HardConstraintEvaluator
trial_budget
random_seed
directions
```

Output testato:

```text
OptunaStudyResult
```

Campi obbligatori output:

```text
study_name
trial_count
completed_trial_count
failed_trial_count
pareto_trial_count
best_trials
```

Logica algoritmo:

1. Creare studio Optuna con sampler `TPESampler(seed=random_seed)`.
2. Suggerire parametri dai domini dichiarati, non inventare valori continui fuori matrice.
3. Tradurre parametri suggeriti in `profile_hash`/semantic key.
4. Valutare profilo con `ParameterProfileEvaluator`.
5. Restituire vettore obiettivi.
6. Persistire trial e metriche.

Test obbligatori:

- stesso seed produce stessa sequenza iniziale;
- ogni trial mappa a un profilo esistente nella matrice;
- trial fallito non interrompe studio;
- constraints salvati.

Base letteratura:

- Bergstra/Bengio: random/TPE evita grid completa inefficiente.
- Snoek et al.: scegliere prove successive usando evidenza precedente migliora efficienza su valutazioni costose.
- Optuna: TPE supporta multi-objective se selezionato esplicitamente.

### Step 5.2 - Early Stopping Per Budget Progressivo

Classe:

```text
ProgressiveBudgetEvaluator
```

File:

```text
rt_parameter_search/optuna_runner.py
```

Parametri in ingresso attesi:

```text
profile_hash
folds_ordered_by_time
min_resource_folds
max_resource_folds
pruning_policy
```

Output testato:

```text
ProgressiveEvaluationResult
```

Campi obbligatori output:

```text
evaluated_folds
pruned
prune_reason
partial_metrics
final_metrics
```

Logica algoritmo:

1. Valutare prima su una finestra storica ridotta.
2. Se profilo fallisce vincoli hard, prune.
3. Se profilo e' dominato in modo forte e stabile, prune.
4. Se passa, aumentare finestre.
5. Non usare holdout nel pruning.

Test obbligatori:

- holdout mai letto durante pruning;
- profilo dannoso viene fermato presto;
- profilo promettente passa a budget pieno;
- ogni prune ha reason.

Base letteratura:

- Hyperband: allocazione progressiva della risorsa e early stop sono adatti quando valutazioni sono costose.

## Fase 6 - Cross-Check Evolutivo NSGA-II

### Step 6.1 - Regione Promettente

Classe:

```text
PromisingRegionBuilder
```

File:

```text
rt_parameter_search/nsga2_crosscheck.py
```

Parametri in ingresso attesi:

```text
OptunaStudyResult
top_pareto_trials
semantic_profile_set
neighborhood_radius
```

Output testato:

```text
PromisingSearchRegion
```

Campi obbligatori output:

```text
allowed_values_per_parameter
seed_profiles
excluded_values
```

Logica algoritmo:

1. Prendere Pareto trials e candidati vicini.
2. Costruire sottospazio discreto ridotto.
3. Non creare valori fuori dominio.
4. Conservare valori esclusi e motivo.

Test obbligatori:

- sottospazio non vuoto;
- ogni valore appartiene a P1-P12;
- seed profiles inclusi.

### Step 6.2 - NSGA-II Cross-Check

Classe:

```text
Nsga2CrossCheckOptimizer
```

File:

```text
rt_parameter_search/nsga2_crosscheck.py
```

Parametri in ingresso attesi:

```text
PromisingSearchRegion
population_size
generation_count
mutation_probability
crossover_probability
random_seed
```

Output testato:

```text
Nsga2CrossCheckResult
```

Campi obbligatori output:

```text
population_count
generation_count
non_dominated_count
overlap_with_tpe_pareto
new_candidates
```

Logica algoritmo:

1. Inizializzare popolazione con candidati Optuna e campioni random del sottospazio.
2. Mutare solo parametri discreti validi.
3. Valutare con stessa objective function.
4. Confrontare frontiera con quella TPE.
5. Segnalare candidati nuovi solo se robusti nei fold.

Test obbligatori:

- nessun individuo fuori matrice;
- risultati riproducibili con seed;
- overlap Pareto calcolato;
- nuovi candidati hanno metriche complete.

Base letteratura:

- Deb et al.: NSGA-II preserva elitismo e diversita' Pareto.
- Uso qui come cross-check, non come primo motore, per ridurre costo e overfitting esplorativo.

## Fase 7 - Validazione Walk-Forward

### Step 7.1 - Split Temporali

Classe:

```text
WalkForwardSplitter
```

File:

```text
rt_parameter_search/walk_forward.py
```

Parametri in ingresso attesi:

```text
start_at
end_at
train_duration
validation_duration
step_duration
embargo_duration
```

Output testato:

```text
WalkForwardFoldSet
```

Campi obbligatori output:

```text
fold_id
train_start
train_end
validation_start
validation_end
embargo_start
embargo_end
```

Logica algoritmo:

1. Creare fold ordinati temporalmente.
2. Applicare embargo tra train e validation.
3. Tenere holdout finale separato.
4. Non mescolare simboli temporalmente se produce leakage.

Test obbligatori:

- validation sempre dopo train;
- embargo non nullo se configurato;
- holdout non compare nei fold Optuna;
- nessuna sovrapposizione illegittima.

Base letteratura:

- White e Bailey/Lopez de Prado: separazione temporale e controllo selection bias sono obbligatori in trading rules.

### Step 7.2 - Validazione Candidati

Classe:

```text
WalkForwardCandidateValidator
```

File:

```text
rt_parameter_search/walk_forward.py
```

Parametri in ingresso attesi:

```text
candidate_profiles
WalkForwardFoldSet
ObjectiveFunction
HardConstraintEvaluator
```

Output testato:

```text
WalkForwardValidationResult
```

Campi obbligatori output:

```text
profile_hash
fold_metrics
mean_validation_net_profit
median_validation_net_profit
validation_loss_rate
fold_pass_rate
stability_score
```

Logica algoritmo:

1. Valutare ogni candidato su ogni fold validation.
2. Non riottimizzare parametri dentro validation.
3. Calcolare stabilita' tra fold.
4. Bocciare profili che vincono solo in un fold isolato.

Test obbligatori:

- candidato con un solo fold vincente ma molti fold pessimi viene bocciato;
- fold con coverage insufficiente non viene contato come successo;
- metriche aggregate riproducibili.

## Fase 8 - Rischio Overfitting E Data Snooping

### Step 8.1 - Penalizzazione Multiple Testing

Classe:

```text
SelectionBiasRiskAnalyzer
```

File:

```text
rt_parameter_search/overfit_risk.py
```

Parametri in ingresso attesi:

```text
trial_history
candidate_metrics
number_of_effective_trials
return_distribution
```

Output testato:

```text
OverfitRiskBundle
```

Campi obbligatori output:

```text
effective_trial_count
naive_sharpe
deflated_sharpe
selection_bias_flag
data_snooping_warning
```

Logica algoritmo:

1. Stimare numero effettivo di trial valutati.
2. Calcolare metrica tipo Sharpe/return stability se applicabile.
3. Applicare penalizzazione per selection bias.
4. Marcare candidati con rischio overfit alto.

Test obbligatori:

- piu' trial aumentano penalizzazione;
- profilo con pochi trade non ottiene alta confidenza;
- distribuzione non normale viene segnalata.

Base letteratura:

- Bailey/Lopez de Prado: Deflated Sharpe Ratio corregge selection bias e non normalita'.
- White: data snooping puo' rendere casuale il miglior risultato apparente.

### Step 8.2 - Reality Check Operativo

Classe:

```text
RealityCheckReporter
```

File:

```text
rt_parameter_search/overfit_risk.py
```

Parametri in ingresso attesi:

```text
candidate_set
benchmark_profiles
trial_history
```

Benchmark minimi:

```text
NO_TRADE
CURRENT_RUNTIME_CONFIG
RANDOM_VALID_PROFILE
LENIENT_PROFILE
STRICT_PROFILE
```

Output testato:

```text
RealityCheckReport
```

Campi obbligatori output:

```text
candidate_vs_current
candidate_vs_random
candidate_vs_no_trade
selection_bias_notes
```

Logica algoritmo:

1. Confrontare candidato con baseline correnti.
2. Non accettare candidato che batte solo `NO_TRADE` ma perde contro current runtime.
3. Non accettare candidato non superiore a random valid profiles.
4. Produrre warning se la significativita' e' fragile.

Test obbligatori:

- se current runtime batte candidato, candidato non passa;
- se random profiles simili battono candidato, warning alto;
- benchmark mancanti falliscono report.

## Fase 9 - Selezione Candidati

### Step 9.1 - Ranking Pareto Robusto

Classe:

```text
RobustParetoCandidateSelector
```

File:

```text
rt_parameter_search/candidate_selection.py
```

Parametri in ingresso attesi:

```text
OptunaStudyResult
Nsga2CrossCheckResult
WalkForwardValidationResult
OverfitRiskBundle
max_candidates_per_mode
```

Output testato:

```text
CandidateSet
```

Campi obbligatori output:

```text
profile_hash
profile_id
search_mode
pareto_rank
robustness_rank
selection_reason
reject_reason
recommended_next_step
```

Logica algoritmo:

1. Partire da candidati Pareto feasible.
2. Richiedere pass walk-forward.
3. Richiedere overfit risk non alto.
4. Separare candidati per le cinque ricerche richieste.
5. Produrre un set piccolo e testabile, non una singola risposta.

Candidate buckets:

```text
MAX_PROFIT_CANDIDATES
LOW_LOSS_RATE_CANDIDATES
LOW_RESIDUAL_LOSS_CANDIDATES
HIGH_GAIN_CAPTURE_CANDIDATES
BALANCED_CANDIDATES
```

Test obbligatori:

- ogni bucket puo' avere ranking diverso;
- profilo non feasible non entra mai nei candidati;
- profilo con overfit alto viene escluso anche se net profit alto.

### Step 9.2 - Export Per Consiglio

Classe:

```text
CouncilCandidateExporter
```

File:

```text
rt_parameter_search/persistence.py
```

Parametri in ingresso attesi:

```text
CandidateSet
output_table = acdc_rt_parameter_search_candidate
```

Output testato:

```text
CandidateExportResult
```

Campi obbligatori output:

```text
exported_count
rejected_count
candidate_table
report_path
```

Logica algoritmo:

1. Persistire candidati.
2. Non applicarli al runtime.
3. Scrivere motivo selezione.
4. Scrivere motivo scarto.

Test obbligatori:

- nessuna update su `acdc_shared_runtime_config`;
- candidati esportati hanno profile_hash valido;
- report contiene metriche train/validation/holdout.

## Fase 10 - Report Finale

### Step 10.1 - Report Markdown

Classe:

```text
MarkdownSearchReportWriter
```

File:

```text
rt_parameter_search/reports.py
```

Parametri in ingresso attesi:

```text
StudyRegistered
CandidateSet
WalkForwardValidationResult
OverfitRiskBundle
RealityCheckReport
```

Output testato:

```text
MarkdownReport
```

Sezioni obbligatorie:

```text
Executive summary
Dataset coverage
Excluded deterministic profiles
Optuna trial summary
Pareto frontier
Search mode winners
Walk-forward validation
Overfit/data snooping risk
Recommended candidates
Rejected candidates
Next actions
```

Logica algoritmo:

1. Generare report leggibile dal Consiglio.
2. Non nascondere failure modes.
3. Mostrare trade count e coverage prima del profit.
4. Evidenziare se risultato e' fragile.

Test obbligatori:

- report generato anche se zero candidati;
- report segnala `NO_CANDIDATE_FOUND`;
- ogni candidato ha profilo parametri completo.

### Step 10.2 - Report CSV/JSON

Classe:

```text
StructuredSearchReportWriter
```

File:

```text
rt_parameter_search/reports.py
```

Parametri in ingresso attesi:

```text
CandidateSet
MetricBundle list
```

Output testato:

```text
candidate_profiles.csv
candidate_metrics.json
pareto_frontier.csv
```

Logica algoritmo:

1. Esportare candidati in formato macchina.
2. Preservare decimal precision.
3. Includere `profile_hash`.

Test obbligatori:

- CSV ricaricabile;
- JSON valido;
- numero candidati coerente con DB.

## Fase 11 - Gate Prima Di RUN PAPER

### Step 11.1 - Council Gate

Classe:

```text
CouncilRunGateEvaluator
```

File:

```text
rt_parameter_search/candidate_selection.py
```

Parametri in ingresso attesi:

```text
CandidateSet
RealityCheckReport
manual_council_decision
```

Output testato:

```text
RunGateDecision
```

Status ammessi:

```text
NO_RUN_AUTHORIZED
CANDIDATE_REQUIRES_CAUSAL_REPLAY
CANDIDATE_READY_FOR_PAPER_PROPOSAL
```

Logica algoritmo:

1. Se non esiste candidato robusto, nessuna RUN.
2. Se candidato robusto ma non causal replay completo, richiedere replay.
3. Se candidato supera replay, generare proposta PAPER.
4. Non avviare PAPER automaticamente.

Test obbligatori:

- default status = `NO_RUN_AUTHORIZED`;
- nessun candidato con holdout mancante puo' diventare `READY_FOR_PAPER_PROPOSAL`;
- output non modifica runtime.

## Algoritmo Completo

Pseudo-flusso:

```text
config = RtParameterSearchConfig.load()
study = SearchStudyRegistry.register(config)

decisions = PaperDecisionExtractor.extract(config)
parsed = FeatureJsonParser.parse(decisions)
qualified = CandleAvailabilityJoiner.join(parsed)
episodes = TradeEpisodeBuilder.build(qualified)

profiles = MySqlResearchRepository.load_profiles()
semantic_profiles = SemanticProfileFilter.collapse(profiles)

objective = ResearchObjectiveMapper.create(PARETO_BALANCED)
optimizer = OptunaTpeParetoOptimizer(config, objective)
study_result = optimizer.run(semantic_profiles, episodes)

region = PromisingRegionBuilder.from_study(study_result)
nsga2_result = Nsga2CrossCheckOptimizer.run(region)

folds = WalkForwardSplitter.create(config)
validation = WalkForwardCandidateValidator.validate(study_result, nsga2_result, folds)

overfit = SelectionBiasRiskAnalyzer.evaluate(validation)
reality = RealityCheckReporter.compare(validation)

candidates = RobustParetoCandidateSelector.select(validation, overfit, reality)
CouncilCandidateExporter.export(candidates)

MarkdownSearchReportWriter.write(...)
StructuredSearchReportWriter.write(...)

gate = CouncilRunGateEvaluator.evaluate(candidates)
```

## Test End-To-End Obbligatori

### Test E2E-1 - Dataset Vuoto

Input:

```text
no decisions
profiles available
```

Output atteso:

```text
NO_CANDIDATE_FOUND
NO_RUN_AUTHORIZED
```

### Test E2E-2 - Profilo Dannoso

Input:

```text
profilo con loss_rate alto e MFE-zero alto
```

Output atteso:

```text
REJECTED_BAD_PROFILE
```

### Test E2E-3 - Profilo Profittevole Ma Fragile

Input:

```text
profit alto in un fold, pessimo negli altri
```

Output atteso:

```text
REJECTED_OVERFIT_RISK
```

### Test E2E-4 - Profilo Robusto

Input:

```text
profit moderato, loss rate basso, fold pass rate alto
```

Output atteso:

```text
CANDIDATE_FOR_COUNCIL_REVIEW
```

### Test E2E-5 - Nessuna Modifica Runtime

Input:

```text
run completa laboratorio
```

Output atteso:

```text
acdc_shared_runtime_config unchanged
rt.strategy.enabled unchanged
no PAPER execution created
```

## Criteri Di Accettazione Del Documento

Il piano e' implementabile solo se:

1. Ogni classe sopra ha test unitari.
2. Ogni fase ha test di integrazione su MySQL operativo o dump MySQL, non H2 come validazione operativa.
3. Ogni output e' persistito o esportato con `profile_hash`.
4. Ogni candidato conserva metriche per le cinque ricerche distinte.
5. Nessuna fase avvia PAPER.
6. Nessuna fase modifica il runtime.
7. Ogni report dichiara coverage, trial count e rischio overfit.

## Decisione Finale Del Consiglio

```text
Lo strumento piu' adatto e' un laboratorio Python offline con Optuna constrained multi-objective optimization.
Il motore primario e' TPE/MOTPE.
NSGA-II entra solo come cross-check sulle regioni promettenti.
Walk-forward, holdout e penalizzazione selection bias sono obbligatori prima di proporre qualsiasi PAPER RUN.
```
