package swyp12.team9.server.api.recommendation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swyp12.team9.server.api.recommendation.dto.SimilarContentResponse;
import swyp12.team9.server.global.annotation.CurrentUserId;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Tag(name = "Recommendation", description = "추천 시스템 API")
@RequestMapping("/api/v1/recommendations")
public interface RecommendationApi {

    @Operation(summary = "폴더 기반 추천", description = "특정 폴더 내 링크들을 기반으로 유사한 링크를 추천합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 목록 조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SimilarContentResponse.class)))),
            @ApiResponse(responseCode = "404", description = "폴더를 찾을 수 없음")
    })
    @GetMapping("/folder/{referenceId}")
    swyp12.team9.server.global.common.dto.ApiResponse<List<SimilarContentResponse>> recommendByFolder(
            @Parameter(description = "레퍼런스(폴더) ID", example = "1") @PathVariable @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long referenceId,
            @Parameter(description = "추천 개수", example = "10") @RequestParam(defaultValue = "10") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(50) int size,
            @CurrentUserId Long userId);

    @Operation(summary = "샘플 데이터 적재", description = "테스트용 샘플 데이터를 Elasticsearch에 색인합니다.")
    @GetMapping("/seed")
    swyp12.team9.server.global.common.dto.ApiResponse<String> seedData();

    @Deprecated
    @Operation(summary = "[Deprecated] 기존 추천 API", description = "이전 버전의 추천 API입니다. /folder/{referenceId}를 사용하세요.")
    @GetMapping
    swyp12.team9.server.global.common.dto.ApiResponse<List<SimilarContentResponse>> recommend(
            @RequestParam List<Long> readIds);
}
