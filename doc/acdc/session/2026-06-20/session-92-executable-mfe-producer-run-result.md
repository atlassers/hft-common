# Session 92 - Executable MFE Producer Run Result

Data: 2026-06-20.

## Scopo

Eseguire il prossimo giro fino alle 08:00 CEST dopo il refinement minimo lato producer DocBrown:

- non aggiungere nuove guardie BUY runtime;
- non introdurre filtri simbolo-specifici;
- pubblicare advice PAPER solo se il campione rolling supera requisiti minimi di MFE eseguibile;
- mantenere il test endpoint-driven e Docker-deployed.

## Modifiche gia' rilasciate prima della RUN

- DocBrown commit `15019f7`: publication gate basato su:
  - `executable_mfe_rate >= 0.70`;
  - `q10_positive_max_net_return >= 0.0005`;
  - `min_executable_entry_edge = 0.0005`.
- ACDC commit `953751a`: config permanenti V68.
- ACDC commit `b21d528`: checklist/deploy record.

## RUN

- Execution: `4`.
- Profilo: `REM_CURRENT`.
- Modalita': `PAPER`.
- Start: `2026-06-20T03:39:01Z` (`05:39:01` CEST).
- Stop: `2026-06-20T06:00:25Z` (`08:00:25` CEST).
- Stop: endpoint-driven, `stop-buy` + `stop`, entrambi HTTP 200.
- Loop: completato autonomamente alle `2026-06-20T06:00:08Z`.

## Rolling/Promotion

- Cicli rolling completati: `32`.
- PASS: `25`.
- FAIL selection bias: `7`.
- Promoted rows: `41`.
- Skip rilevante: `ETHFIUSDC:SKIPPED_NOT_PROMOTABLE`.

Il producer non e' sterile: ha pubblicato molte opportunita'. Il punto critico resta la qualita' delle opportunita' eseguite da ACDC e la capture in uscita.

## Risultato Economico

- Trade: `8`.
- Win/Loss: `5/3`.
- Net profit quote: `-0.05284424302425`.
- Current budget finale: `99.94715575697575`.
- Avg net return: `-0.000264221180917165`.
- Loss cap exits: `1`.
- Timeout exits: `2`.
- Dynamic trailing exits: `5`.

Trade:

| Symbol | Classification | Net return | Post-buy MFE | Capture | Exit |
| --- | --- | ---: | ---: | ---: | --- |
| RAYUSDC | BAD_ADVICE | `-0.005248780487804878` | `-0.015808130086260815` | `0` | `EXIT_ML_ADVICE_LOSS_CAP` |
| BABYUSDC | BAD_SELL | `0.001520084566596195` | `0.012703875968904727` | `0.11965518006606057` | `EXIT_ML_ADVICE_DYNAMIC_TRAILING` |
| ENSUSDC | BAD_SELL | `0.002233050847457627` | `0.007793220326183051` | `0.2865376255249961` | `EXIT_ML_ADVICE_DYNAMIC_TRAILING` |
| AIGENSYNUSDC | BAD_TARGET | `0.001067946257197697` | `0.001422648752285632` | `0.7506745818192517` | `EXIT_ML_ADVICE_DYNAMIC_TRAILING` |
| ENSUSDC | GOOD_FLOW | `0.002215189873417722` | `0.003529113878290628` | `0.6276901085693154` | `EXIT_ML_ADVICE_DYNAMIC_TRAILING` |
| ENSUSDC | BAD_SELL | `0.000098739495798319` | `0.001401680608268898` | `0.07044364830035292` | `EXIT_ML_ADVICE_DYNAMIC_TRAILING` |
| NEWTUSDC | BAD_ADVICE | `-0.002` | `-0.0028000000004736` | `0` | `EXIT_ML_ADVICE_TIMEOUT` |
| BBUSDC | BAD_ADVICE | `-0.002` | `-0.0028000000000448` | `0` | `EXIT_ML_ADVICE_TIMEOUT` |

## Forensics

- Target hit trades: `4/8`.
- Zero post-buy MFE: `3/8`.
- BAD_ADVICE: `3/8`.
- BAD_SELL: `3/8`.
- BAD_TARGET: `1/8`.
- GOOD_FLOW: `1/8`.
- Avg advice age seconds: `7.25`.
- Avg advice-to-buy net move: `-0.001551890134155596`.
- Avg post-advice max net return: `0.00190133071815442`.
- Avg post-buy max net return: `0.000680301180894216`.
- Avg post-buy end net return: `-0.001957961104551593`.
- Avg capture ratio: `0.2318751430349971`.

## Interpretazione Scientifica

Il refinement ha migliorato parzialmente il problema dominante della sessione 91, ma non basta:

- Sessione 91: `BAD_ADVICE 2/3`, zero-MFE `2/3`, net negativo.
- Sessione 92: `BAD_ADVICE 3/8`, zero-MFE `3/8`, net ancora negativo.

Quindi il producer gate riduce la frazione di advice completamente sbagliati, ma non garantisce profitto. La perdita netta e' generata da:

1. Residuo BAD_ADVICE: `RAYUSDC`, `NEWTUSDC`, `BBUSDC` non hanno prodotto MFE eseguibile dopo BUY.
2. Capture insufficiente: `BABYUSDC`, due `ENSUSDC` hanno avuto MFE post-buy ma SELL ha catturato troppo poco.
3. Target non calibrato: `AIGENSYNUSDC` aveva MFE reale, ma il target richiesto era troppo alto rispetto al movimento disponibile.

Il problema non e' un singolo bug del trailing. E' una combinazione:

- advice ancora non abbastanza causalmente robusto;
- target/safe-return non calibrato sulla distribuzione MFE realizzabile;
- trailing che si arma, ma non massimizza la capture nei casi con MFE ampio.

## Verdict

`FAIL_NET_NEGATIVE_PARTIAL_ADVICE_IMPROVEMENT`.

Il run non valida il modello come profittevole. Valida pero' che la linea del charter e' misurabile e sta isolando cause concrete: BAD_ADVICE residuo, BAD_SELL/capture, BAD_TARGET.

## Prossimo Refinement Ammesso

Senza introdurre guardie arbitrarie:

1. Calibrare `safeNetReturn` e target non su valore puntuale ottimistico, ma su quantile realizzabile post-buy della distribuzione rolling.
2. Rendere il trailing retention dipendente dal rapporto tra MFE atteso e MFE osservato:
   - retention piu' alta quando MFE osservato supera rapidamente l'executable edge;
   - break-even floor piu' aggressivo appena il trade supera fee+slippage;
   - timeout piu' breve per advice con MFE atteso basso e nessun progresso iniziale.
3. Aggiungere nel report producer-vs-executor una matrice:
   - advice pubblicato;
   - advice comprato;
   - advice scartato;
   - outcome post-advice anche quando non comprato.

Questo e' un refinement scientifico per calibrazione e misurazione, non una nuova guardia reattiva.
