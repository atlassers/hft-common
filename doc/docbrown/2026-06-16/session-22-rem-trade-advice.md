# Session 22 - REM Trade Advice

Data: 2026-06-16.

## Obiettivo

Evolvere DocBrown da produttore di segnali REM a produttore di advice operativo completo per ACDC.

Il modello deve dire:

- se comprare;
- quanto tempo si prevede duri il segnale;
- quale guadagno netto conservativo catturare;
- quale massimo netto aspettarsi;
- quale perdita netta massima accettare.

## Implementazione

- `HistoricalOutcomeMiningService` arricchisce ogni campione con:
  - `advice_duration_seconds`;
  - `advice_safe_net_return`;
  - `advice_max_net_return`;
  - `advice_loss_cap_net_return`;
  - `advice_end_net_return`.
- I valori sono derivati dall'outcome futuro su microbar e includono gia' costi/frizioni applicati dal labeling.
- `ReversalMlRuleMiningService` aggrega gli advice sui match di validazione:
  - durata mediana;
  - `safe_net_return` come quantile conservativo dei match profittevoli;
  - `max_net_return` come quantile alto dei match profittevoli;
  - `loss_cap_net_return` come quantile negativo/cauto dei match di validazione.
- La promozione richiede:
  - campioni di validazione sufficienti;
  - profit rate minimo da shared config;
  - media validazione positiva;
  - `safe_net_return > 0`;
  - `max_net_return >= safe_net_return`.

## Verifica

- `./mvnw -q test` completato.
- `./mvnw -q package -DskipTests` completato.
- Run manuale:
  - endpoint `POST /docbrown/rem/research/REM_CURRENT/run`;
  - `scannedPoints=5000`;
  - `good=1310`;
  - `bad=3689`;
  - `promotedSignatures=50`;
  - regole ML promosse con advice: `12`.

## Output Rilevante

Esempi di advice promossi:

- `BANKUSDC`:
  - `safe_net_return` circa `0.0136270783847981`;
  - `duration_seconds` circa `640`;
  - `loss_cap_net_return` circa `-0.003`.
- `BANANAS31USDC`:
  - `safe_net_return` circa `0.006018619084561676`;
  - `duration_seconds` circa `665`;
  - `loss_cap_net_return` circa `-0.003`.

## Ruolo Strategico

DocBrown resta il producer scientifico.
ACDC non deve inventare soglie operative: deve consumare l'advice promosso e applicarlo alla lifecycle BUY/SELL.

## Live Audit

Aggiornamento 2026-06-16:

- DocBrown legge le posizioni ACDC SHADOW/PAPER chiuse come audit live, non come training primario.
- Per ogni candidate rule, DocBrown verifica se il `policy_json` della BUY rientra nei range della regola.
- Se una regola produce trade live con `max_net_return=0` e profitto netto negativo, il segnale e' considerato falsificato.
- Se una regola produce trade live che non raggiunge `ml_advice_safe_net_return` e chiude non profittevole, l'advice e' considerato sotto target.
- Le regole con tasso `zero_mfe_loss` superiore alla shared config vengono penalizzate e non promosse.
- Le regole con tasso `under_safe_non_profit` superiore alla shared config vengono penalizzate e non promosse.
- Parametri shared config:
  - `rem.ml.live_audit.limit`;
  - `rem.ml.live_audit.min.samples`;
  - `rem.ml.live_audit.max.zero_mfe_loss_rate`;
  - `rem.ml.live_audit.max.under_safe_non_profit_rate`;
  - `rem.ml.live_audit.penalty.weight`.

Verifica reale:

- dopo SHADOW `38`, il live-audit ha respinto regole `ALLOUSDC`, `BANKUSDC`, `BANANAS31USDC` e diversi fallback `UNKNOWN/GLOBAL` che matchavano loss senza max netto positivo;
- le nuove promozioni sono state spostate su simboli/regole senza falsificazione live recente.
- dopo SHADOW `39`, e' stato aggiunto `under_safe_non_profit`: trade chiuso non profittevole che non ha mai raggiunto `ml_advice_safe_net_return`.
- dopo SHADOW `40`, DocBrown ha ridotto le regole promosse da `99` a `46` e ha rigettato diverse regole `0GUSDC`, `AVNTUSDC`, `2ZUSDC` falsificate live.
- ACDC ora salva nel `policy_json` flag numerici `reversal_ml_rule_<ruleKey>=1`; DocBrown li usa come match diretto nel live-audit, mantenendo il fallback a range solo per run precedenti.

## Advice Validity

Aggiornamento 2026-06-16:

- Ogni regola promossa da DocBrown ora contiene `advice_valid_from` e `advice_valid_until`.
- La validita' della consulenza e' distinta da `duration_seconds`:
  - `advice_valid_from/until` decide fino a quando ACDC puo' aprire una BUY;
  - `duration_seconds` decide quanto puo' durare il trade dopo la BUY.
- La durata della validita' viene dalla shared config `rem.ml.advice.validity.seconds`.
- Le regole simbolo-specifiche possono essere marcate `used` da ACDC quando generano una BUY, evitando il riuso dello stesso segnale puntuale.
- Dopo rigenerazione REM su `REM_CURRENT`: `300` regole valutate, `17` promosse, `17` con validita' popolata.

## Promotion Mode

Aggiornamento 2026-06-16:

- `rem.ml.promotion.mode=STRICT` mantiene il comportamento scientifico con gate di validazione e live-audit.
- `rem.ml.promotion.mode=PURE_REVERSAL` promuove regole derivate dai campioni GOOD di reversal senza bloccarle su profit-rate, campioni minimi, safe target o live-audit.
- In `PURE_REVERSAL`, live-audit e statistiche restano nel report ma sono osservativi.
- SHADOW usa `PURE_REVERSAL` come laboratorio.
- PAPER deve essere preceduta da rigenerazione DocBrown in `STRICT`, salvo esperimenti dichiarati.
- Prima SHADOW pure: `360` regole valutate, `227` promosse, `227` valide; risultato operativo non profittevole e con MFE zero sui tre trade.
