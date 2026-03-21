package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.link.event.LinkCreatedEvent;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final LinkSaveService linkSaveService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * URL로 기존 Link를 조회하거나, 없으면 스크래핑을 통해 새로 생성합니다.
     * 외부 호출(스크래핑, AI 요약)은 트랜잭션 밖에서 실행하여 커넥션 점유를 최소화합니다.
     * DB 저장은 LinkSaveService에 위임하여 REQUIRES_NEW 트랜잭션에서 짧게 처리합니다.
     * 기존 URL이미 존재하는 경우, 업데이트 된지 1일이 지났고, 스크래핑 내용이 바뀌었으면 AI 요약을 새로 업데이트합니다.
     *
     * @param url 조회 또는 생성할 URL
     * @return 기존 또는 새로 생성된 Link 엔티티
     */
    public Link getOrCreateLink(String url, Long userId) {
        String urlHash = Link.generateUrlHash(url);
        return linkRepository.findByUrlHash(urlHash)
                .map(existingLink -> {
                    LinkProcessingStatus processingStatus = existingLink.getProcessingStatus();
                    LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
                    boolean isOlderThanOneDay = existingLink.getUpdatedAt() == null || existingLink.getUpdatedAt().isBefore(oneDayAgo);

                    if (processingStatus == LinkProcessingStatus.READY && !isOlderThanOneDay) {
                        log.info("Link 최신 상태 캐싱 반환 (1일 이내 유지) - linkId: {}", existingLink.getId());
                        return existingLink;
                    }

                    if (processingStatus == LinkProcessingStatus.FAILED) {
                        // 이전 처리에서 실패한 링크는 다시 큐에 태울 수 있도록 PENDING으로 되돌린다.
                        linkSaveService.markLinkPending(existingLink.getId());
                    }

                    eventPublisher.publishEvent(LinkCreatedEvent.of(existingLink.getId(), userId));
                    logReprocessRequest(existingLink.getId(), processingStatus, isOlderThanOneDay);
                    return existingLink;
                })
                .orElseGet(() -> createLink(url, userId));
    }

    private void logReprocessRequest(Long linkId, LinkProcessingStatus processingStatus, boolean isOlderThanOneDay) {
        if (processingStatus == LinkProcessingStatus.FAILED) {
            log.info("실패한 Link 재처리 요청 - linkId: {}", linkId);
            return;
        }

        if (processingStatus == LinkProcessingStatus.PENDING) {
            log.info("처리 중 Link 재요청 - linkId: {}", linkId);
            return;
        }

        if (isOlderThanOneDay) {
            log.info("Link 업데이트 필요 (1일경과: true). 비동기 업데이트 이벤트 재발행 - linkId: {}", linkId);
        }
    }

    private Link createLink(String url, Long userId) {
        log.info("새로운 Link 생성 시작 (비동기) - URL: {}", url);

        // 스크래핑 없이 우선 Placeholder Link 생성 (REQUIRES_NEW)
        // 저장이 완료되면 LinkCreatedEvent가 발행되어 Redis Stream으로 비동기 처리됨
        Link link = linkSaveService.getOrSavePlaceholderLink(url, userId);

        log.info("Link 생성 완료 (Placeholder) - linkId: {}", link.getId());
        return link;
    }
}
