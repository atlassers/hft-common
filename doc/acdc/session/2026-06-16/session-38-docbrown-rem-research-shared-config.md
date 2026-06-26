# Session 38 - DocBrown REM Research e Shared Config

Data: 2026-06-16.

## Obiettivo

Separare ricerca REM e trading runtime:

- DocBrown produce regole/firme REM validate su outcome netto;
- ACDC consuma solo regole/configurazioni promosse da DB;
- ML e trading condividono la stessa configurazione operativa, inclusa ampiezza microbar.

## Implementazione

- Aggiunta tabella `acdc_shared_runtime_config`.
- Seed principali:
  - `market.influx.bucket=binance`;
  - `market.influx.realtime_bucket=binance-realtime`;
  - `market.influx.microbar_bucket=binance-microbar`;
  - `market.microbar.seconds=5`;
  - `rem.feature.window.minutes=15`;
  - `rem.ml.lookback.hours=12`;
  - `rem.ml.horizon.seconds=900`;
  - `rem.ml.sample.every.seconds=60`;
  - `rem.ml.symbol.limit=288`;
  - `rem.ml.max.samples=5000`;
  - `rem.ml.validation.percent=30`;
  - `rem.ml.min.validation.samples=12`;
  - `rem.ml.min.validation.profit_rate=0.58`.
- ACDC legge la shared config in `InfluxSnapshotService`, `HistoricalOutcomeMiningService`, `ReversalMlRuleMiningService`.
- Mining ACDC disabilitato di default: gli endpoint diagnostici mining rispondono `409` salvo flag esplicito.
- `scripts/acdc-run-rem-ml.sh` ora invoca DocBrown.

## DocBrown Research Job

- Aggiunto job Java REM in DocBrown.
- Endpoint manuale:
  - `POST /docbrown/rem/research/{profileKey}/run`.
- Scheduler:
  - dipendenza `quarkus-scheduler`;
  - `docbrown.rem.scheduler.enabled=false` di default;
  - attivabile esplicitamente da configurazione.
- Rimosso il vecchio miner Python REM superseded dal job Java.

## Verifica

- Test ACDC: `./mvnw -q test` completato.
- Test DocBrown: `./mvnw -q test` completato.
- Endpoint mining ACDC verificato:
  - risposta `409`;
  - messaggio: mining spostato su DocBrown.
- Job DocBrown manuale su `REM_CURRENT`:
  - `scannedPoints=5000`;
  - `good=1863`;
  - `bad=3136`;
  - `trainSamples=3470`;
  - `validationSamples=1530`;
  - `evaluatedRules=310`;
  - `promotedRules=6`.
- Parity ACDC dopo DocBrown:
  - `dataSource=INFLUX`;
  - candidate accettate leggendo le regole promosse.

## SHADOW

- Avvio da script ACDC: `scripts/acdc-start-shadow-run.sh`.
- Execution: `35`.
- Stop-buy applicato e drain completato.
- Stato finale:
  - `COMPLETED`;
  - `initialBudget=100.000000000000000000`;
  - `currentBudget=99.918273412662961000`;
  - `reservedBudget=0`;
  - `realizedProfitQuote=-0.081726587337039000`;
  - `4` posizioni chiuse su `4`.
- Trade:
  - `BANANAS31USDC` positiva;
  - `AUSDC`, `ACTUSDC`, `LAYERUSDC` flat sul prezzo ma negative per fee.

## Esito

Il flusso scientifico minimo e' attivo:

1. DocBrown legge dati reali Influx/microbar.
2. DocBrown valida outcome netto e promuove regole.
3. ACDC consuma le regole promosse.
4. ACDC esegue SHADOW con BUY/SELL e ledger completo.

Il risultato economico della SHADOW non e' ancora sufficiente: il prossimo tuning deve migliorare la promozione delle regole su outcome netto e movimento post-reversal, non tornare a soglie entry manuali.
