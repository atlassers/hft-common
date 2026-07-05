package it.mbc.hft.common.rem.model;

import java.math.BigDecimal;
import java.time.Instant;

public record InfluxTick(
        String symbol,
        Instant observedAt,
        BigDecimal price,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        boolean ohlcAvailable,
        boolean syntheticBackfill
) {
    public InfluxTick(String symbol, Instant observedAt, BigDecimal price, BigDecimal volume) {
        this(symbol, observedAt, price, volume, false);
    }

    public InfluxTick(String symbol, Instant observedAt, BigDecimal price, BigDecimal volume,
                      boolean syntheticBackfill) {
        this(symbol, observedAt, price, price, price, price, price, volume, false, syntheticBackfill);
    }

    public InfluxTick {
        if (close == null) {
            close = price;
        }
        if (price == null) {
            price = close;
        }
        if (open == null) {
            open = price;
        }
        if (high == null) {
            high = price;
        }
        if (low == null) {
            low = price;
        }
    }
}
