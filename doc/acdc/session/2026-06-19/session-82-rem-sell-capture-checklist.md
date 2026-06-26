# REM SELL Capture Checklist

Data: 2026-06-19.

Obiettivo: completare la diagnostica SELL/trailing richiesta dal super piano.

## Stato

- [x] Persistenza diagnostica SELL in ACDC.
- [x] Campi minimi: max net reached, capture ratio, target hit, trailing armed, hold/advice age/entry drift.
- [x] Campi estesi: dynamic loss cap, trailing retention/fallout, BUY->MFE, MFE->SELL.
- [x] Endpoint diagnostico legge i campi persistiti.
- [x] Build, commit, push, deploy Docker.
- [x] Verifica endpoint.

## Criteri

- La diagnostica SELL non deve introdurre nuove regole operative.
- Deve solo rendere auditabile se ACDC ha catturato MFE disponibile.
- Deve distinguere BAD_SELL da BAD_ADVICE/LATE_BUY.
- Deve rendere verificabile se la SELL e' avvenuta prima, durante o dopo la perdita dell'MFE disponibile.

## Log

- 2026-06-19: checklist creata.
- 2026-06-19: tabella `acdc_paper_sell_diagnostics`, entity/repository/service ed endpoint `/diagnostics/acdc/paper/sell-capture` implementati; build ACDC OK.
- 2026-06-19: ACDC commit `7d8afc6`, Flyway V62 applicata, endpoint verificato. Le execution storiche precedenti a V62 non hanno righe SELL persistite.
- 2026-06-19: aggiunta Flyway V63 con `max_net_return_at_seconds` sulla posizione e campi SELL `dynamic_loss_cap_net_return`, `trailing_retention`, `trailing_fallout`, `time_to_mfe_seconds`, `time_from_mfe_to_sell_seconds`.
