# Session 21 - Shadow live exploration ranking

Data: 2026-06-15

## Obiettivo

Preparare ACDC a una SHADOW RUN con filtri abbassati, usando dati reali Influx e universo USDC-only, senza contaminare PAPER/REAL.

## Diagnosi

- Gli override DB SHADOW erano gia' corretti:
  - `entry_price_present`: `ACTIVE`;
  - `entry_snapshot_fresh`: `ACTIVE`, `max_threshold=15`;
  - tutte le altre ENTRY guard: `DISABLED`.
- La SHADOW accettava i simboli, ma quando non trovava candidati HFT il ranking restava piatto a `0`.
- Effetto pratico: la SHADOW era permissiva, ma poco utile per scoprire REM live perche' l'ordine era deterministico/alfa invece che per potenziale segnale.

## Implementazione

- `SnapshotRankingService` mantiene il ranking primario da `acdc_ranking_feature`.
- Se il ranking DB produce `0` e `candidate_present < 1`, calcola un fallback `liveExplorationScore` usando:
  - `momentum5`;
  - `momentum10`;
  - `momentum15`;
  - `trend`;
  - `volume_ratio`;
  - `quote_volume_fast`;
  - `distance_from_low`;
  - `pullback_depth`.
- Il fallback serve la SHADOW esplorativa sui dati Influx quando mancano candidati HFT.
- I simboli con candidato continuano a usare il ranking DB/candidate-specific.

## Test

- `SnapshotRankingServiceTest` copre:
  - ranking da feature DB-configured con candidato presente;
  - fallback live exploration per snapshot senza candidato.
- Comando eseguito:
  - `./mvnw -q test`.

## Verifica runtime

- Ricostruita immagine `acdc:latest`.
- Ricreato container `acdc-vpn` sulla network namespace di `acdc-proton-vpn`.
- SHADOW RUN avviata via endpoint:
  - `POST /backoffice/best-winner/actions/start-run`;
  - request `{"executionMode":"SHADOW"}`.
- Risultato:
  - `executionId=20`;
  - `shadowRunId=7`;
  - `dataSource=INFLUX`;
  - `evaluated=200`;
  - `accepted=200`;
  - `rejected=0`;
  - `currentBudget=100`.
- Quote asset:
  - `USDC rows=200`;
  - `USDT rows=0`.
- Prime posizioni ranking post-fallback:
  - `ENSOUSDC ranking_score=2.5096809424753914`;
  - `ALLOUSDC ranking_score=2.4883045905023935`;
  - `METUSDC ranking_score=2.1703966304761906`;
  - `COWUSDC ranking_score=2.0755519279117793`;
  - `CFXUSDC ranking_score=1.9064466542089977`.

## Stato

- SHADOW con filtri abbassati pronta per esplorazione live.
- PAPER resta bloccata dal piano strategico finche' readiness ML cost-aware non torna valida.
- Nessuna REAL avviata.

## Run successiva richiesta

- SHADOW RUN rieseguita via endpoint ACDC/FE.
- Risultato:
  - `executionId=21`;
  - `shadowRunId=8`;
  - `dataSource=INFLUX`;
  - `evaluated=200`;
  - `accepted=200`;
  - `rejected=0`;
  - `currentBudget=100`.
- Quote asset:
  - `USDC rows=200`;
  - `USDT rows=0`.
- Prime posizioni ranking:
  - `ENAUSDC ranking_score=2.3934011887797126`;
  - `CFXUSDC ranking_score=2.369013551239274`;
  - `ENSOUSDC ranking_score=2.071765787022423`;
  - `ICPUSDC ranking_score=2.056037602393255`;
  - `ETHUSDC ranking_score=2.016354655134515`.
