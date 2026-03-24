package swyp12.team9.server.domain.userlink.dto;

/**
 * 공개 링크 인기글 집계용 프로젝션
 *
 * @param linkId           Link ID
 * @param publicViewCount 링크별 전역 공개 조회수 (Link 엔티티의 publicViewCount)
 */
public record PopularLinkProjection(
        Long linkId,
        Long publicViewCount
) {
}
