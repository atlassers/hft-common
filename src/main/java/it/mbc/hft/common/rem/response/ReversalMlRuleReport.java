package it.mbc.hft.common.rem.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record ReversalMlRuleReport(
        String profileKey,
        Instant generatedAt,
        int trainSamples,
        int validationSamples,
        int evaluatedRules,
        int promotedRules,
        List<RuleRow> rules
) {
    public record RuleRow(
            String scopeType,
            String scopeKey,
            String ruleKey,
            int featureCount,
            int trainSamples,
            int validationSamples,
            BigDecimal trainProfitRate,
            BigDecimal validationProfitRate,
            BigDecimal validationAvgNetReturn,
            int liveAuditSamples,
            BigDecimal liveAuditZeroMfeLossRate,
            BigDecimal liveAuditUnderSafeNonProfitRate,
            LocalDateTime adviceValidFrom,
            LocalDateTime adviceValidUntil,
            BigDecimal score,
            String status
    ) {
    }
}
