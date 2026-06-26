# Session 33 - Family Taxonomy and SHADOW Analysis

Data: 2026-06-16.

## Obiettivo

Generare la tassonomia FAMILY mancante, eseguire una SHADOW run e capire perche' le opportunita' GOOD storiche non stanno producendo profitto operativo.

## Implementazione

- Aggiunta migrazione `V22__symbol_family_taxonomy.sql`.
- Aggiunta tabella `acdc_symbol_family_rule`.
- Aggiunti:
  - `SymbolFamilyRule`;
  - `SymbolFamilyRuleRepository`;
  - `SymbolFamilyService`.
- Tassonomia iniziale DB-driven:
  - `MEME_MICRO`;
  - `MEME`;
  - `AI`;
  - `LAYER1`;
  - `LAYER2`;
  - `DEFI`;
  - `GAMING`;
  - `INFRA`;
  - `FIAT_STABLE`;
  - `UNKNOWN`.
- `HistoricalOutcomeMiningService` ora promuove firme in ordine:
  - `SYMBOL`;
  - `FAMILY`;
  - `GLOBAL`.

## Verifiche

- `./mvnw -q test -Dtest=SnapshotRankingServiceTest,RemCurrentConfigurationTest,GuardEvaluatorTest` OK.
- `./mvnw -q test` OK.
- Docker ACDC rebuilt e riavviato.
- Flyway MySQL a versione `22`.

## Mining

Run:

```text
POST /diagnostics/acdc/outcome/REM_CURRENT/mine?lookbackHours=6&horizonSeconds=900&sampleEverySeconds=600&symbolLimit=30&maxSamples=180&validationPercent=30
```

Risultato:

- campioni: `180`;
- GOOD: `55`;
- BAD: `125`;
- promoted signatures: `1`;
- unica firma promossa: `SYMBOL 2ZUSDC pullback_depth`;
- validation samples della firma: `3`;
- nessuna firma `FAMILY` promossa.

Interpretazione:

- La firma promossa e' statisticamente debole perche' ha campione di validation troppo basso.
- Non e' PAPER-ready.
- La tassonomia funziona, ma serve piu' profondita' campionaria e controllo di stabilita'.

## SHADOW Execution 33

- `executionId=33`;
- `dataSource=INFLUX`;
- `status=COMPLETED`;
- `currentBudget=99.501242473431710000`;
- `realizedProfitQuote=-0.498757526568290000`.

BUY:

- `BNBUSDC`;
- `NIGHTUSDC`;
- `CAKEUSDC`;
- `AVNTUSDC`;
- `CKBUSDC`;
- `BNBUSDC`.

Tutti i BUY avevano:

- `candidate_present=0`;
- `outcome_quality_score < 0`;
- nessuna firma promossa;
- nessun candidato HFT/DocBrown valido.

SELL:

- `EXIT_FEE_RANGE_MAX_HOLD`: `4`;
- `EXIT_ABSOLUTE_LOSS`: `1`;
- `EXIT_QUOTE_LOSS_CAP`: `1`.

## Diagnosi

### Sorgente Dati

- `stan_strategy_parameters` contiene `14` righe USDC attive.
- Righe valide ora: `0`.
- Le righe hanno `valid_until` massimo `2026-06-15 00:10:47.538450`, quindi sono scadute.
- ACDC non ha comprato da DocBrown/HFT; ha comprato dal fallback Influx.

### Criteri Di Selezione

- Le guardie ENTRY sono larghe per SHADOW/exploration.
- Il ranking outcome ordina, ma non blocca.
- Di conseguenza vengono comprati simboli con score negativo solo perche' sono i meno negativi del batch.
- Questo e' utile per esplorare, ma non e' una strategy tradabile.

### GOOD Non Profittevoli

I GOOD del miner indicano: in una finestra futura esiste almeno un max net return positivo.
Non garantiscono che la exit policy corrente catturi quel massimo.

Cause principali:

- label GOOD basata su MFE netto, non su PnL della policy completa;
- exit policy con max-hold/cap puo' uscire dopo che il micro-edge e' gia' sparito;
- ranking usa similarity su campioni aggregati, non una firma validata obbligatoria;
- campione piccolo e forte rischio di multiple testing;
- fallback Influx compra anche senza firma promossa.

## Strategia Scientifica

1. Dataset event-based:
   - campioni da Influx al tempo T;
   - label con triple-barrier netto: profit barrier, loss barrier, time barrier;
   - fee, slippage, dust e min notional gia' inclusi.
2. Walk-forward purged validation:
   - split temporali senza leakage;
   - embargo tra train e validation;
   - promozione solo se stabile su piu' finestre.
3. Meta-labeling:
   - prima label: esiste setup reversal;
   - seconda label: conviene tradarlo con la exit reale.
4. Ranking calibrato:
   - probabilita' calibrata, non solo score relativo;
   - expected value netto = `p_win * avg_win - p_loss * avg_loss - cost`.
5. Controllo multiple testing:
   - min sample per `SYMBOL`;
   - min sample per `FAMILY`;
   - niente promozioni con validation sample troppo basso.
6. Gating scientifico:
   - SHADOW puo' esplorare;
   - PAPER deve richiedere firma validata `SYMBOL -> FAMILY -> GLOBAL`;
   - se nessuna firma valida, nessun BUY in PAPER.
7. Componenti da mantenere:
   - Influx ingestion;
   - outcome miner;
   - taxonomy DB;
   - shadow/paper ledger;
   - DB guard engine;
   - cost-aware exits.
8. Componenti da ridurre/deprecare:
   - `EntryQualityModelService` basato sui trade eseguiti;
   - dipendenza da `stan_strategy_parameters` se non viene rigenerata;
   - fallback Influx come sorgente PAPER;
   - ranking senza gating validato.

## Pulizia Componenti

Rimosso dal runtime:

- `EntryQualityModelService`;
- `EntryQualitySample`;
- `EntryQualitySampleRepository`;
- DTO `EntryQualityDiagnostics`;
- DTO `EntryQualityTrainingReport`;
- endpoint `/diagnostics/acdc/shadow/{executionId}/entry-quality/evaluate`;
- endpoint `/diagnostics/acdc/entry-quality/{profileKey}`;
- arricchimento `entry_quality_*` da `CandidateSnapshotService`;
- esposizione `entry_quality_*` da `/diagnostics/acdc/rem/parity`.

Nota:

- La migrazione V20 resta nello storico Flyway per compatibilita' con DB gia' migrati.
- Il codice runtime non usa piu' il modello addestrato sui trade eseguiti.
