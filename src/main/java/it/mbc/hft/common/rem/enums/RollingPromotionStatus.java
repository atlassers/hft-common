package it.mbc.hft.common.rem.enums;

public enum RollingPromotionStatus {
    SKIPPED_NOT_PROMOTABLE("SKIPPED_NOT_PROMOTABLE"),
    SKIPPED_LIVE_REVALIDATION_CONTRACT("SKIPPED_LIVE_REVALIDATION_CONTRACT"),
    PROMOTED("PROMOTED");

    private final String value;

    RollingPromotionStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
