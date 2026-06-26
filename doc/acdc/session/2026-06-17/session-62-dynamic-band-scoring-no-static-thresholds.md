# Session 62 - Dynamic Band Scoring Without Static Thresholds

Data: 2026-06-17

## Obiettivo

Rimuovere il rischio di reintrodurre soglie statiche nella data quality REM e lasciare a DocBrown la responsabilita' di escludere segnali spigolosi tramite scoring outcome-driven dinamico.

## Decisione

Non sono state aggiunte nuove chiavi DB di soglia.

La correzione e' nel band discovery DocBrown:

- ogni ciclo ML genera candidate band dai dati correnti;
- ogni candidate viene valutata su validation temporale;
- la candidate vincente non massimizza piu' solo recall/avg MFE;
- lo score ora premia precisione, MFE netto e discriminazione GOOD/BAD;
- lo score penalizza in modo continuo:
  - band che passano troppi campioni;
  - band di coverage troppo larghe;
  - band con `distinct_price_points` minimo troppo basso.

Formula documentata nei metadati band:

```text
2*precision + 2*(good_recall-bad_recall) + 100*avg_mfe - pass_coverage - coverage_width - 1/distinct_min
```

Questa non e' una soglia operativa: e' una funzione obiettivo. I valori di `coverage`, `distinct`, `gap` e volume restano feature candidate del modello, non guardie hardcoded ACDC.

## Stato Runtime

ACDC non e' stato modificato in questa sessione.

Le guardie ENTRY statiche di qualita' restano disabilitate; PAPER deve continuare a consumare solo advice `PAPER_ELIGIBLE`.

Il prossimo passo resta salvare nella posizione lo snapshot completo dell'advice usata:

- rule id;
- research batch id;
- advice valid from/until;
- advice age at buy;
- promotion class;
- band model version;
- scope/rule key;
- hash o versione del rule json.

## Verifica

- DocBrown `./mvnw -q -DskipTests compile`: OK.
- DocBrown `./mvnw -q test`: avviato durante la sessione.

