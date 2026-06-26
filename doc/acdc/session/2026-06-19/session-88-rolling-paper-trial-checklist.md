# REM Rolling PAPER Trial Checklist

Data: 2026-06-19.

Obiettivo: provare in PAPER i candidati rolling `EDENUSDC` e `BANKUSDC` solo dopo promotion endpoint, misurando se il passaggio rolling -> live BUY resta profittevole.

## Stato Generale

- Stato: `DONE_FAIL_LIVE_DECAY_BAD_ADVICE_WITH_PAPER_CONCURRENCY_FIX_DEPLOYED`.
- Promotion endpoint DocBrown: `DONE`.
- Deploy Docker DocBrown: `DONE`.
- Diagnostica ACDC live advice: `DONE`.
- PAPER endpoint ACDC: `DONE`.
- Forensics post-run DocBrown: `DONE`.
- Fix duplicati/deadlock PAPER: `DONE`.
- Esito scientifico: `FAIL_LIVE_DECAY_BAD_ADVICE`.

## DONE

- [x] Implementato endpoint DocBrown:
  - `POST /docbrown/rem/blank-candidates/{profileKey}/rolling-paper-promotion`;
  - batch rolling come sorgente;
  - simboli espliciti;
  - validita' breve;
  - `maxBuyAgeSeconds`;
  - `entryPriceReference`;
  - `maxEntryDrift`;
  - `promotionClass=PAPER_ELIGIBLE`.
- [x] Build DocBrown senza test H2: `./mvnw -DskipTests package`.
- [x] Commit/push DocBrown: `da38715 Add rolling PAPER promotion endpoint`.
- [x] Deploy DocBrown container.
- [x] Promotion iniziale `EDENUSDC` e `BANKUSDC` con `maxBuyAgeSeconds=20`.
- [x] Verifica diagnostica ACDC live advice.
- [x] Primo PAPER: rifiuto per advice troppo vecchio rispetto al contratto di freschezza.
- [x] Promotion ripetuta con `maxBuyAgeSeconds=60`.
- [x] Secondo PAPER: BUY aperte su `EDENUSDC` e `BANKUSDC`.
- [x] Cicli PAPER successivi per permettere la SELL.
- [x] Forensics DocBrown su `executionIds=1`.
- [x] Classificazione causale completata.
- [x] Fix ACDC per serializzare le run PAPER per profilo dentro una singola transazione.
- [x] Fix ACDC per impedire una seconda posizione PAPER aperta sullo stesso `profileKey+symbol`.
- [x] Build ACDC senza test H2: `./mvnw -DskipTests package`.
- [x] Commit/push ACDC: `ccf51ae Serialize PAPER runs by profile`.
- [x] Deploy Docker ACDC completato.
- [x] Endpoint base ACDC verificato: `GET /acdc/profiles`.

## WIP

- [ ] Correzione modello successivo: live revalidation gate prima della promotion/BUY.

## TODO

- [ ] Non fare nuove prove PAPER sullo stesso modello senza live revalidation gate.
- [ ] Non fare REAL.
- [ ] Implementare controllo che il regime corrente sia ancora quello validato rolling.
- [ ] Implementare controllo che il prezzo corrente non sia gia' decaduto contro l'entry reference oltre la soglia ammessa.
- [ ] Ripetere solo dopo deploy Docker e diagnostica endpoint.
