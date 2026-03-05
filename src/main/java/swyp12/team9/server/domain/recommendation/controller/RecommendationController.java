package swyp12.team9.server.domain.recommendation.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.domain.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.link.exception.InvalidCategoryException;
import swyp12.team9.server.domain.link.model.LinkCategory;
import swyp12.team9.server.domain.recommendation.service.LinkIndexingService;
import swyp12.team9.server.domain.recommendation.service.RecommendationService;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.util.PaginationUtils;

@Validated
@RestController
@RequiredArgsConstructor
public class RecommendationController implements RecommendationApi {

    private final RecommendationService recommendationService;
    private final LinkIndexingService linkIndexingService;

    @Override
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.ok(LinkCategory.getAllDisplayNames());
    }

    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<RecommendationResponse>> searchByKeyword(
            Long userId,
            String keyword,
            String cursor,
            int size) {
        PaginationUtils.Cursor.PageResponse<RecommendationResponse> searchResults =
                recommendationService.searchByKeyword(userId, keyword, cursor, size);
        return ApiResponse.ok(searchResults);
    }

    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<RecommendationResponse>> getRecommendationsByCategory(
            Long userId,
            String category,
            String cursor,
            int size) {
        PaginationUtils.Cursor.PageResponse<RecommendationResponse> recommendations;

        // 카테고리 미지정 시 전체 최신 공개 링크 반환
        if (category == null || category.isBlank()) {
            recommendations = recommendationService.getRecentPublicLinks(userId, cursor, size);
        } else {
            LinkCategory linkCategory = LinkCategory.fromDisplayName(category)
                    .orElseThrow(InvalidCategoryException::new);

            // '기타' 카테고리는 혼합 키워드로 벡터 검색 (독특한 콘텐츠 추천)
            if (linkCategory == LinkCategory.ETC) {
                String etcKeywords = "독특한 새로운 흥미로운 트렌드 화제";
                recommendations = recommendationService.getRecommendationsByCategory(userId, etcKeywords, cursor, size);
            } else {
                recommendations = recommendationService.getRecommendationsByCategory(userId, category, cursor, size);
            }
        }

        return ApiResponse.ok(recommendations);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> indexAllLinks() {
        linkIndexingService.indexAllLinks();
        return ApiResponse.ok("색인이 완료되었습니다.");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> indexLink(
            Long linkId) {
        linkIndexingService.indexLink(linkId);
        return ApiResponse.ok("링크 ID " + linkId + " 색인이 완료되었습니다.");
    }
}
