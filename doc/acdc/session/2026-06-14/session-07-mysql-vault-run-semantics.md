# Sessione 07 - MySQL, Vault e Semantica RUN

Data: 2026-06-14.

## Obiettivo

Correggere tre aspetti operativi:

- Telegram deve leggere credenziali da Vault come HFT.
- SHADOW/PAPER operative devono girare su MySQL, non su H2 locale.
- Budget PAPER deve distinguere budget libero, budget riservato e PnL realizzato.

## Vault Telegram

Verifica Vault:

- path: `hft/prod`;
- KV engine: v1;
- `hft.telegram.bot-token`: presente;
- `hft.telegram.chat-id`: presente.

Non e' stato necessario creare nuove chiavi.

Implementazione:

- `TelegramNotifier` ACDC legge direttamente Vault tramite:
  - `QUARKUS_VAULT_URL`;
  - `QUARKUS_VAULT_AUTHENTICATION_CLIENT_TOKEN`;
  - `QUARKUS_VAULT_SECRET_CONFIG_KV_PATH`.
- Fallback mantenuti:
  - MicroProfile config;
  - system property;
  - environment variable.

## MySQL

ACDC e' stato avviato su MySQL schema `hft`, non su H2.

Problema risolto:

- HFT ha gia' Flyway nello stesso schema.
- ACDC ora usa table Flyway separata `acdc_flyway_schema_history`.
- `baseline-version=0` evita che Flyway salti `V1` in uno schema gia' non vuoto.

Migrazioni applicate su MySQL:

- `V1` ... `V6`;
- stato finale: `v6`.

## Semantica RUN

DRY:

- valuta senza aprire/chiudere ledger;
- serve come prova di decision engine/config.

SHADOW:

- legge Influx;
- usa override DB in `acdc_guard_threshold_override`;
- mantiene solo guardie operative minime;
- bypassa le guardie strategiche ENTRY seedate;
- non apre posizioni.

PAPER:

- legge Influx;
- usa soglie operative configurate in `acdc_guard_definition`;
- apre/chiude posizioni simulate;
- non invia ordini Binance.

REAL:

- non implementata/autorizzata in questa sessione;
- dovra' usare budget Binance e soglie validate.

## Budget PAPER

`RunSummary` ora espone:

- `currentBudget`: budget libero;
- `reservedBudget`: capitale impegnato in posizioni aperte;
- `realizedProfitQuote`: PnL chiuso da SELL.

Quindi un `currentBudget` basso non significa che siano state testate solo BUY: significa che una parte del budget e' ancora riservata in posizioni aperte.

## Run MySQL Reale

SHADOW:

- `runId=1`;
- `executionId=1`;
- `source=INFLUX`;
- `evaluated=200`;
- `accepted=200`;
- `rejected=0`;
- `opened=0`;
- `closed=0`.

PAPER:

| Step | executionId | source | evaluated | accepted | rejected | opened | closed | net | current | reserved | realized |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 2 | INFLUX | 303 | 112 | 191 | 9 | 0 | 0 | 2.7472856494825444 | 97.25271435051745 | 0 |
| 2 | 2 | INFLUX | 295 | 100 | 195 | 3 | 2 | -0.131187849326 | 19.55699061496811 | 80.31182153570589 | -0.131187849326 |
| 3 | 2 | INFLUX | 298 | 98 | 200 | 0 | 0 | 0 | 19.55699061496811 | 80.31182153570589 | -0.131187849326 |
| 4 | 2 | INFLUX | 297 | 97 | 200 | 0 | 0 | 0 | 19.55699061496811 | 80.31182153570589 | -0.131187849326 |

Stop PAPER:

- `executionId=2`;
- `current=19.55699061496811`;
- `reserved=80.31182153570589`;
- `realized=-0.131187849326`.

Persistenza MySQL:

- `acdc_shadow_run=1`;
- `acdc_paper_run=4`;
- `acdc_paper_position open=10 closed=2`;
- `acdc_paper_decision=1193`.

Nessuna REAL RUN e nessun ordine Binance.
