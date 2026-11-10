package swyp12.team9.server.domain.link.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;
import swyp12.team9.server.domain.link.infrastructure.LinkStreamRedisKeys;
import swyp12.team9.server.global.config.RedisStreamConfig;

/**
 * 실제 Redis에서 동일 linkId 동시 발행 시 Stream 작업이 1건만 생기는지 검증한다.
 * Spring 컨텍스트 없이 Producer/Gateway만 조립하므로 Consumer가 메시지를 가져가지 않는다.
 * Redis가 없으면 건너뛴다. 포트: REDIS_TEST_PORT (기본 6379), DB 15 사용 후 비움.
 */
class LinkStreamProducerConcurrencyTest {

    private static final int THREADS = 10;
    private static final int ROUNDS = 20;

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private LinkStreamProducer producer;
    private LinkProcessStreamGateway gateway;

    @BeforeAll
    static void connect() {
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_TEST_PORT", "6379"));
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", port);
        config.setDatabase(15);
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);

        boolean reachable;
        try {
            reachable = "PONG".equals(redis.execute(c -> c.ping(), true));
        } catch (Exception e) {
            reachable = false;
        }
        assumeTrue(reachable, "Redis 미기동 - 동시성 테스트 건너뜀");
    }

    @AfterAll
    static void close() {
        if (connectionFactory != null) {
            try {
                redis.execute(c -> { c.serverCommands().flushDb(); return null; }, true);
            } catch (Exception ignored) {
            }
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        redis.execute(c -> { c.serverCommands().flushDb(); return null; }, true);

        gateway = new LinkProcessStreamGateway(redis);
        ReflectionTestUtils.setField(gateway, "mainStreamMaxLen", 50_000L);
        producer = new LinkStreamProducer(gateway);
        ReflectionTestUtils.setField(producer, "processingStateTtlMinutes", 30L);
    }

    @Test
    void 서로_다른_사용자_10명이_같은_링크를_동시에_저장해도_작업은_1건만_발행된다() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            long linkId = round;
            fireConcurrently(i -> () -> producer.publishLinkProcessTask(linkId, "https://example.com/" + linkId, (long) i + 1));

            assertThat(notifyUserCount(linkId)).isEqualTo(THREADS);
        }

        long published = streamLength();
        System.out.printf("[다른 사용자] %d라운드 x 동시 %d건 = 요청 %d건 → 발행 %d건, 라운드당 대기자 %d명%n",
                ROUNDS, THREADS, ROUNDS * THREADS, published, THREADS);
        assertThat(published).isEqualTo(ROUNDS);
    }

    @Test
    void 같은_사용자가_10번_동시에_저장해도_작업은_1건만_발행된다() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            long linkId = round;
            fireConcurrently(i -> () -> producer.publishLinkProcessTask(linkId, "https://example.com/" + linkId, 1L));

            assertThat(notifyUserCount(linkId)).isEqualTo(1);
        }

        long published = streamLength();
        System.out.printf("[같은 사용자] %d라운드 x 동시 %d건 = 요청 %d건 → 발행 %d건, 라운드당 대기자 1명%n",
                ROUNDS, THREADS, ROUNDS * THREADS, published);
        assertThat(published).isEqualTo(ROUNDS);
    }

    @Test
    void 완료_처리시_대기자_10명을_모두_꺼내고_락을_풀어_다음_저장은_새로_발행된다() throws Exception {
        for (int round = 1; round <= ROUNDS; round++) {
            long linkId = round;
            fireConcurrently(i -> () -> producer.publishLinkProcessTask(linkId, "https://example.com/" + linkId, (long) i + 1));

            Set<Long> drained = gateway.drainTargetUsersAndClearState(linkId, "record-" + linkId);

            assertThat(drained).hasSize(THREADS);
            assertThat(redis.hasKey(LinkStreamRedisKeys.processingLockKey(linkId))).isFalse();
            assertThat(redis.hasKey(LinkStreamRedisKeys.notifyUsersKey(linkId))).isFalse();
        }
        long beforeResave = streamLength();
        producer.publishLinkProcessTask(1L, "https://example.com/1", 99L);

        System.out.printf("[완료 처리] %d라운드 모두 대기자 %d명 전원 반환 + 락 해제, 완료 후 재저장 시 발행 %d건%n",
                ROUNDS, THREADS, streamLength() - beforeResave);
        assertThat(streamLength() - beforeResave).isEqualTo(1);
    }

    @Test
    void 완료_처리_도중_새_요청이_끼어들어도_알림_대상이_누락되지_않는다() throws Exception {
        RaceResult atomic = raceDrainWithJoiners(id -> gateway.drainTargetUsersAndClearState(id, "record-" + id));
        // 대조군: Lua 없이 조회와 삭제를 따로 실행 (그 사이 1ms 간격)
        RaceResult split = raceDrainWithJoiners(this::nonAtomicDrain);

        System.out.printf("[경합-Lua] %d라운드, 신규 요청 %d건 → 누락 %d명, 재발행 발생 라운드 %d%n",
                RACE_ROUNDS, RACE_ROUNDS * THREADS, atomic.lost, atomic.republishedRounds);
        System.out.printf("[경합-분리 대조군] %d라운드, 신규 요청 %d건 → 누락 %d명%n",
                RACE_ROUNDS, RACE_ROUNDS * THREADS, split.lost);
        assertThat(atomic.lost).isZero();
    }

    private static final int RACE_ROUNDS = 200;

    private record RaceResult(long lost, long republishedRounds) {
    }

    // 사용자 0이 작업을 시작한 상태에서, 완료 처리(drain)와 신규 요청 10건을 동시에 출발시킨다.
    // 끼어든 요청이 락을 새로 잡았다면 그 작업도 완료됐다고 보고 남은 락이 없을 때까지 drain한다.
    private RaceResult raceDrainWithJoiners(Function<Long, Set<Long>> drain) throws Exception {
        redis.execute(c -> { c.serverCommands().flushDb(); return null; }, true);
        long lost = 0;
        long republishedRounds = 0;

        for (int round = 1; round <= RACE_ROUNDS; round++) {
            long linkId = round;
            producer.publishLinkProcessTask(linkId, "https://example.com/" + linkId, 0L);
            long before = streamLength();
            Set<Long> notified = ConcurrentHashMap.newKeySet();

            fireConcurrently(THREADS + 1, i -> () -> {
                if (i == THREADS) {
                    notified.addAll(drain.apply(linkId));
                } else {
                    producer.publishLinkProcessTask(linkId, "https://example.com/" + linkId, (long) i + 1);
                }
            });
            if (streamLength() > before) {
                republishedRounds++;
            }
            while (Boolean.TRUE.equals(redis.hasKey(LinkStreamRedisKeys.processingLockKey(linkId)))) {
                notified.addAll(drain.apply(linkId));
            }
            for (long userId = 0; userId <= THREADS; userId++) {
                if (!notified.contains(userId)) {
                    lost++;
                }
            }
        }
        return new RaceResult(lost, republishedRounds);
    }

    private Set<Long> nonAtomicDrain(Long linkId) {
        Set<String> members = redis.opsForSet().members(LinkStreamRedisKeys.notifyUsersKey(linkId));
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        redis.delete(List.of(LinkStreamRedisKeys.notifyUsersKey(linkId), LinkStreamRedisKeys.processingLockKey(linkId)));
        Set<Long> users = new HashSet<>();
        if (members != null) {
            members.forEach(m -> users.add(Long.parseLong(m)));
        }
        return users;
    }

    private void fireConcurrently(IntFunction<Runnable> task) throws Exception {
        fireConcurrently(THREADS, task);
    }

    // 모든 스레드를 준비시킨 뒤 한 번에 출발시켜 락 경합을 만든다.
    private void fireConcurrently(int threads, IntFunction<Runnable> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                Runnable r = task.apply(i);
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    r.run();
                    return null;
                }));
            }
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private long notifyUserCount(long linkId) {
        Long size = redis.opsForSet().size(LinkStreamRedisKeys.notifyUsersKey(linkId));
        return size == null ? 0 : size;
    }

    private long streamLength() {
        Long size = redis.opsForStream().size(RedisStreamConfig.LINK_PROCESS_STREAM);
        return size == null ? 0 : size;
    }
}
