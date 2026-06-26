# Kenshiro Operative Plans

Indice delle sessioni operative del microservizio backoffice REST DB-only.

## Regole Operative

- Kenshiro e' boundary REST per il FE e deve interrogare direttamente gli schemi DB corretti.
- Non deve chiamare HFT o Docbrown via HTTP.
- Gli endpoint backoffice aggiunti dopo la creazione FE vivono qui, non nei moduli HFT/Docbrown.
- Aggiornare `/home/mbc/Documenti/ws/java/hft/CURRENT_CONTEXT.md` a ogni sviluppo rilevante.
- Commit e push seguono le regole globali del context root.

## Sessioni Operative

- [Sessione 5 - REM Preservation](session-05-rem-preservation.md)
- [Sessione 4 - Best Winner Backoffice](session-04-best-winner-backoffice.md)
  - [Piano 1 - Endpoint Best Winner DB-Only](session-04-best-winner-backoffice.md#piano-1---endpoint-best-winner-dbonly)
- [Sessione 2 - Pipeline Flow DB Contract](session-02-pipeline-flow-db-contract.md)
  - [Piano 1 - Endpoint Flow Step-By-Step](session-02-pipeline-flow-db-contract.md#piano-1-endpoint-flow-step-by-step)
- [Sessione 1 - Backoffice DB-Only Runtime](session-01-backoffice-db-only-runtime.md)
  - [Piano 1 - Runtime Check Pipeline](session-01-backoffice-db-only-runtime.md#piano-1-runtime-check-pipeline)
  - [Piano 2 - Dockerizzazione Kenshiro](session-01-backoffice-db-only-runtime.md#piano-2-dockerizzazione-kenshiro)
  - [Piano 3 - Porta 8085 E Runtime Docker](session-01-backoffice-db-only-runtime.md#piano-3-porta-8085-e-runtime-docker)
  - [Piano 4 - Dashboard Scoped Per Execution Mode](session-01-backoffice-db-only-runtime.md#piano-4-dashboard-scoped-per-execution-mode)
  - [Piano 5 - Facciata Docbrown DB-Only](session-01-backoffice-db-only-runtime.md#piano-5-facciata-docbrown-db-only)
  - [Piano 6 - Runtime REAL Batch E Config DB](session-01-backoffice-db-only-runtime.md#piano-6-runtime-real-batch-e-config-db)
  - [Piano 7 - Runtime Pipeline DB Config Completa](session-01-backoffice-db-only-runtime.md#piano-7-runtime-pipeline-db-config-completa)
