package swyp12.team9.server.global.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import swyp12.team9.server.domain.chatbot.service.ChatbotIndexingService;
import swyp12.team9.server.domain.userlink.event.UserLinkCreatedEvent;
import swyp12.team9.server.domain.userlink.event.UserLinkDeletedEvent;
import swyp12.team9.server.domain.userlink.event.UserLinkUpdatedEvent;

/**
 * UserLink 이벤트 리스너
 * - 트랜잭션 커밋 후 비동기로 Elasticsearch 인덱싱 처리
 * - 챗봇용 인덱스만 자동 업데이트 (추천용은 관리자 API로 배치 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingEventListener {

    private final ChatbotIndexingService chatbotIndexingService;

    /**
     * UserLink 생성 이벤트 처리
     * - DB 커밋 완료 후 비동기로 챗봇 인덱싱만 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLinkCreated(UserLinkCreatedEvent event) {
        Long userLinkId = event.userLinkId();
        log.debug("UserLink 생성 이벤트 처리 시작 - userLinkId: {}", userLinkId);

        try {
            chatbotIndexingService.indexUserLink(userLinkId);
            log.info("[챗봇] UserLink Elasticsearch 인덱싱 완료 - userLinkId: {}", userLinkId);
        } catch (Exception e) {
            log.error("[챗봇] UserLink Elasticsearch 인덱싱 실패 - userLinkId: {}, error: {}",
                    userLinkId, e.getMessage(), e);
            // 인덱싱 실패해도 예외를 던지지 않음 (비동기 처리)
        }
    }

    /**
     * UserLink 수정 이벤트 처리
     * - DB 커밋 완료 후 비동기로 챗봇 재인덱싱만 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLinkUpdated(UserLinkUpdatedEvent event) {
        Long userLinkId = event.userLinkId();
        log.debug("UserLink 수정 이벤트 처리 시작 - userLinkId: {}", userLinkId);

        try {
            chatbotIndexingService.indexUserLink(userLinkId);
            log.info("[챗봇] UserLink Elasticsearch 재인덱싱 완료 - userLinkId: {}", userLinkId);
        } catch (Exception e) {
            log.error("[챗봇] UserLink Elasticsearch 재인덱싱 실패 - userLinkId: {}, error: {}",
                    userLinkId, e.getMessage(), e);
        }
    }

    /**
     * UserLink 삭제 이벤트 처리
     * - DB 커밋 완료 후 비동기로 챗봇 인덱스만 삭제
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLinkDeleted(UserLinkDeletedEvent event) {
        Long userLinkId = event.userLinkId();
        log.debug("UserLink 삭제 이벤트 처리 시작 - userLinkId: {}", userLinkId);

        try {
            chatbotIndexingService.deleteUserLink(userLinkId);
            log.info("[챗봇] UserLink Elasticsearch 삭제 완료 - userLinkId: {}", userLinkId);
        } catch (Exception e) {
            log.error("[챗봇] UserLink Elasticsearch 삭제 실패 - userLinkId: {}, error: {}",
                    userLinkId, e.getMessage(), e);
        }
    }
}
