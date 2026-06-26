# Session 10 - HFT-Style Exit Guards

Data: 2026-06-14

## Obiettivo

Integrare in ACDC le uscite SELL principali viste in HFT, mantenendo il vincolo DB-driven:

- niente soglie strategiche hardcoded nel servizio;
- ogni uscita deve essere una guardia di configurazione;
- PAPER deve simulare BUY/SELL senza ordini Binance reali;
- validazione su MySQL e Influx reali.

## Implementazione

- Aggiunti operatori generici:
  - `PROFIT_FLOOR_AFTER_ARM_EXIT`;
  - `MICRO_PROFIT_TAKE_EXIT`;
  - `FEE_RANGE_MAX_HOLD_EXIT`.
- Esteso `GuardEvaluator` con primitive genericamente riusabili:
  - profit floor dopo arm;
  - micro profit take dopo holding time;
  - fee-range max-hold exit.
- Aggiunta migration `V10__add_hft_style_exit_guards.sql`.
- Aggiornata configurazione `REM_CURRENT` con sei guardie EXIT attive:
  - profit floor after arm;
  - dynamic trailing;
  - micro profit take;
  - absolute loss;
  - quote loss cap;
  - fee range max hold.
- PAPER exit snapshot ora calcola anche:
  - sell fee stimata;
  - `net_loss_rate`;
  - `hold_seconds`.
- Corretto reporting PAPER:
  - `BUY` viene persistito solo se la posizione viene davvero aperta;
  - se il segnale ENTRY passa ma budget/regole exchange non consentono apertura, la decisione e' `REJECT`;
  - se gli slot di posizioni aperte sono pieni, la decisione e' `HOLD`.

## Validazione

- `./mvnw -q test` OK.
- `./mvnw -q package -DskipTests` OK.
- V10 applicata su MySQL schema `hft` tramite ACDC prod.

### PAPER execution 6

- Sorgente dati: Influx.
- Step eseguiti: 20.
- Risultato:
  - posizioni chiuse: 3;
  - posizioni aperte residue: 3;
  - realized profit quote: `-0.193284228557085`.
- SELL:
  - `1000CHEEMSUSDT` -> `exit_quote_loss_cap`;
  - `0GUSDC` -> `exit_fee_range_max_hold`;
  - `1000CATUSDT` -> `exit_fee_range_max_hold`.
- Execution fermata dopo la validazione.

### PAPER execution 7

- Smoke dopo correzione reporting BUY.
- Risultato:
  - `evaluated=200`;
  - `opened=3`;
  - action `BUY=3`;
  - action `REJECT=197`;
  - nessun BUY fittizio per segnali non apribili.
- Execution fermata dopo la validazione.

## Note

- Nessuna REAL RUN avviata.
- Nessun ordine Binance inviato.
- Le soglie restano modificabili da DB in `acdc_guard_definition`.
- Le perdite osservate sono uscite risk/fee-range coerenti con le guardie attuali; non dimostrano ancora profit capture positivo nella finestra osservata.
