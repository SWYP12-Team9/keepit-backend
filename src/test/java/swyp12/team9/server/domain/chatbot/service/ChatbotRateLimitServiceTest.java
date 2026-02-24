package swyp12.team9.server.domain.chatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import swyp12.team9.server.domain.chatbot.exception.ChatbotRateLimitExceededException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ChatbotRateLimitService 단위 테스트
 * - Mock RedisTemplate 사용
 */
@DisplayName("ChatbotRateLimitService 테스트")
@ExtendWith(MockitoExtension.class)
class ChatbotRateLimitServiceTest {

    private ChatbotRateLimitService rateLimitService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    // 테스트용 인메모리 저장소
    private Map<String, Integer> redisStore;

    @BeforeEach
    void setUp() {
        // 인메모리 저장소 초기화
        redisStore = new HashMap<>();

        // RedisTemplate Mock 설정 (lenient로 설정하여 사용하지 않는 stubbing 허용)
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // increment() Mock: 실제 Redis INCR 동작 시뮬레이션
        lenient().when(valueOperations.increment(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Integer count = redisStore.getOrDefault(key, 0);
            count++;
            redisStore.put(key, count);
            return count.longValue();
        });

        // get() Mock: 저장소에서 값 조회
        lenient().when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisStore.get(key);
        });

        rateLimitService = new ChatbotRateLimitService(redisTemplate);
    }

    @Nested
    @DisplayName("요청 제한 확인")
    class CheckAndIncrementRequest {

        @Test
        @DisplayName("성공: 첫 요청 시 카운트가 증가하고 TTL이 설정된다")
        void success_FirstRequest() {
            // given
            Long userId = 1L;

            // when
            rateLimitService.checkAndIncrementRequest(userId);

            // then
            int remaining = rateLimitService.getRemainingRequests(userId);
            assertThat(remaining).isEqualTo(9); // 10 - 1

            // TTL 설정 확인 (첫 요청 시 expire 호출)
            verify(redisTemplate, times(1)).expire(anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("성공: 10번째 요청까지 허용된다")
        void success_TenthRequest() {
            // given
            Long userId = 2L;

            // when - 10번 요청
            for (int i = 0; i < 10; i++) {
                rateLimitService.checkAndIncrementRequest(userId);
            }

            // then
            int remaining = rateLimitService.getRemainingRequests(userId);
            assertThat(remaining).isEqualTo(0);
        }

        @Test
        @DisplayName("실패: 11번째 요청 시 예외가 발생한다")
        void fail_EleventhRequest() {
            // given
            Long userId = 3L;

            // when - 10번 요청
            for (int i = 0; i < 10; i++) {
                rateLimitService.checkAndIncrementRequest(userId);
            }

            // then - 11번째 요청 시 예외 발생
            assertThatThrownBy(() -> rateLimitService.checkAndIncrementRequest(userId))
                    .isInstanceOf(ChatbotRateLimitExceededException.class)
                    .hasMessageContaining("하루 챗봇 사용 횟수를 초과했습니다.");
        }

        @Test
        @DisplayName("성공: 다른 사용자는 독립적으로 카운트된다")
        void success_DifferentUsers() {
            // given
            Long user1 = 10L;
            Long user2 = 20L;

            // when
            rateLimitService.checkAndIncrementRequest(user1);
            rateLimitService.checkAndIncrementRequest(user1);
            rateLimitService.checkAndIncrementRequest(user2);

            // then
            assertThat(rateLimitService.getRemainingRequests(user1)).isEqualTo(8);
            assertThat(rateLimitService.getRemainingRequests(user2)).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("남은 요청 횟수 조회")
    class GetRemainingRequests {

        @Test
        @DisplayName("성공: 요청하지 않은 사용자는 10회가 남아있다")
        void success_NoRequest() {
            // given
            Long userId = 100L;

            // when
            int remaining = rateLimitService.getRemainingRequests(userId);

            // then
            assertThat(remaining).isEqualTo(10);
        }

        @Test
        @DisplayName("성공: 5번 요청한 사용자는 5회가 남아있다")
        void success_FiveRequests() {
            // given
            Long userId = 101L;

            // when - 5번 요청
            for (int i = 0; i < 5; i++) {
                rateLimitService.checkAndIncrementRequest(userId);
            }

            // then
            int remaining = rateLimitService.getRemainingRequests(userId);
            assertThat(remaining).isEqualTo(5);
        }

        @Test
        @DisplayName("성공: 10번 요청한 사용자는 0회가 남아있다")
        void success_TenRequests() {
            // given
            Long userId = 102L;

            // when - 10번 요청
            for (int i = 0; i < 10; i++) {
                rateLimitService.checkAndIncrementRequest(userId);
            }

            // then
            int remaining = rateLimitService.getRemainingRequests(userId);
            assertThat(remaining).isEqualTo(0);
        }
    }
}
