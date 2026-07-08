# REALTIME_BB_ADX_NATIVE_F4E954 Cadence Sensitivity Report

Data: 2026-07-08.

## Scopo

Questo report valuta se, nella prima run forward corretta del profilo Melo
`f4e954f0b552ce864535fc2b`, la finestra/cadence decisionale abbia ridotto rumore o abbia solo ritardato segnali
potenzialmente utili.

Fonte dati:

```text
PAPER execution 141
strategy: REALTIME_BB_ADX_NATIVE_F4E954
profile_hash: f4e954f0b552ce864535fc2b
source: MySQL operativo hft.acdc_*
```

Limite metodologico: questa non e' ancora una replay causale parallela 20s vs 60s sugli stessi raw candles. E' una
sensitivity basata sulle decisioni persistite dalla run 141, sul `policyJson` congelato al BUY e sulle EXIT persistite.
La conclusione sulla finestra 1m e' quindi classificata come diagnostica, non definitiva.

## Esito Run 141

Execution 141 e' la prima run utile dopo la correzione MS985 del bug semantico che faceva bloccare il profilo nativo
dal vecchio quality gate.

```text
status = COMPLETED
current_budget = 99.843014361840330000
realized_profit_quote = -0.156985638159670000
open_positions = 0
RT_ENTRY_BLOCKED_DATA_QUALITY = 0
```

Decisioni native:

| Fase | Azione | Reason | Count |
| --- | --- | --- | ---: |
| ENTRY | BUY | RT_NATIVE_RANGE_REENTRY | 3 |
| ENTRY | BUY | RT_NATIVE_BREAKOUT | 2 |
| EXIT | SELL | RT_NATIVE_RANGE_MIDDLE_CAPTURE | 1 |
| EXIT | SELL | RT_NATIVE_BREAKOUT_FALSE_BREAKOUT | 1 |
| EXIT | SELL | RT_NATIVE_TIMEOUT | 3 |

La run esercita davvero il path dedicato: non ci sono piu' reject globali da `RT_ENTRY_BLOCKED_DATA_QUALITY`.

## Trade

| Symbol | Setup | Entry | Exit | Net quote | MFE | Hold | Entry gap |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: |
| CAKEUSDC | Range | RT_NATIVE_RANGE_REENTRY | RT_NATIVE_TIMEOUT | -0.049999999888000000 | 0 | 734s | 1980s |
| GRAMUSDC | Breakout | RT_NATIVE_BREAKOUT | RT_NATIVE_BREAKOUT_FALSE_BREAKOUT | -0.049999999848000000 | 0 | 1049s | 1980s |
| GUNUSDC | Range | RT_NATIVE_RANGE_REENTRY | RT_NATIVE_RANGE_MIDDLE_CAPTURE | +0.015897097623880000 | 0.000635883905013193 | 601s | 1980s |
| LAYERUSDC | Range | RT_NATIVE_RANGE_REENTRY | RT_NATIVE_TIMEOUT | -0.049999999892800000 | 0 | 726s | 1980s |
| FOGOUSDC | Breakout | RT_NATIVE_BREAKOUT | RT_NATIVE_TIMEOUT | -0.022882736154750000 | 0.000169381107491857 | 1210s | 1980s |

Aggregati:

| Setup | Trade | Net quote | Win | Loss | Avg MFE | Max MFE | Protect-armable |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Range | 3 | -0.084102902156920000 | 1 | 2 | 0.000211961301671064 | 0.000635883905013193 | 1 |
| Breakout | 2 | -0.072882736002750000 | 0 | 2 | 0.000084690553745929 | 0.000169381107491857 | 0 |

## Segnale E Finestra

Tutti i BUY hanno `decision_interval_seconds=20` e `decision_max_gap_seconds=1980` nel policyJson. Dopo MS985 il gap e'
diagnostico e non piu' gate strategico, ma resta un indicatore importante: il dato appare discretizzato/squadrato e non
equivale a un flusso 20s denso.

Persistenza del segnale nei 60 secondi precedenti il BUY:

| Symbol | Setup | Pre-entry decisions 60s | Signal/watch nei 60s | Lettura |
| --- | --- | ---: | ---: | --- |
| CAKEUSDC | Range | 1 | 1 | run appena avviata: finestra precedente non informativa |
| GRAMUSDC | Breakout | 1 | 1 | run appena avviata: finestra precedente non informativa |
| GUNUSDC | Range | 1 | 1 | run appena avviata: finestra precedente non informativa |
| LAYERUSDC | Range | 5 | 2 | segnale non singolo-tick, ma non continuo |
| FOGOUSDC | Breakout | 5 | 3 | segnale persistente, ma breakout con MFE insufficiente |

Interpretazione:

- Per i primi tre trade non possiamo stabilire se 60s avrebbe filtrato o ritardato: la run parte quasi nello stesso
  momento dei BUY, quindi manca una finestra precedente completa.
- LAYERUSDC e FOGOUSDC mostrano piu' osservazioni nei 60s precedenti. Quindi il problema non e' solo rumore istantaneo:
  almeno alcuni segnali erano persistenti abbastanza da sopravvivere a una conferma temporale.
- FOGOUSDC e' il caso piu' utile per la domanda cadence: il segnale breakout persiste, ma il MFE massimo e'
  `0.0001693811`, sotto la soglia protect `0.0005`. Una conferma 60s probabilmente non avrebbe salvato il trade; avrebbe
  piu' probabilmente ritardato un segnale gia' debole.

## Trailing / Protect

Il trailing/protect breakout non si e' mai armato.

Regola:

```text
max_net_return >= 0.0005
AND net_return > 0
AND price < native_upper_band
```

Osservazioni:

- GUNUSDC ha superato `0.0005` (`0.0006358839`), ma e' un trade range e ha chiuso correttamente a
  `RT_NATIVE_RANGE_MIDDLE_CAPTURE`.
- FOGOUSDC e' breakout ma si ferma a `0.0001693811`, sotto soglia.
- GRAMUSDC breakout ha MFE `0`.

Conclusione: la run non prova che il trailing sia sbagliato; prova che il profilo breakout non ha ancora prodotto un
caso forward con MFE sufficiente ad armare la protezione.

## Rumore O Ritardo

Verdetto del Consiglio: `MIXED_INCONCLUSIVE`, con inclinazione diversa per setup.

Range:

- Un trade range ha prodotto profitto e quasi compensa tre loss meccaniche da fee/timeout.
- Il range vincente GUNUSDC ha MFE sufficiente e cattura middle band.
- Il segnale range puo' tollerare una conferma piu' lenta meglio del breakout, ma la conferma 60s non va introdotta
  alla cieca: CAKE e LAYER sono rimasti a prezzo piatto e sono usciti a timeout, quindi il problema sembra anche
  densita'/movimento post-entry, non solo rumore di ingresso.

Breakout:

- Nessun breakout ha armato protect.
- GRAMUSDC ha chiuso false breakout.
- FOGOUSDC ha un MFE positivo ma sotto protect e chiude timeout.
- Per breakout il rischio principale sembra ritardo/ingresso su movimento gia' impoverito, non rumore filtrato.

## Decisione Operativa

Non scartare il profilo `f4e954f0b552ce864535fc2b`.

Non passare subito tutto a 1m.

Procedere con due binari:

1. Mantenere il path nativo corrente a 20s per PAPER forward controllate, per accumulare almeno altre finestre con BUY e
   SELL effettive.
2. Implementare un replay diagnostico cadence-specifico 20s vs 60s sugli stessi raw candles/simboli della execution
   141, senza avviare PAPER e senza modificare runtime.

## Test Successivo Richiesto

Report successivo: `native_f4e954_cadence_replay_20s_vs_60s`.

Input minimo:

- simboli: CAKEUSDC, GRAMUSDC, GUNUSDC, LAYERUSDC, FOGOUSDC;
- finestra: da almeno 10 minuti prima del primo BUY a 10 minuti dopo l'ultima SELL della execution 141;
- stesso profilo `f4e954f0b552ce864535fc2b`;
- due resampling causali:
  - 20s, pari alla run;
  - 60s, barre chiuse;
- metriche per ogni segnale:
  - timestamp primo trigger;
  - prezzo ingresso;
  - ritardo 60s rispetto a 20s;
  - MFE perso prima dell'ingresso 60s;
  - MAE dopo ingresso;
  - exit reason simulata;
  - net return simulato;
  - protect armato/non armato.

Soglie decisionali:

- Se 60s elimina i trade timeout/false-breakout senza perdere GUNUSDC-like capture, allora 60s sta riducendo rumore.
- Se 60s entra sugli stessi segnali con prezzo peggiore, MFE minore e protect ancora non armato, allora sta solo
  ritardando.
- Se range migliora e breakout peggiora, valutare profilo ibrido: range con conferma 60s, breakout con trigger 20s e
  filtro di persistenza/volume separato.

## Classificazione

```text
VALID_NATIVE_PATH_EVIDENCE
CADENCE_DIAGNOSTIC_ONLY
MIXED_INCONCLUSIVE_ON_1M_FILTER
NEGATIVE_FINANCIAL_SIGNAL_SMALL_SAMPLE
DO_NOT_PROMOTE
DO_NOT_DISCARD
```

