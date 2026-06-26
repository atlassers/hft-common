# Sessione 7 - Emergency Edge Slice Review

## Piano 1 - Verifica Rapida Quadra REAL

Data: 2026-06-08.

Obiettivo: cercare una via difendibile entro tempi stretti dopo il refit esteso non promuovibile.

Azioni:

- Aggiunto `find_candidate_edge_slices.py`.
- Lo scanner legge in streaming `scalping_scout_candidate_sell_policy_outcome`.
- Aggrega per `symbol+policy`, separando train/test temporale con cutoff 60%.
- Cerca slice robuste con train e test entrambi net-positive.
- Cerca anche slice fragili net-positive e slice gross-positive per capire se i costi stanno uccidendo un edge lordo.

Risultati dataset 18:

- Righe outcome lette: `7496100`.
- Slice `symbol+policy`: `85800`.
- Policy: `300`.
- Slice robuste net-positive train/test: `0`.
- Slice fragili net-positive complessive: `0`.
- Slice gross-positive: `24588`.
- Migliore gross-positive: `BANKUSDC pt=0.006|sl=0.008|hold=15|grace=2`, gross `+0.53817540`, net `-0.09682460`, train net `-0.24631312`, test net `+0.14948852`.

Interpretazione:

- Esiste movimento lordo in alcune slice, ma non resta edge netto stabile dopo fee/slippage.
- `BANKUSDC` e' solo un indizio recente/regime, non una candidata REAL perche' il train precedente e' negativo.

Verifica corrente:

- Probe mirato corrente `BANKUSDC` 24h/6h: `PAPER_WAIT`, `evaluatedRows=0`.
- Probe corrente 300 simboli USDC 24h/6h: `PAPER_WAIT`, `evaluatedRows=0`, `symbolsWithTrades=0`.

Decisione:

- Nessuna REAL RUN.
- La strategia attuale deve restare fail-closed.
- Prossimo sviluppo utile: cambiare il segnale o ridurre drasticamente il modello costi/slippage verificando prima dati reali Binance, non cercare altre soglie sullo stesso segnale.

## Piano 2 - Soglia Alternativa Sul Costo Netto

Data: 2026-06-08.

Obiettivo: provare una soglia diversa per capire se il problema e' il segnale o il costo di esecuzione.

Modifica:

- `find_candidate_edge_slices.py` supporta `--net-cost-rate`.
- Con questa opzione il netto viene ricalcolato come `gross_return - net_cost_rate`.
- La scansione resta identica: stesso dataset, stesso split train/test, stessi minimi `min-train-trades=10`, `min-test-trades=10`, `min-profit-factor=1.1`.

Risultati:

- Costo `0.003`: robuste `0`, fragili positive `47`.
- Costo `0.002`: robuste `0`, fragili positive `268`.
- Costo `0.0015`: robuste `112`, fragili positive `585`.
- Costo `0.001`: robuste `372`, fragili positive `1757`.

Prima slice robusta a costo `0.0015`:

- `SAHARAUSDC pt=0.008|sl=0.003|hold=20|grace=2`.
- Trade: `130`.
- All net: `+0.13315089`, PF `1.3526`.
- Train net: `+0.06268828`, PF `1.2605`.
- Test net: `+0.07046261`, PF `1.5144`.

Conclusione:

- La soglia critica e' tra `0.0015` e `0.002` di costo totale per trade simulato.
- Con il costo attuale implicito circa `0.005` non esiste robustezza.
- Il progetto non e' necessariamente morto, ma la REAL e' difendibile solo se si dimostra con dati reali che fee+slippage effettivo stanno sotto circa `0.0015`; altrimenti il segnale resta non tradabile.
