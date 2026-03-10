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

        @Schema(description = "URL", example = "https://example.com/article")
        String url,

        @Schema(description = "제목", example = "추천 아티클 제목")
        String title,

        @Schema(description = "AI 요약", example = "이 글의 핵심 요약...")
        String aiSummary,

        @Schema(description = "카테고리 정보")
        CategoryInfo category,

        @Schema(description = "첫 발견자 정보 (가장 먼저 공개 저장한 사용자)")
        UserInfo user,

        @Schema(description = "조회수 (인기글 탭에서만 제공)", example = "128")
        Long viewCount
) {
    public static RecommendationResponse from(Link link, UserLink firstUserLink, String category) {
        return new RecommendationResponse(
                link.getId(),
                link.getUrl(),
                link.getTitle(),
                link.getAiSummary(),
                category != null ? CategoryInfo.from(category) : null,
                UserInfo.from(firstUserLink.getUser()),
                null
        );
    }

    public static RecommendationResponse fromPopular(Link link, UserLink firstUserLink, Long viewCount) {
        return new RecommendationResponse(
                link.getId(),
                link.getUrl(),
                link.getTitle(),
                link.getAiSummary(),
                null,
                UserInfo.from(firstUserLink.getUser()),
                viewCount
        );
    }
}
