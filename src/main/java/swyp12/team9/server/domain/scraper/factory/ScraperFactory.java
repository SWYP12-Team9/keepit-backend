package swyp12.team9.server.domain.scraper.factory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.scraper.strategy.ScraperStrategy;

import java.util.Comparator;
import java.util.List;

/**
 * URL 패턴에 따라 적절한 스크래퍼 전략을 선택하는 팩토리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperFactory {

  private final List<ScraperStrategy> strategies;

  /**
   * URL에 맞는 스크래퍼 전략을 선택
   * 여러 전략이 지원하는 경우 우선순위가 높은 전략 선택
   *
   * @param url 스크래핑할 URL
   * @return 적절한 스크래퍼 전략
   */
  public ScraperStrategy getStrategy(String url) {
    ScraperStrategy selectedStrategy = strategies.stream()
        .filter(strategy -> strategy.supports(url))
        .min(Comparator.comparingInt(ScraperStrategy::priority))
        .orElseThrow(() -> new IllegalStateException("URL을 지원하는 스크래퍼를 찾을 수 없습니다: " + url));

    log.debug("스크래퍼 전략 선택 - url: {}, strategy: {}", url, selectedStrategy.getClass().getSimpleName());
    return selectedStrategy;
  }
}
