# REALTIME_BB_ADX_NATIVE_F4E954 Strategy Integration Plan

Data: 2026-07-08.

## Stato

Questo documento definisce il piano di integrazione runtime del profilo Melo:

```text
f4e954f0b552ce864535fc2b
```

Decisione del Consiglio: il profilo diventa la strategia target `REALTIME_BB_ADX_NATIVE_F4E954` per WATCH, BUY e SELL.
La strategia resta PAPER-only fino a validazione forward tramite `/management`; REAL resta vietata.

Il profilo e' il migliore tra `51.840` configurazioni native Bollinger verificate in modo esaustivo da Melo. Non era
promuovibile secondo i guardrail di laboratorio per trade aperti e assenza di chiusure in validation/holdout, ma l'utente
ha scelto di adottarlo come nuova ipotesi strategica. Il codice deve quindi implementare esattamente questo profilo, senza
aggiungere gate legacy non presenti nella configurazione.

Fonte evidenza:

```text
melo/reports/native-bollinger-rt130-139-exhaustive/REALTIME_BB_NATIVE_BOLLINGER_SEARCH_REPORT.md
```

## Valori Da Inserire In hft-common

Tutti i valori sotto devono vivere in `hft-common`, preferibilmente in `RemConstants` o in un nuovo value object comune
esposto da `hft-common`. ACDC, Kenshiro, hft-fe, report e diagnostiche devono riferirsi a questi simboli comuni, non a
literal locali.

| Campo comune proposto | Valore | Uso |
| --- | ---: | --- |
| `RT_NATIVE_F4E954_PROFILE_HASH` | `f4e954f0b552ce864535fc2b` | Identita' strategia/report |
| `RT_NATIVE_F4E954_BB_PERIOD` | `20` | Calcolo Bollinger |
| `RT_NATIVE_F4E954_BB_STDDEV_MULTIPLIER` | `2.00` | Ampiezza bande runtime |
| `RT_NATIVE_F4E954_RANGE_MIN_DISCOUNT_TO_MIDDLE_PCT` | `0.0010` | WATCH/BUY range |
| `RT_NATIVE_F4E954_RANGE_MIN_VOLUME_RATIO` | `0.30` | WATCH/BUY range |
| `RT_NATIVE_F4E954_RANGE_DMI_MODE` | `OFF` | WATCH/BUY range: nessun blocco `+DI/-DI` |
| `RT_NATIVE_F4E954_BREAKOUT_MIN_PREMIUM_TO_MIDDLE_PCT` | `0.0010` | WATCH/BUY breakout |
| `RT_NATIVE_F4E954_BREAKOUT_MAX_PREMIUM_TO_MIDDLE_PCT` | `0.0200` | WATCH/BUY breakout anti-chase |
| `RT_NATIVE_F4E954_BREAKOUT_MIN_VOLUME_RATIO` | `1.00` | WATCH/BUY breakout |
| `RT_NATIVE_F4E954_BREAKOUT_MIN_RSI` | `45` | WATCH/BUY breakout |
| `RT_NATIVE_F4E954_LOSS_CAP_ABS` | `0.0050` | SELL risk cap |
| `RT_NATIVE_F4E954_BREAKOUT_MIN_PROTECT_NET_RETURN` | `0.0005` | SELL breakout protect |
| `RT_NATIVE_F4E954_RANGE_MAX_HOLD_CANDLES` | `36` | SELL range timeout |
| `RT_NATIVE_F4E954_BREAKOUT_MAX_HOLD_CANDLES` | `60` | SELL breakout timeout |

Config MySQL `acdc_shared_runtime_config` da allineare ai simboli comuni:

| Config key | Valore | Nota |
| --- | ---: | --- |
| `rt.strategy.family` | `REALTIME_BB_ADX_NATIVE_F4E954` | Nuova famiglia strategica o variante dichiarata |
| `rt.native.profile_hash` | `f4e954f0b552ce864535fc2b` | Audit run |
| `rt.bb.period` | `20` | Non hardcoded in ACDC |
| `rt.bb.stddev_multiplier` | `2.00` | Deve sostituire ogni assunzione implicita `Bollinger 20/2` locale |
| `rt.entry.range.min_discount_to_middle_pct` | `0.0010` | Soglia relativa a `bb_middle` |
| `rt.entry.range.min_volume_ratio` | `0.30` | Sostituisce il floor A3 `0.50` |
| `rt.entry.range.dmi_mode` | `OFF` | Disattiva il blocco `minus_di > plus_di` |
| `rt.entry.breakout.min_premium_to_middle_pct` | `0.0010` | Soglia relativa a `bb_middle` |
| `rt.entry.breakout.max_premium_to_middle_pct` | `0.0200` | Soglia relativa a `bb_middle` |
| `rt.entry.breakout.min_volume_ratio` | `1.00` | Sostituisce `1.30` |
| `rt.entry.breakout.min_rsi` | `45` | Sostituisce floor `50` |
| `rt.exit.loss_cap_mode` | `STATIC` | Il profilo Melo usa loss cap assoluto statico |
| `rt.exit.static_loss_cap_abs` | `0.0050` | Sostituisce `0.0035`/ATR adaptive |
| `rt.exit.breakout.min_protect_net_return` | `0.0005` | Protezione profitto breakout |
| `rt.exit.range.max_hold_candles` | `36` | Timeout range |
| `rt.exit.breakout.max_hold_candles` | `60` | Timeout breakout |

## Regole Strategiche

### Regole Generali

- La strategia usa barre decisionali chiuse, stessa cadence per WATCH, BUY, SELL e forensics.
- La fonte decisionale non puo' essere `binance-realtime`.
- `decision_synthetic_backfill=1` blocca WATCH/BUY/SELL strategici.
- I valori di soglia vengono congelati nel `policyJson` della posizione al BUY.
- Ogni decisione deve riportare `profile_hash`, setup, trigger, config version e valori soglia effettivi.
- WATCH non compra: WATCH classifica solo `RT_RANGE_REENTRY_WATCH`, `RT_SQUEEZE_BREAKOUT_WATCH` o blocker.
- BUY non usa cap concorrenti su posizioni/osservazioni; resta valido solo il limite budget/exchange sizing.

### WATCH Range

Setup: `BB_REENTRY_MEAN_REVERSION_LONG`.

Regola:

```text
price <= native_lower_band
AND (bb_middle - price) / bb_middle >= 0.0010
AND volume_ratio_1m_20m >= 0.30
AND range_dmi_mode = OFF
```

La banda nativa va calcolata da `bb_middle` e dalla sigma ricavata dalle bande correnti:

```text
sigma = max(abs(bb_upper - bb_middle), abs(bb_middle - bb_lower)) / current_runtime_multiplier
native_lower_band = bb_middle - sigma * 2.00
native_upper_band = bb_middle + sigma * 2.00
```

Motivazione: Melo ha verificato le soglie relative alla mediana, non prezzi fissi. La regola range non deve reintrodurre
ADX, `last_close_return`, ATR chaos o DMI se il profilo selezionato li ha esclusi.

### BUY Range

BUY range scatta solo se la WATCH range e' confermata sulla barra decisionale chiusa corrente. Il `policyJson` deve
salvare almeno:

- `profile_hash=f4e954f0b552ce864535fc2b`;
- `bb_setup_code=1`;
- `bb_stddev_multiplier=2.00`;
- `range_min_discount_to_middle_pct=0.0010`;
- `range_min_volume_ratio=0.30`;
- `range_dmi_mode=OFF`;
- `entry_native_lower_band`;
- `entry_native_upper_band`;
- `entry_discount_to_middle_pct`;
- `entry_volume_ratio_1m_20m`.

### WATCH Breakout

Setup: `BB_SQUEEZE_BREAKOUT_LONG`.

Regola:

```text
price >= native_upper_band
AND (price - bb_middle) / bb_middle >= 0.0010
AND (price - bb_middle) / bb_middle <= 0.0200
AND volume_ratio_1m_20m >= 1.00
AND rsi14 >= 45
AND bb_bandwidth_delta > 0
```

Motivazione: la configurazione migliore non usa ADX minimo, `+DI > -DI`, OBV, hot RSI extension, follow-through su
`last_close_return` o cap `%B=1.20`. Questi filtri appartengono al precedente esperimento RT e vanno rimossi dal path
strategico di questo profilo.

### BUY Breakout

BUY breakout scatta solo se la WATCH breakout e' confermata sulla barra decisionale chiusa corrente. Il `policyJson`
deve salvare almeno:

- `profile_hash=f4e954f0b552ce864535fc2b`;
- `bb_setup_code=2`;
- `bb_stddev_multiplier=2.00`;
- `breakout_min_premium_to_middle_pct=0.0010`;
- `breakout_max_premium_to_middle_pct=0.0200`;
- `breakout_min_volume_ratio=1.00`;
- `breakout_min_rsi=45`;
- `entry_native_lower_band`;
- `entry_native_upper_band`;
- `entry_premium_to_middle_pct`;
- `entry_volume_ratio_1m_20m`;
- `entry_rsi14`;
- `entry_bb_bandwidth_delta`.

### SELL Range

Regole in ordine:

1. Loss cap:

```text
net_return <= -0.0050
```

2. Middle capture:

```text
net_return > 0
AND price >= bb_middle
```

3. Upper capture:

```text
net_return > 0
AND price >= native_upper_band
```

4. Timeout:

```text
hold_candles >= 36
```

Nota: `upper capture` dopo `middle capture` e' ridondante se entrambe usano lo stesso ordine del replay Melo; puo'
restare come reason diagnostica solo se il codice distingue il primo target raggiunto. La semantica economica obbligatoria
e' catturare profitto quando il range torna almeno a `bb_middle`.

### SELL Breakout

Regole in ordine:

1. Loss cap:

```text
net_return <= -0.0050
```

2. Upper protect:

```text
max_net_return >= 0.0005
AND net_return > 0
AND price < native_upper_band
```

3. False breakout:

```text
net_return <= 0
AND price < bb_middle
```

4. Timeout:

```text
hold_candles >= 60
```

Motivazione: la logica SELL deve riprodurre il replay Melo. Il Chandelier/ATR stop resta diagnostica del ciclo precedente
e non puo' sostituire `price < native_upper_band` per la protezione del breakout.

## Punti Di Intervento

### hft-common

- Aggiungere costanti e config key comuni per `REALTIME_BB_ADX_NATIVE_F4E954`.
- Aggiungere enum/stringhe operative per:
  - `RT_NATIVE_RANGE_REENTRY`;
  - `RT_NATIVE_BREAKOUT`;
  - `RT_NATIVE_RANGE_MIDDLE_CAPTURE`;
  - `RT_NATIVE_RANGE_UPPER_CAPTURE`;
  - `RT_NATIVE_BREAKOUT_UPPER_PROTECT`;
  - `RT_NATIVE_BREAKOUT_FALSE_BREAKOUT`;
  - `RT_NATIVE_LOSS_CAP`;
  - `RT_NATIVE_TIMEOUT`.
- Aggiungere un modello comune opzionale, ad esempio `RtNativeBollingerProfile`, per evitare mappe stringa/stringa in
  ACDC/Kenshiro/hft-fe.
- Esporre i nomi delle feature audit: `native_lower_band`, `native_upper_band`, `discount_to_middle_pct`,
  `premium_to_middle_pct`, `native_profile_hash`, `native_config_version`.

### ACDC

- `InfluxSnapshotService`: calcolare bande native usando il multiplier da `hft-common`/config, non il solo
  `BOLLINGER_DEFAULT_STDDEV_MULTIPLIER`.
- `RealtimeDecisionService.entry`: sostituire i gate range/breakout con le regole native di questo documento.
- `RealtimeDecisionService.exit`: sostituire SELL range/breakout con le regole native di questo documento.
- `PaperRunService`: congelare tutti i valori del profilo nel `policyJson` al BUY e riusarli per SELL, senza rileggere
  valori mutabili se la posizione e' gia' aperta.
- Migrazione DB: inserire/aggiornare `acdc_shared_runtime_config` con le key `rt.native.*` e i valori sopra.
- Test: aggiungere unit test su `RealtimeDecisionService` per range entry, breakout entry, range middle capture,
  breakout upper protect, loss cap e timeout.
- Replay: aggiungere un test di parita' Melo/ACDC su almeno i trade delle RUN 135-139 usate dal report.

### Kenshiro

- `/management/state`: esporre famiglia `REALTIME_BB_ADX_NATIVE_F4E954`, hash profilo, valori soglia effettivi e
  readiness specifica.
- `/management/runs/{executionId}`: aggregare reason native WATCH/BUY/SELL.
- Bloccare PAPER se ACDC non espone `native_profile_hash=f4e954f0b552ce864535fc2b`.

### hft-fe

- Management: mostrare profilo nativo, hash, soglie, readiness e blocker.
- Trades/replay: mostrare bande native e premium/discount relativi alla middle band.
- Rimuovere o nascondere etichette legacy che suggeriscono uso di ADX/OBV/Chandelier come gate primari di questa
  strategia.

### Melo

- Mantenere il replay esaustivo come benchmark di regressione.
- Aggiungere, se necessario, export compatto dei trade attesi per `f4e954f0b552ce864535fc2b`, cosi' ACDC puo'
  verificare parita' senza caricare il JSON raw da 149 MB.

## Nuovo Codice Necessario

Serve nuovo codice per tre motivi:

1. Il runtime corrente usa gate non presenti nel profilo vincente.
2. Il runtime corrente assume in piu' punti `Bollinger 20/2` come default locale, mentre questa strategia richiede valori
   comuni in `hft-common` e bande native auditabili.
3. La SELL corrente contiene logiche ATR/Chandelier/DMI del ciclo precedente; il replay Melo ha valutato una SELL piu'
   semplice e setup-specifica.

Implementazione minima richiesta:

- builder comune del profilo nativo da `hft-common`;
- calcolo bande native;
- entry evaluator nativo;
- exit evaluator nativo;
- audit serializer per `policyJson`;
- diagnostica management;
- test di parita' con Melo.

## Codice Legacy Da Rimuovere O Disattivare

Nel path `REALTIME_BB_ADX_NATIVE_F4E954` vanno rimossi come gate strategici:

- `rt.entry.range.adx_max`;
- `rt.entry.range.adx_soft_max`;
- `rt.entry.range.minus_di_dominance_block`;
- `rt.entry.range.volume_chaos_block`;
- `rt.entry.range.atr_chaos_block`;
- `rt.entry.range.percent_b_recovery_required`;
- `rt.entry.range.min_last_close_return`;
- `rt.entry.breakout.adx_min`;
- `rt.entry.breakout.adx_rising_soft_min`;
- `rt.entry.breakout.require_plus_di_gt_minus_di`;
- `rt.entry.breakout.require_obv_non_negative`;
- `rt.entry.breakout.follow_through_mode`;
- `rt.entry.breakout.static_min_last_close_return`;
- `rt.entry.breakout.atr_follow_through_multiplier`;
- `rt.entry.breakout.max_percent_b`;
- `rt.entry.breakout.rsi_cap_mode`;
- `rt.entry.breakout.hot_rsi_min_volume_ratio`;
- `rt.entry.breakout.max_upper_edge_pct`;
- `rt.exit.loss_cap_mode=ATR_ADAPTIVE`;
- `rt.exit.loss_cap_atr_multiplier`;
- `rt.exit.breakout.chandelier_period`;
- `rt.exit.breakout.chandelier_atr_multiplier`;
- `rt.exit.breakout.require_plus_di_for_hold`;
- `rt.exit.breakout.false_breakout_percent_b` se resta espresso in `%B` invece che `price < bb_middle`.

Questi parametri possono restare in DB solo come memoria storica o diagnostica di run precedenti, ma non devono essere
letti dal path strategico nativo. Se restano attivi, il runtime non sta usando il profilo Melo selezionato.

Da rimuovere come concetto operativo per questa strategia:

- qualsiasi fallback da `rt.exit.breakout.loss_cap_net_pct` o `rt.exit.range.loss_cap_net_pct` dentro `effectiveLossCap`;
- qualsiasi soglia `bb.context.*` usata come override del profilo nativo;
- qualsiasi advice legacy DocBrown/ML usata per selezionare BUY in `REALTIME_BB_ADX_NATIVE_F4E954`.

## Validazione Richiesta

Prima di PAPER:

- build `hft-common`, `acdc`, `kenshiro`, `hft-fe`;
- migration test su MySQL;
- unit test ACDC per ogni regola;
- replay parita' Melo/ACDC:
  - stesso hash profilo;
  - stessi BUY;
  - stessi SELL;
  - stessi exit reason;
  - stesso net return sui trade chiusi;
  - stesso conteggio trade aperti.

Prima di considerarla strategia operativamente valida:

- PAPER avviata solo da `/management`;
- nessuna posizione aperta da run precedenti;
- `rt.strategy.enabled=false` prima dell'avvio controllato;
- report post-run con classificazione evidenza;
- confronto con benchmark Melo;
- se validation forward produce trade aperti non chiusi, la strategia resta `INCONCLUSIVE`, non promossa.

## Revisioni Del Consiglio

1. Revisione Saggio ascoltatore: il documento conserva continuita' con Bollinger come segnale centrale e non cambia la
   governance PAPER-only.
2. Revisione Scienziato severo: il profilo non era promuovibile; il documento dichiara il rischio e vieta di chiamarlo
   evidenza finanziaria validata prima di forward PAPER.
3. Revisione Mediano pragmatico: gli interventi sono limitati a hft-common, ACDC, Kenshiro, hft-fe e Melo benchmark.
4. Revisione Saggio ascoltatore: i valori sono espliciti e leggibili in un unico punto comune.
5. Revisione Scienziato severo: i gate legacy non inclusi nel profilo sono elencati come rimozioni, evitando una strategia
   ibrida non testata.
6. Revisione Mediano pragmatico: la migrazione DB resta necessaria solo per esporre/configurare il profilo; i default
   devono comunque nascere da hft-common.
7. Revisione Saggio ascoltatore: WATCH, BUY e SELL sono separati, con policy congelata al BUY per audit post-trade.
8. Revisione Scienziato severo: la parita' Melo/ACDC e' exit criterion obbligatorio prima di PAPER.
9. Revisione finale congiunta: approvato come piano di implementazione della strategia
   `REALTIME_BB_ADX_NATIVE_F4E954`, con classificazione iniziale `BEST_AVAILABLE_NOT_YET_VALIDATED_FORWARD`.

## Decisione Finale

Il Consiglio approva l'integrazione del profilo `f4e954f0b552ce864535fc2b` come nuova strategia target, a condizione che
il codice implementi esattamente le regole native sopra e rimuova i gate legacy dal path strategico. La prima evidenza
valida dopo implementazione dovra' essere PAPER forward governata da `/management`, non REAL e non replay contaminato.
