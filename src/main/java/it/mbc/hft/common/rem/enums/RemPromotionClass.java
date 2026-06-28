package it.mbc.hft.common.rem.enums;

public enum RemPromotionClass {
    PAPER_ELIGIBLE("PAPER_ELIGIBLE");

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
