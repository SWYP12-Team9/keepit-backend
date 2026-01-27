package swyp12.team9.server.api.recommendation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.recommendation.service.RecommendationService;
import swyp12.team9.server.global.common.dto.ApiResponse;

@Tag(name = "Recommendation", description = "추천 콘텐츠 API (파이썬 서버 연동)")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "개인화 추천 콘텐츠 조회",
            description = "사용자가 읽은 링크 ID 목록을 기반으로 추천 콘텐츠를 가져옵니다."
    )
    @GetMapping
    public ApiResponse<List<RecommendationResponse>> getRecommendations(
            @Parameter(description = "읽은 링크 ID 목록", example = "1,2,3")
            @RequestParam(required = false) List<Long> readIds,
            @Parameter(description = "가져올 추천 콘텐츠 수", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        List<RecommendationResponse> recommendations = recommendationService.getRecommendations(readIds, size);
        return ApiResponse.ok(recommendations);
    }

    @Operation(
            summary = "카테고리별 추천 콘텐츠 조회",
            description = "특정 카테고리의 추천 콘텐츠를 가져옵니다."
    )
    @GetMapping("/category")
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
}
