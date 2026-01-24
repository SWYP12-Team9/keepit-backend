package swyp12.team9.server.domain.scraper.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.scraper.dto.ScrapedContent;
import swyp12.team9.server.domain.scraper.exception.ScrapingException;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Apify API를 사용한 인스타그램 스크래퍼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramScraperStrategy implements ScraperStrategy {

  @Value("${scraper.apify.api-key}")
  private String apiKey;

  private static final String APIFY_RUN_URL = "https://api.apify.com/v2/acts/apify~instagram-post-scraper/run-sync-get-dataset-items?token=%s";

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private final OkHttpClient httpClient = new OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build();

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public ScrapedContent scrape(String url) {

    if (apiKey == null || apiKey.isBlank()) {
      throw new ScrapingException("Apify API 키가 설정되지 않았습니다");
    }

    // 1. URL 정규화 (utm 제거, / 추가)
    String normalizedUrl = normalizeInstagramUrl(url);
    log.info("Instagram URL Normalized: {} -> {}", url, normalizedUrl);

    try {
      log.debug("Instagram scrape start: {}", normalizedUrl);

      // Apify Instagram Actor - 특이하게도 URL을 'username' 필드에 넣음
      // JSON Injection 방지를 위해 ObjectMapper 사용
      Map<String, Object> payload = Map.of(
          "username", List.of(normalizedUrl),
          "resultsLimit", 1);
      String requestBody = objectMapper.writeValueAsString(payload);

      // run-sync-get-dataset-items 엔드포인트 사용 (동기식 대기 및 결과 반환)
      Request request = new Request.Builder()
          .url(String.format(APIFY_RUN_URL, apiKey))
          .post(RequestBody.create(requestBody, JSON))
          .build();

      try (Response response = httpClient.newCall(request).execute()) {

        if (!response.isSuccessful()) {
          throw new ScrapingException("Apify 호출 실패: " + response.code());
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
          throw new ScrapingException("Apify 응답 바디가 비어있습니다.");
        }
        JsonNode items = objectMapper.readTree(responseBody.string());

        if (!items.isArray() || items.isEmpty()) {
          throw new ScrapingException("Instagram 데이터 없음");
        }

        JsonNode item = items.get(0);

        String caption = item.path("caption").asText(null);
        String imageUrl = item.path("displayUrl").asText(null);

        // 릴스면 videoUrl 우선
        String videoUrl = item.path("videoUrl").asText(null);
        if (videoUrl != null && !videoUrl.isBlank()) {
          imageUrl = videoUrl;
        }

        return ScrapedContent.of(
            caption != null ? caption.substring(0, Math.min(100, caption.length())) : "Instagram Post",
            caption,
            imageUrl);
      }

    } catch (Exception e) {
      log.error("Instagram scraping failed: {}", normalizedUrl, e); // 정규화된 URL로 로그
      throw new ScrapingException("Instagram 스크래핑 실패", e);
    }
  }

  private String normalizeInstagramUrl(String url) {
    if (url == null)
      return null;

    // utm, igsh 등 쿼리 파라미터 제거
    int idx = url.indexOf("?");
    String cleaned = idx > 0 ? url.substring(0, idx) : url;

    // 끝에 슬래시 보장 (Canonical URL)
    if (!cleaned.endsWith("/")) {
      cleaned += "/";
    }
    return cleaned;
  }

  @Override
  public boolean supports(String url) {
    if (url == null)
      return false;
    return url.contains("instagram.com/p/") ||
        url.contains("instagram.com/reel/") ||
        url.contains("instagram.com/tv/");
  }

  @Override
  public int priority() {
    return 10; // Default(999)보다 작으므로 우선순위 높음
  }
}
