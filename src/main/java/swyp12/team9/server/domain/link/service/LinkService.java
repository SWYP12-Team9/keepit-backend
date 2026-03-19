package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ScrapingService scrapingService;
    private final LinkAiService linkAiService;
    private final LinkSaveService linkSaveService;

    /**
     * URL로 기존 Link를 조회하거나, 없으면 스크래핑을 통해 새로 생성합니다.
     * 외부 호출(스크래핑, AI 요약)은 트랜잭션 밖에서 실행하여 커넥션 점유를 최소화합니다.
     * DB 저장은 LinkSaveService에 위임하여 REQUIRES_NEW 트랜잭션에서 짧게 처리합니다.
     *
     * @param url 조회 또는 생성할 URL
     * @return 기존 또는 새로 생성된 Link 엔티티
     */
    public Link getOrCreateLink(String url) {
        String urlHash = Link.generateUrlHash(url);
        return linkRepository.findByUrlHash(urlHash)
                .orElseGet(() -> createLink(url));
    }

    private Link createLink(String url) {
        log.info("새로운 Link 생성 시작 - URL: {}", url);

        // 트랜잭션 밖: 스크래핑 (커넥션 안 씀)
        ScrapingResponse scrapingData = scrapingService.scrapeUrl(url, 500);

        // 트랜잭션 밖: AI 요약 (커넥션 안 씀)
        String aiSummary = linkAiService.summarizeLink(
                scrapingData.getTitle(),
                scrapingData.getDescription(),
                scrapingData.getContent()
        );

        // REQUIRES_NEW 트랜잭션: find + save (수십ms)
        Link link = linkSaveService.findOrSaveLink(url, scrapingData, aiSummary);

        log.info("Link 생성 완료 - linkId: {}, title: {}", link.getId(), scrapingData.getTitle());
        return link;
    }

    /**
     * 외부(추천/인기 탭 등)에서 링크 카드를 클릭했을 때 공개 조회수를 1 증가시킵니다.
     */
    @Transactional
    public void incrementPublicViewCount(Long linkId) {
        linkRepository.incrementPublicViewCount(linkId);
        log.debug("Link 공개 조회수 증가 - linkId: {}", linkId);
    }
}
