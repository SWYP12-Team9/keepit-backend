package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * 키워드 검색 응답 DTO (카테고리 필드 없음)
 */
@Schema(description = "키워드 검색 응답")
public record RecommendationSearchResponse(

        @Schema(description = "콘텐츠 ID (Link ID)", example = "1")
        Long id,

        @Schema(description = "URL", example = "https://example.com/article")
        String url,

        @Schema(description = "제목", example = "추천 아티클 제목")
        String title,

        @Schema(description = "AI 요약", example = "이 글의 핵심 요약...")
        String aiSummary,

        @Schema(description = "첫 발견자 정보 (가장 먼저 공개 저장한 사용자)")
        UserInfo user
) {
    public static RecommendationSearchResponse from(Link link, UserLink firstUserLink) {
        return new RecommendationSearchResponse(
                link.getId(),
                link.getUrl(),
                link.getTitle(),
                link.getAiSummary(),
                UserInfo.from(firstUserLink.getUser())
        );
    }
}
