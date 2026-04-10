package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * 추천 콘텐츠 응답 DTO
 */
@Schema(description = "추천 콘텐츠 응답")
public record RecommendationResponse(
        @Schema(description = "콘텐츠 제목", example = "개발자 커리어 가이드")
        String title,

        @Schema(description = "콘텐츠 URL", example = "https://example.com/dev-guide")
        String url,

        @Schema(description = "썸네일 이미지 URL", example = "https://example.com/image.png")
        String thumbnail,

        @Schema(description = "AI 요약 내용")
        String aiSummary,

        @Schema(description = "조회수", example = "150")
        Long viewCount,

        @Schema(description = "관련 카테고리")
        String category,

        @Schema(description = "최초 등록자 정보")
        UserInfo userInfo
) {
    public static RecommendationResponse from(Link link, UserLink firstUserLink, String category) {
        return new RecommendationResponse(
                link.getTitle(),
                link.getUrl(),
                link.getThumbnail(),
                link.getAiSummary(),
                link.getPublicViewCount(), // 전체 링크의 전역 공개 조회수 사용
                category,
                UserInfo.from(firstUserLink.getUser())
        );
    }

    public static RecommendationResponse fromPopular(Link link, UserLink firstUserLink, Long publicViewCount) {
        return new RecommendationResponse(
                link.getTitle(),
                link.getUrl(),
                link.getThumbnail(),
                link.getAiSummary(),
                publicViewCount, // 인기순 정렬에 사용된 조회수를 우선 사용
                null,            // 인기 탭은 특정 카테고리가 없음
                UserInfo.from(firstUserLink.getUser())
        );
    }
}
