package swyp12.team9.server.domain.auth.jwt.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * RefreshTokenStore 단위 테스트
 * - Mock StringRedisTemplate 위에 인메모리 저장소를 얹어 Redis 동작을 흉내낸다
 * - removeAllByUsername의 Lua 스크립트는 동일한 의미(SMEMBERS -> 개별 DEL -> 인덱스 DEL)로 시뮬레이션한다
 */
@DisplayName("RefreshTokenStore 테스트")
@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    private static final Duration TTL = Duration.ofDays(28);

    private RefreshTokenStore refreshTokenStore;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    // 인메모리 Redis 대체 저장소
    private Map<String, String> values;
    private Map<String, Set<String>> sets;

    @BeforeEach
    void setUp() {
        values = new HashMap<>();
        sets = new HashMap<>();

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);

        // SET key value EX ttl
        lenient().doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // EXISTS key
        lenient().when(stringRedisTemplate.hasKey(anyString()))
                .thenAnswer(invocation -> values.containsKey(invocation.getArgument(0)));

        // DEL key
        lenient().when(stringRedisTemplate.delete(anyString()))
                .thenAnswer(invocation -> values.remove(invocation.getArgument(0)) != null);

        // EXPIRE key ttl
        lenient().when(stringRedisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        // SADD key member
        lenient().when(setOperations.add(anyString(), any(String[].class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Set<String> set = sets.computeIfAbsent(key, k -> new LinkedHashSet<>());
            return varargsOf(invocation).stream().filter(set::add).count();
        });

        // SREM key member
        lenient().when(setOperations.remove(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            Set<String> set = sets.get(invocation.<String>getArgument(0));
            if (set == null) {
                return 0L;
            }
            return varargsOf(invocation).stream().filter(set::remove).count();
        });

        // SMEMBERS key
        lenient().when(setOperations.members(anyString()))
                .thenAnswer(invocation -> {
                    Set<String> set = sets.get(invocation.getArgument(0));
                    return set == null ? null : new HashSet<>(set);
                });

        // EVAL: removeAllByUsername의 Lua 스크립트 시뮬레이션
        lenient().when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(1);

                    String userSessionsKey = keys.get(0);
                    String sessionKeyPrefix = scriptArgsOf(invocation).get(0);

                    Set<String> tokenHashes = sets.get(userSessionsKey);
                    if (tokenHashes == null) {
                        return 0L;
                    }

                    List<String> targets = new ArrayList<>(tokenHashes);
                    targets.forEach(tokenHash -> values.remove(sessionKeyPrefix + tokenHash));
                    sets.remove(userSessionsKey);

                    return (long) targets.size();
                });

        refreshTokenStore = new RefreshTokenStore(stringRedisTemplate);
    }

    // execute(script, keys, args...)의 세 번째 이후 인자
    private static List<String> scriptArgsOf(InvocationOnMock invocation) {
        return flattenFrom(invocation, 2);
    }

    // Mockito는 varargs를 호출 형태에 따라 개별 인자 또는 배열로 넘겨주므로 양쪽을 모두 평탄화한다
    private static List<String> varargsOf(InvocationOnMock invocation) {
        return flattenFrom(invocation, 1);
    }

    private static List<String> flattenFrom(InvocationOnMock invocation, int startIndex) {
        Object[] arguments = invocation.getArguments();
        List<String> members = new ArrayList<>();

        for (int i = startIndex; i < arguments.length; i++) {
            Object argument = arguments[i];
            if (argument instanceof Object[] array) {
                for (Object element : array) {
                    members.add(String.valueOf(element));
                }
            } else {
                members.add(String.valueOf(argument));
            }
        }

        return members;
    }

    @Nested
    @DisplayName("세션 저장 및 조회")
    class SaveAndExists {

        @Test
        @DisplayName("성공: 저장한 토큰은 화이트리스트에 존재한다")
        void success_SaveThenExists() {
            // given
            String refreshToken = "refresh-token-a";

            // when
            refreshTokenStore.save("user1", refreshToken, TTL);

            // then
            assertThat(refreshTokenStore.exists(refreshToken)).isTrue();
        }

        @Test
        @DisplayName("성공: 저장하지 않은 토큰은 화이트리스트에 없다")
        void success_NotSaved() {
            // when & then
            assertThat(refreshTokenStore.exists("unknown-token")).isFalse();
        }

        @Test
        @DisplayName("성공: 토큰 원문이 아니라 SHA-256 해시가 키로 저장된다")
        void success_TokenIsHashed() {
            // given
            String refreshToken = "refresh-token-a";

            // when
            refreshTokenStore.save("user1", refreshToken, TTL);

            // then - 저장된 키 어디에도 토큰 원문이 노출되지 않는다
            assertThat(values.keySet()).noneMatch(key -> key.contains(refreshToken));
            assertThat(values).hasSize(1);
        }

        @Test
        @DisplayName("성공: 이미 만료된 토큰은 저장하지 않는다")
        void success_ExpiredTokenNotSaved() {
            // given
            String refreshToken = "expired-token";

            // when
            refreshTokenStore.save("user1", refreshToken, Duration.ZERO);

            // then - TTL 없는 영구 키가 남지 않아야 한다
            assertThat(refreshTokenStore.exists(refreshToken)).isFalse();
            assertThat(values).isEmpty();
        }

        @Test
        @DisplayName("성공: 같은 유저가 여러 기기에서 로그인하면 세션이 각각 유지된다")
        void success_MultipleSessions() {
            // when
            refreshTokenStore.save("user1", "token-phone", TTL);
            refreshTokenStore.save("user1", "token-desktop", TTL);

            // then
            assertThat(refreshTokenStore.exists("token-phone")).isTrue();
            assertThat(refreshTokenStore.exists("token-desktop")).isTrue();
            assertThat(refreshTokenStore.countActiveSessions("user1")).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("세션 1건 폐기")
    class Remove {

        @Test
        @DisplayName("성공: 폐기한 토큰은 즉시 무효화된다")
        void success_RemoveImmediately() {
            // given
            refreshTokenStore.save("user1", "token-a", TTL);

            // when
            refreshTokenStore.remove("user1", "token-a");

            // then
            assertThat(refreshTokenStore.exists("token-a")).isFalse();
        }

        @Test
        @DisplayName("성공: 한 기기를 로그아웃해도 다른 기기 세션은 유지된다")
        void success_OtherSessionSurvives() {
            // given
            refreshTokenStore.save("user1", "token-phone", TTL);
            refreshTokenStore.save("user1", "token-desktop", TTL);

            // when
            refreshTokenStore.remove("user1", "token-phone");

            // then
            assertThat(refreshTokenStore.exists("token-phone")).isFalse();
            assertThat(refreshTokenStore.exists("token-desktop")).isTrue();
            assertThat(refreshTokenStore.countActiveSessions("user1")).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: 폐기 시 세션 인덱스에서도 제거된다")
        void success_IndexCleaned() {
            // given
            refreshTokenStore.save("user1", "token-a", TTL);

            // when
            refreshTokenStore.remove("user1", "token-a");

            // then
            assertThat(sets.get(RefreshTokenRedisKeys.userSessionsKey("user1"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("유저 전체 세션 폐기")
    class RemoveAllByUsername {

        @Test
        @DisplayName("성공: 유저의 모든 세션이 한 번에 폐기된다")
        void success_RevokeAll() {
            // given
            refreshTokenStore.save("user1", "token-phone", TTL);
            refreshTokenStore.save("user1", "token-desktop", TTL);

            // when
            long revoked = refreshTokenStore.removeAllByUsername("user1");

            // then
            assertThat(revoked).isEqualTo(2);
            assertThat(refreshTokenStore.exists("token-phone")).isFalse();
            assertThat(refreshTokenStore.exists("token-desktop")).isFalse();
            assertThat(refreshTokenStore.countActiveSessions("user1")).isZero();
        }

        @Test
        @DisplayName("성공: 다른 유저의 세션은 영향받지 않는다")
        void success_OtherUserUnaffected() {
            // given
            refreshTokenStore.save("user1", "token-user1", TTL);
            refreshTokenStore.save("user2", "token-user2", TTL);

            // when
            refreshTokenStore.removeAllByUsername("user1");

            // then
            assertThat(refreshTokenStore.exists("token-user1")).isFalse();
            assertThat(refreshTokenStore.exists("token-user2")).isTrue();
        }

        @Test
        @DisplayName("성공: 세션이 없는 유저를 폐기해도 예외가 발생하지 않는다")
        void success_NoSession() {
            // when
            long revoked = refreshTokenStore.removeAllByUsername("ghost");

            // then
            assertThat(revoked).isZero();
        }
    }

    @Nested
    @DisplayName("활성 세션 수 조회")
    class CountActiveSessions {

        @Test
        @DisplayName("성공: TTL로 만료된 세션은 인덱스에 남아 있어도 세지 않는다")
        void success_ExcludeExpiredSession() {
            // given
            refreshTokenStore.save("user1", "token-alive", TTL);

            // 세션 키만 TTL로 사라지고 인덱스에는 해시가 남은 고아 상태를 흉내낸다
            sets.get(RefreshTokenRedisKeys.userSessionsKey("user1")).add("orphan-token-hash");

            // when & then
            assertThat(refreshTokenStore.countActiveSessions("user1")).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: 세션이 없으면 0을 반환한다")
        void success_NoSession() {
            assertThat(refreshTokenStore.countActiveSessions("ghost")).isZero();
        }
    }
}
