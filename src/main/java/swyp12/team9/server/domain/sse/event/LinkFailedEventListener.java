package swyp12.team9.server.domain.sse.event;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.link.event.LinkFailedEvent;
import swyp12.team9.server.domain.sse.service.SseEmitterService;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkFailedEventListener {

    private final SseEmitterService sseEmitterService;
    private final UserLinkRepository userLinkRepository;

    @Async
    @EventListener
    public void handleLinkFailed(LinkFailedEvent event) {
        Long linkId = event.linkId();
        Long userId = event.userId();
        String reason = event.reason();

        log.debug("LinkFailedEvent 수신 - linkId: {}, userId: {}", linkId, userId);

        try {
            userLinkRepository.findByUserIdAndLinkId(userId, linkId).ifPresent(userLink -> {
                Long targetUserId = userLink.getUser().getId();
                Long userLinkId = userLink.getId();

                LinkFailedSsePayload eventData = LinkFailedSsePayload.of(userLinkId, linkId, reason);

                sseEmitterService.sendToUsers(List.of(targetUserId), "link_failed", eventData);
                log.info("SSE 'link_failed' 발송 완료 - linkId: {}, userId: {}", linkId, targetUserId);
            });
        } catch (Exception e) {
            log.error("SSE 'link_failed' 발송 실패 - linkId: {}, error: {}", linkId, e.getMessage(), e);
        }
    }
}
