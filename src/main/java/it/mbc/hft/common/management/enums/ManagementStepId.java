package it.mbc.hft.common.management.enums;

public enum ManagementStepId {
    AUTO_PREFILTER("auto-prefilter"),
    AUTO_LIVE_SCORE("auto-live-score"),
    AUTO_VALIDATION("auto-validation"),
    AUTO_PROMOTION("auto-promotion"),
    AUTO_PAPER_START("auto-paper-start"),
    RUN_MONITOR("run-monitor"),
    ML_PREFILTER("ml-prefilter"),
    ML_HEAVY_RESEARCH("ml-heavy-research"),
    ML_ROUND_ROBIN("ml-round-robin"),
    PROMOTION("promotion"),
    WATCH("watch"),
    PAPER("paper"),
    REAL("real");

    private final String value;

    ManagementStepId(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
