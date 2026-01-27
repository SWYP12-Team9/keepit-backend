package swyp12.team9.server.api.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 파이썬 서버에서 받아오는 추천 콘텐츠 응답 DTO
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

        @Schema(description = "썸네일 URL", example = "https://example.com/thumb.jpg")
        String thumbnailUrl,

        @Schema(description = "카테고리", example = "경제/시사")
        String category,

        @Schema(description = "추천 점수", example = "0.95")
        Double score
) {
}
