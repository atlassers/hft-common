# Sessione 8 - ACDC Runtime Actions

Data: 2026-06-15.

## Obiettivo

Da ora ogni RUN deve partire da `hft-fe`, ma l'esecuzione runtime deve usare ACDC invece degli endpoint HFT/Kenshiro legacy.

## Modifiche

- Aggiunto `src/lib/server/acdcProxy.ts`.
- La route `src/routes/backoffice/best-winner/actions/[action]/+server.ts` ora inoltra ad ACDC.
- Il selettore run supporta `DRY`, `SHADOW`, `PAPER`, `REAL`.
- Aggiunte variabili `.env.example`:
  - `PUBLIC_ACDC_API_BASE`;
  - `VITE_ACDC_API_BASE`.
- Aggiornato `backend-endpoints.md` con la nuova ownership delle runtime action.

## Semantica

- Il browser e gli script ACDC continuano a chiamare `hft-fe`.
- `hft-fe` chiama ACDC su `POST /backoffice/best-winner/actions/{action}`.
- REAL resta bloccata lato ACDC finche' non viene implementata e autorizzata.
