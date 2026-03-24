package swyp12.team9.server.domain.link.service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.event.LinkCompletedEvent;
import swyp12.team9.server.domain.link.event.LinkFailedEvent;
import swyp12.team9.server.domain.link.exception.LinkHashGenerationException;
import swyp12.team9.server.domain.link.exception.LinkProcessingException;
import swyp12.team9.server.domain.link.exception.LinkScrapingServerException;
import swyp12.team9.server.domain.link.exception.LinkScrapingTimeoutException;
import swyp12.team9.server.domain.link.exception.LinkStreamInvalidMessageException;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.global.exception.BusinessException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkStreamConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final LinkSaveService linkSaveService;
    private final ScrapingService scrapingService;
    private final LinkAiService linkAiService;
    private final LinkProcessStreamGateway linkProcessStreamGateway;
    private final ApplicationEventPublisher eventPublisher;
    private final LinkStreamDlqService linkStreamDlqService;

    @Value("${link.stream.processing-state-ttl-minutes:30}")
    private long processingStateTtlMinutes;

    private final Semaphore scrapingSemaphore = new Semaphore(5);
    private final Semaphore aiSemaphore = new Semaphore(8);

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        consumeMessage(message.getId().getValue(), message.getValue());
    }

    // Live consumer의 메인 진입점. 성공 시 ACK, 재시도 대상이면 pending 유지, 영구 실패면 DLQ로 보낸다.
    public void consumeMessage(String recordId, String payload) {
        Long linkId = null;

        try {
            // stream payload는 "{linkId}|{url}" 규약을 전제로 한다.
            String[] parts = payload.split("\\|", 2);
            if (parts.length < 2) {
                handlePermanentFailure(
                        recordId,
                        payload,
                        null,
                        "unknown",
                        1L,
                        new LinkStreamInvalidMessageException()
                );
                return;
            }

            linkId = Long.parseLong(parts[0]);
            String url = parts[1];

            log.info("Redis Stream 메시지 수신 - recordId: {}, linkId: {}", recordId, linkId);
            touchProcessingState(linkId, recordId);

            processUrl(linkId, url, recordId);
            linkProcessStreamGateway.ack(recordId);
        } catch (Exception e) {
            log.error("Redis Stream 소비 중 에러 발생 - recordId: {}, error: {}", recordId, e.getMessage(), e);
            FailureCategory failureCategory = classifyFailure(e);

            if (failureCategory == FailureCategory.RETRYABLE) {
                log.warn("재시도 대상 메시지 - recordId: {}, linkId: {}, errorType: {}",
                        recordId, linkId, e.getClass().getSimpleName());
                return;
            }

            handlePermanentFailure(
                    recordId,
                    payload,
                    linkId,
                    "live-consumer",
                    1L,
                    e
            );
        }
    }

    // Stream payload를 실제 링크 데이터로 바꾸는 단계다: 스크래핑 -> 필요 시 AI 요약 -> DB 반영 -> 대기 유저 SSE 발송.
    private void processUrl(Long linkId, String url, String recordId) {
        try {
            scrapingSemaphore.acquire();
            ScrapingResponse scrapingData;
            try {
                scrapingData = scrapingService.scrapeUrl(url, 500);
            } finally {
                scrapingSemaphore.release();
            }
            touchProcessingState(linkId, recordId);

            Link existingLink = linkSaveService.findById(linkId);
            boolean scrapedDataChanged = existingLink.isScrapedDataChanged(
                    scrapingData.getTitle(),
                    scrapingData.getDescription(),
                    scrapingData.getFaviconUrl(),
                    scrapingData.getContent()
            );
            boolean shouldPersist = shouldPersist(existingLink, scrapedDataChanged);
            String aiSummary = existingLink.getAiSummary();

            // READY 링크라도 스크래핑 결과가 달라졌거나 요약이 비어 있으면 AI 요약을 다시 생성한다.
            if (shouldGenerateSummary(existingLink, scrapingData, scrapedDataChanged)) {
                log.info("AI 요약 생성 또는 갱신 필요 - linkId: {}", linkId);

                aiSemaphore.acquire();
                try {
                    aiSummary = linkAiService.summarizeLink(
                            scrapingData.getTitle(),
                            scrapingData.getDescription(),
                            scrapingData.getContent()
                    );
                } finally {
                    aiSemaphore.release();
                }
                touchProcessingState(linkId, recordId);
            }

            String title;
            if (shouldPersist) {
                // complete()에서 스크래핑 필드와 aiSummary, processingStatus를 한 번에 갱신한다.
                Link updated = linkSaveService.updateLink(linkId, scrapingData, aiSummary, null);
                title = updated.getTitle();
                log.info("Link 처리 완료 (상태/스크래핑 데이터 반영) - linkId: {}", updated.getId());
            } else {
                title = existingLink.getTitle();
                log.info("기존 READY 데이터 재사용 - linkId: {}", linkId);
            }

            notifyAndClearLock(recordId, linkId, title);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Link 처리 인터럽트 - linkId: {}", linkId);
            throw new LinkProcessingException();
        } catch (RuntimeException e) {
            log.error("Link 처리 실패 - linkId: {}, error: {}", linkId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Link 처리 실패 - linkId: {}, error: {}", linkId, e.getMessage(), e);
            throw new LinkProcessingException();
        }
    }

    public void handlePermanentFailure(
            String recordId,
            String payload,
            Long linkId,
            String consumerName,
            long deliveryCount,
            Exception exception
    ) {
        // 영구 실패로 판단되면 링크 상태와 대기열을 정리하고, 운영 추적을 위해 DLQ에 남긴다.
        String reason = exception.getMessage() != null ? exception.getMessage() : "링크 처리에 실패했습니다.";
        String errorType = exception.getClass().getSimpleName();

        boolean failedStateRecorded = false;
        if (linkId != null) {
            failedStateRecorded = linkSaveService.markLinkFailed(linkId, errorType, reason);
        }

        // READY로 이미 복구된 링크는 실패 상태로 덮어쓰지 않고, 대기 유저에게 성공 알림을 보낸다.
        if (linkId != null && failedStateRecorded) {
            notifyFailureAndClearLock(recordId, linkId, reason);
        } else if (linkId != null) {
            notifyAlreadyReadyAndClearLock(recordId, linkId);
        } else {
            clearRecordMetadata(recordId);
        }

        linkStreamDlqService.moveToDlq(
                linkId,
                recordId,
                consumerName,
                payload,
                deliveryCount,
                reason,
                errorType,
                buildStackSummary(exception),
                "link:process:stream",
                consumerName
        );

        linkProcessStreamGateway.ack(recordId);
    }

    private void notifyAndClearLock(String recordId, Long linkId, String title) {
        Set<Long> targetUsers = linkProcessStreamGateway.drainTargetUsersAndClearState(linkId, recordId);

        for (Long userId : targetUsers) {
            eventPublisher.publishEvent(LinkCompletedEvent.of(linkId, title, userId));
        }
        log.info("SSE 'link_completed' 대상 유저 이벤트 일괄 발행 - linkId: {}, 대상 수: {}", linkId, targetUsers.size());
    }

    private void notifyFailureAndClearLock(String recordId, Long linkId, String reason) {
        Set<Long> targetUsers = linkProcessStreamGateway.drainTargetUsersAndClearState(linkId, recordId);

        for (Long userId : targetUsers) {
            eventPublisher.publishEvent(LinkFailedEvent.of(linkId, userId, reason));
        }
        log.info("SSE 'link_failed' 대상 유저 이벤트 일괄 발행 - linkId: {}, 대상 수: {}", linkId, targetUsers.size());
    }

    private void touchProcessingState(Long linkId, String recordId) {
        // 처리 중 heartbeat를 갱신해 recovery 서비스가 정상 진행 중인 작업을 회수하지 않게 한다.
        linkProcessStreamGateway.touchProcessingState(
                linkId,
                recordId,
                Duration.ofMinutes(processingStateTtlMinutes)
        );
    }

    private boolean shouldPersist(Link existingLink, boolean scrapedDataChanged) {
        // placeholder/failed 링크는 항상 갱신하고, READY 링크는 스크래핑 결과가 달라질 때만 다시 저장한다.
        return !existingLink.isReady() || scrapedDataChanged;
    }

    private boolean shouldGenerateSummary(Link existingLink, ScrapingResponse scrapingData, boolean scrapedDataChanged) {
        if (!hasSummarySource(scrapingData)) {
            return false;
        }

        return !existingLink.isReady() || scrapedDataChanged || !existingLink.hasAiSummary();
    }

    private void notifyAlreadyReadyAndClearLock(String recordId, Long linkId) {
        Link link = linkSaveService.findById(linkId);
        Set<Long> targetUsers = linkProcessStreamGateway.drainTargetUsersAndClearState(linkId, recordId);

        for (Long userId : targetUsers) {
            eventPublisher.publishEvent(LinkCompletedEvent.of(linkId, link.getTitle(), userId));
        }
        log.warn("Consumer 실패했으나 이미 READY 상태 - 대기 유저에게 성공 알림 발행 - recordId: {}, linkId: {}, 대상 수: {}", recordId, linkId, targetUsers.size());
    }

    private void clearRecordMetadata(String recordId) {
        linkProcessStreamGateway.clearRecordMetadata(recordId);
    }

    private boolean hasSummarySource(ScrapingResponse scrapingData) {
        boolean titlePresent = scrapingData.getTitle() != null && !scrapingData.getTitle().isBlank();
        boolean descriptionPresent = scrapingData.getDescription() != null && !scrapingData.getDescription().isBlank();
        boolean contentPresent = scrapingData.getContent() != null && !scrapingData.getContent().isBlank();
        return titlePresent && (descriptionPresent || contentPresent);
    }

    private FailureCategory classifyFailure(Exception exception) {
        if (exception instanceof LinkStreamInvalidMessageException || exception instanceof LinkHashGenerationException) {
            return FailureCategory.NON_RETRYABLE;
        }

        if (exception instanceof LinkScrapingTimeoutException || exception instanceof LinkScrapingServerException) {
            return FailureCategory.RETRYABLE;
        }

        if (exception instanceof BusinessException businessException) {
            int status = businessException.getErrorCode().getStatus();
            return status >= 500 ? FailureCategory.RETRYABLE : FailureCategory.NON_RETRYABLE;
        }

        return FailureCategory.RETRYABLE;
    }

    private String buildStackSummary(Exception exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace.length == 0) {
            return exception.toString();
        }

        StackTraceElement top = stackTrace[0];
        return "%s at %s.%s:%d".formatted(
                exception.getClass().getSimpleName(),
                top.getClassName(),
                top.getMethodName(),
                top.getLineNumber()
        );
    }

    private enum FailureCategory {
        RETRYABLE,
        NON_RETRYABLE
    }
}
