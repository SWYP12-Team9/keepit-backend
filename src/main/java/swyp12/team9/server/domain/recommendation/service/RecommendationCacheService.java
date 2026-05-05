package swyp12.team9.server.domain.recommendation.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * 추천 API에서 반복되는 외부 검색/DB 조회 결과를 Redis에 캐싱한다.
 * - categoryRecommendationIds: 카테고리별 추천 후보 UserLink ID 목록
 * - userLinkIds: 사용자별 저장 Link ID 목록
 */
@Service
@RequiredArgsConstructor
public class RecommendationCacheService {

    private final VectorStore vectorStore;
    private final UserLinkRepository userLinkRepository;
    private final MeterRegistry meterRegistry;

    private static final int MAX_TOP_K = 1000;

    /**
     * 카테고리 추천 후보를 조회한다.
     * 캐시 miss 시 Elasticsearch vector search를 수행하고, 결과 UserLink ID 목록을 Redis에 저장한다.
     */
    @Cacheable(value = "categoryRecommendationIds", key = "#category")
    public List<Long> getCategoryRecommendationUserLinkIds(String category) {
        return loadCategoryRecommendationUserLinkIds(category);
    }

    /**
     * 카테고리별 추천 후보 UserLink ID 목록을 Elasticsearch에서 직접 조회한다.
     * 동일 Link가 여러 UserLink로 인덱싱된 경우, 추천 응답 중복을 막기 위해 첫 번째 Link만 유지한다.
     */
    public List<Long> loadCategoryRecommendationUserLinkIds(String category) {
        meterRegistry.counter("recommendation.category.source.calls", "source", "elasticsearch").increment();

        SearchRequest request = SearchRequest.builder()
                .query(category)
                .topK(MAX_TOP_K)
                .filterExpression("indexType == 'recommendation'")
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        Set<Long> seenLinkIds = new HashSet<>();

        return results.stream()
                .filter(doc -> {
                    Long linkId = getLongFromMetadata(doc.getMetadata(), "linkId");
                    return linkId != null && seenLinkIds.add(linkId);
                })
                .map(doc -> getLongFromMetadata(doc.getMetadata(), "userLinkId"))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 사용자가 이미 저장한 원본 Link ID 목록을 조회한다.
     * 추천/검색 응답에서 이미 저장한 링크를 제외하기 위해 UserLink ID가 아닌 Link ID를 캐싱한다.
     */
    @Cacheable(value = "userLinkIds", key = "#userId")
    public List<Long> getUserLinkIds(Long userId) {
        return loadUserLinkIds(userId);
    }

    /**
     * 사용자별 저장 Link ID 목록을 DB에서 직접 조회한다.
     */
    public List<Long> loadUserLinkIds(Long userId) {
        meterRegistry.counter("recommendation.user_link_ids.source.calls", "source", "db").increment();
        return userLinkRepository.findLinkIdsByUserId(userId);
    }

    /**
     * UserLink 생성/삭제처럼 사용자의 저장 링크 목록이 바뀐 경우 해당 사용자 캐시만 제거한다.
     */
    @CacheEvict(value = "userLinkIds", key = "#userId")
    public void evictUserLinkIds(Long userId) {
    }

    /**
     * 추천 인덱싱 결과가 바뀐 경우 카테고리 후보 캐시를 전체 제거한다.
     * 현재 카테고리 수가 적어 부분 무효화보다 전체 무효화가 단순하고 안전하다.
     */
    @CacheEvict(value = "categoryRecommendationIds", allEntries = true)
    public void evictCategoryRecommendationIds() {
    }

    /**
     * Elasticsearch metadata 값은 Integer/Long/String 등으로 역직렬화될 수 있어 Long으로 통일한다.
     */
    private Long getLongFromMetadata(java.util.Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
