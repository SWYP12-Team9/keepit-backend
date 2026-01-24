package swyp12.team9.server.domain.scraper.strategy;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.scraper.dto.ScrapedContent;
import swyp12.team9.server.domain.scraper.exception.ScrapingException;

/**
 * 네이버뉴스 전용 스크래퍼
 * 한국 뉴스 기사의 대부분이 네이버뉴스로 유입되므로 네이버뉴스 구조에 최적화
 */
@Slf4j
@Component
public class NewsScraperStrategy implements ScraperStrategy {

    private static final int TIMEOUT_MS = 7000;
    private static final String NAVER_NEWS_DOMAIN = "news.naver.com";

    @Override
    public ScrapedContent scrape(String url) {
        try {
            log.debug("네이버 뉴스 스크래핑 시작 - url: {}", url);

            Document doc = Jsoup.connect(url)
                .timeout(TIMEOUT_MS)
                .userAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .referrer("https://www.naver.com")
                .get();

            // ===== 제목 =====
            String title = firstNonEmpty(
                doc.select("#title_area span").text(),
                doc.select("h2.media_end_head_headline").text(),
                doc.select("meta[property=og:title]").attr("content")
            );

            // ===== 본문 =====
            String content = firstNonEmpty(
                doc.select("#dic_area").text(),                 // PC
                doc.select("#articleBodyContents").text(),      // 구형
                doc.select("div#newsct_article").text()         // 모바일
            );

            content = cleanContent(content);

            // ===== 대표 이미지 =====
            String imageUrl = firstNonEmpty(
                doc.select("meta[property=og:image]").attr("content"),
                doc.select("meta[name=twitter:image]").attr("content")
            );

            if (content.length() < 100) {
                throw new ScrapingException("본문 길이가 너무 짧음");
            }

            log.info("네이버 뉴스 스크래핑 성공 - title: {}", title);

            return ScrapedContent.of(
                title != null ? title : "뉴스 제목 없음",
                content,
                imageUrl
            );

        } catch (Exception e) {
            log.error("네이버 뉴스 스크래핑 실패 - url: {}", url, e);
            throw new ScrapingException("네이버 뉴스 스크래핑 실패", e);
        }
    }

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("naver.com") && url.contains("/article/");
    }

    @Override
    public int priority() {
        return 10;
    }

    // ===== 유틸 =====

    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    private String cleanContent(String content) {
        if (content == null) return null;

        return content
            .replaceAll("\\[.*?기자.*?\\]", "")
            .replaceAll("무단전재.*?금지", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
