# Sessione 15 - Stop-Buy PAPER Drain

Data: 2026-06-15.

## Obiettivo

Evitare che una PAPER RUN continui ad aprire nuove BUY quando l'operatore vuole interromperla, permettendo comunque alle posizioni aperte di uscire tramite le guardie SELL.

## Implementazione

- Aggiunta migration `V16__add_paper_stop_buy_flag.sql`.
- Aggiunta colonna `acdc_run_execution.buy_stopped_at`.
- Aggiunto endpoint:
  - `POST /acdc/paper/stop-buy/{profileKey}`.
- Aggiunto `PaperRunService.stopBuy(profileKey)`.
- Modificata la PAPER RUN:
  - se `buy_stopped_at` e' valorizzato e non esiste una posizione aperta sul simbolo, non valuta ENTRY;
  - registra `HOLD` con reason `PAPER_BUY_STOPPED`;
  - se esiste una posizione aperta, continua a valutare EXIT.
- `CounterfactualEntryService` ignora le decisioni `HOLD` ENTRY, quindi `PAPER_BUY_STOPPED` non sporca il dataset counterfactual.

## Verifiche

- `./mvnw -q test`: OK.
- `./mvnw -q package -DskipTests`: OK.
- `docker build -f docker/Dockerfile.jvm -t acdc:latest .`: OK.
- `acdc-vpn` ricreato.
- Flyway MySQL ACDC aggiornato a v16.

## PAPER Execution 13

Stop-buy applicato su execution `13`.

Drain step:

- `opened=0`;
- `closed=1`;
- `PAPER_BUY_STOPPED=8`;
- `reservedBudget` passato a `0`.

Stop finale:

- execution `13` status `STOPPED`;
- `currentBudget=99.414811775016558000`;
- `reservedBudget=0`;
- `realizedProfitQuote=-0.585188224983442000`.

Trade:

- BUY: 12;
- SELL: 12;
- posizioni aperte residue: 0.

Nessuna REAL RUN avviata e nessun ordine Binance inviato.
