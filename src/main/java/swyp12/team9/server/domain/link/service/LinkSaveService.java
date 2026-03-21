package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.exception.LinkNotFoundException;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.link.event.LinkAiSummaryUpdatedEvent;
import swyp12.team9.server.domain.link.event.LinkCompletedEvent;
import swyp12.team9.server.domain.link.event.LinkCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkSaveService {

    private final LinkRepository linkRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * REQUIRES_NEW 트랜잭션에서 Link를 조회하거나 저장합니다.
     * 스크래핑 데이터 없이 URL만으로 우선 저장(Placeholder)하고, 나중에 비동기로 채웁니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Link getOrSavePlaceholderLink(String url, Long userId) {
        String urlHash = Link.generateUrlHash(url);
        return linkRepository.findByUrlHash(urlHash)
                .orElseGet(() -> {
                    Link link = Link.createPlaceholder(url);

                    try {
                        Link saved = linkRepository.save(link);
                        
                        // AI 요약 및 스크래핑을 위한 이벤트 발행
                        eventPublisher.publishEvent(LinkCreatedEvent.of(saved.getId(), userId));
                        
                        log.info("Placeholder Link 저장 및 이벤트 발행 완료 - linkId: {}, url: {}", saved.getId(), url);
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        Link existing = linkRepository.findByUrlHash(urlHash)
                                .orElseThrow(LinkNotFoundException::new);
                        // 동시성으로 병합된 유저도 처리 완료 알림을 받아야 하므로 이벤트를 발행 (Redis Set 대기열 삽입 용도)
                        eventPublisher.publishEvent(LinkCreatedEvent.of(existing.getId(), userId));
                        return existing;
                    }
                });
    }

    @Transactional(readOnly = true)
    public Link findById(Long linkId) {
        return linkRepository.findById(linkId)
                .orElseThrow(LinkNotFoundException::new);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Link updateLink(Long linkId, ScrapingResponse scrapingData, String aiSummary, Long userId) {
        Link link = linkRepository.findById(linkId).orElseThrow(LinkNotFoundException::new);
        
        link.complete(
                scrapingData.getTitle(),
                scrapingData.getDescription(),
                scrapingData.getFaviconUrl(),
                scrapingData.getContent(),
                aiSummary
        );

        // 트랜잭션 내에서 이벤트 발행 → @TransactionalEventListener가 커밋 후 재인덱싱 수행
        eventPublisher.publishEvent(LinkAiSummaryUpdatedEvent.of(linkId));

        // SSE 알림을 위한 대상 유저에게만 완료 이벤트 발행
        if (userId != null) {
            eventPublisher.publishEvent(LinkCompletedEvent.of(linkId, scrapingData.getTitle(), userId));
            log.info("SSE 알림 대상 이벤트 발행 - linkId: {}, requestUserId: {}", linkId, userId);
        }

        return link;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markLinkPending(Long linkId) {
        Link link = linkRepository.findById(linkId).orElseThrow(LinkNotFoundException::new);
        link.markPending();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markLinkFailed(Long linkId, String errorType, String errorMessage) {
        Link link = linkRepository.findById(linkId).orElseThrow(LinkNotFoundException::new);

        if (link.isReady()) {
            return false;
        }

        log.warn("Link 실패 상태 기록 - linkId: {}, errorType: {}, reason: {}", linkId, errorType, errorMessage);
        link.markFailed();
        return true;
    }
}
