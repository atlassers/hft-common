# Session 46 - Model-Derived Advice Validity And PURE_REVERSAL Shadow

Data: 2026-06-16.

## Obiettivo

Correggere la validita' delle consulenze REM ML: non deve essere la frequenza tecnica del job, ma una stima outcome-first dell'intervallo in cui il segnale resta utilizzabile per una BUY.

## Modifiche

- DocBrown `HistoricalOutcomeMiningService` calcola `advice_entry_validity_seconds` simulando delayed-entry sui tick futuri:
  - per ogni possibile ritardo dopo il segnale misura se il max net return resta positivo;
  - include fee, slippage e dust gia' presenti nel labeling;
  - salva la validita' nei feature JSON dei campioni.
- DocBrown `ReversalMlRuleMiningService` usa il quantile basso della distribuzione di delayed-entry per impostare:
  - `advice.validity_seconds`;
  - `advice.validity_source=delayed_entry_outcome_distribution`;
  - `advice_valid_until`.
- `rem.ml.advice.validity.seconds` resta fallback quando il modello non ha validita' derivabile.
- Aggiunta configurazione DB `rem.ml.advice.validity.max.seconds` come cap massimo.
- Aggiunte feature di qualita' dati microbar in DocBrown e ACDC:
  - `reversal_data_coverage_ratio`;
  - `reversal_distinct_price_points`;
  - `reversal_max_gap_seconds`.
- Aggiunte configurazioni DB per validazione STRICT:
  - `rem.ml.data_quality.min.coverage_ratio=0.50`;
  - `rem.ml.data_quality.min.distinct_price_points=3`;
  - `rem.ml.data_quality.max.microbar_gap.seconds=60`.
- In `PURE_REVERSAL` la qualita' dati resta osservazionale; in `STRICT` blocca promozioni con campioni stale/spigolosi.

## Verifiche

- ACDC:
  - `./mvnw -q -Dtest=RemCurrentConfigurationTest,InfluxSnapshotServiceTest test`
  - `./mvnw -q package`
- DocBrown:
  - `./mvnw -q test`
  - `./mvnw -q package`
- Docker ACDC:
  - rebuild immagine `acdc:latest`;
  - restart compose VPN;
  - Flyway MySQL applicato fino a V38.

## Job ML

DocBrown REM job su MySQL/Influx reali:

- samples: `5000`;
- GOOD: `2010`;
- BAD: `2990`;
- promoted rules: `226`;
- advice validity distinte: `42`;
- min validity: `20s`;
- max validity: `785s`.

La validita' non e' piu' fissa a `300s`.

## SHADOW Execution 43

Avvio da hft-fe script wrapper:

- execution: `43`;
- evaluated: `200`;
- opened: `3`;
- stop-buy immediato;
- status finale: `COMPLETED`;
- budget finale: `99.690696855778600000`;
- realized: `-0.309303144221400000`.

Posizioni:

| Symbol | Exit | Net quote | Max net return | Coverage | Distinct prices |
| --- | --- | ---: | ---: | ---: | ---: |
| `CAKEUSDC` | `EXIT_ML_ADVICE_LOSS_CAP` | `-0.085350318048000000` | `0` | `0.16666666666666666` | `4` |
| `COMPUSDC` | `EXIT_ML_ADVICE_LOSS_CAP` | `-0.078140806000000000` | `0` | `0.09467455621301776` | `2` |
| `1INCHUSDC` | `EXIT_ML_ADVICE_LOSS_CAP` | `-0.145812020173400000` | `0` | `0.08875739644970414` | `1` |

## Lettura

La SELL ha reagito correttamente: tutte le posizioni sono uscite per `EXIT_ML_ADVICE_LOSS_CAP`.

La perdita deriva dagli ingressi PURE_REVERSAL su dati microbar di qualita' insufficiente:

- coverage molto sotto `0.50`;
- prezzi distinti troppo bassi;
- nessuna posizione ha prodotto MFE positiva.

Questo conferma la separazione:

- SHADOW/PURE_REVERSAL serve a vedere cosa compra il reversal grezzo;
- PAPER non va avviata con PURE_REVERSAL;
- PAPER deve usare regole rigenerate in `STRICT`, con validita' modello-derived e data-quality gate attivi.

## Aggiornamento - Guardie Qualita' Anche In SHADOW

Decisione successiva:

- il reversal resta valido;
- i trade perdenti di execution `43` sono stati generati da segnali con poco volume, pochi price point e curve spigolose;
- anche SHADOW deve escludere questi ingressi, perche' non sono reversal tradabili ma dati non affidabili;
- il volume basso puo' passare solo se c'e' conferma di aumento volume.

Migrazione `V39__rem_entry_microbar_quality_guards.sql`:

- `entry_reversal_data_coverage_min`
  - feature `reversal_data_coverage_ratio`;
  - soglia DB `rem.ml.data_quality.min.coverage_ratio=0.50`;
  - reject `REVERSAL_DATA_COVERAGE_LOW`.
- `entry_reversal_distinct_price_points_min`
  - feature `reversal_distinct_price_points`;
  - soglia DB `rem.ml.data_quality.min.distinct_price_points=3`;
  - reject `REVERSAL_PRICE_POINTS_LOW`.
- `entry_reversal_microbar_gap_max`
  - feature `reversal_max_gap_seconds`;
  - soglia DB `rem.ml.data_quality.max.microbar_gap.seconds=60`;
  - reject `REVERSAL_MICROBAR_GAP_HIGH`.
- `entry_reversal_volume_confirmation_min`
  - feature `reversal_volume_confirmation`;
  - soglia DB `rem.ml.data_quality.min.volume_confirmation=1.05`;
  - reject `REVERSAL_VOLUME_CONFIRMATION_LOW`.

Queste guardie sono ENTRY infrastrutturali, quindi restano attive anche in `SHADOW/PURE_REVERSAL`.

Verifiche:

- ACDC `./mvnw -q -Dtest=RemCurrentConfigurationTest,GuardEvaluatorTest,InfluxSnapshotServiceTest test` completato;
- DocBrown `./mvnw -q test` completato;
- ACDC `./mvnw -q package` completato;
- DocBrown `./mvnw -q package` completato;
- container ACDC rebuildato e riavviato;
- Flyway MySQL applicato a `v39`.

## SHADOW Post-V39

Execution `44`:

- avviata dopo V39;
- `200` valutati;
- `0` BUY;
- chiusa subito;
- motivo: advice ML precedenti scadute, rejection principale `REVERSAL_ML_RULE_MISSING`.

DocBrown rigenerato con dati reali:

- campioni: `5000`;
- GOOD: `1163`;
- BAD: `3837`;
- regole PURE promosse: `177`;
- advice attive al rilancio: `167`.

Execution `45`:

- avviata via hft-fe;
- stop-buy immediato;
- `200` valutati;
- `3` BUY;
- status finale `COMPLETED`;
- budget finale `99.734541959041605000`;
- realized `-0.265458040958395000`.

Ingressi:

| Symbol | Exit | Net quote | Max net return | Coverage | Distinct prices | Max gap | Volume confirmation |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `PENGUUSDC` | `EXIT_ML_ADVICE_LOSS_CAP` | `-0.137057306164975000` | `0` | `0.5056179775280899` | `32` | `25` | `1.075351814959009` |
| `DOGEUSDC` | `EXIT_ML_ADVICE_TIMEOUT` | `-0.072899713393420000` | `0` | `0.519774011299435` | `30` | `30` | `3.6329885192931104` |
| `LTCUSDC` | `EXIT_ML_ADVICE_TIMEOUT` | `-0.055501021400000000` | `0` | `0.5738636363636364` | `22` | `30` | `1.4598553615905638` |

Reject principali:

- `REVERSAL_DATA_COVERAGE_LOW`: `362`;
- `REVERSAL_ML_RULE_MISSING`: `22`;
- `REVERSAL_VOLUME_CONFIRMATION_LOW`: `7`.

Lettura:

- V39 funziona: gli ingressi spigolosi/radi vengono esclusi anche in SHADOW;
- i 3 BUY rimasti hanno qualita' microbar sufficiente;
- nessuno dei 3 ha prodotto MFE positiva (`max_net_return=0`);
- quindi il prossimo problema e' la qualita' del segnale PURE_REVERSAL, non piu' la qualita' tecnica dei dati;
- PAPER resta da evitare finche' non passiamo a `STRICT` o miglioriamo il ranking/consiglio ML per richiedere probabilita' reale di MFE positiva.

## Aggiornamento - Ranking Outcome-First

La SHADOW `45` ha mostrato che la qualita' dati non basta: i BUY erano tecnicamente puliti ma non hanno prodotto MFE positiva.
Inoltre `LTCUSDC` e' entrato con `reversal_ml_score` negativo, segnale che il ranking precedente consumava budget anche quando il consiglio ML era debole.

Migrazione `V40__rem_entry_advice_ranking.sql`:

- aggiunta guardia `entry_reversal_ml_score_positive`;
- `reversal_ml_score < 0` viene rifiutato con `REVERSAL_ML_SCORE_NEGATIVE`;
- ranking ENTRY sostituito con ranking composito DB-driven:
  - `reversal_ml_score`;
  - `ml_advice_safe_net_return`;
  - `ml_advice_max_net_return`;
  - `reversal_ml_profit_probability`;
  - `reversal_data_coverage_ratio`;
  - `reversal_volume_confirmation`;
  - `reversal_max_gap_seconds` ASC.

Verifiche:

- `./mvnw -q -Dtest=RemCurrentConfigurationTest,GuardEvaluatorTest,AcdcRunServiceTest,SnapshotRankingServiceTest test`;
- `./mvnw -q package`;
- rebuild container ACDC;
- Flyway MySQL applicato a `v40`;
- DB reale conferma ranking e guardia attivi.

Decisione vincolante:

- l'ordinamento BUY deve sempre essere outcome-first;
- la presenza di una advice non basta;
- la BUY deve consumare budget prima sui consigli ML con migliore rendimento netto atteso e migliore qualita' dati;
- advice con score ML negativo non devono entrare nemmeno in SHADOW.
