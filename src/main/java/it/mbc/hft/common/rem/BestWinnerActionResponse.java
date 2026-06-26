package it.mbc.hft.common.rem;

import java.time.Instant;

public record BestWinnerActionResponse(
        String script,
        String status,
        int exitCode,
        int timeoutSeconds,
        String logPath,
        String outputTail,
        String dataSource,
        Instant completedAt) {

    public static BestWinnerActionResponse ok(String script, String outputTail, String dataSource) {
        return new BestWinnerActionResponse(script, RemConstants.OK, 0, 0, "", outputTail, dataSource, Instant.now());
    }

    public static BestWinnerActionResponse failed(String script, String outputTail) {
        return new BestWinnerActionResponse(script, RemConstants.FAILED, 1, 0, "", outputTail, RemConstants.ACDC,
                Instant.now());
    }
}
