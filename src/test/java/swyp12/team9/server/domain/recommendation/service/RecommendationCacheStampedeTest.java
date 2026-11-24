package swyp12.team9.server.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.config.RedisConfig;

/**
 * 카테고리 추천 후보 캐시가 비었을 때 동시 요청이 몰리면 Elasticsearch 벡터 검색이 몇 번 실행되는지 측정한다.
 * 실제 RedisCacheManager(RedisConfig)와 Spring 캐시 프록시를 쓰고, 벡터 검색만 지연을 준 가짜로 대체한다
 * (카테고리명 임베딩 호출 + ES 검색을 VECTOR_SEARCH_DELAY_MS로 가정).
 * Redis가 없으면 건너뛴다. REDIS_TEST_PORT를 지정했을 때만 실행 (다른 로컬 Redis를 건드리지 않도록 기본 포트 없음), DB 14 사용 후 비움.
 */
class RecommendationCacheStampedeTest {

    private static final long VECTOR_SEARCH_DELAY_MS = 300;
    private static final int ROUNDS = 5;

    private static AnnotationConfigApplicationContext context;
    private static RecommendationCacheService cacheService;
    private static CacheManager cacheManager;
    private static final AtomicInteger vectorSearchCalls = new AtomicInteger();

    @Configuration
    @EnableCaching
    @Import({RedisConfig.class, RecommendationCacheService.class})
    static class TestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            int port = Integer.parseInt(System.getenv("REDIS_TEST_PORT"));
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", port);
            config.setDatabase(14);
            return new LettuceConnectionFactory(config);
        }

        @Bean
        VectorStore vectorStore() {
            VectorStore vectorStore = mock(VectorStore.class);
            stubSearch(vectorStore, 50);
            return vectorStore;
        }

        @Bean
        UserLinkRepository userLinkRepository() {
            return mock(UserLinkRepository.class);
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

    }

    @BeforeAll
    static void startContext() {
        assumeTrue(System.getenv("REDIS_TEST_PORT") != null, "REDIS_TEST_PORT 미지정 - 캐시 스탬피드 테스트 건너뜀");
        try {
            context = new AnnotationConfigApplicationContext(TestConfig.class);
            context.getBean(RedisConnectionFactory.class).getConnection().ping();
        } catch (Exception e) {
            if (context != null) {
                context.close();
            }
            context = null;
        }
        assumeTrue(context != null, "Redis 미기동 - 캐시 스탬피드 테스트 건너뜀");
        cacheService = context.getBean(RecommendationCacheService.class);
        cacheManager = context.getBean(CacheManager.class);
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            cacheService.evictCategoryRecommendationIds();
            context.close();
        }
    }

    @BeforeEach
    void clear() {
        cacheService.evictCategoryRecommendationIds();
        vectorSearchCalls.set(0);
    }

    @Test
    void 캐시가_빈_카테고리에_동시_요청_100건이_몰려도_벡터_검색은_1번만_실행된다() throws Exception {
        int threads = 100;
        List<Long> latencies = new ArrayList<>();
        int totalCalls = 0;

        for (int round = 0; round < ROUNDS; round++) {
            cacheService.evictCategoryRecommendationIds();
            vectorSearchCalls.set(0);
            latencies.addAll(fire(threads, i -> "경제/시사"));
            totalCalls += vectorSearchCalls.get();
            assertThat(cacheManager.getCache("categoryRecommendationIds").get("경제/시사")).isNotNull();
        }

        report("[같은 카테고리] 동시 " + threads + "건 x " + ROUNDS + "회", totalCalls, ROUNDS, latencies);
        assertThat(totalCalls).isEqualTo(ROUNDS);
    }

    @Test
    void 전체_무효화_직후_카테고리_10개에_요청이_몰려도_카테고리마다_벡터_검색은_1번이다() throws Exception {
        int categories = 10;
        int perCategory = 20;
        List<Long> latencies = new ArrayList<>();
        int totalCalls = 0;

        for (int round = 0; round < ROUNDS; round++) {
            cacheService.evictCategoryRecommendationIds();
            vectorSearchCalls.set(0);
            latencies.addAll(fire(categories * perCategory, i -> "category-" + (i % categories)));
            totalCalls += vectorSearchCalls.get();
        }

        report("[전체 무효화 후 카테고리 " + categories + "개 x " + perCategory + "건] x " + ROUNDS + "회",
                totalCalls, categories * ROUNDS, latencies);
        assertThat(totalCalls).isEqualTo(categories * ROUNDS);
    }

    @Test
    void 결과가_비면_캐싱하지_않고_다음_요청에서_다시_조회한다() throws Exception {
        VectorStore vectorStore = context.getBean(VectorStore.class);
        stubSearch(vectorStore, 0);
        try {
            fire(50, i -> "empty-category");
            int firstBurst = vectorSearchCalls.get();
            cacheService.getCategoryRecommendationUserLinkIds("empty-category");

            assertThat(firstBurst).isEqualTo(1);
            assertThat(vectorSearchCalls.get()).isEqualTo(2);
            assertThat(cacheManager.getCache("categoryRecommendationIds").get("empty-category")).isNull();
        } finally {
            stubSearch(vectorStore, 50);
        }
    }

    // 벡터 검색 1회 = 지연 후 문서 resultSize개 반환. 호출 횟수를 센다.
    private static void stubSearch(VectorStore vectorStore, int resultSize) {
        doAnswer(invocation -> {
            vectorSearchCalls.incrementAndGet();
            Thread.sleep(VECTOR_SEARCH_DELAY_MS);
            List<Document> docs = new ArrayList<>();
            for (long i = 1; i <= resultSize; i++) {
                docs.add(new Document("doc-" + i, "content", Map.of("linkId", i, "userLinkId", i)));
            }
            return docs;
        }).when(vectorStore).similaritySearch(any(SearchRequest.class));
    }

    private List<Long> fire(int threads, java.util.function.IntFunction<String> categoryOf) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                String category = categoryOf.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    long begin = System.nanoTime();
                    cacheService.getCategoryRecommendationUserLinkIds(category);
                    return (System.nanoTime() - begin) / 1_000_000;
                }));
            }
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            List<Long> latencies = new ArrayList<>();
            for (Future<Long> f : futures) {
                latencies.add(f.get(30, TimeUnit.SECONDS));
            }
            return latencies;
        } finally {
            pool.shutdownNow();
        }
    }

    private void report(String label, int calls, int ideal, List<Long> latencies) {
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
        System.out.printf("%s → 벡터 검색 %d회 (이상적 %d회), 요청 %d건 응답 avg %.1fms p95 %dms max %dms%n",
                label, calls, ideal, sorted.size(), avg, p95, sorted.getLast());
    }
}
