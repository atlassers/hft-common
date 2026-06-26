# Session 145 - Telegram SELL Chart Link Checklist

Data: 2026-06-21.

## Obiettivo

Correggere il messaggio Telegram di SELL: la riga `Chart: Grafana` non deve apparire come testo non cliccabile o ambiguo.

## Decisione Del Consiglio

Saggio ascoltatore:

Il messaggio SELL deve restare leggibile anche se il client Telegram non renderizza correttamente l'anchor HTML.

Scienziato severo:

Una label `Grafana` senza URL visibile e' evidenza operativa incompleta: in forensics live l'operatore deve poter aprire o copiare il chart.

Mediano pragmatico:

Non serve cambiare il flusso trading. Basta rendere il link esplicito e verificabile prima della prossima RUN.

Decisione unica:

La riga SELL deve contenere sia un anchor HTML sia l'URL esplicito in chiaro.

## Checklist

- [x] Identificare il generatore del link: `TelegramTradeChartLinkService`.
- [x] Verificare che PAPER e SHADOW usino lo stesso servizio.
- [x] Modificare la riga chart in modo che non sia solo `Chart: Grafana`.
- [x] Build ACDC.
- [x] Deploy container ACDC.
- [x] Verificare endpoint/container post-deploy.
- [ ] Aggiornare handoff.

## Stato

`SELL_CHART_LINK_FIX_DEPLOYED`.
