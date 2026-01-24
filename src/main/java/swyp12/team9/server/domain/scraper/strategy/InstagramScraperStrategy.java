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
import java.util.concurrent.TimeUnit;

/**
 * Apify API를 사용한 인스타그램 스크래퍼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstagramScraperStrategy implements ScraperStrategy {

  private static final String INSTAGRAM_DOMAIN = "instagram.com";
  private static final String APIFY_API_URL = "https://api.apify.com/v2/acts/%s/run-sync-get-dataset-items";
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  @Value("${scraper.apify.api-key:}")
  private String apifyApiKey;

  @Value("${scraper.apify.instagram-actor-id:apify/instagram-scraper}")
  private String instagramActorId;

  private final OkHttpClient httpClient = new OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build();

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public ScrapedContent scrape(String url) throws ScrapingException {
    if (apifyApiKey == null || apifyApiKey.isBlank()) {
      log.warn("Apify API 키가 설정되지 않음 - 인스타그램 스크래핑 스킵");
      throw new ScrapingException("Apify API 키가 설정되지 않았습니다");
    }

    try {
      log.debug("인스타그램 스크래핑 시작 - url: {}", url);

      // Apify API 요청 바디 구성
      String requestBody = String.format("""
          {
            "directUrls": ["%s"],
            "resultsType": "posts"
          }
          """, url);

      String apiUrl = String.format(APIFY_API_URL, instagramActorId) + "?token=" + apifyApiKey;

      Request request = new Request.Builder()
          .url(apiUrl)
          .post(RequestBody.create(requestBody, JSON))
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          throw new ScrapingException("Apify API 호출 실패: " + response.code());
        }

        String responseBody = response.body().string();
        JsonNode items = objectMapper.readTree(responseBody);

        if (items.isArray() && items.size() > 0) {
          JsonNode item = items.get(0);

          String caption = item.path("caption").asText(null);
          String imageUrl = item.path("displayUrl").asText(null);

          // 릴스인 경우 비디오 URL도 추출
          String videoUrl = item.path("videoUrl").asText(null);
          if (videoUrl != null && !videoUrl.isBlank()) {
            imageUrl = videoUrl; // 비디오 썸네일 대신 비디오 URL 사용
          }

          log.info("인스타그램 스크래핑 완료 - url: {}, caption: {}", url,
              caption != null ? caption.substring(0, Math.min(50, caption.length())) : "없음");

          return ScrapedContent.of(
              caption != null ? caption.substring(0, Math.min(100, caption.length())) : "Instagram Post",
              caption,
              imageUrl);
        }

        throw new ScrapingException("Apify 응답에 데이터가 없습니다");
      }

    } catch (IOException e) {
      log.error("인스타그램 스크래핑 실패 - url: {}, error: {}", url, e.getMessage());
      throw new ScrapingException("인스타그램 스크래핑 실패: " + url, e);
    }
  }

  @Override
  public boolean supports(String url) {
    return url != null && url.contains(INSTAGRAM_DOMAIN);
  }

  @Override
  public int priority() {
    return 5; // 매우 높은 우선순위
  }
}
