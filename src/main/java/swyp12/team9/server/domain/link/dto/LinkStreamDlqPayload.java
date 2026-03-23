package swyp12.team9.server.domain.link.dto;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record LinkStreamDlqPayload(
        String jobId,
        String linkId,
        String originalRecordId,
        String consumerName,
        String deliveryCount,
        String reason,
        String errorType,
        String stackSummary,
        String sourceStream,
        String recoveredBy,
        String firstSeenAt,
        String failedAt,
        String payload
) {

    public static LinkStreamDlqPayload of(
            Long linkId,
            String recordId,
            String consumerName,
            String payload,
            Long deliveryCount,
            String reason,
            String errorType,
            String stackSummary,
            String sourceStream,
            String recoveredBy
    ) {
        return new LinkStreamDlqPayload(
                linkId != null ? "link:" + linkId : "unknown",
                linkId != null ? String.valueOf(linkId) : "",
                recordId,
                consumerName,
                String.valueOf(deliveryCount),
                reason,
                errorType,
                stackSummary,
                sourceStream,
                recoveredBy,
                extractFirstSeenAt(recordId),
                Instant.now().toString(),
                payload
        );
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("jobId", jobId);
        map.put("linkId", linkId);
        map.put("originalRecordId", originalRecordId);
        map.put("consumerName", consumerName);
        map.put("deliveryCount", deliveryCount);
        map.put("reason", reason);
        map.put("errorType", errorType);
        map.put("stackSummary", stackSummary);
        map.put("sourceStream", sourceStream);
        map.put("recoveredBy", recoveredBy);
        map.put("firstSeenAt", firstSeenAt);
        map.put("failedAt", failedAt);
        map.put("payload", payload);
        return map;
    }

    private static String extractFirstSeenAt(String recordId) {
        try {
            String timestamp = recordId.split("-")[0];
            return Instant.ofEpochMilli(Long.parseLong(timestamp)).toString();
        } catch (Exception e) {
            return "";
        }
    }
}
