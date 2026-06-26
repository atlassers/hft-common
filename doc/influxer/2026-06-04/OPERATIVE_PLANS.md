# Influxer Operative Plans

Indice delle sessioni operative Influxer.

## Regole Operative

- In prod-like resta fuori VPN e scrive dati market su InfluxDB.
- Non cancellare o ricreare bucket operativi durante una REAL RUN con posizioni aperte.
- `clear-bucket-on-startup` deve restare disabilitato in produzione.
- Il context operativo breve unico e' `/home/mbc/Documenti/ws/java/hft/CURRENT_CONTEXT.md`.

## Sessioni Operative

- [Sessione 1 - Bucket Operativo Unico E Repair](session-01-bucket-operativo-unico-e-repair.md)
  - [Piano 1 - Retention 48h E Repair Binance](session-01-bucket-operativo-unico-e-repair.md#piano-1---retention-48h-e-repair-binance)
