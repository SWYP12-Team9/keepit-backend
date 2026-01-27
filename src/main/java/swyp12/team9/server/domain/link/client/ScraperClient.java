package swyp12.team9.server.domain.link.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperClient {

  private final ObjectMapper objectMapper;
  private final OkHttpClient okHttpClient = new OkHttpClient();

  @Value("${scraper.url:http://localhost:8000/api/scrape}")
  private String scraperUrl;

  public ScrapingResponse scrapeUrl(String url) {
    try {
      String requestBody = objectMapper.writeValueAsString(Map.of("url", url));
      Request request = new Request.Builder()
          .url(scraperUrl)
          .post(RequestBody.create(requestBody, MediaType.get("application/json")))
          .build();

      try (Response response = okHttpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          log.error("Scraper API failed: code={}, body={}", response.code(),
              response.body() != null ? response.body().string() : "null");
          return null;
        }

        if (response.body() != null) {
          String responseString = response.body().string();
          JsonNode root = objectMapper.readTree(responseString);

          // 파이썬 서버의 응답 필드명을 snake_case로 가정하고 매핑
          String title = getText(root, "title");
          String description = getText(root, "description");
          String imageUrl = getText(root, "preview_image_url"); // 이미지 1 참조하여 preview_image_url로 추정
          if (imageUrl == null)
            imageUrl = getText(root, "image_url"); // fallback
          String aiSummary = getText(root, "ai_summary");

          return new ScrapingResponse(title, description, imageUrl, aiSummary);
        }
      }
    } catch (Exception e) {
      log.error("Scraping failed for URL: {}", url, e);
    }
    return null;
  }

  private String getText(JsonNode node, String fieldName) {
    if (node.has(fieldName) && !node.get(fieldName).isNull()) {
      return node.get(fieldName).asText();
    }
    return null;
  }
}
