package swyp12.team9.server.domain.scraper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.scraper.dto.ScrapedContent;
import swyp12.team9.server.domain.scraper.exception.ScrapingException;
import swyp12.team9.server.domain.scraper.factory.ScraperFactory;
import swyp12.team9.server.domain.scraper.strategy.ScraperStrategy;

/**
 * 웹 스크래핑을 조율하는 파사드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

  private final ScraperFactory scraperFactory;

  @Value("${scraper.retry-attempts:1}")
  private int retryAttempts;

  /**
   * URL에서 콘텐츠를 스크래핑
   * 실패 시 재시도 로직 포함
   *
   * @param url 스크래핑할 URL
   * @return 스크래핑된 콘텐츠 (실패 시 빈 콘텐츠)
   */
  public ScrapedContent scrapeUrl(String url) {
    if (url == null || url.isBlank()) {
      log.warn("스크래핑할 URL이 비어있음");
      return ScrapedContent.empty();
    }

    int attempt = 0;
    Exception lastException = null;

    while (attempt <= retryAttempts) {
      try {
        ScraperStrategy strategy = scraperFactory.getStrategy(url);
        ScrapedContent content = strategy.scrape(url);

        log.info("스크래핑 성공 - url: {}, attempt: {}", url, attempt + 1);
        return content;

      } catch (ScrapingException e) {
        lastException = e;
        attempt++;

        if (attempt <= retryAttempts) {
          log.warn("스크래핑 실패, 재시도 중 - url: {}, attempt: {}/{}",
              url, attempt, retryAttempts + 1);

          // 재시도 전 짧은 대기
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      } catch (Exception e) {
        lastException = e;
        log.error("예상치 못한 스크래핑 오류 - url: {}, error: {}", url, e.getMessage());
        break;
      }
    }

    log.error("스크래핑 최종 실패 - url: {}, attempts: {}, error: {}",
        url, attempt, lastException != null ? lastException.getMessage() : "알 수 없음");

    // 실패 시 빈 콘텐츠 반환 (링크 저장은 계속 진행)
    return ScrapedContent.empty();
  }
}
