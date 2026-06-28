package it.mbc.hft.common.management.enums;

public enum ManagementActionId {
    AUTO_BOLLINGER_START("AUTO_BOLLINGER_START"),
    AUTO_BOLLINGER_STOP("AUTO_BOLLINGER_STOP"),
    AUTO_BOLLINGER_CYCLE_START("AUTO_BOLLINGER_CYCLE_START"),
    UNIVERSE_PREFILTER("UNIVERSE_PREFILTER"),
    RUN_RESEARCH("RUN_RESEARCH"),
    LIVE_SCORE("LIVE_SCORE"),
    ROLLING_VALIDATION("ROLLING_VALIDATION"),
    ROLLING_PROMOTION("ROLLING_PROMOTION"),
    PAPER_BOLLINGER_START("PAPER_BOLLINGER_START"),
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
