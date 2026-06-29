package it.mbc.hft.common.rem.enums;

public enum BbReadinessStatus {
    BB_READY("BB_READY"),
    NOT_READY("NOT_READY");

    private final String value;

    BbReadinessStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
