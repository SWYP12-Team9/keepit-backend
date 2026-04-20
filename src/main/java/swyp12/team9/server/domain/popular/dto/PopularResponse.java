package swyp12.team9.server.domain.popular.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * 인기 콘텐츠 응답 DTO (인기글 탭 전용)
 */
@Schema(description = "인기 콘텐츠 응답")
public record PopularResponse(

        @Schema(description = "콘텐츠 ID (Link ID)", example = "1")
        Long id,

        @Schema(description = "URL", example = "https://example.com/article")
        String url,

        @Schema(description = "제목", example = "인기 아티클 제목")
        String title,

        @Schema(description = "AI 요약", example = "이 글의 핵심 요약...")
        String aiSummary,

        @Schema(description = "카테고리 정보")
        PopularCategoryInfo category,

        @Schema(description = "첫 발견자 정보 (가장 먼저 공개 저장한 사용자)")
        PopularUserInfo user,

        @Schema(description = "전역 공개 조회수", example = "128")
        Long publicViewCount
) {
    public static PopularResponse from(Link link, UserLink firstUserLink, Long publicViewCount) {
        return new PopularResponse(
                link.getId(),
                link.getUrl(),
                link.getTitle(),
                link.getAiSummary(),
                null, // 인기글은 현재 전역 조회수 기반이므로 특정 카테고리는 null 처리 (필요시 추가)
                PopularUserInfo.from(firstUserLink.getUser()),
                publicViewCount
        );
    }
}
