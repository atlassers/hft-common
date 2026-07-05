# Realtime BB ADX Parameter Verification Plan

Data: 2026-07-05.

## Stato Del Documento

Questo documento e' il piano tecnico per correggere le lacune non pienamente allineate alla letteratura nel profilo
`REALTIME_BB_ADX_V1` e per preparare una verifica sistematica dei parametri.

Questo documento non modifica codice e non autorizza nuove RUN. Serve come traccia operativa vincolante per lo sviluppo
successivo.

Decisione richiesta dal Consiglio:

```text
Non reintrodurre il concetto di A/B come approccio primario.
Preparare una matrice di verifica parametrica completa.
Solo in una fase successiva il Consiglio potra' decidere se filtrare la matrice con esclusione manuale,
algoritmi genetici, ML o reti neurali.
```

La matrice descritta qui sotto e' una matrice fattoriale completa sui parametri dichiarati. Alcune combinazioni saranno
probabilmente non sensate, ma non vengono escluse in questo documento: vengono marcate per revisione successiva.

## Evidenza Che Motiva Il Piano

RUN considerate:

```text
RUN 135: 1 trade, net -0.049999999957600000, MFE positivo perso, SELL breakout incompleta.
RUN 136: 1 trade, net -0.097301136264000000, range reentry su barra povera di volume, MFE zero.
RUN 137: 2 trade, 2 win, net +0.170766512778600000, SELL breakout profit-protect valida su campione minimo.
RUN 138: 37 trade, 10 win, 27 loss, net -1.209233198249985840.
```

Diagnosi RUN `138`:

```text
positions = 37
wins = 10
losses = 27
net_profit_quote = -1.209233198249985840
MFE_ZERO trades = 27, net = -2.180021210910175840
MFE_POS trades = 10, net = +0.970788012660190000
```

Conclusione del Consiglio:

- il problema dominante non e' la SELL tardiva sui trade vincenti;
- la SELL `RT_EXIT_BREAKOUT_UPPER_BAND_PROFIT` cattura profitto quando il trade genera MFE;
- il problema dominante e' che troppi BUY breakout sono head fake e non generano mai MFE positivo;
- quindi lo sviluppo successivo deve agire prima sulla qualita' del segnale BUY e sulla fedelta' degli indicatori
  ADX/DMI/ATR, poi sulla ricerca parametrica.

## Fonti Tecniche Vincolanti

Le fonti qui sotto sono usate per definire formule, ruoli degli indicatori e default tecnici. Non sono garanzia di
profitto.

- John Bollinger, Bollinger Band rules:
  https://www.bollingerbands.com/bollinger-band-rules
  - le bande definiscono alto/basso relativo;
  - i tag delle bande non sono segnali autonomi;
  - chiusure fuori banda sono inizialmente segnali di continuazione, non inversione;
  - Bollinger richiede conferme da indicatori complementari non collineari.
- StockCharts, Bollinger Bands:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-overlays/bollinger-bands
  - default tecnico BB20/2;
  - le bande vanno usate come contesto, non come sistema isolato.
- StockCharts, Percent B:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/b-indicator
  - `%B=1` prezzo alla upper band;
  - `%B=0` prezzo alla lower band;
  - `%B>1` prezzo sopra upper band;
  - `%B<0` prezzo sotto lower band.
- StockCharts, ADX:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/average-directional-index-adx
  - ADX misura forza del trend;
  - Wilder usa area `>25` come trend forte;
  - area `<20` indica trend assente/debole;
  - `20-25` e' zona grigia.
- StockCharts, ATR:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/average-true-range-atr
  - True Range usa high, low e close precedente;
  - ATR misura volatilita', non direzione.
- StockCharts, Chandelier Exit:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-overlays/chandelier-exit
  - default classico `22` periodi e `3.0 * ATR`;
  - Chandelier e' trailing stop / gestione rischio, non segnale BUY.
- StockCharts, breakout con volume:
  https://help.stockcharts.com/scanning-and-alerts/scan-writing-resource-center/scanning-case-studies/scanning-for-consolidation-and-breakouts
  - il volume tende a essere basso in consolidamento;
  - il breakout e' piu' forte quando il volume aumenta insieme al prezzo.
- StockCharts, RSI:
  https://chartschool.stockcharts.com/table-of-contents/technical-indicators-and-overlays/technical-indicators/relative-strength-index-rsi
  - Wilder usa RSI 14;
  - `>70` overbought, `<30` oversold;
  - in trend forte RSI puo' restare elevato.

## Principi Di Implementazione

1. Le formule ufficiali diventano vincoli, non parametri:
   - BB20/2;
   - `%B`;
   - ADX/DMI Wilder su OHLC;
   - ATR/True Range Wilder su OHLC;
   - Chandelier `highest_high - ATR * multiplier`.
2. I parametri non ufficiali restano parametri dichiarati:
   - follow-through minimo breakout;
   - massimo inseguimento sopra upper band;
   - volume minimo range;
   - volume minimo breakout;
   - RSI breakout;
   - loss cap.
3. Ogni parametro deve avere:
   - origine;
   - ruolo tecnico;
   - formula;
   - default operativo;
   - dominio di verifica;
   - motivo per cui puo' aumentare o ridurre il profitto.
4. Ogni combinazione testata deve produrre metriche per setup e reason:
   - numero trade;
   - win/loss;
   - net profit quote;
   - average net;
   - MFE zero count;
   - MFE positive count;
   - average MFE;
   - max drawdown per trade;
   - exit reason distribution;
   - telegram notification count solo come controllo idempotenza.

## Intervento 1 - ADX/DMI/ATR Wilder OHLC

### Stato Attuale

Il runtime ACDC calcola ADX/DMI su una proxy close-to-close derivata dalle microbar. Questo non e' Wilder completo.
La lacuna e' gia' dichiarata nel documento scientifico A3.

### Formula Corretta

Per ogni barra decisionale chiusa:

```text
up_move = high_t - high_{t-1}
down_move = low_{t-1} - low_t

+DM_t = up_move if up_move > down_move and up_move > 0 else 0
-DM_t = down_move if down_move > up_move and down_move > 0 else 0

TR_t = max(
  high_t - low_t,
  abs(high_t - close_{t-1}),
  abs(low_t - close_{t-1})
)

ATR14_t = WilderSmooth(TR, 14)
+DI14_t = 100 * WilderSmooth(+DM, 14) / ATR14_t
-DI14_t = 100 * WilderSmooth(-DM, 14) / ATR14_t
DX_t = 100 * abs(+DI14_t - -DI14_t) / (+DI14_t + -DI14_t)
ADX14_t = WilderSmooth(DX, 14)
```

Per Chandelier breakout long:

```text
chandelier_long = highest_high(22) - ATR22 * multiplier
default multiplier = 3.0
```

### Valore Per La Nostra Configurazione

Con profilo A3:

```text
bar_width = 20s
ADX/DMI period = 14 barre = 280s nominali
ATR14 = 280s nominali
Chandelier period = 22 barre = 440s nominali
```

Il periodo resta espresso in barre, non in secondi assoluti. Se in futuro si testa una cadence diversa, il numero di
barre resta quello di letteratura; cambia la finestra temporale effettiva.

### Punti Codice Da Verificare/Modificare

Modulo `influxer`:

- verificare che il bucket sorgente `binance-microbar` contenga o possa produrre OHLCV reali;
- se oggi persiste solo prezzo/close, aggiungere high/low/open/close/volume per la microbar;
- se il dato raw non ha high/low nativi, aggregare high/low dai tick interni della finestra prima della scrittura;
- vietare high=low=close sintetico come fonte ADX/DMI/ATR strategica, salvo flag diagnostico.

Modulo `hft-common`:

- introdurre eventuale modello condiviso `DecisionCandle` con:
  - `open`;
  - `high`;
  - `low`;
  - `close`;
  - `volume`;
  - `opened_at`;
  - `closed_at`;
  - `source_bucket`;
  - `interval_seconds`;
  - `synthetic_backfill`.
- centralizzare costanti:
  - `ADX_PERIOD = 14`;
  - `ATR_PERIOD = 14`;
  - `CHANDELIER_PERIOD = 22`;
  - `CHANDELIER_ATR_MULTIPLIER_DEFAULT = 3.0`.

Modulo `acdc`:

- `InfluxSnapshotService` deve leggere o aggregare OHLCV, non solo close/price;
- `directional(...)` deve essere sostituito con calcolo Wilder completo;
- `atr(...)` deve usare True Range OHLC;
- `highest_high_since_entry` deve usare high reale della barra, non solo snapshot price;
- feature JSON deve esporre:
  - `ohlc_wilder_indicators=1`;
  - `adx14`;
  - `previous_adx14`;
  - `plus_di14`;
  - `minus_di14`;
  - `atr14`;
  - `atr22`;
  - `true_range`;
  - `decision_high`;
  - `decision_low`.

Modulo `kenshiro`:

- readiness deve bloccare PAPER RT se `ohlc_wilder_indicators != 1`;
- `/management/state` deve esporre blocker dedicato:
  - `RT_WILDER_OHLC_NOT_READY`.

Modulo `hft-fe`:

- management deve mostrare:
  - `ohlc_wilder_indicators`;
  - source bucket;
  - interval;
  - high/low availability;
  - ADX/DMI mode.

### Test Tecnici Obbligatori

1. Serie piatta:
   - high/low/close costanti;
   - ATR prossimo a zero;
   - ADX basso;
   - nessun breakout BUY.
2. Serie trend up:
   - high e low crescenti;
   - `+DI > -DI`;
   - ADX rising;
   - breakout potenzialmente comprabile se Bollinger/volume passano.
3. Serie trend down:
   - `-DI > +DI`;
   - range reentry bloccata se DMI bearish.
4. Gap bar:
   - TR deve usare `abs(high - prev_close)` o `abs(low - prev_close)`;
   - ATR deve aumentare correttamente.
5. Regressione RUN 138:
   - ricalcolare feature ingresso con OHLC-Wilder;
   - misurare quanti dei 27 MFE-zero sarebbero stati bloccati.

## Intervento 2 - Follow-Through Breakout

### Stato Attuale

```text
rt.entry.breakout.min_last_close_return = 0.0007
```

### Perche' Esiste

Serve a evitare BUY su breakout statici o senza spinta dopo upper breach. Nasce da evidenza PAPER negativa sui breakout
con MFE zero.

### Letteratura

Bollinger non definisce un valore di follow-through percentuale. La letteratura Bollinger dice che una chiusura fuori
banda puo' essere segnale di continuazione, ma avverte dei head fake. Quindi il concetto di conferma e' coerente; il
numero fisso e' nostro.

### Formula Target

Sostituire o affiancare la soglia fissa con soglia adattiva:

```text
effective_min_last_close_return =
  max(
    static_min_last_close_return,
    roundtrip_fee_return + min_executable_entry_edge,
    atr_multiplier_for_follow_through * atr_pct_20s
  )
```

Default iniziali per verifica:

```text
static_min_last_close_return in {0.0000, 0.0005, 0.0007, 0.0010}
atr_multiplier_for_follow_through in {0.00, 0.05, 0.10, 0.15, 0.20}
```

### Punti Codice

`hft-common`:

- aggiungere costanti:
  - `RT_ENTRY_BREAKOUT_FOLLOW_THROUGH_MODE`;
  - `RT_ENTRY_BREAKOUT_STATIC_MIN_LAST_CLOSE_RETURN`;
  - `RT_ENTRY_BREAKOUT_ATR_FOLLOW_THROUGH_MULTIPLIER`.

`acdc`:

- in `RealtimeDecisionService.breakoutEntry(...)` sostituire confronto diretto con:
  - calcolo `effective_min_last_close_return`;
  - feature audit `effective_min_last_close_return`;
  - feature audit `follow_through_mode`.

DB migration:

- aggiungere config con valori default e descrizione.

Kenshiro/FE:

- esporre il valore effettivo calcolato nelle diagnostics.

## Intervento 3 - Anti-Chase Sopra Upper Band

### Stato Attuale

```text
rt.entry.breakout.max_upper_edge_pct = 0.0060
```

### Perche' Esiste

Serve a evitare BUY troppo lontani dalla upper band, cioe' inseguimento del prezzo dopo movimento gia' esteso.

### Letteratura

Bollinger non fornisce un massimo percentuale oltre upper band. La metrica coerente con Bollinger e' `%B`, perche'
misura la posizione del prezzo rispetto alla banda. Un cap su `price-edge pct` e' meno coerente perche' non tiene conto
della larghezza corrente delle bande.

### Formula Target

Rendere il cap esprimibile in `%B`:

```text
breakout_extension_b = current_percent_b
allow BUY if current_percent_b <= max_breakout_percent_b
```

Dominio di verifica:

```text
max_breakout_percent_b in {1.05, 1.10, 1.15, 1.20, 1.30, INF}
```

Il valore `INF` significa nessun cap su `%B`; resta comunque attivo il controllo volume/ADX/follow-through.

### Punti Codice

`hft-common`:

- aggiungere `RT_ENTRY_BREAKOUT_MAX_PERCENT_B`;
- mantenere `RT_ENTRY_BREAKOUT_MAX_UPPER_EDGE_PCT` come legacy/diagnostico finche' la migrazione non e' completa.

`acdc`:

- in `breakoutEntry(...)` usare il cap `%B` come regola primaria;
- scrivere in feature JSON:
  - `breakout_extension_b`;
  - `max_breakout_percent_b`;
  - `upper_band_edge_pct` solo diagnostico.

DB:

- aggiungere `rt.entry.breakout.max_percent_b`.

FE:

- mostrare `%B` e cap `%B`, non solo distance percent.

## Intervento 4 - Range Reentry Volume Floor

### Stato Attuale

```text
rt.entry.range.min_volume_ratio = 0.50
```

### Perche' Esiste

RUN `136` ha aperto una range reentry con:

```text
volume_ratio = 0.3091023688218919
volume_confirmation = 0
MFE = 0
exit = RT_EXIT_RANGE_LOSS_CAP
```

Il floor blocca barre quasi morte, non conferma breakout.

### Letteratura

La letteratura non definisce un volume floor ufficiale per Bollinger reentry. Pero' volume nullo o molto basso rende il
segnale fragile e meno eseguibile. Questo parametro e' quindi una guardia di qualita'/liquidita', non una regola
Bollinger.

### Formula Target

```text
allow range reentry if volume_ratio >= min_range_volume_ratio
```

Dominio di verifica:

```text
min_range_volume_ratio in {0.00, 0.30, 0.50, 0.70, 1.00}
```

Interpretazione:

- `0.00`: nessun floor, solo controllo chaos;
- `0.30`: blocca solo barre estremamente morte;
- `0.50`: default attuale post-RUN 136;
- `0.70`: floor piu' severo;
- `1.00`: richiede volume almeno pari alla media, probabilmente troppo restrittivo per mean-reversion.

### Punti Codice

Il parametro e' gia' presente. Gli sviluppi richiesti sono:

- esporlo in diagnostics FE/Kenshiro;
- includerlo nel replay/forensics;
- includerlo nella matrice di verifica;
- verificare che non sia applicato al breakout, dove resta `breakout.min_volume_ratio`.

## Intervento 5 - Breakout Volume Ratio

### Stato Attuale

```text
bb.context.breakout.min_volume_ratio = 1.30
rt.entry.breakout.volume_ratio_min = 1.30
```

### Perche' Esiste

Il breakout senza volume e' piu' sospetto. La conferma volume e' coerente con letteratura tecnica e con StockCharts.

### Letteratura

Il principio e' confermato: durante consolidamento il volume tende a essere basso; breakout con aumento volume e'
segnale piu' forte. Il valore `1.30` non e' ufficiale, ma e' un default iniziale ragionevole.

### Formula Target

```text
volume_ratio = volume_current_decision_bar / average(volume_decision_bar, 20)
allow breakout if volume_ratio >= min_breakout_volume_ratio
```

Dominio di verifica:

```text
min_breakout_volume_ratio in {1.00, 1.30, 1.50, 1.80, 2.00, 2.50}
```

### Ottimizzazione

Metriche specifiche:

- MFE-zero rate;
- net profit;
- trade count;
- missed winner count;
- false breakout count;
- profit per accepted trade.

Il parametro va ottimizzato per setup `RT_ENTRY_SQUEEZE_BREAKOUT_LONG`, non globalmente.

### Punti Codice

Il parametro esiste gia'. Gli sviluppi richiesti:

- unificare `bb.context.breakout.min_volume_ratio` e `rt.entry.breakout.volume_ratio_min`, o dichiarare chiaramente
  quale prevale;
- aggiungere audit `effective_breakout_min_volume_ratio`;
- includere il valore nella matrice e nei report.

## Intervento 6 - RSI Breakout

### Stato Attuale

```text
bb.context.breakout.min_rsi = 50
bb.context.breakout.max_rsi = 75
```

### Perche' Esiste

Serve a evitare breakout senza momentum e breakout troppo esausti.

### Letteratura

Wilder RSI classico:

```text
period = 14
overbought > 70
oversold < 30
```

Tuttavia in trend forte RSI elevato puo' restare elevato. Quindi un cap rigido puo' eliminare winner reali.

### Formula Target

RSI non deve essere trigger primario. Deve restare filtro momentum:

```text
breakout_momentum_ok = rsi14 >= min_breakout_rsi
```

Il cap superiore deve diventare condizionale:

```text
if rsi_cap_mode = HARD:
  allow if rsi14 <= max_breakout_rsi

if rsi_cap_mode = CONDITIONAL_EXTENSION:
  allow rsi14 > max_breakout_rsi only if:
    percent_b <= max_breakout_percent_b_when_rsi_hot
    AND volume_ratio >= hot_rsi_min_volume_ratio
    AND ADX confirms trend strength

if rsi_cap_mode = OFF:
  no upper RSI cap
```

Dominio di verifica:

```text
min_breakout_rsi in {45, 50, 55}
max_breakout_rsi in {70, 75, 80, INF}
rsi_cap_mode in {HARD, CONDITIONAL_EXTENSION, OFF}
hot_rsi_min_volume_ratio in {1.50, 2.00}
```

### Punti Codice

`hft-common`:

- enum/costanti per `rsi_cap_mode`;
- config `rt.entry.breakout.rsi_cap_mode`;
- config `rt.entry.breakout.hot_rsi_min_volume_ratio`.

`acdc`:

- sostituire `RSI_BREAKOUT_OK` booleano rigido con audit che spiega:
  - min pass;
  - max pass;
  - cap bypass condizionale;
  - reason di blocco.

FE/Kenshiro:

- mostrare RSI mode e motivo pass/fail.

## Intervento 7 - Loss Cap

### Stato Attuale

```text
rt.exit.breakout.loss_cap_net_pct = -0.0035
rt.exit.range.loss_cap_net_pct = -0.0035
```

### Perche' Esiste

Serve a limitare la perdita quando il trade e' sbagliato. Non e' regola Bollinger; e' risk management.

### Letteratura

Bollinger non fornisce loss cap. Chandelier/ATR e' piu' vicino alla letteratura per trailing/stop dinamico, ma il loss
cap netto resta necessario nei micro-trade con fee e slippage.

### Formula Target

Sostituire il loss cap fisso con valore effettivo adattivo:

```text
effective_loss_cap =
  -max(
    static_loss_cap_abs,
    roundtrip_fee_return + min_executable_entry_edge,
    loss_cap_atr_multiplier * atr_pct_20s
  )
```

Dominio di verifica:

```text
static_loss_cap_abs in {0.0025, 0.0035, 0.0050}
loss_cap_atr_multiplier in {0.25, 0.50, 0.75, 1.00}
loss_cap_mode in {STATIC, ATR_ADAPTIVE}
```

### Punti Codice

`hft-common`:

- aggiungere costanti:
  - `RT_EXIT_LOSS_CAP_MODE`;
  - `RT_EXIT_LOSS_CAP_ATR_MULTIPLIER`;
  - `RT_EXIT_STATIC_LOSS_CAP_ABS`.

`acdc`:

- calcolare `effective_loss_cap` in `rangeExit(...)` e `breakoutExit(...)`;
- persistire:
  - `effective_loss_cap`;
  - `loss_cap_mode`;
  - `loss_cap_atr_multiplier`;
  - `static_loss_cap_abs`.

Forensics:

- distinguere:
  - trade che hanno avuto MFE positivo e poi loss cap;
  - trade MFE zero fermati da loss cap;
  - trade fermati da Chandelier;
  - trade fermati da false breakout.

## Matrice Completa Dei Parametri

### Parametri Fissi Non Ottimizzabili

Questi non entrano nella matrice perche' derivano direttamente dalla letteratura o dal vincolo tecnico corrente.

| ID | Parametro | Valore | Motivo |
|---|---:|---:|---|
| F1 | Bollinger period | `20` barre | Default Bollinger/StockCharts/Fidelity |
| F2 | Bollinger stddev | `2` | Default Bollinger/StockCharts/Fidelity |
| F3 | RSI period | `14` | Wilder RSI |
| F4 | ADX/DMI period | `14` | Wilder ADX/DMI |
| F5 | ATR period | `14` | Wilder ATR |
| F6 | Chandelier period | `22` | Default Chandelier |
| F7 | Chandelier ATR multiplier base | `3.0` | Default Chandelier |
| F8 | Decision source | `binance-microbar` reali | Vincolo A3 |
| F9 | Synthetic backfill | `false` | Vincolo A3 |

### Parametri Variabili

Questa e' la tabella base della matrice fattoriale completa.

| ID | Parametro | Valori Da Verificare | Cardinalita' | Tipo |
|---|---|---:|---:|---|
| P1 | `static_min_last_close_return` | `0.0000`, `0.0005`, `0.0007`, `0.0010` | 4 | breakout |
| P2 | `atr_follow_through_multiplier` | `0.00`, `0.05`, `0.10`, `0.15`, `0.20` | 5 | breakout |
| P3 | `max_breakout_percent_b` | `1.05`, `1.10`, `1.15`, `1.20`, `1.30`, `INF` | 6 | breakout |
| P4 | `min_range_volume_ratio` | `0.00`, `0.30`, `0.50`, `0.70`, `1.00` | 5 | range |
| P5 | `min_breakout_volume_ratio` | `1.00`, `1.30`, `1.50`, `1.80`, `2.00`, `2.50` | 6 | breakout |
| P6 | `min_breakout_rsi` | `45`, `50`, `55` | 3 | breakout |
| P7 | `max_breakout_rsi` | `70`, `75`, `80`, `INF` | 4 | breakout |
| P8 | `rsi_cap_mode` | `HARD`, `CONDITIONAL_EXTENSION`, `OFF` | 3 | breakout |
| P9 | `hot_rsi_min_volume_ratio` | `1.50`, `2.00` | 2 | breakout |
| P10 | `static_loss_cap_abs` | `0.0025`, `0.0035`, `0.0050` | 3 | sell |
| P11 | `loss_cap_atr_multiplier` | `0.25`, `0.50`, `0.75`, `1.00` | 4 | sell |
| P12 | `loss_cap_mode` | `STATIC`, `ATR_ADAPTIVE` | 2 | sell |

Matrice completa teorica:

```text
4 * 5 * 6 * 5 * 6 * 3 * 4 * 3 * 2 * 3 * 4 * 2 = 6,220,800 combinazioni
```

Questa cardinalita' e' intenzionalmente completa. Non significa che tutte le combinazioni siano sensate o che vadano
testate brute-force in realtime. La fase successiva dovra' decidere come filtrare:

- esclusione manuale dei profili incoerenti;
- campionamento;
- ricerca grid ridotta;
- algoritmo genetico;
- ML/ranking;
- rete neurale o surrogate model su storico.

Questo documento non sceglie ancora il metodo di filtraggio.

Nota di implementazione 2026-07-05:

- il valore `1,036,800` precedentemente indicato era un errore aritmetico rispetto ai domini P1-P12 dichiarati;
- la matrice fattoriale completa generata dai domini dichiarati contiene `6,220,800` combinazioni;
- ogni sottoinsieme `rsi_cap_mode x loss_cap_mode` contiene `1,036,800` combinazioni.

### Regole Di Coerenza Da Valutare Dopo La Matrice

Queste regole non escludono ancora combinazioni. Sono marcatori per il Consiglio.

| Regola | Possibile Esclusione Futura | Motivo |
|---|---|---|
| `rsi_cap_mode=OFF` con `max_breakout_percent_b=INF` | probabile esclusione | permette breakout iper-estesi senza cap momentum |
| `min_breakout_volume_ratio=1.00` con follow-through basso | probabile esclusione | rischio head fake elevato |
| `static_min_last_close_return=0` e `atr_follow_through_multiplier=0` | probabile esclusione | nessun follow-through reale |
| `min_range_volume_ratio=1.00` | possibile esclusione | puo' bloccare mean-reversion valide |
| `loss_cap_mode=STATIC` con `static_loss_cap_abs=0.0050` | possibile esclusione | puo' aumentare loss su MFE-zero |
| `loss_cap_atr_multiplier=1.00` su micro-trade | da verificare | puo' essere troppo largo se ATR alto |

## Schema Della Tabella Test

La tabella test finale deve essere materializzata in una tabella DB o file CSV generato, non scritta manualmente nel
documento. Ogni riga corrisponde a una combinazione della matrice.

Colonne obbligatorie:

```text
profile_id
static_min_last_close_return
atr_follow_through_multiplier
max_breakout_percent_b
min_range_volume_ratio
min_breakout_volume_ratio
min_breakout_rsi
max_breakout_rsi
rsi_cap_mode
hot_rsi_min_volume_ratio
static_loss_cap_abs
loss_cap_atr_multiplier
loss_cap_mode
coherence_flags
test_status
trade_count
win_count
loss_count
net_profit_quote
avg_net_profit_quote
mfe_zero_count
mfe_positive_count
avg_mfe
max_mfe
exit_breakout_upper_band_profit_count
exit_breakout_false_breakout_count
exit_breakout_loss_cap_count
exit_breakout_chandelier_stop_count
exit_range_profit_count
exit_range_loss_cap_count
notes
```

Esempio di generazione concettuale:

```text
for P1 in static_min_last_close_return_values:
for P2 in atr_follow_through_multiplier_values:
for P3 in max_breakout_percent_b_values:
for P4 in min_range_volume_ratio_values:
for P5 in min_breakout_volume_ratio_values:
for P6 in min_breakout_rsi_values:
for P7 in max_breakout_rsi_values:
for P8 in rsi_cap_mode_values:
for P9 in hot_rsi_min_volume_ratio_values:
for P10 in static_loss_cap_abs_values:
for P11 in loss_cap_atr_multiplier_values:
for P12 in loss_cap_mode_values:
  emit profile row
```

## Piano Implementativo Dettagliato

### Step 1 - Fonte OHLCV Decisionale

Obiettivo:

```text
Ogni barra decisionale deve avere OHLCV vero o aggregato da dati reali.
```

Azioni:

1. Verificare schema dati Influx corrente per `binance-microbar`.
2. Verificare se esistono campi `open`, `high`, `low`, `close`, `volume`.
3. Se mancano `open/high/low/close`, modificare `influxer` per produrli.
4. Se `influxer` non ha tick sufficienti per high/low reale, marcare `ohlc_quality=DEGRADED` e non usare ADX/DMI
   strategico.
5. ACDC deve fallire readiness RT se `ohlc_quality != READY`.

Verifica:

```text
/management/state:
  rt_ohlc_ready = true
  rt_wilder_indicators = true
  rtBlockers does not contain RT_WILDER_OHLC_NOT_READY
```

### Step 2 - Calcolo Wilder In ACDC

Obiettivo:

```text
ADX/DMI/ATR calcolati da OHLC con formula Wilder.
```

Azioni:

1. Sostituire calcolo close-to-close in `InfluxSnapshotService`.
2. Implementare `trueRange`.
3. Implementare `plusDM` e `minusDM`.
4. Implementare Wilder smoothing.
5. Calcolare `ADX14`, `previousADX14`, `+DI14`, `-DI14`, `ATR14`, `ATR22`.
6. Persistire feature audit.

Verifica:

- unit test su serie sintetiche;
- replay RUN 138 con doppio output:
  - old close-to-close;
  - new Wilder OHLC;
- report differenze sui 27 MFE-zero.

### Step 3 - Parametri Breakout Adattivi

Obiettivo:

```text
Rimuovere dipendenza da soglie fisse non motivate.
```

Azioni:

1. Introdurre `effective_min_last_close_return`.
2. Introdurre `max_breakout_percent_b`.
3. Rendere `upper_band_edge_pct` diagnostico.
4. Introdurre `effective_breakout_min_volume_ratio`.
5. Introdurre RSI cap mode.

Verifica:

- ogni decisione ENTRY deve salvare:
  - valore raw;
  - valore soglia;
  - valore effettivo;
  - pass/fail reason.

### Step 4 - Loss Cap Adattivo

Obiettivo:

```text
Loss cap coerente con fee, edge e ATR.
```

Azioni:

1. Aggiungere loss cap mode.
2. Calcolare `effective_loss_cap`.
3. Applicare lo stesso modello a breakout/range, con parametri setup-specifici se necessario.
4. Esportare in forensics.

Verifica:

- nessun trade deve uscire con loss cap senza feature `effective_loss_cap`;
- report MFE-zero loss cap separato da MFE-positive loss cap.

### Step 5 - Generatore Matrice Parametrica

Obiettivo:

```text
Materializzare tutte le 6,220,800 combinazioni in forma interrogabile.
```

Azioni:

1. Creare script diagnostico `DIAGNOSTIC_ONLY` per generare CSV o tabella DB.
2. Lo script non deve avviare PAPER.
3. Lo script deve scrivere:
   - `profile_id`;
   - parametri;
   - coherence flags;
   - hash configurazione.
4. Il Consiglio usera' questa matrice nello step successivo per decidere filtro/test.

### Step 6 - Replay Storico Causale

Obiettivo:

```text
Valutare ogni profilo su trade passati senza lookahead.
```

Azioni:

1. Usare solo dati disponibili fino alla barra decisionale corrente.
2. Vietare feature future.
3. Valutare BUY/SELL con stessi costi PAPER.
4. Separare risultati per:
   - breakout;
   - range;
   - reason SELL;
   - simbolo;
   - fascia oraria.

Output:

```text
profile_id
metrics
validity
reason_distribution
```

### Step 7 - Decisione Successiva

Questo documento si ferma prima della scelta del metodo di filtro. Lo step successivo dovra' decidere se usare:

- esclusione manuale;
- grid ridotta;
- algoritmo genetico;
- modello ML;
- rete neurale;
- combinazione dei metodi.

Nessun profilo diventa operativo realtime solo perche' performa sul passato. Serve poi validazione PAPER forward.

## Exit Criteria Del Piano

Il piano e' implementato solo quando:

1. ACDC usa ADX/DMI/ATR Wilder OHLC.
2. `/management/state` espone readiness OHLC/Wilder.
3. Ogni soglia non ufficiale e' parametrizzata e auditata.
4. La matrice completa dei parametri e' generabile.
5. Ogni riga matrice ha `profile_id` stabile.
6. Il replay storico causale puo' valutare un profilo senza lookahead.
7. Il report distingue MFE-zero da MFE-positive.
8. Nessuna RUN PAPER usa profilo non dichiarato.
