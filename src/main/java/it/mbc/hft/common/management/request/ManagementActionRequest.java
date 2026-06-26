package it.mbc.hft.common.management.request;

import java.util.Map;

public record ManagementActionRequest(
        String profileKey,
        Map<String, Object> payload,
        Boolean dryRun) {
}
