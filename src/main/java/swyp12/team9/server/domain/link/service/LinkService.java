package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.event.LinkCreatedEvent;
import swyp12.team9.server.domain.link.exception.LinkNotFoundException;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.link.service.ViewCountCacheService;


@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final LinkSaveService linkSaveService;
    private final ApplicationEventPublisher eventPublisher;
    private final ViewCountCacheService viewCountCacheService;

    /**
     * URL로 기존 Link를 조회하거나, 없으면 스크래핑을 통해 새로 생성합니다.
     * 외부 호출(스크래핑, AI 요약)은 트랜잭션 밖에서 실행하여 커넥션 점유를 최소화합니다.
     * DB 저장은 LinkSaveService에 위임하여 REQUIRES_NEW 트랜잭션에서 짧게 처리합니다.
     *
     * @param url 조회 또는 생성할 URL
     * @return 기존 또는 새로 생성된 Link 엔티티
     */
    public Link getOrCreateLink(String url, Long userId) {
        String urlHash = Link.generateUrlHash(url);
        return linkRepository.findByUrlHash(urlHash)
                .map(existingLink -> {
                    LinkProcessingStatus processingStatus = existingLink.getProcessingStatus();

                    if (processingStatus == LinkProcessingStatus.FAILED) {
                        // 이전 처리에서 실패한 링크는 다시 큐에 태울 수 있도록 PENDING으로 되돌린다.
                        linkSaveService.markLinkPending(existingLink.getId());
                    }

                    eventPublisher.publishEvent(LinkCreatedEvent.of(existingLink.getId(), userId));
                    logReprocessRequest(existingLink.getId(), processingStatus);
                    return existingLink;
                })
                .orElseGet(() -> createLink(url, userId));
    }

    private void logReprocessRequest(Long linkId, LinkProcessingStatus processingStatus) {
        if (processingStatus == LinkProcessingStatus.FAILED) {
            log.info("실패한 Link 재처리 요청 - linkId: {}", linkId);
            return;
        }

        if (processingStatus == LinkProcessingStatus.PENDING) {
            log.info("처리 중 Link 재요청 - linkId: {}", linkId);
            return;
        }

        log.info("기존 Link 재처리 요청 - linkId: {}", linkId);
    }

    private Link createLink(String url, Long userId) {
        log.info("새로운 Link 생성 시작 (비동기) - URL: {}", url);

        // 스크래핑 없이 우선 Placeholder Link 생성 (REQUIRES_NEW)
        // 저장이 완료되면 LinkCreatedEvent가 발행되어 Redis Stream으로 비동기 처리됨
        Link link = linkSaveService.getOrSavePlaceholderLink(url, userId);

        log.info("Link 생성 완료 (Placeholder) - linkId: {}", link.getId());
        return link;
    }

    /**
     * 외부(추천/인기 탭 등)에서 링크 카드를 클릭했을 때 공개 조회수를 1 증가시킵니다.
     * Redis를 통해 실시간 기록 후, 스케줄러가 주기적으로 DB에 일괄 반영합니다.
     */
    @Transactional
    public void incrementPublicViewCount(Long linkId) {
        if (!linkRepository.existsById(linkId)) {
            throw new LinkNotFoundException();
        }
        viewCountCacheService.incrementPublicViewCount(linkId);
        log.debug("Link 공개 조회수 증가 (Redis) - linkId: {}", linkId);
    }
}
