# Session 10 - DB-driven SCALPING_SCOUT run controls

Data: 2026-06-08

## Obiettivo

Allineare Docbrown alla regola DB-driven: mode/stage/persist/apply/artifact del percorso operativo non devono piu' arrivare da CLI/env.

## Modifiche

- `run_scalping_scout.py` legge `execution_mode`, `pipeline_stage`, `refresh_enabled`, `paper_gate_decision_file` da `hft.real_trading_runtime_config`.
- `run_scalping_scout.py` persiste solo se `refresh_enabled=true`; gli argomenti legacy `--persist`, `--dry-run`, `--pipeline-stage`, `--execution-mode`, `--artifact` restano accettati ma nascosti e non guidano la RUN.
- Artifact PAPER gate scritto sul path DB `paper_gate_decision_file`, non da argomento CLI.
- `run_scalping_parameter_optimizer.py` legge mode/stage/apply da `real_trading_runtime_config`; applica/persiste solo se `refresh_enabled=true`.
- `run_real_candidate_refresh_batch.py` non passa piu' mode/stage/persist/apply/artifact a scout/optimizer. Il batch continua a generare il file config tecnico per connessioni/finestre, ma i controlli RUN sono DB-backed.

## Verifiche

- `python3 -m py_compile` OK su:
  - `run_scalping_scout.py`
  - `run_scalping_parameter_optimizer.py`
  - `run_real_candidate_refresh_batch.py`
- `pytest` non disponibile nell'ambiente (`python3 -m pytest`: modulo mancante).
- `./mvnw -q -DskipTests package` OK.
- `docker build -f Dockerfile.batch -t docbrown-batch:latest .` OK.

## Stato finale

- Immagine `docbrown-batch:latest` aggiornata.
- `refresh_enabled=0` nel DB operativo, quindi Docbrown non puo' promuovere/applicare candidati finche' non viene riabilitato via DB.
- Nessuna REAL RUN avviata.
