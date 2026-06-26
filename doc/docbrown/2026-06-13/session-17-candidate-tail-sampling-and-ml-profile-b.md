# Session 17 - Candidate tail sampling and ML profile B

Data: 2026-06-13

## Obiettivo

Rendere affidabile il dataset usato dai cicli ML e usare i dati recenti per cercare un profilo scalping PAPER con profitto maggiore delle perdite.

## Fix dataset

Problema trovato:

- il dataset copriva correttamente la finestra massima;
- pero' `max_observations_per_symbol` campionava gli indici partendo dall'inizio;
- la coda recente poteva essere esclusa, producendo training non allineato al regime live.

Fix:

- aggiunta `sample_observation_indexes`;
- il campionamento ora e' uniforme e include sempre l'ultimo indice osservabile;
- aggiunto test dedicato.

Dataset verificati:

- dataset `81`: range fino a `2026-06-13 11:28`, max entry `2026-06-13 11:08`;
- dataset `82`: range fino a `2026-06-13 11:43`, max entry `2026-06-13 11:23`.

## ML tuning

Eseguiti cicli `run_real_safe_profile_optimizer.py`:

- globale 2h dataset `82`: non deployable;
- globale 6h dataset `82`: non deployable;
- segmento top-winner senza `MEGAUSDC`: deployable.

Profilo migliore persistito con optimizer run `1354`:

- all 6h: `37` trade, `24` win, `13` loss, net `+0.12683916`, PF `3.5716`;
- recent 1h/test: `5` trade, `5` win, net `+0.04844214`;
- validation: `9` trade, net `+0.01950147`, PF `2.9054`.

## Verifica

- `PYTHONPATH=python python3 -m unittest python.tests.test_scalping_scout_candidate_dataset python.tests.test_scalping_scout` -> `47` test OK.

## Decisione

Il profilo segmentato e' adatto a PAPER validation, non a REAL. Il profilo globale resta non deployable nel regime corrente.
