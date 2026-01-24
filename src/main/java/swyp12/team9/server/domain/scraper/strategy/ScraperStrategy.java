package swyp12.team9.server.domain.scraper.strategy;

import swyp12.team9.server.domain.scraper.dto.ScrapedContent;
import swyp12.team9.server.domain.scraper.exception.ScrapingException;

/**
 * 스크래핑 전략 인터페이스
 * Strategy Pattern을 사용하여 다양한 웹사이트에 대한 스크래핑 로직을 구현
 */
public interface ScraperStrategy {

  /**
   * URL에서 콘텐츠를 스크래핑
   *
   * @param url 스크래핑할 URL
   * @return 스크래핑된 콘텐츠
   * @throws ScrapingException 스크래핑 실패 시
   */
  ScrapedContent scrape(String url) throws ScrapingException;

  /**
   * 해당 전략이 주어진 URL을 지원하는지 확인
   *
   * @param url 확인할 URL
   * @return 지원 여부
   */
  boolean supports(String url);

  /**
   * 전략의 우선순위 (낮을수록 우선)
   * 여러 전략이 동일한 URL을 지원할 경우 우선순위가 높은 전략 사용
   *
   * @return 우선순위 값
   */
  default int priority() {
    return 100;
  }
}
