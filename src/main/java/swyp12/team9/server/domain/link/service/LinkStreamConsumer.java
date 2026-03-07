package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.event.LinkCompletedEvent;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.global.config.RedisStreamConfig;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkStreamConsumer implements StreamListener<String, ObjectRecord<String, String>> {

    private final LinkSaveService linkSaveService;
    private final ScrapingService scrapingService;
    private final LinkAiService linkAiService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private final Semaphore scrapingSemaphore = new Semaphore(5);  // Cloud Run maxScale=1, concurrency=5
    private final Semaphore aiSemaphore = new Semaphore(8);       // OpenAI 동시 8개 이상이면 개별 응답 급격히 느려짐

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        String streamKey = message.getStream();
        String recordId = message.getId().getValue();
        String payload = message.getValue();

        try {
            // payload 포맷: "linkId|url"
            String[] parts = payload.split("\\|");
            if (parts.length < 2) {
                log.error("잘못된 Stream 메시지 포맷: {}", payload);
                ackMessage(streamKey, recordId);
                return;
            }

            Long linkId = Long.parseLong(parts[0]);
            String url = parts[1];

            log.info("Redis Stream 메시지 수신 - recordId: {}, linkId: {}", recordId, linkId);

            processUrl(linkId, url);

            // 정상 처리 완료 시 ACK
            ackMessage(streamKey, recordId);

        } catch (Exception e) {
            log.error("Redis Stream 소비 중 에러 발생 - recordId: {}, error: {}", recordId, e.getMessage(), e);
            // 에러 발생 시 ACK를 하지 않고 보류(Pending) 상태로 남겨 나중에 재처리(DLQ 등)할 수 있도록 함
        }
    }

    private void processUrl(Long linkId, String url) {
        try {
            scrapingSemaphore.acquire();
            ScrapingResponse scrapingData;
            try {
                scrapingData = scrapingService.scrapeUrl(url, 500);
            } finally {
                scrapingSemaphore.release();
            }

            aiSemaphore.acquire();
            String aiSummary;
            try {
                aiSummary = linkAiService.summarizeLink(
                        scrapingData.getTitle(),
                        scrapingData.getDescription(),
                        scrapingData.getContent()
                );
            } finally {
                aiSemaphore.release();
            }

            // userId로 null을 넘겨서 LinkSaveService 내부의 이벤트 개별 발행을 건너뜀 (Consumer 직접 일괄 발행 트리거)
            Link updated = linkSaveService.updateLink(linkId, scrapingData, aiSummary, null);
            log.info("Link 처리 완료 (Redis Stream 기반) - linkId: {}", updated.getId());

            // 성공 시 원자적으로 대기 유저 목록을 빼오고 Lock을 해제하여 SSE 일괄 발송
            notifyAndClearLock(linkId, scrapingData.getTitle());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Link 처리 인터럽트 - linkId: {}", linkId);
            clearLockOnly(linkId);
        } catch (Exception e) {
            log.error("Link 처리 실패 - linkId: {}, error: {}", linkId, e.getMessage(), e);
            clearLockOnly(linkId);
            throw new swyp12.team9.server.domain.link.exception.LinkProcessingException();
        }
    }

    private void notifyAndClearLock(Long linkId, String title) {
        // 원자적으로 집합의 모든 멤버를 가져온 뒤 집합(Set)과 락(Lock)을 삭제하는 Lua 스크립트
        String script = 
                "local users = redis.call('SMEMBERS', KEYS[1]); " +
                "redis.call('DEL', KEYS[1]); " +
                "redis.call('DEL', KEYS[2]); " +
                "return users;";
        
        @SuppressWarnings("unchecked")
        List<String> userIds = stringRedisTemplate.execute(
                new DefaultRedisScript<>(script, List.class),
                Arrays.asList("link:notify_users:" + linkId, "link:processing_lock:" + linkId)
        );
        
        Set<Long> targetUsers = new HashSet<>();
        if (userIds != null && !userIds.isEmpty()) {
            for (String u : userIds) {
                targetUsers.add(Long.parseLong(u));
            }
        }
        
        // 대상 유저들 각각에게 LinkCompletedEvent 발행 (개별 SSE 알림 발송 목적)
        for (Long userId : targetUsers) {
            eventPublisher.publishEvent(LinkCompletedEvent.of(linkId, title, userId));
        }
        log.info("SSE 'link_completed' 대상 유저 이벤트 일괄 발행 - linkId: {}, 대상 수: {}", linkId, targetUsers.size());
    }

    private void clearLockOnly(Long linkId) {
        stringRedisTemplate.delete("link:processing_lock:" + linkId);
    }

    private void ackMessage(String streamKey, String recordId) {
        stringRedisTemplate.opsForStream().acknowledge(streamKey, RedisStreamConfig.LINK_PROCESS_GROUP, recordId);
    }
}
