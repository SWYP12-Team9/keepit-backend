package swyp12.team9.server.domain.link.service;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.link.exception.LinkStreamPayloadMetadataMissingException;
import swyp12.team9.server.domain.link.exception.LinkStreamRetryLimitExceededException;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkStreamRecoveryService {

    private static final String RECOVERY_CONSUMER_NAME = "recovery-worker";

    private final LinkProcessStreamGateway linkProcessStreamGateway;
    private final LinkStreamConsumer linkStreamConsumer;

    @Value("${link.stream.recovery.enabled:true}")
    private boolean enabled;

    @Value("${link.stream.recovery.min-idle-seconds:30}")
    private long minIdleSeconds;

    @Value("${link.stream.recovery.batch-size:20}")
    private long batchSize;

    @Value("${link.stream.recovery.max-delivery-count:5}")
    private long maxDeliveryCount;

    @Value("${link.stream.processing-state-ttl-minutes:30}")
    private long processingStateTtlMinutes;

    @Value("${link.stream.recovery.backoff.enabled:true}")
    private boolean backoffEnabled;

    @Value("${link.stream.recovery.backoff.base-seconds:30}")
    private long backoffBaseSeconds;

    @Value("${link.stream.recovery.backoff.multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${link.stream.recovery.backoff.max-delay-seconds:300}")
    private long backoffMaxDelaySeconds;

    // ACK 없이 남아 있는 pending 메시지를 주기적으로 다시 집어 와서 재처리하거나 DLQ로 넘긴다.
    @Scheduled(fixedDelayString = "${link.stream.recovery.fixed-delay-ms:10000}")
    public void recoverPendingMessages() {
        if (!enabled) {
            return;
        }

        List<LinkProcessStreamGateway.PendingEntry> pendingEntries = linkProcessStreamGateway.findPendingEntries(batchSize);
        if (pendingEntries.isEmpty()) {
            return;
        }

        for (LinkProcessStreamGateway.PendingEntry pendingEntry : pendingEntries) {
            try {
                Duration requiredIdle = calculateRequiredIdle(pendingEntry.deliveryCount());
                if (pendingEntry.idleTime().compareTo(requiredIdle) < 0) {
                    log.debug("Pending 메시지 backoff 대기 중 - recordId: {}, deliveryCount: {}, idle: {}s, required: {}s",
                            pendingEntry.recordId(),
                            pendingEntry.deliveryCount(),
                            pendingEntry.idleTime().toSeconds(),
                            requiredIdle.toSeconds());
                    continue;
                }

                if (!linkProcessStreamGateway.claimPendingEntry(RECOVERY_CONSUMER_NAME, requiredIdle, pendingEntry.recordId())) {
                    continue;
                }

                recoverPendingMessage(pendingEntry);
            } catch (Exception e) {
                log.error("Pending 메시지 복구 실패 - recordId: {}, error: {}",
                        pendingEntry.recordId(), e.getMessage(), e);
            }
        }
    }

    // Recovery worker가 가져온 메시지를 다시 실행한다. 재시도 한도를 넘기면 여기서 영구 실패 처리한다.
    private void recoverPendingMessage(LinkProcessStreamGateway.PendingEntry pendingEntry) {
        String recordId = pendingEntry.recordId();
        String consumerName = pendingEntry.consumerName();
        long deliveryCount = pendingEntry.deliveryCount() + 1;

        String payload = linkProcessStreamGateway.readMessageById(recordId);

        if (payload == null) {
            Long linkId = linkProcessStreamGateway.readBackupLinkId(recordId);
            log.warn("Pending 메시지 payload 조회 실패 - recordId: {}, linkId: {}", recordId, linkId);

            linkStreamConsumer.handlePermanentFailure(
                    recordId,
                    "",
                    linkId,
                    consumerName,
                    deliveryCount,
                    new LinkStreamPayloadMetadataMissingException()
            );
            return;
        }

        Long linkId = extractLinkId(payload);

        if (deliveryCount >= maxDeliveryCount) {
            linkStreamConsumer.handlePermanentFailure(
                    recordId,
                    payload,
                    linkId,
                    consumerName,
                    deliveryCount,
                    new LinkStreamRetryLimitExceededException()
            );
            return;
        }

        log.info("Pending 메시지 재처리 시작 - recordId: {}, previousConsumer: {}, deliveryCount: {}",
                recordId, consumerName, deliveryCount);

        touchProcessingState(linkId, recordId);
        linkStreamConsumer.consumeMessage(recordId, payload);
    }

    private Long extractLinkId(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(payload.split("\\|", 2)[0]);
        } catch (Exception e) {
            return null;
        }
    }

    private void touchProcessingState(Long linkId, String recordId) {
        if (linkId == null) {
            return;
        }

        linkProcessStreamGateway.touchProcessingState(
                linkId,
                recordId,
                Duration.ofMinutes(processingStateTtlMinutes)
        );
    }

    private Duration calculateRequiredIdle(long deliveryCount) {
        if (!backoffEnabled) {
            return Duration.ofSeconds(minIdleSeconds);
        }

        if (deliveryCount <= 0) {
            return Duration.ofSeconds(backoffBaseSeconds);
        }

        double exponent = Math.max(0, deliveryCount - 1);
        double calculatedSeconds = backoffBaseSeconds * Math.pow(backoffMultiplier, exponent);
        long boundedSeconds = Math.min(backoffMaxDelaySeconds, Math.max(minIdleSeconds, (long) calculatedSeconds));
        return Duration.ofSeconds(boundedSeconds);
    }
}
