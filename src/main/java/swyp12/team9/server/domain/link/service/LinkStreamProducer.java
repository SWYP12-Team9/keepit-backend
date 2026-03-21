package swyp12.team9.server.domain.link.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkStreamProducer {

    private final LinkProcessStreamGateway linkProcessStreamGateway;

    @Value("${link.stream.processing-state-ttl-minutes:30}")
    private long processingStateTtlMinutes;

    // 저장 요청마다 바로 Stream을 발행하지 않고
    // 1. 이 linkId 완료/실패 알림을 받아야 하는 유저를 먼저 누적한 뒤
    // 2. 같은 linkId에 대한 실제 작업 메시지는 한 번만 발행
    // 같은 링크를 여러 사용자가 거의 동시에 저장해도 외부 스크래핑/AI 호출은 1회만 수행
    public void publishLinkProcessTask(Long linkId, String url, Long userId) {
        Duration ttl = Duration.ofMinutes(processingStateTtlMinutes);

        // 완료 시 SSE를 받아야 하는 유저를 linkId 기준 대기열에 추가
        // 실제 처리가 이미 진행 중이어도 이 단계는 항상 수행되어야 나중에 알림 대상이 빠지지 않음
        linkProcessStreamGateway.addTargetUser(linkId, userId, ttl);

        // 같은 linkId 작업이 아직 큐에 없거나 처리 중이 아니면 lock을 선점하고 최초 작업으로 간주
        // 이미 누군가 선점했다면 이번 요청은 기존 작업에 합류만 하고 Stream 재발행은 생략
        boolean isNewTask = linkProcessStreamGateway.acquireProcessingLock(linkId, ttl);

        if (isNewTask) {
            // Consumer가 파싱할 최소 정보(linkId|url)만 payload로 넣음
            String payload = linkId + "|" + url;
            RecordId recordId = linkProcessStreamGateway.add(payload);

            // Stream 본문이 trim/장애로 사라져도 recovery/DLQ가 recordId 기준으로 원본 payload를 복원할 수 있게 백업
            backupStreamMetadata(recordId, linkId, payload, ttl);
            log.info("Redis Stream 메시지 발행 완료 (첫 요청) - linkId: {}", linkId);
        } else {
            // 이미 처리 중인 작업이라도 lock TTL은 연장해 두어, 느린 스크래핑/AI 처리 중 만료로 인한 중복 발행을 막음
            linkProcessStreamGateway.extendProcessingLock(linkId, ttl);
            log.info("이미 처리 중인 Link 작업 존재 (Stream 발행 생략, 대기 유저 추가) - linkId: {}, userId: {}", linkId, userId);
        }
    }

    private void backupStreamMetadata(RecordId recordId, Long linkId, String payload, Duration ttl) {
        if (recordId == null) {
            log.warn("Redis Stream recordId 생성 실패 - linkId: {}", linkId);
            return;
        }

        linkProcessStreamGateway.backupMessageMetadata(recordId.getValue(), linkId, payload, ttl);
    }
}
