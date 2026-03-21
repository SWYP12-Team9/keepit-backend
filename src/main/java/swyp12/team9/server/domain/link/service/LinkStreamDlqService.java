package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.link.dto.LinkStreamDlqPayload;
import swyp12.team9.server.domain.link.infrastructure.DiscordWebhookGateway;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessDlqGateway;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkStreamDlqService {

    private final LinkProcessDlqGateway linkProcessDlqGateway;
    private final DiscordWebhookGateway discordWebhookGateway;

    public void moveToDlq(
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
        LinkStreamDlqPayload dlqPayload = LinkStreamDlqPayload.of(
                linkId,
                recordId,
                consumerName,
                payload,
                deliveryCount,
                reason,
                errorType,
                stackSummary,
                sourceStream,
                recoveredBy
        );

        RecordId dlqRecordId = linkProcessDlqGateway.sendToDlq(dlqPayload);

        log.error("Redis Stream DLQ 적재 - originalRecordId: {}, dlqRecordId: {}, deliveryCount: {}, reason: {}",
                recordId, dlqRecordId, deliveryCount, reason);

        try {
            discordWebhookGateway.send(buildDiscordContent(
                    linkId,
                    recordId,
                    consumerName,
                    payload,
                    deliveryCount,
                    reason,
                    errorType,
                    sourceStream
            ));
        } catch (Exception e) {
            // Discord 알림 장애 시 무한 반복 방지
            log.warn("Discord 알림 실패 (DLQ 적재는 완료) - recordId: {}", recordId, e);
        }
    }

    private String buildDiscordContent(
            Long linkId,
            String recordId,
            String consumerName,
            String payload,
            Long deliveryCount,
            String reason,
            String errorType,
            String sourceStream
    ) {
        return """
                [KEEPIT] Redis Stream DLQ 적재
                - linkId: %s
                - recordId: %s
                - consumer: %s
                - deliveryCount: %d
                - reason: %s
                - errorType: %s
                - sourceStream: %s
                - payload: %s
                """.formatted(linkId, recordId, consumerName, deliveryCount, reason, errorType, sourceStream, payload);
    }
}
