package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkCategory;
import swyp12.team9.server.domain.link.repository.LinkRepository;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkService {

    private final LinkRepository linkRepository;
    private final ScrapingService scrapingService;
    private final LinkAiService linkAiService;

    /**
     * 스크래핑을 통해 새로운 Link를 생성합니다.
     *
     * @param url 스크래핑할 URL
     * @return 생성된 Link 엔티티
     */
    @Transactional
    public Link createLink(String url) {
        log.info("새로운 Link 생성 시작 - URL: {}", url);

        // 스크래핑 API 호출
        ScrapingResponse scrapingData = scrapingService.scrapeUrl(url, 500);

        // Link 생성 및 저장
        Link link = createLinkFromScrapingData(scrapingData, url);

        // AI 요약 추가 TODO 박현제: AI 요청 로직 비동기로 수정
        addAiSummaryToLink(link, scrapingData);

        // AI 카테고리 분류
        addCategoryToLink(link, scrapingData);

        return link;
    }

    /**
     * 스크래핑 데이터로부터 Link를 생성하고 저장합니다.
     *
     * @param scrapingData 스크래핑 결과
     * @param url 원본 URL
     * @return 저장된 Link 엔티티
     */
    private Link createLinkFromScrapingData(ScrapingResponse scrapingData, String url) {
        Link link = Link.create(
                url,
                scrapingData.getTitle(),
                scrapingData.getDescription(),
                scrapingData.getFaviconUrl(),
                scrapingData.getContent()
        );

        link = linkRepository.save(link);
        log.info("Link 저장 완료 - linkId: {}, title: {}", link.getId(), scrapingData.getTitle());

        return link;
    }

    /**
     * Link에 AI 요약을 추가합니다.
     *
     * @param link 대상 Link
     * @param scrapingData 스크래핑 데이터
     */
    private void addAiSummaryToLink(Link link, ScrapingResponse scrapingData) {
        String aiSummary = linkAiService.summarizeLink(
                scrapingData.getTitle(),
                scrapingData.getDescription(),
                scrapingData.getContent()
        );

        if (aiSummary != null && !aiSummary.trim().isEmpty()) {
            link.updateAiSummary(aiSummary);
            log.info("AI 요약 완료 - linkId: {}", link.getId());
        } else {
            log.warn("AI 요약 실패 - linkId: {}", link.getId());
        }
    }

    /**
     * Link에 AI 카테고리 분류를 추가합니다.
     *
     * @param link 대상 Link
     * @param scrapingData 스크래핑 데이터
     */
    private void addCategoryToLink(Link link, ScrapingResponse scrapingData) {
        LinkCategory category = linkAiService.classifyCategory(
                scrapingData.getTitle(),
                scrapingData.getDescription(),
                scrapingData.getContent(),
                link.getAiSummary()
        );

        link.updateCategory(category);
        log.info("AI 카테고리 분류 완료 - linkId: {}, category: {}", link.getId(), category);
    }

    /**
     * category가 null인 모든 Link에 대해 AI 카테고리 분류를 수행합니다. (관리자 백필용)
     */
    @Transactional
    public void classifyAllLinks() {
        List<Link> links = linkRepository.findAll().stream()
                .filter(link -> link.getCategory() == null)
                .toList();

        log.info("카테고리 미분류 링크 {} 개 백필 시작", links.size());

        for (Link link : links) {
            try {
                LinkCategory category = linkAiService.classifyCategory(
                        link.getTitle(),
                        link.getDescription(),
                        link.getContent(),
                        link.getAiSummary()
                );
                link.updateCategory(category);
                log.info("백필 완료 - linkId: {}, category: {}", link.getId(), category);
            } catch (Exception e) {
                log.error("백필 실패 - linkId: {}, error: {}", link.getId(), e.getMessage());
            }
        }

        log.info("카테고리 백필 완료 - 처리 건수: {}", links.size());
    }
}