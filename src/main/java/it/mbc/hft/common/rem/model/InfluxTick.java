package it.mbc.hft.common.rem.model;

import java.math.BigDecimal;
import java.time.Instant;

public record InfluxTick(
        String symbol,
        Instant observedAt,
        BigDecimal price,
        BigDecimal volume
) {
}
