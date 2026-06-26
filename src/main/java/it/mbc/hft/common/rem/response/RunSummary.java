package it.mbc.hft.common.rem.response;

import java.math.BigDecimal;
import java.util.List;

public record RunSummary(
        Long runId,
        Long executionId,
        String profileKey,
        String type,
        String executionMode,
        String dataSource,
        BigDecimal initialBudget,
        BigDecimal currentBudget,
        BigDecimal reservedBudget,
        BigDecimal realizedProfitQuote,
        BigDecimal dailyBudget,
        int evaluated,
        int accepted,
        int rejected,
        int opened,
        int closed,
        BigDecimal netProfitQuote,
        List<DecisionResult> decisions) {
}
