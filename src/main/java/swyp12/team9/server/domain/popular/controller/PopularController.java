package swyp12.team9.server.domain.popular.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.domain.popular.service.PopularService;
import swyp12.team9.server.domain.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.util.PaginationUtils;

@Validated
@RestController
@RequiredArgsConstructor
public class PopularController implements PopularApi {

    private final PopularService popularService;

    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<RecommendationResponse>> getPopularLinks(
            Long userId,
            String cursor,
            int size) {
        PaginationUtils.Cursor.PageResponse<RecommendationResponse> popularLinks =
                popularService.getPopularPublicLinks(userId, cursor, size);
        return ApiResponse.ok(popularLinks);
    }
}
