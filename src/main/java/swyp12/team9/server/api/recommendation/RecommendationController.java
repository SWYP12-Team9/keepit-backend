package swyp12.team9.server.api.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.recommendation.dto.SimilarContentResponse;
import swyp12.team9.server.domain.recommendation.service.RecommendationService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RecommendationController implements RecommendationApi {

  private final RecommendationService recommendationService;

  @Override
  public ApiResponse<List<SimilarContentResponse>> recommendByFolder(Long referenceId, int size,
      @CurrentUserId Long userId) {
    log.info("폴더 기반 추천 요청 - userId: {}, referenceId: {}, size: {}",
        userId, referenceId, size);

    List<SimilarContentResponse> recommendations = recommendationService.recommendByFolder(userId, referenceId, size);

    return ApiResponse.ok(recommendations, "추천 목록을 조회했습니다.");
  }

  @Override
  public ApiResponse<String> seedData() {
    log.info("샘플 데이터 적재 요청");
    recommendationService.seedData();
    return ApiResponse.ok("Seed data indexed successfully!");
  }

  @Override
  @Deprecated
  public ApiResponse<List<SimilarContentResponse>> recommend(List<Long> readIds) {
    log.info("기존 추천 API 호출 (Deprecated) - readIds: {}", readIds);

    float[] userInterest = recommendationService.calculateUserInterest(readIds);
    List<SimilarContentResponse> recommendations = recommendationService.recommend(userInterest);

    return ApiResponse.ok(recommendations);
  }
}
