package swyp12.team9.server.domain.popular.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swyp12.team9.server.domain.popular.dto.PopularResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.util.PaginationUtils;

@Tag(name = "Popular", description = "인기 콘텐츠 API - 탐색 페이지")
@RequestMapping("/api/v1/popular-links")
public interface PopularApi {

    @Operation(summary = "인기글 조회", description = """
                    공개 링크를 조회수 합계 기준으로 인기순 조회합니다.
                    - 기준: 링크별 전역 공개 조회수 (publicViewCount)
                    - 정렬: publicViewCount 내림차순, 동률이면 linkId 내림차순
                    - 커서 형식: publicViewCount:linkId (예: 120:45)
                    """)
    @ApiSpec(status = HttpStatus.OK, errors = {
            ErrorCode.VALIDATION_ERROR
    })
    @GetMapping
    ApiResponse<PaginationUtils.Cursor.PageResponse<PopularResponse>> getPopularLinks(
            @CurrentUserId(required = false) Long userId,
            @Parameter(description = "커서 (첫 요청 시 null, 형식: publicViewCount:linkId)", example = "120:45")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "가져올 인기 콘텐츠 수", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    );
}
