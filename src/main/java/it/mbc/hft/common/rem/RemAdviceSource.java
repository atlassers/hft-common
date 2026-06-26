package it.mbc.hft.common.rem;

public enum RemAdviceSource {
    PROMOTED_RULE("PROMOTED_RULE"),
    LIVE_SCORE_ONLY("LIVE_SCORE_ONLY"),
    ROLLING_PAPER("ROLLING_PAPER");

    private final String value;

    RemAdviceSource(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
