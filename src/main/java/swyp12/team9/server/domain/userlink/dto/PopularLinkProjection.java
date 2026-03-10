package swyp12.team9.server.domain.userlink.dto;

/**
 * 공개 링크 인기글 집계용 프로젝션
 *
 * @param linkId    Link ID
 * @param viewCount 공개 UserLink들의 조회수 합계
 */
public record PopularLinkProjection(
        Long linkId,
        Long viewCount
) {
}
