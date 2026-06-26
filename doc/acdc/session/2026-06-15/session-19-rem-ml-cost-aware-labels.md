# Sessione 19 - REM ML Cost-Aware Labels

Data: 2026-06-15.

## Obiettivo

Rendere il modello ML REM piu' certo prima della PAPER RUN:

- fee ingresso/uscita;
- slippage;
- step-size;
- dust post-fee;
- minQty/minNotional;
- budget PAPER reale ACDC.

## Implementazione

- DocBrown `reversal_event_mining.py` ora supporta `ExecutionCostModel`.
- `mine_reversal_events(...)` puo' ricevere un cost model per simbolo.
- `dynamic_exit_outcome(...)` calcola il ritorno netto su quantita' realmente eseguibile:
  - entry price peggiorato da slippage;
  - exit price peggiorato da slippage;
  - quantita' arrotondata a step-size;
  - fee buy conservativa;
  - fee sell;
  - dust non vendibile;
  - minNotional su BUY e SELL.
- Il runner REM legge `exchangeInfo` prima del mining e non produce eventi per simboli senza cost model valido.
- Default `REVERSAL_ASSUMED_QUOTE_BUDGET` portato a `25.00`, coerente con ACDC.
- Il wrapper ACDC `scripts/acdc-run-rem-ml.sh`:
  - passa `--assumed-quote-budget`;
  - scrive report candidati;
  - aggiorna `latest.json` solo se il miner produce firme e termina con successo.

## Verifiche

- `PYTHONPATH=python python3 -m unittest python.tests.test_reversal_event_mining -v`: OK.
- `python3 -m py_compile python/docbrown_ml/reversal_event_mining.py python/docbrown_ml/scripts/run_reversal_event_miner.py`: OK.
- `bash -n scripts/acdc-run-rem-ml.sh`: OK.

## Run Diagnostica

Comando:

```bash
REM_ML_MINUTES=60 REM_ML_MIN_HISTORY_BARS=30 scripts/acdc-run-rem-ml.sh
```

Risultato:

- `symbols=11`;
- `ticks=1054`;
- `events=460`;
- `BAD_REVERSAL=459`, `NEUTRAL_REVERSAL=1`;
- `signatures=0`;
- `promoted=0`;
- cost model:
  - `assumedQuoteBudget=25`;
  - `feeRate=0.001`;
  - `slippageRate=0.001`;
  - `symbolsWithExecutableCostModel=11`.

## Stato

ACDC readiness dopo il nuovo modello:

- `status=NOT_READY`;
- `ML_NO_SIGNATURES`;
- `PARITY_NO_HFT_CANDIDATES`;
- `LIFECYCLE_NO_PROFITABLE_CLOSES`;
- `LIFECYCLE_NO_PROFITABLE_CLOSES`.

La PAPER RUN resta bloccata finche' una run ML cost-aware non produce firme nette validate.
