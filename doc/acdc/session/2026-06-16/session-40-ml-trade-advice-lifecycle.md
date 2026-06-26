# Session 40 - ML Trade Advice Lifecycle

Data: 2026-06-16.

## Obiettivo

Far diventare il modello REM ML il decisore operativo completo:

- BUY acquista solo se DocBrown promuove un segnale REM profittevole netto;
- SELL vende usando le soglie consigliate dal modello;
- soglie manuali di tuning fuori da questa logica sono legacy e devono essere disabilitate o degradate a fallback espliciti.

## Contratto Advice

DocBrown scrive in `rule_json.advice`:

- `duration_seconds`: durata indicativa del segnale;
- `safe_net_return`: guadagno conservativo atteso al netto di fee, dust e slippage;
- `max_net_return`: massimo guadagno netto atteso;
- `loss_cap_net_return`: perdita massima netta consigliata.

ACDC copia questi campi nello snapshot runtime:

- `ml_advice_duration_seconds`;
- `ml_advice_safe_net_return`;
- `ml_advice_max_net_return`;
- `ml_advice_loss_cap_net_return`.

## Implementazione ACDC

- `OutcomeQualityModelService` legge l'advice dalla migliore regola REM ML promossa.
- `ShadowRunService` e `PaperRunService` persistono lo snapshot/advice della BUY in `policy_json`.
- Le SELL fondono `policy_json` della BUY con lo snapshot corrente, cosi' l'uscita non perde l'advice se il segnale live scompare.
- `GuardOperator` aggiunge:
  - `ML_ADVICE_TAKE_PROFIT_EXIT`;
  - `ML_ADVICE_LOSS_CAP_EXIT`.
- `GuardEvaluator` vende:
  - quando `net_return >= ml_advice_safe_net_return`;
  - oppure quando e' scaduta la durata prevista e il trade e' comunque netto positivo;
  - oppure quando `net_return <= ml_advice_loss_cap_net_return`.

## Migrazioni

- `V30__ml_advice_exit_policy.sql` disabilita le vecchie guardie EXIT trailing/profit/loss e attiva le exit ML advice.
- `V31__align_entry_gates_to_shared_ml_config.sql` allinea le ENTRY guard alla shared config.
- `V32__require_ml_rule_for_entry.sql` obbliga almeno una regola ML promossa per aprire una BUY.

## Verifica

- Test ACDC completati con `./mvnw -q test`.
- Build ACDC completata con `./mvnw -q package -DskipTests`.
- Container ACDC rebuildato.
- Flyway DB arrivato a `v32`.

## SHADOW 36

Risultato finale:

- status `COMPLETED`;
- budget finale `100.113360390274670000`;
- realized profit quote `+0.113360390274670000`.

Trade:

- `BANKUSDC`: advice ML presente, SELL `EXIT_ML_ADVICE_TAKE_PROFIT`, profitto netto `+0.310216345915600000`;
- `KMNOUSDC`: aperta prima del gate V32, senza advice, chiusa da safety `EXIT_ML_ADVICE_LOSS_CAP`, perdita `-0.146855955652530000`;
- `ENAUSDC`: aperta prima del gate V32, senza advice, chiusa da safety `EXIT_ML_ADVICE_LOSS_CAP`, perdita `-0.049999999988400000`.

Conclusione:

- il percorso ML advice BUY -> ML advice SELL funziona;
- la run non e' pulita per valutare il modello perche' e' partita prima della `V32`;
- una nuova SHADOW/PAPER valida deve contenere solo posizioni con `reversal_ml_rules >= 1` e advice ML persistito.
