package it.mbc.hft.common.management.response;

public record ManagementActionResponse(
        String action,
        String status,
        String message,
        Integer downstreamStatus,
        Object result,
        Object state) {
}
