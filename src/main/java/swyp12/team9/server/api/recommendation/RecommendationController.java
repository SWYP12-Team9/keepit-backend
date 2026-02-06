package swyp12.team9.server.api.recommendation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.link.exception.InvalidCategoryException;
import swyp12.team9.server.domain.link.model.LinkCategory;
import swyp12.team9.server.domain.recommendation.service.LinkIndexingService;
import swyp12.team9.server.domain.recommendation.service.RecommendationService;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;

@Tag(name = "Recommendation", description = "추천 콘텐츠 API (Elasticsearch 벡터 검색) - 탐색 페이지")
@Validated
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final LinkIndexingService linkIndexingService;

    @Operation(
            summary = "카테고리 목록 조회",
            description = "탐색 탭에서 사용할 카테고리 목록을 반환합니다. (총 8개 카테고리)"
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {}
    )
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.ok(LinkCategory.getAllDisplayNames());
    }

    @Operation(
            summary = "키워드 검색",
            description = """
                    검색 키워드를 벡터 유사도 검색하여 관련성 높은 순서로 공개된 링크를 가져옵니다. (키워드는 1~50자 제한)
                    - Elasticsearch 벡터 검색 사용 (OpenAI 임베딩)
                    - 로그인한 사용자의 링크는 자동 제외
                    - 동일 링크는 한 번만 노출
                    - Elasticsearch 장애 시 DB 키워드 검색으로 자동 전환
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR
            }
    )
    @GetMapping("/search")
    public ApiResponse<List<RecommendationResponse>> searchByKeyword(
            @CurrentUserId(required = false) Long userId,
            @Parameter(description = "검색 키워드", example = "프론트엔드 성능 최적화")
            @RequestParam @NotBlank @Size(min = 1, max = 50) String keyword,
            @Parameter(description = "가져올 결과 수", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        List<RecommendationResponse> searchResults = recommendationService.searchByKeyword(userId, keyword, size);
        return ApiResponse.ok(searchResults);
    }

    @Operation(
            summary = "추천 콘텐츠 조회 (전체/카테고리별)",
            description = """
                    공개된 링크를 추천합니다.

                    **카테고리 파라미터:**
                    - 미지정 시: 전체 최신 공개 링크 반환 (전체 탭)
                    - 지정 시: 해당 카테고리 유사도 높은 순 반환

                    **카테고리 목록:**
                    경제/시사, 뷰티/패션, 건강/운동, 여행/맛집, 문화/엔터, 자기계발, IT/테크, 기타

                    **동작 방식:**
                    - Elasticsearch 벡터 검색 사용 (OpenAI 임베딩)
                    - '기타' 카테고리 또는 미지정 시 최신 공개 링크 반환
                    - 로그인한 사용자의 링크는 자동 제외
                    - 동일 링크는 한 번만 노출
                    - Elasticsearch 장애 시 DB 키워드 검색으로 자동 전환
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.INVALID_CATEGORY
            }
    )
    @GetMapping
    public ApiResponse<List<RecommendationResponse>> getRecommendationsByCategory(
            @CurrentUserId(required = false) Long userId,
            @Parameter(description = "카테고리명 (미지정 시 전체 조회)", example = "경제/시사")
            @RequestParam(required = false) String category,
            @Parameter(description = "가져올 추천 콘텐츠 수", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        List<RecommendationResponse> recommendations;

        // 카테고리 미지정 시 전체 최신 공개 링크 반환
        if (category == null || category.isBlank()) {
            recommendations = recommendationService.getRecentPublicLinks(userId, size);
        } else {
            LinkCategory linkCategory = LinkCategory.fromDisplayName(category)
                    .orElseThrow(InvalidCategoryException::new);

            // '기타' 카테고리는 최신 공개 링크로 처리 (벡터 검색 대신 최신순)
            if (linkCategory == LinkCategory.ETC) {
                recommendations = recommendationService.getRecentPublicLinks(userId, size);
            } else {
                recommendations = recommendationService.getRecommendationsByCategory(userId, category, size);
            }
        }

        return ApiResponse.ok(recommendations);
    }

    @Operation(
            summary = "[관리자] 전체 링크 색인",
            description = """
                    DB의 모든 공개 링크를 Elasticsearch에 색인합니다.
                    - 색인 대상: is_public=true인 UserLink
                    - title, aiSummary, why, memo를 통합하여 벡터화
                    - OpenAI API를 통해 임베딩 생성
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.ACCESS_DENIED
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/index")
    public ApiResponse<String> indexAllLinks() {
        linkIndexingService.indexAllLinks();
        return ApiResponse.ok("색인이 완료되었습니다.");
    }

    @Operation(
            summary = "[관리자] 단일 링크 색인",
            description = """
                    특정 링크를 참조하는 모든 공개 UserLink를 Elasticsearch에 색인합니다.
                    - 링크 정보 수정 시 재색인용
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.ACCESS_DENIED,
                    ErrorCode.LINK_NOT_FOUND
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/index/link")
    public ApiResponse<String> indexLink(
            @Parameter(description = "링크 ID", example = "1")
            @RequestParam Long linkId
    ) {
        linkIndexingService.indexLink(linkId);
        return ApiResponse.ok("링크 ID " + linkId + " 색인이 완료되었습니다.");
    }
}
