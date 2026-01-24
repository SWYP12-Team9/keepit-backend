package swyp12.team9.server.api.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "유사 콘텐츠 검색 결과 객체")
public record SimilarContentResponse(
        @Schema(description = "추천 콘텐츠 상세 정보") RecommendationResponse content,

        @Schema(description = "유사도 점수", example = "0.95") double score) {
}
