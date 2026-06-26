package it.mbc.hft.common.rem.model;

import it.mbc.hft.common.rem.constants.RemConstants;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record FeatureSnapshot(
        String symbol,
        BigDecimal price,
        BigDecimal quantity,
        Instant observedAt,
        Instant receivedAt,
        Map<String, BigDecimal> features
) {
    public Map<String, BigDecimal> normalizedFeatures(Instant now) {
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        if (features != null) {
            normalized.putAll(features);
        }
        if (price != null) {
            normalized.put(RemConstants.PRICE, price);
        }
        normalized.put(RemConstants.QUANTITY, quantity == null ? BigDecimal.ONE : quantity);
        if (receivedAt != null) {
            long ageSeconds = Math.max(0, Duration.between(receivedAt, now).toSeconds());
            normalized.put(RemConstants.SNAPSHOT_AGE_SECONDS, BigDecimal.valueOf(ageSeconds));
        }
        return normalized;
    }
}
