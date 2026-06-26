# Sessione 14 - Refresh Batch Reporting

Data: 2026-06-11

## Piano 1 - Reporting Finale Non Ambiguo Del Refresh SCALPING_SCOUT

Contesto:

- Durante la PAPER V90/V91 HFT e' emersa un'ambiguita' operativa: `scalping_scout_optimizer_run` poteva mostrare `NO_SYMBOLS`, mentre subito dopo il fallback scout/persist del batch Docbrown applicava candidati.
- La tabella optimizer misura solo lo step optimizer; il batch completo ha due fasi (`optimizer`, `scoutFallback`) e deve dichiarare l'esito finale senza lasciare inferenze manuali.

Interventi:

- `run_real_candidate_refresh_batch.py` ora calcola un outcome finale strutturato per ogni ciclo:
  - `OPTIMIZER_APPLIED`
  - `SCOUT_FALLBACK_APPLIED`
  - `NO_CANDIDATES`
  - `STEP_FAILED`
- Il log finale include sempre `applied`, `appliedPhase`, `reason`, summary optimizer e summary fallback.
- Aggiunto report macchina `/tmp/docbrown-scalping-refresh-batch.json`, configurabile con `DOCBROWN_SCALPING_REFRESH_BATCH_REPORT`.
- I report degli step vengono letti da:
  - `DOCBROWN_SCALPING_OPTIMIZER_REPORT`, default `/tmp/docbrown-scalping-optimizer.json`
  - `DOCBROWN_SCALPING_SCOUT_REPORT`, default `/tmp/docbrown-scalping-scout.json`
- Il significato di `scalping_scout_optimizer_run` resta optimizer-only: il batch report e' la fonte corretta per sapere se un fallback ha applicato candidati.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python/tests/test_scalping_parameter_optimizer.py` OK, 25 test.
- `PYTHONPATH=python python3 -m unittest discover -s python/tests` OK, 219 test.
- Rebuild/recreate `docbrown-real-refresh:latest` OK con `docker compose --env-file ../hft/docker/vpn/.env up -d --build --force-recreate docbrown-real-refresh`.

Verifica runtime:

- Primo ciclo post-restart:
  - optimizer exit `1`, optimizer reason `NO_SYMBOLS`, runId `1062`, probed `288`.
  - fallback scout exit `1`, reason `NO_SYMBOLS`, probed `288`.
  - outcome finale: `NO_CANDIDATES`, `applied=false`, `appliedPhase=null`, reason `OPTIMIZER_NO_SYMBOLS_SCOUT_NO_SYMBOLS`.
- Il file `/tmp/docbrown-scalping-refresh-batch.json` e il log `SCALPING_SCOUT final refresh outcome ...` sono coerenti.

Stato finale:

- `docbrown-real-refresh` running con nuovo codice.
- HFT resta in PAPER; `real_buy_enabled=0`.
- Nessuna REAL RUN avviata.
