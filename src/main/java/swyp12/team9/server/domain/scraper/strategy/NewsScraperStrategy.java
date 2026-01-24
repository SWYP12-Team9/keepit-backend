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

  private static final int TIMEOUT_MS = 5000;
  private static final String NAVER_NEWS_DOMAIN = "n.news.naver.com";

  @Override
  public ScrapedContent scrape(String url) throws ScrapingException {
    log.debug("뉴스 스크래퍼 실행 (비워둠) - url: {}", url);
    return ScrapedContent.of("뉴스 제목 없음", "뉴스 설명 없음", null);
  }

  @Override
  public boolean supports(String url) {
    // 네이버뉴스 URL 지원
    return url != null && url.contains(NAVER_NEWS_DOMAIN);
  }

  @Override
  public int priority() {
    return 10; // 높은 우선순위
  }
}
