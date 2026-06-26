package it.mbc.hft.common.rem.response;

import it.mbc.hft.common.rem.constants.RemConstants;
import it.mbc.hft.common.rem.enums.DecisionAction;
import it.mbc.hft.common.rem.enums.GuardPhase;

import java.math.BigDecimal;
import java.util.Map;

public record DecisionResult(
        String symbol,
        GuardPhase phase,
        DecisionAction action,
        boolean accepted,
        String reason,
        String guardKey,
        Map<String, BigDecimal> features) {

    public static DecisionResult accepted(String symbol, GuardPhase phase, Map<String, BigDecimal> features) {
        return new DecisionResult(symbol, phase, actionForAccepted(phase), true, RemConstants.ACCEPTED, null, features);
    }

    public static DecisionResult accepted(String symbol, GuardPhase phase, String reason, String guardKey,
            Map<String, BigDecimal> features) {
        return new DecisionResult(symbol, phase, actionForAccepted(phase), true, reason, guardKey, features);
    }

    public static DecisionResult rejected(String symbol, GuardPhase phase, String reason, String guardKey,
            Map<String, BigDecimal> features) {
        return new DecisionResult(symbol, phase, DecisionAction.REJECT, false, reason, guardKey, features);
    }

    public static DecisionResult hold(String symbol, GuardPhase phase, String reason, Map<String, BigDecimal> features) {
        return new DecisionResult(symbol, phase, DecisionAction.HOLD, false, reason, null, features);
    }

    private static DecisionAction actionForAccepted(GuardPhase phase) {
        return phase == GuardPhase.EXIT ? DecisionAction.SELL : DecisionAction.BUY;
    }
}
