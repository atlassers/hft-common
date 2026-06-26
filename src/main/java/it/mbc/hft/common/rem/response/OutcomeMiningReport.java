package it.mbc.hft.common.rem.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OutcomeMiningReport(
        String profileKey,
        Instant generatedAt,
        Instant start,
        Instant stop,
        int horizonSeconds,
        int symbols,
        int scannedPoints,
        int samplesCreated,
        int samplesUpdated,
        int skipped,
        int good,
        int bad,
        int neutral,
        BigDecimal averageMaxNetReturn,
        BigDecimal averageEndNetReturn,
        int promotedSignatures,
        List<OutcomeSignatureRow> signatures
) {
    public record OutcomeSignatureRow(
            String scopeType,
            String scopeKey,
            String featureKey,
            BigDecimal minValue,
            BigDecimal maxValue,
            int trainSamples,
            int validationSamples,
            BigDecimal trainProfitRate,
            BigDecimal validationProfitRate,
            BigDecimal validationAvgNetReturn,
            String status
    ) {
    }
}
