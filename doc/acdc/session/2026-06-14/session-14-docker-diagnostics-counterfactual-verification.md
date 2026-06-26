# Sessione 14 - Docker, Diagnostics e Verifica Counterfactual

Data: 2026-06-14.

## Obiettivo

Allineare ACDC al modello operativo Docker di HFT e verificare il job counterfactual dal container, non da processo host.

## Implementazione

- Aggiunta cartella `docker/vpn`.
- Aggiunto `docker/Dockerfile.jvm`.
- Aggiunto compose equivalente a HFT:
  - `acdc-proton-vpn`;
  - `acdc-vpn`;
  - `acdc-vpn-curl`;
  - porta default `8091`.
- Il compose usa MySQL, Vault e Influx tramite `host.docker.internal`.
- Il materiale VPN viene riusato di default da HFT:
  - `../../../hft/docker/vpn/proton`;
  - `../../../hft/docker/vpn/wireguard/wg0.conf`.
- Allineato default Influx ACDC a HFT:
  - `INFLUXDB_ORG=dgsoft`.

## Diagnostics

Aggiunta resource:

- `POST /diagnostics/acdc/counterfactual/evaluate/{executionId}?horizonSeconds=600`;
- `GET /diagnostics/acdc/counterfactual/{executionId}?horizonSeconds=600&limit=20`.

La response include:

- execution metadata;
- conteggi per status;
- report aggregato;
- top missed opportunities ordinate per `netMaxReturn`.

Il job counterfactual ora marca `ERROR` per riga se una chiamata Influx fallisce, senza abortire tutta la valutazione.

## Verifiche

- `./mvnw -q test`: OK.
- `./mvnw -q package -DskipTests`: OK.
- `docker build -f docker/Dockerfile.jvm -t acdc:latest .`: OK.
- `docker compose --env-file /home/mbc/Documenti/ws/java/hft/hft/docker/vpn/.env -f docker/vpn/compose.yml up -d acdc-proton-vpn acdc`: OK.
- `acdc-proton-vpn`: healthy.
- `acdc-vpn`: up, profilo `prod`, HTTP `8091`.
- Flyway su MySQL `hft`: schema ACDC a versione `13`.

## Verifica Execution 12

Prima del job:

- `EVALUATED=10`;
- `PENDING=39`.

Dopo:

- `EVALUATED=49`;
- `PENDING=0`;
- `NO_DATA=0`.

Report finale:

| Reason | Decisions | Would profit | Would stop | Avg net max | Missed net |
| --- | ---: | ---: | ---: | ---: | ---: |
| `ENTRY_MOMENTUM5_OUT_OF_BAND` | 28 | 27 | 3 | 0.01358169 | 0.38028731 |
| `ENTRY_MOMENTUM10_OUT_OF_BAND` | 18 | 18 | 2 | 0.01599681 | 0.28794262 |
| `ENTRY_MOMENTUM15_OUT_OF_BAND` | 3 | 3 | 0 | 0.01139454 | 0.03418363 |

Conclusione:

- il problema precedente era operativo/configurativo: processo host fuori Docker con Influx non allineato a HFT;
- dal container ACDC il job legge Influx correttamente;
- la diagnosi sui reject resta forte: le guardie momentum candidate-specific stanno scartando segnali che nei 600 secondi successivi avrebbero raggiunto profitto.

Nessuna REAL RUN avviata e nessun ordine Binance inviato.
