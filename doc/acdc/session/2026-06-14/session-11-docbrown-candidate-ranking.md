# Session 11 - DocBrown Candidate Ranking

Data: 2026-06-14

## Obiettivo

Correggere ACDC per non lavorare piu' su tutto l'universo Influx:

- usare candidati HFT/DocBrown promossi;
- filtrare quote asset `USDC`;
- ordinare i candidati per potenziale profitto;
- aggiungere messaggi Telegram di start/stop execution;
- mantenere ranking e universo come configurazioni generiche DB-driven.

## Scelta Architetturale

Il ranking non e' una guardia e non e' una feature specifica REM.

E' un componente trasversale:

- le guardie decidono pass/fail;
- il ranking ordina i candidati quando il budget/slot non basta per tutti;
- ogni profilo puo' scegliere feature e pesi diversi da DB.

Tabelle aggiunte:

- `acdc_symbol_universe_config`;
- `acdc_ranking_feature`.

## Implementazione

- Aggiunta migration `V11__add_symbol_universe_and_ranking.sql`.
- Aggiunto filtro universo:
  - `allowed_quote_assets_csv=USDC`;
  - `candidate_source=HFT_STAN_ACTIVE`;
  - `max_candidates=200`.
- Aggiunta lettura readonly da `hft.stan_strategy_parameters`.
- Aggiunto parser `params_json` con flatten ricorsivo:
  - supporta condizioni REM annidate come `reversal_event_conditions.momentum5.min`;
  - espone feature candidate con prefisso `candidate_`.
- Aggiunto ranking generico:
  - `live_candidate_score`;
  - `candidate_net_profit`;
  - `candidate_win_rate`;
  - `candidate_trades`;
  - `candidate_min_expected_return`.
- SHADOW e PAPER ora:
  - arricchiscono snapshot live con candidato HFT;
  - filtrano a USDC;
  - ordinano per `ranking_score`.
- Telegram PAPER:
  - `PAPER EXECUTION STARTED`;
  - `PAPER EXECUTION STOPPED`;
  - colonna `acdc_run_execution.start_notified_at`.

## Validazione

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- V11 applicata su MySQL schema `hft`.

### SHADOW execution 10

- Sorgente: Influx + candidati HFT.
- `evaluated=2`.
- `accepted=2`.
- `usdt=0`.
- Ranking:
  - `BABYUSDC`, `ranking_score=5.879527683475422`, `live_candidate_score=4.075131316951873`;
  - `BANANAS31USDC`, `ranking_score=3.4055681544220033`, `live_candidate_score=1.9682419083082783`.

### PAPER execution 9

- Sorgente: Influx + candidati HFT.
- `evaluated=2`.
- `opened=1`.
- `usdt=0`.
- Execution fermata dopo la validazione.
- `start_notified_at` valorizzato.

## Note

- Nessuna REAL RUN avviata.
- Nessun ordine Binance inviato.
- Le guardie candidate-specific dinamiche sono il prossimo punto naturale: ACDC ora espone le soglie candidate nel feature map, ma le guardie ENTRY statiche legacy-seed restano ancora presenti.
