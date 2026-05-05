package swyp12.team9.server.domain.recommendation.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import swyp12.team9.server.domain.link.event.LinkAiSummaryUpdatedEvent;
import swyp12.team9.server.domain.reference.event.ReferenceVisibilityChangedEvent;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.recommendation.service.LinkIndexingService;
import swyp12.team9.server.domain.recommendation.service.RecommendationCacheService;
import swyp12.team9.server.domain.userlink.event.UserLinkCreatedEvent;
import swyp12.team9.server.domain.userlink.event.UserLinkDeletedEvent;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * 추천 인덱싱과 추천 캐시 무효화를 도메인 이벤트 기반으로 처리한다.
 * 트랜잭션이 커밋된 뒤 비동기로 실행해, 저장/수정 요청의 응답 시간을 인덱싱 작업과 분리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationIndexingEventListener {

    private final RecommendationCacheService cacheService;
    private final UserLinkRepository userLinkRepository;
    private final ReferenceUserLinkRepository referenceUserLinkRepository;
    private final LinkIndexingService linkIndexingService;

    /**
     * UserLink 생성 후 사용자의 저장 Link ID 캐시를 지우고 추천 인덱싱을 시도한다.
     * 생성 직후 Link가 아직 READY가 아니면 LinkIndexingService에서 추천 인덱스 삭제/제외로 처리된다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLinkCreated(UserLinkCreatedEvent event) {
        userLinkRepository.findUserIdById(event.userLinkId()).ifPresent(userId -> {
            cacheService.evictUserLinkIds(userId);
            linkIndexingService.indexUserLink(event.userLinkId());
            log.debug("[추천 캐시] userLinkIds 캐시 무효화 - userId: {}", userId);
        });
    }

    /**
     * UserLink 삭제 후 해당 사용자의 저장 Link ID 캐시와 추천 인덱스를 함께 제거한다.
     * 삭제 후에는 DB에서 UserLink를 다시 조회할 수 없으므로 이벤트에 userId를 함께 담아 전달한다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserLinkDeleted(UserLinkDeletedEvent event) {
        cacheService.evictUserLinkIds(event.userId());
        linkIndexingService.deleteUserLink(event.userLinkId());
        log.debug("[추천 캐시] userLinkIds 캐시 무효화 - userId: {}", event.userId());
    }

    /**
     * Reference 공개 여부가 바뀌면 연결된 모든 UserLink를 다시 인덱싱한다.
     * 공개 전환은 추천 인덱스 추가, 비공개 전환은 추천 인덱스 삭제로 이어진다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReferenceVisibilityChanged(ReferenceVisibilityChangedEvent event) {
        referenceUserLinkRepository.findByReferenceId(event.referenceId()).stream()
                .map(ReferenceUserLink::getUserLink)
                .forEach(userLink -> linkIndexingService.indexUserLink(userLink.getId()));

        log.debug("[추천 인덱싱] Reference 공개 상태 변경 반영 - referenceId: {}, isPublic: {}",
                event.referenceId(), event.isPublic());
    }

    /**
     * Link AI 요약이 완료되면 해당 Link를 사용하는 공개 UserLink들을 추천 인덱스에 반영한다.
     * PENDING 상태라 제외됐던 링크가 READY가 된 뒤 추천 후보로 들어오는 흐름이다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLinkAiSummaryUpdated(LinkAiSummaryUpdatedEvent event) {
        linkIndexingService.indexLink(event.linkId());
        log.debug("[추천 인덱싱] Link AI 요약 완료 반영 - linkId: {}", event.linkId());
    }
}
