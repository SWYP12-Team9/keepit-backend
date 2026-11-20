package swyp12.team9.server.domain.link.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.link.service.LinkStreamProducer;

/**
 * Link 이벤트 리스너
 * - Link 생성 후 비동기로 AI 요약 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkEventListener {

    private final LinkStreamProducer linkStreamProducer;
    private final LinkRepository linkRepository;

    /**
     * Link 생성 이벤트 처리
     * - DB 커밋 완료 후 비동기로 AI 요약 생성 및 업데이트
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true)
    public void handleLinkCreated(LinkCreatedEvent event) {
        Long linkId = event.linkId();
        Long userId = event.userId();
        log.debug("Link 생성 이벤트 처리 시작 - linkId: {}, userId: {}", linkId, userId);

        linkRepository.findById(linkId).ifPresent(link -> {
            linkStreamProducer.publishLinkProcessTask(link.getId(), link.getUrl(), userId);
            log.info("Redis Stream 처리 요청 완료 - linkId: {}", linkId);
        });
    }
}
