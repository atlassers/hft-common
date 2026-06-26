package it.mbc.hft.common.management;

public record ManagementActionResponse(
        String action,
        String status,
        String message,
        Integer downstreamStatus,
        Object result,
        Object state) {
}
