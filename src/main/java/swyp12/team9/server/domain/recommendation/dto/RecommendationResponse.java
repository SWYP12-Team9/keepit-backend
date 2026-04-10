package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * 추천 콘텐츠 응답 DTO
 */
@Schema(description = "추천 콘텐츠 응답")
public record RecommendationResponse(
        @Schema(description = "콘텐츠 ID (Link ID)", example = "1")
        Long id,

        @Schema(description = "콘텐츠 제목", example = "개발자 커리어 가이드")
        String title,

        @Schema(description = "콘텐츠 URL", example = "https://example.com/dev-guide")
        String url,

        @Schema(description = "썸네일 이미지 URL (favicon)", example = "https://example.com/favicon.png")
        String thumbnail,

        @Schema(description = "AI 요약 내용")
        String aiSummary,

        @Schema(description = "전역 공개 조회수", example = "150")
        Long publicViewCount,

        @Schema(description = "관련 카테고리")
        String category,

        @Schema(description = "최초 등록자 정보")
        UserInfo user
) {
    public static RecommendationResponse from(Link link, UserLink firstUserLink, String category) {
        return new RecommendationResponse(
                link.getId(),
                link.getTitle(),
                link.getUrl(),
                link.getFaviconUrl(),
                link.getAiSummary(),
                link.getPublicViewCount(),
                category,
                UserInfo.from(firstUserLink.getUser())
        );
    }

    public static RecommendationResponse fromPopular(Link link, UserLink firstUserLink, Long publicViewCount) {
        return new RecommendationResponse(
                link.getId(),
                link.getTitle(),
                link.getUrl(),
                link.getFaviconUrl(),
                link.getAiSummary(),
                publicViewCount,
                null, 
                UserInfo.from(firstUserLink.getUser())
        );
    }
}
