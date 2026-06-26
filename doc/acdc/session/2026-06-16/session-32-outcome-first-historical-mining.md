# Session 32 - Outcome-First Historical Mining

Data: 2026-06-16.

## Obiettivo

Spostare il training dal comportamento dei trade ACDC ai dati reali Influx.
Il modello deve cercare ingressi potenzialmente profittevoli nel mercato osservato, etichettarli outcome-first e promuovere solo cio' che supera la validazione temporale prevista dal charter.

## Implementazione

- Aggiunta migrazione `V21__outcome_first_historical_mining.sql`.
- Nuove tabelle:
  - `acdc_outcome_training_sample`;
  - `acdc_outcome_signature`.
- Nuovi componenti:
  - `HistoricalOutcomeMiningService`;
  - `OutcomeQualityModelService`;
  - repository outcome sample/signature;
  - DTO `OutcomeMiningReport`.
- Nuovo endpoint:
  - `POST /diagnostics/acdc/outcome/{profileKey}/mine`.
- `InfluxSnapshotService` ora supporta:
  - lista simboli USDC storici;
  - ticks storici da bucket `binance`;
  - snapshot feature al tempo T.
- Ranking:
  - `rank_entry_quality_score` disattivato;
  - `rank_outcome_quality_score` attivato;
  - parity mostra entrambe le feature, ma il ranking operativo usa `outcome_quality_score`.
- Promozione:
  - firme `SYMBOL` prima delle globali quando hanno campione sufficiente;
  - firme `FAMILY` tramite tassonomia DB;
  - firme `GLOBAL` come fallback.

## Regole Applicate

- Nessuna soglia manuale promossa.
- Label economiche nette con fee, slippage e dust.
- Split temporale TRAIN/VALIDATION obbligatorio.
- Firme salvate come `PROMOTED` solo se validation profit rate e ritorno medio netto sono positivi.
- Trade SHADOW/PAPER usati solo come audit/replay, non come training primario.

## Verifiche

- `./mvnw -q test -Dtest=SnapshotRankingServiceTest,RemCurrentConfigurationTest,GuardEvaluatorTest` OK.
- `./mvnw -q test` OK prima della correzione getter bucket; test mirati OK dopo la correzione.
- `./mvnw -q package -DskipTests` OK.
- Docker ACDC rebuilt e riavviato.
- Flyway MySQL applicata a versione `21`.

## Mining Di Verifica

Comando:

```text
POST /diagnostics/acdc/outcome/REM_CURRENT/mine?lookbackHours=6&horizonSeconds=900&sampleEverySeconds=600&symbolLimit=10&maxSamples=80&validationPercent=30
```

Risultato:

- simboli Influx rilevati: `288`;
- scanned points: `80`;
- samples created: `80`;
- GOOD: `25`;
- BAD: `55`;
- NEUTRAL: `0`;
- average max net return: `-0.000621609425709216`;
- average end net return: `-0.002529127504167617`;
- promoted signatures: `0`.

Split DB:

- TRAIN GOOD: `13`;
- TRAIN BAD: `31`;
- VALIDATION GOOD: `12`;
- VALIDATION BAD: `24`.

Tutte le firme globali estratte sono state `REJECTED`.

## Lettura

Il processo ora fa training su dati reali storici Influx, non sui trade della policy corrente.
La prima finestra testata non contiene edge validato: il sistema ha correttamente evitato promozioni.
Per arrivare a profitto bisogna aumentare campioni/finestra e aggiungere mining per simbolo/famiglia, senza saltare la validation temporale.
