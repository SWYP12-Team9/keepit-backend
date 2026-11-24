package swyp12.team9.server.domain.recommendation.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * 추천 API에서 반복되는 외부 검색/DB 조회 결과를 Redis에 캐싱한다.
 * - categoryRecommendationIds: 카테고리별 추천 후보 UserLink ID 목록
 * - userLinkIds: 사용자별 저장 Link ID 목록
 * 빈 카테고리 추천 결과는 색인 전 조회 결과가 Redis에 고정되지 않도록 캐시하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationCacheService {

    private final VectorStore vectorStore;
    private final UserLinkRepository userLinkRepository;
    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;

    private static final int MAX_TOP_K = 1000;
    private static final String CATEGORY_CACHE = "categoryRecommendationIds";

    // 캐시 miss 시 같은 카테고리의 동시 요청을 하나의 조회로 병합하기 위한 진행 중 조회 목록
    // ponytail: JVM 안에서만 병합한다. 서버가 여러 대면 서버마다 1번씩 조회하므로, 그때는 Redis 락(SET NX)으로 확장
    private final ConcurrentMap<String, CompletableFuture<List<Long>>> inFlightCategoryLoads = new ConcurrentHashMap<>();

    /**
     * 카테고리 추천 후보를 조회한다.
     * 캐시 miss 시 Elasticsearch vector search를 수행하고, 결과 UserLink ID 목록을 Redis에 저장한다.
     * 결과가 비어 있으면 추후 색인 데이터가 들어왔을 때 다시 조회할 수 있도록 캐시하지 않는다.
     *
     * 캐시 스탬피드 방지: 캐시가 빈 순간(만료, 전체 무효화) 같은 카테고리 요청이 몰리면 첫 요청만 벡터 검색을 하고
     * 나머지는 그 결과를 기다려 함께 받는다. @Cacheable(sync = true)는 unless(빈 결과 미캐싱)와 함께 쓸 수 없어 직접 구현했다.
     */
    @SuppressWarnings("unchecked")
    public List<Long> getCategoryRecommendationUserLinkIds(String category) {
        Cache cache = cacheManager.getCache(CATEGORY_CACHE);
        Cache.ValueWrapper cached = cache.get(category);
        if (cached != null) {
            return (List<Long>) cached.get();
        }

        CompletableFuture<List<Long>> myLoad = new CompletableFuture<>();
        CompletableFuture<List<Long>> runningLoad = inFlightCategoryLoads.putIfAbsent(category, myLoad);
        if (runningLoad != null) {
            try {
                return runningLoad.join();
            } catch (CompletionException e) {
                // 첫 요청의 조회 실패(ES 장애 등)를 그대로 전달해 호출부의 DB fallback이 동일하게 동작하게 한다.
                if (e.getCause() instanceof RuntimeException cause) {
                    throw cause;
                }
                throw e;
            }
        }

        try {
            // 병합 목록에 등록하기 직전에 다른 요청이 조회를 끝내고 캐시에 넣었을 수 있다.
            Cache.ValueWrapper filled = cache.get(category);
            List<Long> result = filled != null
                    ? (List<Long>) filled.get()
                    : loadCategoryRecommendationUserLinkIds(category);
            if (filled == null && result != null && !result.isEmpty()) {
                cache.put(category, result);
            }
            myLoad.complete(result);
            return result;
        } catch (RuntimeException e) {
            myLoad.completeExceptionally(e);
            throw e;
        } finally {
            inFlightCategoryLoads.remove(category, myLoad);
        }
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
     * 저장 링크가 없는 사용자는 빈 목록을 캐시해 반복 DB 조회를 줄인다.
     */
    @Cacheable(value = "userLinkIds", key = "#userId", unless = "#result == null")
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
    @CacheEvict(value = CATEGORY_CACHE, allEntries = true)
    public void evictCategoryRecommendationIds() {
    }

    /**
     * Elasticsearch metadata 값은 Integer/Long/String 등으로 역직렬화될 수 있어 Long으로 통일한다.
     * Redis 캐시 hit 이후의 ID 타입 정규화는 RecommendationService에서 한 번 더 수행한다.
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
