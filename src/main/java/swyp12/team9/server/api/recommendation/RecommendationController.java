package swyp12.team9.server.api.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import swyp12.team9.server.api.recommendation.dto.SimilarContentResponse;
import swyp12.team9.server.domain.recommendation.service.RecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {

  private final RecommendationService recommendationService;

  /**
   * 테스트용 목데이터를 Elasticsearch에 적재합니다.
   */
  @GetMapping("/seed")
  public String seed() {
    recommendationService.seedData();
    return "Seed data indexed successfully!";
  }

  /**
   * 사용자가 읽은 글 ID 목록을 전달받아 유사 콘텐츠 추천 목록을 반환합니다.
   * 예: /api/recommend?readIds=1,5
   */
  @GetMapping
  public List<SimilarContentResponse> getSimilarContents(
      @RequestParam(value = "readIds", required = false) List<Long> readIds) {
    if (readIds == null || readIds.isEmpty()) {
      return recommendationService.recommend(new float[] { 0.0f, 0.0f });
    }

    float[] interestTemplate = recommendationService.calculateUserInterest(readIds);
    return recommendationService.recommend(interestTemplate);
  }
}
