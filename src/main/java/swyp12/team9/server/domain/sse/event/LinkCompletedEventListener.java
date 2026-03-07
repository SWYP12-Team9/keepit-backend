package swyp12.team9.server.domain.sse.event;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import swyp12.team9.server.domain.link.event.LinkCompletedEvent;
import swyp12.team9.server.domain.sse.service.SseEmitterService;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkCompletedEventListener {

    private final SseEmitterService sseEmitterService;
    private final UserLinkRepository userLinkRepository;

    /**
     * Link 저장(스크래핑+AI 요약 완료) 트랜잭션이 커밋된 직후 비동기로 실행되어
     * 해당 Link를 소장 중인 사용자들에게 SSE 알림을 발송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLinkCompleted(LinkCompletedEvent event) {
        Long linkId = event.linkId();
        String title = event.title();
        Long userId = event.userId();
        
        log.debug("LinkCompletedEvent 수신 - linkId: {}, userId: {}", linkId, userId);

        try {
            // 해당 유저가 소장하고 있는 UserLink 하나만 조회 (본인이 추가한 링크에 대한 알림만)
            userLinkRepository.findByUserIdAndLinkId(userId, linkId).ifPresent(userLink -> {
                Long targetUserId = userLink.getUser().getId();
                Long userLinkId = userLink.getId();

                // 개별 유저 맞춤형 이벤트 데이터 생성 (userLinkId 포함)
                // Map으로 설정해 SseEmitter 내부 Jackson을 통해 직렬화
                Map<String, Object> eventData = Map.of(
                        "userLinkId", userLinkId,
                        "linkId", linkId,
                        "title", title != null ? title : "제목 없음",
                        "status", "COMPLETED"
                );

                // 실제 SSE 전송 (개별 전송)
                sseEmitterService.sendToUsers(List.of(targetUserId), "link_completed", eventData);
                log.info("SSE 'link_completed' 발송 완료 - linkId: {}, userId: {}", linkId, targetUserId);
            });
        } catch (Exception e) {
            log.error("SSE 'link_completed' 발송 실패 - linkId: {}, error: {}", linkId, e.getMessage(), e);
        }
    }
}
