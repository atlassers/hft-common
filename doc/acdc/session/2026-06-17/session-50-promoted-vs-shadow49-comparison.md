# Session 50 - Promoted Advice vs SHADOW 49

Data: 2026-06-17.

## Obiettivo

Confrontare:

- simboli promossi da DocBrown dopo floor economici;
- simboli effettivamente valutati in SHADOW `49`;
- motivi per cui non sono entrati in BUY.

## Sintesi

DocBrown aveva prodotto `30` advice attive su `8` simboli:

- `ALLOUSDC`;
- `CHIPUSDC`;
- `BREVUSDC`;
- `CRVUSDC`;
- `CFGUSDC`;
- `2ZUSDC`;
- `AIGENSYNUSDC`;
- `CUSDC`.

Tutti gli `8` simboli sono stati valutati da SHADOW `49`.
Quindi non e' vero che mancavano dal live universe.

Nessuno e' entrato in BUY.

## Confronto Per Simbolo

| Symbol | Rules | Evaluations | Main Reasons |
| --- | ---: | ---: | --- |
| `ALLOUSDC` | 5 | 104 | `REVERSAL_VOLUME_CONFIRMATION_LOW`, poi `REVERSAL_ML_RULE_MISSING`, poi `REVERSAL_DATA_COVERAGE_LOW` |
| `CHIPUSDC` | 5 | 104 | `REVERSAL_DATA_COVERAGE_LOW`, poi `REVERSAL_ML_RULE_MISSING` |
| `BREVUSDC` | 5 | 104 | `REVERSAL_DATA_COVERAGE_LOW`, poi `REVERSAL_ML_RULE_MISSING` |
| `CRVUSDC` | 1 | 104 | `REVERSAL_ML_RULE_MISSING`, poi qualche `REVERSAL_DATA_COVERAGE_LOW` |
| `CFGUSDC` | 5 | 104 | `REVERSAL_DATA_COVERAGE_LOW`, poi `REVERSAL_ML_RULE_MISSING` |
| `2ZUSDC` | 5 | 104 | `REVERSAL_DATA_COVERAGE_LOW`, poi `REVERSAL_ML_RULE_MISSING` |
| `AIGENSYNUSDC` | 3 | 104 | `REVERSAL_ML_RULE_MISSING` |
| `CUSDC` | 1 | 104 | `REVERSAL_DATA_COVERAGE_LOW`, poi `REVERSAL_ML_RULE_MISSING` |

Aggregato:

- promoted symbols: `8`;
- not evaluated: `0`;
- evaluated: `8`;
- accepted symbols: `0`;
- symbols with `REVERSAL_ML_RULE_MISSING`: `8`;
- symbols with `REVERSAL_DATA_COVERAGE_LOW`: `7`;
- symbols with `REVERSAL_VOLUME_CONFIRMATION_LOW`: `1`.

## Validita' Advice

Problema rilevato:

- le advice sono state salvate durante il job DocBrown;
- la validita' parte dal salvataggio di ogni singola rule;
- la SHADOW e' partita dopo la fine del mining;
- alcune advice avevano gia' poca validita' residua quando la SHADOW e' iniziata.

Secondi residui all'avvio SHADOW `49` (`2026-06-17 06:34:44 UTC`):

| Symbol | Rule | Seconds Left |
| --- | --- | ---: |
| `2ZUSDC` | `pullback_rebound_volume` | 28 |
| `2ZUSDC` | `curve_reversal` | 87 |
| `2ZUSDC` | `quality_structure` | 88 |
| `AIGENSYNUSDC` | `curve_reversal` | 107 |
| `AIGENSYNUSDC` | `pullback_rebound_volume` | 107 |
| `BREVUSDC` | most rules | 68-69 |
| `CFGUSDC` | `curve_reversal` | 37 |
| `CFGUSDC` | `acceleration_reversal` | 97 |
| `CHIPUSDC` | best rules | 239-359 |
| `ALLOUSDC` | best rules | 384-564 |
| `CRVUSDC` | only rule | 559 |
| `CUSDC` | only rule | 321 |

Quindi il dominio live e' stato valutato, ma molte advice erano quasi scadute o sono scadute durante l'osservazione.
Questo ha gonfiato `REVERSAL_ML_RULE_MISSING`.

## Qualita' Dato Live

Anche prima della scadenza, diversi simboli promossi non erano tradabili per qualita' dati:

- `ALLOUSDC`: volume confirmation insufficiente nei primi minuti;
- `CHIPUSDC`, `BREVUSDC`, `CFGUSDC`, `2ZUSDC`, `CUSDC`: coverage basso;
- `BREVUSDC`: snapshot live molto debole (`coverage 0.0887`, `distinct_price_points 1`);
- `CHIPUSDC`: `coverage 0.1479`, volume confirmation `0.2719`;
- `CRVUSDC`: `coverage 0.1637`, volume confirmation `0.7420`.

Questo conferma che i floor economici da soli non bastano: la advice puo' essere economicamente forte nello storico, ma non deve entrare se il dato live e' degradato.
ACDC ha bloccato correttamente questi casi.

## Lettura

Non e' un problema di universo: i simboli promossi sono stati visti.

Le cause del no-buy sono:

1. validita' advice consumata durante il mining;
2. rule range non matchati nel live appena la configurazione non era piu' fresca;
3. qualita' dati live sotto soglia su diversi simboli;
4. alcune promozioni con score negativo (`CUSDC`, parte di `AIGENSYNUSDC`) restano promosse dal miner per economia advice, ma ACDC le blocca con `entry_reversal_ml_score_positive`.

## Prossimo Intervento Consigliato

Correggere DocBrown:

- calcolare tutte le candidate;
- salvare/promuovere le rule usando un unico `validFrom` pari alla fine del job, non al momento di salvataggio di ciascuna rule;
- opzionale: non promuovere rule con score finale negativo, anche se superano i floor economici;
- mantenere ACDC invariato.

Questo evita che una SHADOW parta gia' con advice parzialmente scadute e rende il test piu' pulito.
