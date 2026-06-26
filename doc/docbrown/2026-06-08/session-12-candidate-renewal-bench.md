# Sessione 12 - Candidate Renewal Bench

## Piano 1 - Renew DB-Driven E Cap Max Symbols

Data: 2026-06-08 21:25 CEST.

Obiettivo:

- Automatizzare il rinnovo candidati senza dipendere da run manuali.
- Evitare che Docbrown dorma mentre i candidati sono scaduti o sotto soglia minima.
- Rimuovere il cap hardcoded a 5 simboli nell'optimizer.

Interventi Docbrown:

- `run_real_candidate_refresh_batch.py` ora:
  - disattiva candidati expired se `candidate_deactivate_expired=1`;
  - controlla ogni `candidate_renew_check_interval_seconds`;
  - rinnova se i candidati validi sono sotto `candidate_min_active_symbols`;
  - rinnova se la prossima scadenza e' entro `candidate_renew_margin_minutes`;
  - mantiene `refresh_enabled` come kill-switch DB.
- `run_scalping_parameter_optimizer.py` ora rispetta `max_symbols` da DB, senza cap `min(..., 5)`.
- Aggiunte migration `V21__add_candidate_renewal_runtime_config.sql` e `V22__tune_candidate_bench_runtime_config.sql`.

Verifiche:

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_parameter_optimizer -v` OK.
- Docker image `docbrown-real-refresh:latest` rebuilt.
- Log runtime verificato: `REAL candidate renewal triggered: active valid symbols 2 below DB minimum 5`.

Esito PAPER osservato:

- Prima del fix il ciclo aveva promosso `STOUSDC`, poi loss PAPER `-0.16800949 USDC` per stop loss.
- Dopo il fix Docbrown ha triggerato il renewal sotto soglia minima, ma il ciclo successivo non ha trovato nuovi candidati promuovibili; `active_valid=0`.
- Quindi il cap era un bug reale, ma la disponibilita' candidati resta vincolata dalla severita' dei criteri/scenario mercato.
