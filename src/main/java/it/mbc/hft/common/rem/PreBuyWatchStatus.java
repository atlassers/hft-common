package it.mbc.hft.common.rem;

public enum PreBuyWatchStatus {
    WATCHING("WATCHING"),
    CONFIRMED("CONFIRMED"),
    BUY_OPENED("BUY_OPENED"),
    BUY_REJECTED_RUNTIME("BUY_REJECTED_RUNTIME"),
    EXPIRED("EXPIRED"),
    INVALIDATED("INVALIDATED"),
    ABANDONED("ABANDONED");

    private final String value;

    PreBuyWatchStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
