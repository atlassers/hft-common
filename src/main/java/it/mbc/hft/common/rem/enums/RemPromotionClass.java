package it.mbc.hft.common.rem.enums;

public enum RemPromotionClass {
    PAPER_ELIGIBLE("PAPER_ELIGIBLE"),
    PURE_REVERSAL_OBSERVED("PURE_REVERSAL_OBSERVED");

    private final String value;

    RemPromotionClass(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
