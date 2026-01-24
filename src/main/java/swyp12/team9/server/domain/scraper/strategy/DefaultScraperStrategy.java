package swyp12.team9.server.domain.scraper.strategy;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.scraper.dto.ScrapedContent;
import swyp12.team9.server.domain.scraper.exception.ScrapingException;

/**
 * Open Graph 메타데이터를 사용하는 일반 웹페이지 스크래퍼
 */
@Slf4j
@Component
public class DefaultScraperStrategy implements ScraperStrategy {

  private static final int TIMEOUT_MS = 5000;

  @Override
  public ScrapedContent scrape(String url) throws ScrapingException {
    log.debug("기본 스크래퍼 실행 (비워둠) - url: {}", url);
    return ScrapedContent.of("제목 없음", "설명 없음", null);
  }

  @Override
  public boolean supports(String url) {
    // 기본 전략은 모든 URL 지원 (가장 낮은 우선순위)
    return true;
  }

  @Override
  public int priority() {
    return 999; // 가장 낮은 우선순위 (마지막 폴백)
  }

  private String extractOpenGraphTag(Document doc, String property) {
    String content = doc.select("meta[property=" + property + "]").attr("content");
    return (content != null && !content.isBlank()) ? content : null;
  }
}
