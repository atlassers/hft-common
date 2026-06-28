package it.mbc.hft.common.rem.enums;

public enum BollingerSetupType {
    BB_REENTRY_MEAN_REVERSION_LONG("BB_REENTRY_MEAN_REVERSION_LONG"),
    BB_SQUEEZE_BREAKOUT_LONG("BB_SQUEEZE_BREAKOUT_LONG");

    private final String value;

    BollingerSetupType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
