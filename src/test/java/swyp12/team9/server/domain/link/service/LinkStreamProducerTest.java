package swyp12.team9.server.domain.link.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.RecordId;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkStreamProducer 단위 테스트")
class LinkStreamProducerTest {

    @Mock
    private LinkProcessStreamGateway linkProcessStreamGateway;

    @InjectMocks
    private LinkStreamProducer linkStreamProducer;

    @Nested
    @DisplayName("publishLinkProcessTask() 테스트")
    class PublishLinkProcessTask {

        @Test
        @DisplayName("성공: 이전 작업이 없으면 스트림에 이벤트를 발행하고 분산락과 유저Set을 세팅한다")
        void success_PublishNewTask() {
            // given
            Long linkId = 1L;
            String url = "http://example.com";
            Long userId = 100L;

            given(linkProcessStreamGateway.acquireProcessingLock(eq(linkId), any(Duration.class))).willReturn(true);
            given(linkProcessStreamGateway.add(anyString())).willReturn(RecordId.of("1-0"));

            // when
            linkStreamProducer.publishLinkProcessTask(linkId, url, userId);

            // then
            verify(linkProcessStreamGateway).addTargetUser(eq(linkId), eq(userId), any(Duration.class));
            verify(linkProcessStreamGateway).acquireProcessingLock(eq(linkId), any(Duration.class));
            verify(linkProcessStreamGateway).add(eq(linkId + "|" + url));
            verify(linkProcessStreamGateway).backupMessageMetadata(anyString(), eq(linkId), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("성공: 동일한 URL(linkId) 작업이 이미 처리 중이면 락에 막혀 Stream 발행을 건너뛴다")
        void success_SkipPublishWhenTaskIsAlreadyProcessing() {
            // given
            Long linkId = 1L;
            String url = "http://example.com";
            Long userId = 100L;

            given(linkProcessStreamGateway.acquireProcessingLock(eq(linkId), any(Duration.class))).willReturn(false);

            // when
            linkStreamProducer.publishLinkProcessTask(linkId, url, userId);

            // then
            verify(linkProcessStreamGateway).addTargetUser(eq(linkId), eq(userId), any(Duration.class));
            verify(linkProcessStreamGateway, never()).add(anyString());
            verify(linkProcessStreamGateway).extendProcessingLock(eq(linkId), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("publishLinkProcessTask() 동시성 테스트")
    class PublishLinkProcessTaskConcurrent {

        @Test
        @DisplayName("성공: 동일 linkId로 10개 스레드가 동시 요청해도 Stream에는 1번만 발행되고 모든 userId가 대기 Set에 추가된다")
        void success_OnlyOneStreamPublishWhenConcurrentSameUrlRequests() throws InterruptedException {
            // given
            Long linkId = 1L;
            String url = "https://example.com";
            int threadCount = 10;

            // 실제 Redis SETNX처럼 첫 번째 호출만 true, 나머지는 false 반환
            AtomicBoolean firstCall = new AtomicBoolean(true);
            given(linkProcessStreamGateway.acquireProcessingLock(eq(linkId), any(Duration.class)))
                    .willAnswer(inv -> firstCall.compareAndSet(true, false));
            given(linkProcessStreamGateway.add(anyString())).willReturn(RecordId.of("1-0"));

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
            verify(linkProcessStreamGateway, times(1)).add(anyString());

            // 10명 모두 대기 Set에 추가되어야 함 (알림 대상 누락 방지)
            for (int i = 1; i <= threadCount; i++) {
                verify(linkProcessStreamGateway).addTargetUser(eq(linkId), eq((long) i), any(Duration.class));
            }
        }
    }
}
