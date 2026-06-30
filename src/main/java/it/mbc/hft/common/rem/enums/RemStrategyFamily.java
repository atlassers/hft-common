package it.mbc.hft.common.rem.enums;

public enum RemStrategyFamily {
    BOLLINGER_ONLY("BOLLINGER_ONLY"),
    BOLLINGER_ONLY_V2("BOLLINGER_ONLY_V2"),
    BOLLINGER_CONTEXT_V1("BOLLINGER_CONTEXT_V1");

    private final String value;

    RemStrategyFamily(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean is(String raw) {
        return value.equals(raw);
    }
}
