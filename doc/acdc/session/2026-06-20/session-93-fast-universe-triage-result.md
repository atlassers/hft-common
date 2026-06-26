# Session 93 - Fast Universe Triage Result

Data: 2026-06-20.

## Obiettivo

Valutare se un pre-filtro conservativo puo' ridurre l'universo REM da circa 288 simboli senza perdere opportunita' reversal utili.

## Implementazione

DocBrown espone ora:

`POST /docbrown/rem/blank-candidates/{profileKey}/universe-triage/shadow`

Caratteristiche:

- shadow-only;
- nessun advice promosso;
- nessun effetto su BUY/SELL;
- report per simbolo;
- misura `missedUsefulSymbols`, `missedExecutableMfeSymbols`, `missedProfitableSymbols`;
- decisione outcome-free dopo fix metodologico.

Commits:

- `35a5811` - endpoint shadow universe triage.
- `2815539` - rispetto effettivo di `symbolLimit`.
- `97842a1` - decisioni triage outcome-free.
- `2d1a100` - default conservativi.

## Correzione Scientifica Importante

La prima versione del test non era accettabile: la decisione di KEEP usava l'outcome futuro per non scartare simboli utili. Questo rendeva i falsi negativi artificialmente zero.

La logica e' stata corretta:

- decisione DROP/KEEP basata solo su dati osservabili prima dell'outcome:
  - tick count;
  - distinct price points;
  - range netto recente;
- outcome MFE/end-return usato solo per valutare falsi negativi shadow.

## Test Endpoint-Driven

### Variante A: `minTick=8`, `distinct>=2`, `range>=0.0005`

Risultato:

- 2 finestre PASS;
- 2 finestre FAIL;
- una finestra con `missedUsefulSymbols=34`, `usefulFalseNegativeRate=0.7906976744186046`;
- causa principale: `LOW_TICK_COUNT`.

Verdetto: FAIL.

### Variante B: `minTick=2`, `distinct>=2`, `range>=0.0005`

Risultato:

- riduce l'universo;
- restano falsi negativi;
- cause: `LOW_DISTINCT_PRICE_POINTS` e `LOW_RECENT_RANGE_AFTER_COSTS`.

Verdetto: FAIL.

### Variante C: `minTick=1`, `distinct>=1`, `range=0`

Risultato:

- `missedUsefulSymbols=0`;
- `missedExecutableMfeSymbols=0`;
- `reductionRatio=0`.

Verdetto: INCONCLUSIVE_NO_REDUCTION.

### Variante D: `minTick=1`, `distinct>=2`, `range=0`

Risultato:

- 3 finestre PASS;
- 1 finestra FAIL;
- nella finestra FAIL: `missedUsefulSymbols=6`, `missedExecutableMfeSymbols=6`, `missedProfitableSymbols=5`;
- reduction circa 8-26% nelle finestre testate.

Verdetto: FAIL.

## Conclusione

Il triage e' promettente come strumento diagnostico, ma non e' validato come filtro operativo.

I criteri che riducono davvero l'universo possono perdere reversal utili. I criteri che non perdono opportunita' non riducono l'universo. Quindi il piano dei tre scienziati resta corretto, ma questa specifica metrica strutturale non supera la validazione.

## Decisione Charter

Non aggiornata come vincolo operativo/enforced.

Motivo: il criterio non e' validato. Inserirlo nel charter ora violerebbe il piano scientifico, perche' ci sono falsi negativi misurati.

## Prossimo Step Scientificamente Ammesso

Il prossimo tentativo non deve usare `range recente` o `distinct price` come esclusione secca. Possibili direzioni:

1. triage solo per costo computazionale adattivo, non DROP definitivo:
   - simboli sospetti analizzati con cadenza ridotta;
   - full scan periodico obbligatorio;
   - nessuna esclusione totale.
2. ranking di scheduling:
   - processare prima simboli piu' liquidi/mobili;
   - processare comunque tutti entro N cicli;
   - misurare advice age migliorato senza falsi negativi strutturali.
3. caching incrementale:
   - non ridurre universo;
   - ridurre tempo evitando ricalcolo completo per simboli invariati.

Verdetto finale: `FAIL_NOT_VALIDATED_FOR_ENFORCEMENT`, `PASS_DIAGNOSTIC_ENDPOINT_AVAILABLE`.
