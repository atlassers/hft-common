# Session 48 - Economic Advice Floor

Data: 2026-06-16.

## Obiettivo

Evitare advice ML direzionalmente interessanti ma non economicamente tradabili dopo fee, slippage e dust.

La SHADOW `46` aveva mostrato `EURUSDC`:

- gross profit leggermente positivo;
- netto negativo per costi;
- MFE pari a `0`.

La correzione non deve aggiungere soglie runtime ACDC.
Il filtro deve stare nel miner DocBrown, prima della promozione delle advice.

## Config DB

Migrazione ACDC `V41__rem_ml_economic_advice_floor.sql`:

- `rem.ml.advice.min.safe_net_return=0.0020`;
- `rem.ml.advice.min.max_net_return=0.0040`;
- `rem.ml.advice.min.profit_probability=0.60`.

Queste soglie sono configurazione condivisa e vengono lette da DocBrown.

## Codice

DocBrown `ReversalMlRuleMiningService` ora richiede, anche in `PURE_REVERSAL`:

- `safe_net_return >= rem.ml.advice.min.safe_net_return`;
- `max_net_return >= rem.ml.advice.min.max_net_return`;
- `max_net_return >= safe_net_return`;
- profit probability matched >= `rem.ml.advice.min.profit_probability`.

Dopo la SHADOW `47`, il live audit e' stato reso vincolante anche in `PURE_REVERSAL`:

- regole con `under_safe_non_profit_rate` oltre config non vengono ripromosse;
- lo score viene penalizzato dal live audit anche fuori da `STRICT`.

## Verifiche

- ACDC `./mvnw -q -Dtest=RemCurrentConfigurationTest test`;
- ACDC `./mvnw -q package`;
- DocBrown `./mvnw -q test`;
- DocBrown `./mvnw -q package`;
- container ACDC rebuildato e riavviato;
- Flyway MySQL applicato a `v41`.

## Mining Dopo Floor Economico

Prima del filtro economico:

- regole ML promosse: `225`.

Dopo V41:

- outcome samples: `5000`;
- GOOD: `1628`;
- BAD: `3372`;
- firme outcome promosse: `142`;
- regole ML valutate: `280`;
- regole ML promosse: `64`;
- advice attive: `62`.

Distribuzione promosse:

- min safe return SYMBOL: `0.0020050050`;
- min max return SYMBOL: `0.0045037519`;
- min probability SYMBOL: `0.6000000000`;
- min safe return FAMILY: `0.0020228311`;
- min max return FAMILY: `0.0049051383`;
- min probability FAMILY: `0.6153846154`.

## SHADOW 47

Run con floor economico attivo:

- execution: `47`;
- BUY: `NEARUSDC`;
- ranking score: `98.35757531224074`;
- `reversal_ml_score=0.6614667784922189`;
- `ml_advice_safe_net_return=0.002022831050228311`;
- `ml_advice_max_net_return=0.005941877794336811`;
- `reversal_ml_profit_probability=0.9591836734693875`.

Risultato:

- SELL: `EXIT_ML_ADVICE_LOSS_CAP`;
- net: `-0.092384383130000000`;
- MFE/max net return: `0.000543063215952482`.

Lettura:

- il filtro economico ha migliorato la qualita' del segnale: MFE positiva invece di zero;
- la MFE non ha raggiunto il safe return promesso;
- quindi il feedback live deve impedire la ripromozione della stessa famiglia/regola sotto-safe.

## Mining Dopo Live Audit Comune

DocBrown rigenerato dopo SHADOW `47` con live audit vincolante anche in `PURE_REVERSAL`:

- outcome samples: `5000`;
- GOOD: `1611`;
- BAD: `3389`;
- firme outcome promosse: `81`;
- regole ML valutate: `260`;
- regole ML promosse: `55`;
- advice attive: `55`.

Le advice promosse hanno:

- max live under-safe non-profit rate: `0`;
- safe return minimo SYMBOL: `0.0020050050`;
- max return minimo SYMBOL: `0.0041998953`;
- probability minima SYMBOL: `0.6000000000`;
- safe return minimo FAMILY: `0.0029171598`;
- max return minimo FAMILY: `0.0049051383`;
- probability minima FAMILY: `0.6111111111`.

## SHADOW 48

Run dopo autocorrezione live-audit:

- execution: `48`;
- durata osservata: circa 3 minuti;
- BUY: `0`;
- status finale: `STOPPED`;
- budget finale: `100.000000000000000000`.

Reject principali:

- `REVERSAL_ML_RULE_MISSING`: `4380`;
- `REVERSAL_DATA_COVERAGE_LOW`: `401`;
- `REVERSAL_VOLUME_CONFIRMATION_LOW`: `19`.

## Conclusione

La pipeline ora e' piu' selettiva:

- non promuove piu' advice con edge inferiore ai floor economici;
- usa il feedback live per rimuovere advice che non raggiungono il proprio safe return;
- preferisce non comprare se non c'e' reversal economicamente tradabile.

Questo non prova ancora profittabilita'.
Prova pero' che il sistema sta smettendo di comprare segnali deboli e sta usando SHADOW come feedback scientifico.
