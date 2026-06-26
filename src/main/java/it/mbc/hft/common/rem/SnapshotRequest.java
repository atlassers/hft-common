package it.mbc.hft.common.rem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record SnapshotRequest(
        String symbol,
        BigDecimal price,
        BigDecimal quantity,
        Instant observedAt,
        Instant receivedAt,
        Map<String, BigDecimal> features) {
}
