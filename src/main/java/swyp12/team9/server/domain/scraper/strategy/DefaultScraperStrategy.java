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
    try {
      log.debug("기본 스크래퍼 실행 - url: {}", url);
      Document doc = Jsoup.connect(url)
          .timeout(TIMEOUT_MS)
          .userAgent(
              "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
          .get();

      // Open Graph 태그 우선 추출
      String title = extractOpenGraphTag(doc, "og:title");
      if (title == null || title.isBlank()) {
        title = doc.title();
      }

      String description = extractOpenGraphTag(doc, "og:description");
      if (description == null || description.isBlank()) {
        description = doc.select("meta[name=description]").attr("content");
      }

      String imageUrl = extractOpenGraphTag(doc, "og:image");

      log.info("기본 스크래핑 성공 - title: {}", title);
      return ScrapedContent.of(
          (title != null && !title.isBlank()) ? title : "제목 없음",
          (description != null && !description.isBlank()) ? description : "설명 없음",
          imageUrl);

    } catch (Exception e) {
      log.error("기본 스크래핑 실패 - url: {}, error: {}", url, e.getMessage());
      return ScrapedContent.of("제목 없음", "스크래핑 실패: " + e.getMessage(), null);
    }
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
