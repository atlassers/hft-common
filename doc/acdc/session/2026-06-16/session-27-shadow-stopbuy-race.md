# Session 27 - SHADOW Stop-Buy Race

Data: 2026-06-16

## Obiettivo

Eseguire una SHADOW live di test con dati reali Influx e verificare:

- BUY iniziale;
- stop-buy;
- drain automatico SELL;
- assenza di nuovi BUY dopo stop-buy;
- chiusura execution.

## Risultato SHADOW 1

- Execution: `28`.
- BUY iniziali: `HMSTRUSDC`, `EPICUSDC`, `ETHUSDC`.
- Bug trovato: dopo il primo stop-buy il flag `buy_stopped_at` e' stato sovrascritto da un tick schedulato con entity stale.
- Effetto: `MOVEUSDC` aperta durante il drain.
- RUN poi completata con `reservedBudget=0`.
- Realized: `-0.380161054525908800`.

## Fix

- Aggiunto `@DynamicUpdate` a `RunExecution`.
- `RunBudgetService.stopBuy()` ora fa update DB esplicito e idempotente.
- `ShadowRunService` e `PaperRunService` sincronizzano `buyStoppedAt` dal DB a inizio run.
- Prima di aprire una posizione accettata viene ricontrollato il flag stop-buy dal DB.

## Verifica SHADOW 2

- Execution: `29`.
- BUY iniziali: `LAYERUSDC`, `ICPUSDC`, `EPICUSDC`.
- Stop-buy persistito: `2026-06-16 08:18:27`.
- `JTOUSDC` e' stata aperta alle `08:18:24`, quindi prima dello stop-buy effettivo.
- Dopo `buy_stopped_at`, i tick mostrano `opened=0`.
- Decisioni ENTRY dopo stop-buy: `SHADOW_BUY_STOPPED`.
- SELL:
  - `ICPUSDC`: `EXIT_QUOTE_LOSS_CAP`;
  - `JTOUSDC`: `EXIT_QUOTE_LOSS_CAP`;
  - `LAYERUSDC`: `EXIT_ABSOLUTE_LOSS`;
  - `EPICUSDC`: `EXIT_MICRO_PROFIT_TAKE`.
- Stato finale: `COMPLETED`.
- `reservedBudget=0`.
- Realized: `-0.259892282178600000`.

## Verifica tecnica

- `./mvnw -q test` passato.
- `./mvnw -q package -DskipTests` passato.
- Immagine `acdc:latest` ricostruita.
- Container `acdc-vpn` riavviato.
- Nessun `Telegram notifier Notification unsent` nei log degli ultimi minuti.

## Decisione

- La SHADOW dimostra che il processo SELL/drain funziona.
- Le soglie/ingressi non sono ancora PAPER-ready: due SHADOW live hanno chiuso negative.
