package it.mbc.hft.common.rem.enums;

public enum RemMarketRegime {
    REGIME_TREND_UP("REGIME_TREND_UP"),
    REGIME_TREND_DOWN("REGIME_TREND_DOWN"),
    REGIME_RANGE("REGIME_RANGE"),
    REGIME_SQUEEZE("REGIME_SQUEEZE"),
    REGIME_EXPANSION("REGIME_EXPANSION"),
    REGIME_CHAOS("REGIME_CHAOS");

    private final String value;

    RemMarketRegime(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
