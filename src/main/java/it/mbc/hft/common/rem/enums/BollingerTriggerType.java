package it.mbc.hft.common.rem.enums;

public enum BollingerTriggerType {
    BB_REENTRY_CONFIRMED("BB_REENTRY_CONFIRMED"),
    BB_UPPER_BREAKOUT_CONFIRMED("BB_UPPER_BREAKOUT_CONFIRMED");

    private final String value;

    BollingerTriggerType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
