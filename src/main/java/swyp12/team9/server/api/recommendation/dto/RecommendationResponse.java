package swyp12.team9.server.api.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "추천 콘텐츠 응답 객체")
public record RecommendationResponse(
        @Schema(description = "링크 아이디", example = "1") Long id,

        @Schema(description = "링크 제목", example = "자바 스트림 API") String title,

        @Schema(description = "벡터 데이터 (UI에서는 주로 null로 전달)", example = "[0.1, 0.2, ...]") float[] embedding) {
}
