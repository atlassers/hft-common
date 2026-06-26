package it.mbc.hft.common.management;

import java.util.Map;

public record ManagementActionRequest(
        String profileKey,
        Map<String, Object> payload,
        Boolean dryRun) {
}
