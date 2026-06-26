package it.mbc.hft.common.management;

public enum ManagementRuntimeStatus {
    ML_READY("ML_READY"),
    NOT_READY("NOT_READY"),
    AUTO_PREFLIGHT_NOT_READY("AUTO_PREFLIGHT_NOT_READY"),
    BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE("BLOCKED_WAITING_PAPER_ELIGIBLE_ADVICE"),
    FAIL_SELECTION_BIAS("FAIL_SELECTION_BIAS"),
    NO_BATCH("NO_BATCH"),
    PROMOTION_NO_ADVICE("PROMOTION_NO_ADVICE"),
    NO_PROMOTABLE_CANDIDATE("NO_PROMOTABLE_CANDIDATE"),
    SKIPPED_LIVE_REVALIDATION_CONTRACT("SKIPPED_LIVE_REVALIDATION_CONTRACT");

    private final String value;

    ManagementRuntimeStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
