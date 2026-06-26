# Session 29 - Telegram VPN e SHADOW 5 Minuti

Data: 2026-06-16

## Obiettivo

- Fixare Telegram su ACDC.
- Avviare una SHADOW che accetti BUY per 5 minuti.
- Verificare che dopo lo stop-buy la run dreni tutte le posizioni senza nuovi acquisti.

## Telegram

- Il notifier ACDC usava gia' le credenziali da Vault/env compatibili con HFT.
- Il problema osservato non era di credenziali:
  - ACDC loggava `UnknownHostException: api.telegram.org` e `Connect timed out`;
  - HFT, nello stesso momento, raggiungeva `api.telegram.org` con `HTTP/2 302`.
- ACDC e HFT usavano lo stesso file WireGuard:
  - `acdc/docker/vpn/wireguard/wg0.conf`;
  - `hft/docker/vpn/wireguard/wg0.conf`;
  - stesso hash `182fbf0a8596e0282163fa751fb6e9957abc9fd2a19973eedceeac459e6b8a1a`.
- Fermando `hft-vpn` e `proton-vpn`, ACDC ha raggiunto immediatamente Telegram.

## Fix Applicato

- `TelegramNotifier`:
  - retry aumentati da 3 a 12;
  - delay retry aumentato da 2s a 5s;
  - cache DNS negativa JVM disabilitata;
  - target Telegram ricaricato a ogni tentativo;
  - log esplicito a ogni retry fallito.
- Vincolo operativo confermato:
  - HFT e ACDC non devono usare simultaneamente la stessa identita' WireGuard;
  - per esecuzione concorrente serve una seconda configurazione VPN dedicata ad ACDC.

## SHADOW 5 Minuti

- Run: `executionId=32`.
- Avvio: `2026-06-16 08:48:24 UTC`.
- Stop-buy: `2026-06-16 08:53:34 UTC`.
- Completamento: `2026-06-16 08:56:00 UTC`.
- Status: `COMPLETED`.
- Budget:
  - iniziale `100.000000000000000000`;
  - finale `99.639385699441910000`;
  - reserved finale `0`;
  - realized `-0.360614300558090000`.

## Posizioni

- Chiuse: 13.
- Aperte dopo stop-buy: 0.
- BUY nella finestra:
  - `BABYUSDC`;
  - `MEGAUSDC`;
  - `JUPUSDC`;
  - `MITOUSDC`;
  - `PENDLEUSDC`;
  - `JUPUSDC`;
  - `BABYUSDC`;
  - `FETUSDC`;
  - `EIGENUSDC`;
  - `BIOUSDC`;
  - `DASHUSDC`;
  - `RAREUSDC`;
  - `NOMUSDC`.
- SELL finali di drain:
  - `RAREUSDC` con `EXIT_FEE_RANGE_MAX_HOLD`;
  - `NOMUSDC` con `EXIT_FEE_RANGE_MAX_HOLD`.

## Esito

- SHADOW completata correttamente.
- Stop-buy rispettato.
- Drain automatico completato.
- Nessun errore Telegram nei log della SHADOW dopo aver lasciato ad ACDC il tunnel VPN.
