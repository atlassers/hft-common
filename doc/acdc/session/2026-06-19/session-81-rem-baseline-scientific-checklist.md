# REM Baseline Scientific Checklist

Data: 2026-06-19.

Obiettivo: completare la parte scientifica del super piano con baseline automatiche confrontabili.

## Stato

- [x] Baseline `NO_TRADE` gia' presente nel multi-window report.
- [x] Baseline `RANDOM_TOP_VOLUME` implementata.
- [x] Baseline `NAIVE_REVERSAL` implementata.
- [x] Baseline confrontate con `CURRENT_MODEL`.
- [x] Incertezza multi-finestra calcolata e richiesta dall'acceptance.
- [x] Endpoint verificato via HTTP.
- [x] Verifica finale acceptance eseguita via endpoint.
- [x] Build, commit, push, deploy Docker.

## Criteri

- Le baseline devono usare lo stesso cost model del modello.
- Le baseline devono essere dichiarate nel report, non calcolate a mano.
- Se una baseline non e' calcolabile per assenza dati, il report deve dichiararlo.
- Il PASS scientifico richiede almeno due finestre valutabili per stimare un intervallo di confidenza.

## Log

- 2026-06-19: checklist creata.
- 2026-06-19: baseline automatiche implementate in DocBrown multi-window report; build OK.
- 2026-06-19: endpoint multi-window verificato con baseline `NO_TRADE`, `RANDOM_TOP_VOLUME`, `NAIVE_REVERSAL`; DocBrown commit `053f653`, deploy Docker completato.
- 2026-06-19: aggiunta incertezza multi-finestra con approssimazione normale 95%; acceptance FAIL se l'incertezza e' inconclusiva.
- 2026-06-19: verifica finale endpoint `POST /docbrown/rem/live-advice/scientific/acceptance` eseguita con esito tecnico `PASS` su due finestre recenti valutabili: 10 advice valutate, weighted avg end 15m `0.000045991451420329`, weighted avg MFE `0.007137710358306382`, zero-MFE rate `0.60`. Baseline battute: `NO_TRADE=0`, `RANDOM_TOP_VOLUME=-0.004036198422161987`, `NAIVE_REVERSAL=-0.005023060744421715`. Incertezza 95% ampia: lower `-0.012766009412718455`, upper `0.012857992315559112`; la validazione resta prudenziale.
- 2026-06-19: warning finale: forensics post-run su execution `79` tutta `INCONCLUSIVE_DATA` per dati microbar insufficienti/fallback storico. Non promuovere conclusioni strategiche senza RUN nuova con diagnostics SELL V63 e forensics non inconclusiva.
- 2026-06-19: robust validation post-RUN eseguita su execution `82` dopo aggiunta metriche robuste DocBrown. Esito `status=FAIL`, `strategicStatus=FAIL`; modello sotto `NO_TRADE`, `RANDOM_TOP_VOLUME`, `NAIVE_REVERSAL`, `weightedAvgEnd15mNetReturn=-0.00221575308935471`, zero-MFE `0.50`. Forensics non inconclusiva: 1 `BAD_ADVICE`, 1 `BAD_SELL`, 1 `BAD_TARGET`.
