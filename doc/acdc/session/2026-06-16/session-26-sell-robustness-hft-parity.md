# Session 26 - SELL Robustness HFT Parity

Data: 2026-06-16

## Obiettivo

Verificare se il processo di vendita ACDC ha la stessa robustezza operativa di HFT su:

- ciclo batch;
- inclusione posizioni aperte;
- uso Docbrown/ranking/candidati senza perdere SELL;
- check di uscita;
- protezione da dati deteriorati o pre-BUY;
- messaggi Telegram e today net.

## Confronto HFT

- HFT `Stan.checkAndTrade()` gira ogni `1s`.
- La tabella `runtime_scheduler_config` ha portato `stan.check-and-trade` da `30s` a `5s` e poi a `1s`.
- HFT aggiunge sempre `runningOrders` all'universo dei simboli attivi.
- HFT non vende usando solo dati precedenti al BUY.
- HFT mantiene piu' logiche SELL specifiche, incluse trailing, micro profit, stop loss, catastrophic stop, grace, achievement e controlli indicatori.

## Fix ACDC

- `acdc.run.scheduler.interval` default portato a `1s`.
- `ShadowRunService` e `PaperRunService` includono le posizioni aperte mancanti nel batch live tramite `InfluxSnapshotService.latestSnapshotsForSymbols`.
- Il recupero live usa `binance-realtime` con filtro `not exists r.base` e microbar da `binance-microbar`.
- Nessun fallback storico `binance` per BUY/SELL live.
- Aggiunto `EXIT_WAITING_POST_ENTRY_TICK` se il tick non e' successivo al BUY.
- I batch `REQUEST` non fanno recupero Influx, per non confondere test/diagnostica con runtime live.
- Rimossi i falsi `COMPLETED` per step con SELL.
- `stop()` ora invia `COMPLETED` oppure `STOPPED`, mai entrambi.
- `Today net` usa repository SHADOW per RUN SHADOW e repository PAPER per RUN PAPER.

## Stato

- Robustezza operativa SELL recuperata rispetto a HFT.
- Rimane gap sulla ricchezza delle regole HFT non ancora tutte modellate come guardie/funzioni DB.
- Prima di PAPER serve una SHADOW cost-aware misurabile e promozione scientifica delle soglie.

## Verifica

- `./mvnw -q test` passato.
- `./mvnw -q package -DskipTests` passato.
- Immagine Docker `acdc:latest` ricostruita.
- Container `acdc-vpn` riavviato in prod con MySQL, Vault e scheduler.
- `GET /acdc/profiles` risponde correttamente.
- `GET /diagnostics/acdc/rem/readiness` risponde `NOT_READY` per assenza di firme ML/promoted e lifecycle profittevole.
- Nessuna RUN `RUNNING` attiva dopo il deploy.
- Storico SHADOW `executionId=27`: SELL operative confermate su `EXIT_ABSOLUTE_LOSS`, `EXIT_MICRO_PROFIT_TAKE`, `EXIT_FEE_RANGE_MAX_HOLD`.
