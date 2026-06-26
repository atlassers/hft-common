# Sessione 05 - Telegram PAPER Notifications

Data: 2026-06-14.

## Obiettivo

Aggiungere ad ACDC le notifiche Telegram come su HFT, mantenendo gli stessi messaggi operativi per BUY, SELL e completamento execution PAPER.

## Riferimenti HFT Usati

- `external/telegram/TelegramNotifier.java`;
- `xchange/actions/BuyAction.java`;
- `xchange/actions/SellAction.java`;
- `service/PaperTradingCompletionNotifier.java`;
- `service/TelegramRuntimeSummaryService.java`;
- migrazioni HFT con costanti Telegram runtime.

## Implementazione

- Aggiunta dipendenza `java-telegram-bot-api` versione `6.5.0`.
- Aggiunto package `external.telegram`.
- Aggiunto `TelegramNotifier` ACDC:
  - legge `hft.telegram.bot-token`;
  - legge `hft.telegram.chat-id`;
  - fallback su `HFT_TELEGRAM_BOT_TOKEN`;
  - fallback su `HFT_TELEGRAM_CHAT_ID`;
  - se mancano credenziali, disabilita Telegram senza bloccare la pipeline.
- Aggiunto `TelegramRuntimeSummaryService` ACDC:
  - budget corrente;
  - today net PAPER dal ledger ACDC.
- Aggiunto `PaperTelegramNotificationService`:
  - messaggio BUY in formato HFT;
  - messaggio SELL in formato HFT;
  - messaggio PAPER EXECUTION COMPLETED in formato HFT.
- Aggiunta migrazione `V5__add_telegram_completion_state.sql`.
- Aggiunto `completion_notified_at` su `acdc_run_execution`.
- `PaperRunService` ora notifica:
  - dopo BUY PAPER effettivo;
  - dopo SELL PAPER effettivo;
  - quando una execution non ha piu' posizioni aperte.

## Vincoli

- SHADOW e DRY non inviano Telegram.
- PAPER non invia ordini Binance.
- La logica non introduce guardie o soglie strategiche nel codice.
- Strategy e paramId del messaggio sono mappati su:
  - `profileKey`;
  - `acdc_paper_runtime_config.config_key`.

## Verifica

Comando eseguito:

```bash
./mvnw -q test
```

Esito: OK.

Durante i test, senza credenziali Telegram, ACDC ha loggato una sola volta il notifier disabilitato, senza interrompere le run.
