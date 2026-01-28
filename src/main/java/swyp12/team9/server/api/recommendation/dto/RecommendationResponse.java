package swyp12.team9.server.api.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.link.model.Link;

/**
 * DB에서 조회한 추천 콘텐츠 응답 DTO
 */
@Schema(description = "추천 콘텐츠 응답")
public record RecommendationResponse(

        @Schema(description = "콘텐츠 ID", example = "1")
        Long id,

        @Schema(description = "URL", example = "https://example.com/article")
        String url,

        @Schema(description = "제목", example = "추천 아티클 제목")
        String title,

        @Schema(description = "설명", example = "아티클 설명")
        String description,

        @Schema(description = "AI 요약", example = "이 글의 핵심 요약...")
        String aiSummary,

        @Schema(description = "썸네일 URL", example = "https://example.com/thumb.jpg")
        String thumbnailUrl,

        @Schema(description = "카테고리", example = "경제/시사")
        String category
) {
    public static RecommendationResponse from(Link link) {
        return new RecommendationResponse(
                link.getId(),
                link.getUrl(),
                link.getTitle(),
                link.getDescription(),
                link.getAiSummary(),
                link.getPreviewImageUrl(),
                link.getCategory() != null ? link.getCategory().getDisplayName() : null
        );
    }
}
