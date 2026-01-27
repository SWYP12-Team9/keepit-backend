package swyp12.team9.server.domain.recommendation.service;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import swyp12.team9.server.api.recommendation.dto.RecommendationResponse;

/**
 * 파이썬 서버와 연동하여 추천 콘텐츠를 가져오는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RestTemplate restTemplate;

    @Value("${python.server.url:http://localhost:8000}")
    private String pythonServerUrl;

    /**
     * 사용자가 읽은 링크 ID 목록을 기반으로 추천 콘텐츠를 가져옵니다.
     *
     * @param readLinkIds 사용자가 읽은 링크 ID 목록
     * @param size        가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록
     */
    public List<RecommendationResponse> getRecommendations(List<Long> readLinkIds, int size) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(pythonServerUrl)
                    .path("/api/recommend")
                    .queryParam("read_ids",
                            readLinkIds != null ? String.join(",", readLinkIds.stream().map(String::valueOf).toList())
                                    : "")
                    .queryParam("size", size)
                    .toUriString();

            log.debug("파이썬 서버 추천 API 호출: {}", url);

            ResponseEntity<List<RecommendationResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<RecommendationResponse>>() {
                    }
            );

            log.info("추천 콘텐츠 {} 개 조회 완료", response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();

        } catch (Exception e) {
            log.error("파이썬 서버 추천 API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 카테고리별 추천 콘텐츠를 가져옵니다.
     *
     * @param category 카테고리명
     * @param size     가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록
     */
    public List<RecommendationResponse> getRecommendationsByCategory(String category, int size) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(pythonServerUrl)
                    .path("/api/recommend/category")
                    .queryParam("category", category)
                    .queryParam("size", size)
                    .toUriString();

            log.debug("파이썬 서버 카테고리별 추천 API 호출: {}", url);

            ResponseEntity<List<RecommendationResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<RecommendationResponse>>() {
                    }
            );

            log.info("카테고리 [{}] 추천 콘텐츠 {} 개 조회 완료", category,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();

        } catch (Exception e) {
            log.error("파이썬 서버 카테고리별 추천 API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
