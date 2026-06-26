package it.mbc.hft.common.rem;

public enum MlReadinessStatus {
    ML_READY("ML_READY"),
    NOT_READY("NOT_READY");

    private final String value;

    MlReadinessStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
