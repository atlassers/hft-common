package it.mbc.hft.common.rem;

public enum RemCandidateStatus {
    PASS_CANDIDATE("PASS_CANDIDATE"),
    FAIL_SELECTION_BIAS("FAIL_SELECTION_BIAS"),
    FAIL_HOLDOUT("FAIL_HOLDOUT"),
    INCONCLUSIVE("INCONCLUSIVE");

    private final String value;

    RemCandidateStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
