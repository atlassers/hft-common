# Zero Reset First Run Table/Field Inventory

Data: 2026-06-27.

## Scope

Questo report registra la prima classificazione dopo reset assoluto dei database `hft` e `back_test`.

Operazioni gia' completate prima della RUN:

- dump full e schema-only creati in `hft-common/doc/db-backups/reset-zero-20260627T180843+0200`;
- tutte le tabelle di `hft` e `back_test` droppate;
- servizi riavviati;
- migration ACDC e DocBrown applicate su schema vuoto;
- runtime config `rem.ml.live_advice.max_buy_age_seconds` ripristinata a `75`;
- ciclo avviato da FE `/management` tramite `AUTO_AB_START`, poi fermato con `AUTO_AB_STOP` dopo il bootstrap.

I dump DB restano locali e non versionati.

## First Run Outcome

La prima RUN da zero non ha raggiunto PAPER.

Stato finale da `/management`:

- `globalStatus = BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE`
- `paperRunning = false`
- `openPositions = 0`
- `activeAdvice = 0`
- `paperEligibleActiveAdvice = 0`
- `paperEligibleContractActiveAdvice = 0`
- `latestResearchStatus = NO_RUN`

Batch rolling osservati:

| Batch | Selected symbols | Candidates | Result |
| --- | --- | ---: | --- |
| `management-rolling-20260627T163327Z` | `SUSDC,JUPUSDC,BTCUSDC` | 1350 | `PROMOTION_NO_ADVICE` |
| `management-rolling-20260627T163525Z` | `ALLOUSDC,XPLUSDC,ETHUSDC` | 1350 | `PROMOTION_NO_ADVICE` |

Totale osservato: 2700 righe in `hft.acdc_rem_observation_candidate`.

Promozione:

- nessuna riga in `hft.acdc_live_ml_advice`;
- nessuna PAPER;
- nessuna SHADOW;
- nessuna posizione aperta;
- motivo operativo: `PROMOTION_NO_ADVICE`;
- causa tecnica osservata: candidati non promotable o scartati da live revalidation contract.

Esempio diagnostico significativo dal primo batch: `SUSDC` e' stato scartato per `reversal_trough_age_seconds=840` fuori range `[375,600]` e `reversal_distance_from_trough=0.012100677637947725` fuori range `[0.001942690626517727,0.006839276990718124]`.

## Table Classification

Questa classificazione e' iniziale. "Candidato a rimozione" significa solo "non dimostrato necessario dal bootstrap da zero"; non autorizza drop senza audit producer/consumer per colonna e almeno una RUN PAPER che attraversi BUY->SELL.

### Core Schema And Config Required

Queste tabelle sono richieste gia' al boot o alla diagnostica management:

| Table | Evidence |
| --- | --- |
| `hft.acdc_flyway_schema_history` | controllo migration ACDC |
| `hft.docbrown_schema_history` | controllo migration DocBrown |
| `hft.acdc_shared_runtime_config` | config management, automation, live-advice freshness |
| `hft.acdc_paper_runtime_config` | budget/rule PAPER runtime |
| `hft.acdc_guard_definition` | guardie operative REM/PAPER/SELL |
| `hft.acdc_guard_threshold_override` | override guardie per run type |
| `hft.acdc_run_mode_config` | configurazione run type |
| `hft.acdc_symbol_universe_config` | universo candidati |
| `hft.acdc_symbol_family_rule` | regole famiglia simboli |
| `hft.acdc_ranking_feature` | ranking feature |
| `hft.real_trading_runtime_config` | config legacy/real refresh richiesta da migration e servizio, REAL resta vietata |
| `hft.scalping_scout_runtime_config` | config richiesta da migration/servizi legacy |
| `hft.scalping_scout_candidate_dataset_config` | config richiesta da migration DocBrown zero-safe |
| `hft.best_winner_window_config` | config richiesta da migration DocBrown zero-safe |

### REM Runtime Required But Not Fully Populated

Queste tabelle fanno parte del percorso REM/Forward A/B o della diagnostica runtime. Alcune sono rimaste vuote solo perche' la prima RUN si e' fermata prima di advice/PAPER.

| Table | First-run state | Role |
| --- | ---: | --- |
| `hft.acdc_rem_observation_candidate` | 2700 rows | output rolling validation e input promotion |
| `hft.acdc_live_ml_advice` | 0 rows | contratto advice PAPER-eligible; vuota per `PROMOTION_NO_ADVICE` |
| `hft.acdc_reversal_ml_rule` | 0 rows | regole ML promosse/abilitate |
| `hft.acdc_run_execution` | 0 rows | execution lifecycle |
| `hft.acdc_paper_run` | 0 rows | run PAPER |
| `hft.acdc_paper_position` | 0 rows | posizioni PAPER |
| `hft.acdc_paper_decision` | 0 rows | decisioni BUY/SELL PAPER |
| `hft.acdc_paper_sell_diagnostics` | 0 rows | diagnostica SELL PAPER |
| `hft.acdc_paper_post_sell_forensics` | 0 rows | forensics post SELL |
| `hft.acdc_pre_buy_watch` | 0 rows | WATCH pre-BUY |
| `hft.acdc_shadow_run` | 0 rows | run SHADOW braccio A/B |
| `hft.acdc_shadow_position` | 0 rows | posizioni SHADOW |
| `hft.acdc_shadow_decision` | 0 rows | decisioni SHADOW |
| `hft.docbrown_rem_research_run` | 0 rows | stato research/mining DocBrown |

### Candidate For Removal Or Legacy Audit

Queste tabelle non sono state usate dal bootstrap operativo appena osservato oppure risultano legacy/backtest. Non vanno droppate ora: serve audit per colonna e verifica dei moduli che ancora le referenziano.

| Table group | Evidence | Required action |
| --- | --- | --- |
| `back_test.*` | tutte le 18 tabelle sono vuote dopo reset; riferimenti statici ancora presenti | distinguere storico backtest ancora utile da legacy removibile |
| `hft.stan_strategy_parameters` | vuota; riferimenti statici presenti | audit legacy strategy |
| `hft.best_winner_signature` | vuota; riferimenti statici presenti | audit DocBrown best-winner legacy |
| `hft.scalping_scout_candidate_dataset_run` | vuota; tabella seed/infrastruttura | verificare se ancora usata da dataset generation |
| `hft.acdc_market_snapshot` | vuota | audit diagnostica/telemetria |
| `hft.acdc_outcome_signature` | vuota | audit outcome model legacy/current |
| `hft.acdc_outcome_training_sample` | vuota | audit training sample producer |
| `hft.acdc_rem_data_quality_band_model` | vuota | audit data-quality model |
| `hft.acdc_strategy_profile` | vuota ma molti riferimenti statici | verificare se sostituita da profile key/config flat o se migration seed mancante |

## Field-Level First Findings

### `hft.acdc_rem_observation_candidate`

Campi presenti:

`id`, `profile_id`, `batch_id`, `split_name`, `symbol`, `observed_at`, `entry_price`, `feature_json`, `horizon_seconds`, `max_net_return`, `min_net_return`, `end_net_return`, `end_5m_net_return`, `end_10m_net_return`, `end_15m_net_return`, `positive_mfe`, `zero_mfe`, `tick_count`, `created_at`.

Conclusione iniziale:

- `batch_id`, `profile_id`, `symbol`, `observed_at`, `entry_price` sono necessari per lifecycle e diagnostica batch;
- `feature_json` e' il contratto feature rolling principale;
- `max_net_return`, `min_net_return`, `end_*`, `positive_mfe`, `zero_mfe`, `tick_count` sono usati per scoring/selection/forensics;
- nessun campo e' candidato a rimozione senza audit producer/consumer dettagliato.

### `hft.acdc_live_ml_advice`

Campi presenti:

`id`, `profile_id`, `symbol`, `rule_id`, `scope_type`, `scope_key`, `rule_key`, `promotion_class`, `source_generation_id`, `advice_source`, `feature_json`, `advice_json`, `score`, `validation_samples`, `validation_profit_rate`, `validation_avg_net_return`, `advice_valid_from`, `advice_valid_until`, `used_at`, `used_execution_id`, `status`, `created_at`.

Conclusione iniziale:

- la tabella e' vuota solo perche' la promozione rolling non ha prodotto advice;
- i campi sono parte del contratto PAPER-eligible e della freshness management;
- non e' candidata a rimozione.

### Config And Guard Tables

Campi osservati nelle tabelle config/guard sono compatibili con boot, management state e gating corrente. In particolare:

- `acdc_shared_runtime_config` contiene le chiavi operative management/autociclo/freshness;
- `acdc_guard_definition` e `acdc_guard_threshold_override` restano il registry DB-driven delle guardie;
- `acdc_paper_runtime_config` resta necessario per budget/min-notional/quote sizing;
- `acdc_symbol_*` e `acdc_ranking_feature` restano necessari per universo e ranking.

## Static Reference Snapshot

Conteggio riferimenti statici grezzi rilevato con `rg` sui repository locali:

| Schema | Table | Rows after reset/bootstrap | Static refs |
| --- | --- | ---: | ---: |
| `back_test` | all 18 tables | 0 each | 1-7 each |
| `hft` | `acdc_rem_observation_candidate` | 2700 actual rows | 3 |
| `hft` | `acdc_live_ml_advice` | 0 | 11 |
| `hft` | `acdc_paper_position` | 0 | 15 |
| `hft` | `acdc_run_execution` | 0 | 17 |
| `hft` | `acdc_strategy_profile` | 0 | 51 |
| `hft` | `acdc_shared_runtime_config` | 66 | 26 |
| `hft` | `acdc_guard_definition` | 25 | 37 |

Il file temporaneo completo dell'inventario grezzo e' stato generato come `/tmp/hft_table_usage.tsv` durante la sessione.

## Council Position

Saggio ascoltatore: il reset e' utile; ora il sistema e' leggibile e non sta consumando residui storici. Non bisogna pero' confondere "vuoto" con "inutile".

Scienziato severo: nessuna tabella va rimossa sulla base di una RUN che non ha attraversato advice, WATCH, BUY e SELL. Serve mapping per colonna: producer, consumer, endpoint, test, migration.

Mediano pragmatico: si puo' iniziare la pulizia classificando subito `back_test.*` e i blocchi legacy come candidati ad audit, ma il percorso REM runtime va preservato finche' una PAPER completa non dimostra il grafo dati effettivo.

Decisione unica: procedere con una seconda fase di audit statico table/column -> producer/consumer prima di ogni drop. La prossima RUN operativa resta finalizzata a generare advice PAPER-eligible; solo dopo BUY->SELL completo si potra' marcare come removibile cio' che non entra mai nel flusso.

## Next Actions

1. Generare una matrice table/column -> producer/consumer distinguendo migration, repository, query native, endpoint diagnostici e UI.
2. Ripetere RUN da `/management` per arrivare almeno a advice PAPER-eligible e, se `ML_READY=true`, a `FORWARD_AB_98`.
3. Dopo una RUN BUY->SELL completa, confrontare tabelle/colonne effettivamente scritte con la matrice statica.
4. Proporre drop solo tramite migration, con backup gia' disponibile e rollback documentato.
