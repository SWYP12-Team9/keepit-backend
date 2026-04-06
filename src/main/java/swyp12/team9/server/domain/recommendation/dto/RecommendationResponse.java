package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * 추천 콘텐츠 응답 DTO
 */
import lombok.*;

/**
 * 추천 콘텐츠 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "추천 콘텐츠 응답")
public class RecommendationResponse {

    @Schema(description = "콘텐츠 ID (Link ID)", example = "1")
    private Long id;

    @Schema(description = "URL", example = "https://example.com/article")
    private String url;

    @Schema(description = "제목", example = "추천 아티클 제목")
    private String title;

    @Schema(description = "AI 요약", example = "이 글의 핵심 요약...")
    private String aiSummary;

    @Schema(description = "카테고리 정보")
    private CategoryInfo category;

    @Schema(description = "첫 발견자 정보 (가장 먼저 공개 저장한 사용자)")
    private UserInfo user;

    @Schema(description = "전역 공개 조회수 (인기글 탭에서만 제공)", example = "128")
    private Long publicViewCount;

    public static RecommendationResponse from(Link link, UserLink firstUserLink, String category) {
        return RecommendationResponse.builder()
                .id(link.getId())
                .url(link.getUrl())
                .title(link.getTitle())
                .aiSummary(link.getAiSummary())
                .category(category != null ? CategoryInfo.from(category) : null)
                .user(UserInfo.from(firstUserLink.getUser()))
                .build();
    }

    public static RecommendationResponse fromPopular(Link link, UserLink firstUserLink, Long publicViewCount) {
        return RecommendationResponse.builder()
                .id(link.getId())
                .url(link.getUrl())
                .title(link.getTitle())
                .aiSummary(link.getAiSummary())
                .user(UserInfo.from(firstUserLink.getUser()))
                .publicViewCount(publicViewCount)
                .build();
    }
}
