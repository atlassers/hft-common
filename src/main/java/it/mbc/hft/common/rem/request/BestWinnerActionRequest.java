package it.mbc.hft.common.rem.request;

public record BestWinnerActionRequest(
        String executionMode,
        Boolean allowRealRun) {
}
