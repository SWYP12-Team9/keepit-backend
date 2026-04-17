package swyp12.team9.server.domain.popular.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.popular.dto.PopularLinkProjection;
import swyp12.team9.server.domain.popular.repository.PopularRepository;
import swyp12.team9.server.domain.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.global.util.PaginationUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularService {

    private final PopularRepository popularRepository;

    /**
     * 인기 공개 링크 조회 (링크별 전역 공개 조회수 기준)
     * - Link 기준 전역 공개 조회수(publicViewCount) 내림차순 정렬
     * - 커서 기반 페이징: publicViewCount:linkId
     *
     * @param userId 현재 로그인한 사용자 ID (현재는 필터링에 사용하지 않음)
     * @param cursor 커서 (형식: publicViewCount:linkId)
     * @param size   페이지 크기
     * @return 인기 콘텐츠 목록
     */
    @Cacheable(
            value = "popularLinks",
            sync = true,
            key = "'cursor:' + (#cursor != null ? #cursor : 'null') + '_size:' + #size"
    )
    public PaginationUtils.Cursor.PageResponse<RecommendationResponse> getPopularPublicLinks(Long userId,
            String cursor,
            int size) {
        PopularCursor popularCursor = parsePopularCursor(cursor);

        List<PopularLinkProjection> results = popularRepository.findPopularPublicLinks(
                popularCursor != null ? popularCursor.publicViewCount() : null,
                popularCursor != null ? popularCursor.linkId() : null,
                size);

        if (results.isEmpty()) {
            return PaginationUtils.Cursor.PageResponse.empty();
        }

        boolean hasNext = results.size() > size;
        List<PopularLinkProjection> pageContents = hasNext
                ? results.subList(0, size)
                : results;

        List<RecommendationResponse> responses = buildPopularResponses(pageContents);

        if (responses.isEmpty()) {
            return PaginationUtils.Cursor.PageResponse.empty();
        }

        String nextCursor = null;
        if (hasNext) {
            PopularLinkProjection last = pageContents.getLast();
            nextCursor = toPopularCursor(last);
        }

        return PaginationUtils.Cursor.PageResponse.of(responses, nextCursor, hasNext);
    }

    /**
     * 인기 링크 응답 생성
     * - 링크별 첫 공개 저장자(가장 빠른 createdAt)를 user 정보로 사용
     * - projection 순서(link 인기순)를 그대로 유지
     */
    private List<RecommendationResponse> buildPopularResponses(List<PopularLinkProjection> projections) {
        List<Long> linkIds = projections.stream()
                .map(PopularLinkProjection::linkId)
                .toList();

        // DB 레벨에서 링크별 첫 번째 공개 저장자(최초 등록자)만 1건씩 조회 (성능 최적화)
        List<UserLink> firstUserLinks = popularRepository.findFirstPublicUserLinksByLinkIds(linkIds);
        Map<Long, UserLink> firstUserLinkByLinkId = firstUserLinks.stream()
                .collect(Collectors.toMap(ul -> ul.getLink().getId(), ul -> ul));

        Map<Long, Long> publicViewCountByLinkId = projections.stream()
                .collect(Collectors.toMap(PopularLinkProjection::linkId, PopularLinkProjection::publicViewCount));

        return linkIds.stream()
                .map(linkId -> {
                    UserLink firstUserLink = firstUserLinkByLinkId.get(linkId);
                    Long publicViewCount = publicViewCountByLinkId.get(linkId);
                    if (firstUserLink == null || publicViewCount == null) {
                        return null;
                    }
                    return RecommendationResponse.fromPopular(firstUserLink.getLink(), firstUserLink, publicViewCount);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private PopularCursor parsePopularCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        String[] parts = cursor.split(":");
        if (parts.length != 2) {
            log.warn("잘못된 인기글 커서 형식: {}", cursor);
            return null;
        }

        try {
            long publicViewCount = Long.parseLong(parts[0]);
            long linkId = Long.parseLong(parts[1]);
            if (publicViewCount < 0 || linkId <= 0) {
                return null;
            }
            return new PopularCursor(publicViewCount, linkId);
        } catch (NumberFormatException e) {
            log.warn("잘못된 인기글 커서 형식: {}", cursor);
            return null;
        }
    }

    private String toPopularCursor(PopularLinkProjection projection) {
        return projection.publicViewCount() + ":" + projection.linkId();
    }

    private record PopularCursor(
            Long publicViewCount,
            Long linkId) {
    }
}
