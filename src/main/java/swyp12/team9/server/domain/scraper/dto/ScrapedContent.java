package swyp12.team9.server.domain.scraper.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 웹 스크래핑 결과를 담는 DTO
 */
@Schema(description = "스크래핑된 콘텐츠 정보")
public record ScrapedContent(
    @Schema(description = "제목", example = "Spring Boot 완벽 가이드") String title,

    @Schema(description = "설명", example = "Spring Boot 애플리케이션 개발을 위한 종합 가이드") String description,

    @Schema(description = "이미지 URL", example = "https://example.com/image.jpg") String imageUrl,

    @Schema(description = "본문 내용 (뉴스 기사 등)", example = "Spring Boot는...") String content) {
  public static ScrapedContent empty() {
    return new ScrapedContent(null, null, null, null);
  }

  public static ScrapedContent of(String title, String description, String imageUrl) {
    return new ScrapedContent(title, description, imageUrl, null);
  }

  public static ScrapedContent withContent(String title, String description, String imageUrl, String content) {
    return new ScrapedContent(title, description, imageUrl, content);
  }
}
