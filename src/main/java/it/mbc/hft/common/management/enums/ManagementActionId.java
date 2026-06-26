package it.mbc.hft.common.management.enums;

public enum ManagementActionId {
    AUTO_AB_START("AUTO_AB_START"),
    AUTO_AB_STOP("AUTO_AB_STOP"),
    AUTO_FORWARD_AB_CYCLE_START("AUTO_FORWARD_AB_CYCLE_START"),
    UNIVERSE_PREFILTER("UNIVERSE_PREFILTER"),
    RUN_RESEARCH("RUN_RESEARCH"),
    ENABLE_SIGNATURE_ADVICE("ENABLE_SIGNATURE_ADVICE"),
    LIVE_SCORE("LIVE_SCORE"),
    RESTORE_BASELINE_98_PROFILE("RESTORE_BASELINE_98_PROFILE"),
    ENABLE_PAPER_CANDIDATES("ENABLE_PAPER_CANDIDATES"),
    ROLLING_VALIDATION("ROLLING_VALIDATION"),
    ROLLING_PROMOTION("ROLLING_PROMOTION"),
    SHADOW_RUN("SHADOW_RUN"),
    PAPER_FORWARD_AB_START("PAPER_FORWARD_AB_START"),
    SAVE_MANAGEMENT_CONFIG("SAVE_MANAGEMENT_CONFIG");

    private final String value;

    ManagementActionId(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
