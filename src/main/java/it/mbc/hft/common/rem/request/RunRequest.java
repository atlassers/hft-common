package it.mbc.hft.common.rem.request;

import java.util.List;
import java.util.Map;

public record RunRequest(
        String executionMode,
        List<SnapshotRequest> snapshots,
        List<String> symbols,
        String validationProtocol,
        String baselineReferenceSession,
        String comparisonArm,
        String sourceBatchId,
        String experimentTrigger,
        Map<String, Object> technicalOptions) {

    public RunRequest(String executionMode, List<SnapshotRequest> snapshots) {
        this(executionMode, snapshots, List.of(), null, null, null, null, null, Map.of());
    }

    public RunRequest(String executionMode, List<SnapshotRequest> snapshots, List<String> symbols) {
        this(executionMode, snapshots, symbols, null, null, null, null, null, Map.of());
    }
}
