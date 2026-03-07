package swyp12.team9.server.domain.link.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkStreamProducer 단위 테스트")
class LinkStreamProducerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private LinkStreamProducer linkStreamProducer;

    @Nested
    @DisplayName("publishLinkProcessTask() 테스트")
    @SuppressWarnings("unchecked")
    class PublishLinkProcessTask {

        @Test
        @DisplayName("성공: 이전 작업이 없으면 스트림에 이벤트를 발행하고 분산락과 유저Set을 세팅한다")
        void success_PublishNewTask() {
            // given
            Long linkId = 1L;
            String url = "http://example.com";
            Long userId = 100L;

            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);
            
            // setIfAbsent가 true 반환 (락 획득 성공)
            given(valueOperations.setIfAbsent(eq("link:processing_lock:1"), eq("PROCESSING"), any(Duration.class)))
                    .willReturn(true);

            // when
            linkStreamProducer.publishLinkProcessTask(linkId, url, userId);

            // then
            // 1. 유저 Set 대기열 삽입 및 TTL 설정 검증
            verify(setOperations).add("link:notify_users:1", "100");
            verify(stringRedisTemplate).expire(eq("link:notify_users:1"), any(Duration.class));
            
            // 2. Stream 발행 검증
            verify(streamOperations).add(any(ObjectRecord.class));
        }

        @Test
        @DisplayName("성공: 동일한 URL(linkId) 작업이 이미 처리 중이면 락에 막혀 Stream 발행을 건너뛴다")
        void success_SkipPublish_WhenTaskIsAlreadyProcessing() {
            // given
            Long linkId = 1L;
            String url = "http://example.com";
            Long userId = 100L;

            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

            // setIfAbsent가 false 반환 (이미 누군가 락 획득함)
            given(valueOperations.setIfAbsent(eq("link:processing_lock:1"), eq("PROCESSING"), any(Duration.class)))
                    .willReturn(false);

            // when
            linkStreamProducer.publishLinkProcessTask(linkId, url, userId);

            // then
            // 1. 대기 유저 Set에는 그대로 추가되어야 함
            verify(setOperations).add("link:notify_users:1", "100");

            // 2. 하지만 중복 Stream 이벤트 발행은 막혀야 함
            verify(stringRedisTemplate, never()).opsForStream();
        }
    }

    @Nested
    @DisplayName("concurrent 동시성 테스트")
    @SuppressWarnings("unchecked")
    class Concurrent {

        @Test
        @DisplayName("성공: 동일 linkId로 10개 스레드가 동시 요청해도 Stream에는 1번만 발행되고 모든 userId가 대기 Set에 추가된다")
        void concurrent_SameUrl_OnlyOneStreamPublish_AllUsersAddedToSet() throws InterruptedException {
            // given
            Long linkId = 1L;
            String url = "https://example.com";
            int threadCount = 10;

            given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
            given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
            given(stringRedisTemplate.opsForStream()).willReturn(streamOperations);

            // 실제 Redis SETNX처럼 첫 번째 호출만 true, 나머지는 false 반환
            AtomicBoolean firstCall = new AtomicBoolean(true);
            given(valueOperations.setIfAbsent(eq("link:processing_lock:" + linkId), eq("PROCESSING"), any(Duration.class)))
                    .willAnswer(inv -> firstCall.compareAndSet(true, false));

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            // when - 10개 스레드가 동시에 서로 다른 userId로 같은 URL 요청
            for (int i = 0; i < threadCount; i++) {
                final Long userId = (long) (i + 1);
                new Thread(() -> {
                    try {
                        startLatch.await(); // 모든 스레드 동시 시작
                        linkStreamProducer.publishLinkProcessTask(linkId, url, userId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown(); // 일제히 시작
            boolean completed = doneLatch.await(5, TimeUnit.SECONDS);

            // then
            assertThat(completed).isTrue();

            // Stream에는 정확히 1번만 발행되어야 함 (중복 방지)
            verify(streamOperations, times(1)).add(any(ObjectRecord.class));

            // 10명 모두 대기 Set에 추가되어야 함 (알림 대상 누락 방지)
            for (int i = 1; i <= threadCount; i++) {
                verify(setOperations).add(eq("link:notify_users:" + linkId), eq(String.valueOf(i)));
            }
        }
    }
}
