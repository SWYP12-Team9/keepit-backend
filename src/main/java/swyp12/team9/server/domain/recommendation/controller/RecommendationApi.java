package swyp12.team9.server.domain.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swyp12.team9.server.domain.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.util.PaginationUtils;

@Tag(name = "Recommendation", description = "추천 콘텐츠 API (Elasticsearch 벡터 검색) - 탐색 페이지")
@RequestMapping("/api/v1/recommendations")
public interface RecommendationApi {

    @Operation(summary = "카테고리 목록 조회", description = "탐색 탭에서 사용할 카테고리 목록을 반환합니다. (총 8개 카테고리)")
    @ApiSpec(status = HttpStatus.OK, errors = {})
    @GetMapping("/categories")
    ApiResponse<List<String>> getCategories();

    @Operation(summary = "키워드 검색", description = """
                    검색 키워드를 벡터 유사도 검색하여 관련성 높은 순서로 공개된 링크를 가져옵니다. (키워드는 1~50자 제한)
                    - Elasticsearch 벡터 검색 사용 (OpenAI 임베딩)
                    - 로그인한 사용자의 링크는 자동 제외
                    - 동일 링크는 한 번만 노출
                    - Elasticsearch 장애 시 DB 키워드 검색으로 자동 전환
                    - 응답의 category 필드는 null입니다. (카테고리별 추천 조회와 다름)
                    """)
    @ApiSpec(status = HttpStatus.OK, errors = {
            ErrorCode.VALIDATION_ERROR
    })
    @GetMapping("/search")
    ApiResponse<PaginationUtils.Cursor.PageResponse<RecommendationResponse>> searchByKeyword(
            @CurrentUserId(required = false) Long userId,
            @Parameter(description = "검색 키워드", example = "프론트엔드 성능 최적화") @RequestParam @NotBlank @Size(min = 1, max = 50) String keyword,
            @Parameter(description = "커서 (첫 요청 시 null)", example = "20") @RequestParam(required = false) String cursor,
            @Parameter(description = "가져올 결과 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    );

    @Operation(summary = "카테고리별 추천 콘텐츠 조회", description = """
                    카테고리명을 검색어로 유사도 높은 순서로 공개된 링크를 가져옵니다.
                    - Elasticsearch 벡터 검색 사용 (OpenAI 임베딩)
                    - '기타' 카테고리는 다양한 주제의 독특한 콘텐츠를 추천
                    - 로그인한 사용자의 링크는 자동 제외
                    - 동일 링크는 한 번만 노출
                    - Elasticsearch 장애 시 DB 키워드 검색으로 자동 전환
                    - 카테고리 지정 시 응답의 category 필드에 해당 카테고리명이 포함됩니다. (미지정 시 null)
                    """)
    @ApiSpec(status = HttpStatus.OK, errors = {
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.INVALID_CATEGORY
    })
    @GetMapping
    ApiResponse<PaginationUtils.Cursor.PageResponse<RecommendationResponse>> getRecommendationsByCategory(
            @CurrentUserId(required = false) Long userId,
            @Parameter(description = "카테고리명 (미지정 시 전체 조회)", example = "경제/시사") @RequestParam(required = false) String category,
            @Parameter(description = "커서 (첫 요청 시 null)", example = "20") @RequestParam(required = false) String cursor,
            @Parameter(description = "가져올 추천 콘텐츠 수", example = "10") @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    );


    @Operation(summary = "외부/공개 링크 조회수 증가", description = """
                    추천 탭이나 인기글 탭 등에서 링크 카드를 클릭했을 때 호출합니다.
                    특정 링크의 전역 공개 조회수(publicViewCount)를 1 증가시킵니다.
                    """)
    @ApiSpec(status = HttpStatus.OK, errors = {
            ErrorCode.LINK_NOT_FOUND
    })
    @PostMapping("/links/{linkId}/view")
    ApiResponse<Void> incrementPublicViewCount(
            @Parameter(description = "클릭한 링크 ID", example = "10") @PathVariable Long linkId
    );

    @Operation(summary = "[관리자] 전체 링크 색인", description = """
                    DB의 모든 공개 링크를 Elasticsearch에 색인합니다.
                    - 색인 대상: is_public=true인 UserLink
                    - title, aiSummary, why, memo를 통합하여 벡터화
                    - OpenAI API를 통해 임베딩 생성
                    """)
    @ApiSpec(status = HttpStatus.OK, errors = {
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED
    })
    @PostMapping("/index")
    ApiResponse<String> indexAllLinks();

    @Operation(summary = "[관리자] 단일 링크 색인", description = """
                    특정 링크를 참조하는 모든 공개 UserLink를 Elasticsearch에 색인합니다.
                    - 링크 정보 수정 시 재색인용
                    """)
    @ApiSpec(status = HttpStatus.OK, errors = {
            ErrorCode.UNAUTHORIZED,
            ErrorCode.ACCESS_DENIED,
            ErrorCode.LINK_NOT_FOUND
    })
    @PostMapping("/index/link")
    ApiResponse<String> indexLink(
            @Parameter(description = "링크 ID", example = "1") @RequestParam Long linkId
    );
}
