# REM Run Acceptance Checklist

Data: 2026-06-19.

Obiettivo: rendere ogni RUN REM verificabile prima e dopo l'esecuzione.

## Stato

- [x] Preflight scientifico disponibile.
- [x] Acceptance report pre-RUN disponibile.
- [x] Post-RUN forensic report disponibile.
- [x] Checklist scientifica verificata automaticamente.
- [x] Checklist ingegneristica verificata automaticamente.
- [x] Acceptance vincolata a incertezza multi-finestra non inconclusiva.
- [x] Verifica finale acceptance eseguita via endpoint.
- [x] Build, commit, push, deploy Docker.

## Criteri

- Nessuna PAPER non esplorativa senza preflight PASS.
- Nessuna conclusione strategica senza baseline e holdout.
- Nessuna acceptance PASS se la multi-finestra non contiene almeno due finestre valutabili.
- Nessuna REAL RUN senza autorizzazione esplicita.

## Log

- 2026-06-19: checklist creata.
- 2026-06-19: preflight, multi-window baseline, forensics e live-advice diagnostics disponibili via endpoint. Resta da consolidare un singolo endpoint PASS/FAIL che unisca tutte le checklist.
- 2026-06-19: implementato endpoint unico DocBrown `POST /docbrown/rem/live-advice/scientific/acceptance` per PASS/FAIL su preflight, baseline multi-finestra, forensics opzionale e checklist ingegneristica.
- 2026-06-19: endpoint acceptance verificato via HTTP con esito `PASS`; warning atteso: nessuna execution post-run fornita, quindi forensics post-run non valutata.
- 2026-06-19: acceptance irrigidita: ora richiede incertezza multi-finestra `PASS`; la prossima verifica finale deve usare finestre multiple ed executionId post-RUN quando disponibili.
- 2026-06-19: verifica finale eseguita con execution `79`. Esito endpoint `PASS`, failed requirements vuoti, warning `all post-run forensic trades are INCONCLUSIVE_DATA`. Il PASS certifica pipeline/preflight/baseline/uncertainty/checklist; non certifica ancora promozione strategica perche' manca una RUN nuova con forensics classificabile.
- 2026-06-19: aggiunto `strategicStatus` e rieseguita acceptance post-RUN su execution `82`. Esito `FAIL` tecnico e strategico: modello negativo, sotto tutte le baseline, uncertainty inconclusiva per una sola finestra valutabile, forensics classificabile con cause miste `BAD_ADVICE`/`BAD_SELL`/`BAD_TARGET`.
