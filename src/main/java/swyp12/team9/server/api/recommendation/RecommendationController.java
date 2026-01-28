package swyp12.team9.server.api.recommendation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.link.model.LinkCategory;
import swyp12.team9.server.domain.recommendation.service.LinkIndexingService;
import swyp12.team9.server.domain.recommendation.service.RecommendationService;
import swyp12.team9.server.global.common.dto.ApiResponse;

@Tag(name = "Recommendation", description = "추천 콘텐츠 API (Elasticsearch 벡터 검색)")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final LinkIndexingService linkIndexingService;

    @Operation(
            summary = "카테고리 목록 조회",
            description = "탐색 탭에서 사용할 카테고리 목록을 반환합니다."
    )
    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.ok(LinkCategory.getAllDisplayNames());
    }

    @Operation(
            summary = "카테고리별 추천 콘텐츠 조회",
            description = "카테고리명을 검색어로 유사도 높은 순서로 공개된 링크를 가져옵니다."
    )
    @GetMapping
    public ApiResponse<List<RecommendationResponse>> getRecommendationsByCategory(
            @Parameter(description = "카테고리명 (경제/시사, 뷰티/패션 등)", example = "경제/시사")
            @RequestParam String category,
            @Parameter(description = "가져올 추천 콘텐츠 수", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        List<RecommendationResponse> recommendations = recommendationService.getRecommendationsByCategory(category,
                size);
        return ApiResponse.ok(recommendations);
    }

    @Operation(
            summary = "[관리자] 전체 링크 색인",
            description = "DB의 모든 링크를 Elasticsearch에 색인합니다. (title, description, aiSummary 합쳐서 벡터화)"
    )
    @PostMapping("/index")
    public ApiResponse<String> indexAllLinks() {
        linkIndexingService.indexAllLinks();
        return ApiResponse.ok("색인이 완료되었습니다.");
    }

    @Operation(
            summary = "[관리자] 단일 링크 색인",
            description = "특정 링크를 Elasticsearch에 색인합니다."
    )
    @PostMapping("/index/link")
    public ApiResponse<String> indexLink(
            @Parameter(description = "링크 ID", example = "1")
            @RequestParam Long linkId
    ) {
        linkIndexingService.indexLink(linkId);
        return ApiResponse.ok("링크 ID " + linkId + " 색인이 완료되었습니다.");
    }
}
