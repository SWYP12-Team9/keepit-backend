package swyp12.team9.server.domain.auth.jwt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.auth.dto.TokenRefreshRequest;
import swyp12.team9.server.domain.auth.dto.TokenResponse;
import swyp12.team9.server.domain.auth.jwt.infrastructure.RefreshTokenStore;
import swyp12.team9.server.global.util.JwtUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtService 테스트
 * - RefreshTokenStore를 인메모리 Fake로 대체해 rotate/로그아웃 시 화이트리스트 상태를 검증한다
 */
@DisplayName("JwtService 테스트")
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "tester";
    private static final String ROLE = "ROLE_USER";

    private FakeRefreshTokenStore refreshTokenStore;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        refreshTokenStore = new FakeRefreshTokenStore();
        jwtService = new JwtService(refreshTokenStore);
    }

    private String issueRefreshToken() {
        return JwtUtil.createJWT(USER_ID, USERNAME, ROLE, false);
    }

    private TokenRefreshRequest refreshRequest(String refreshToken) {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(refreshToken);
        return request;
    }

    @Nested
    @DisplayName("Refresh 토큰 발급")
    class AddRefresh {

        @Test
        @DisplayName("성공: 발급한 토큰이 화이트리스트에 등록된다")
        void success_Add() {
            // given
            String refreshToken = issueRefreshToken();

            // when
            jwtService.addRefresh(USERNAME, refreshToken);

            // then
            assertThat(jwtService.existsRefresh(refreshToken)).isTrue();
        }

        @Test
        @DisplayName("성공: TTL이 토큰의 잔여 유효기간으로 설정된다")
        void success_TtlFollowsTokenExpiry() {
            // given
            String refreshToken = issueRefreshToken();

            // when
            jwtService.addRefresh(USERNAME, refreshToken);

            // then - 28일(7일 * 4) 만료 토큰이므로 TTL도 27일보다 크고 28일 이하여야 한다
            Duration ttl = refreshTokenStore.ttlOf(refreshToken);
            assertThat(ttl).isBetween(Duration.ofDays(27), Duration.ofDays(28));
        }
    }

    @Nested
    @DisplayName("Refresh 토큰 회전(Rotate)")
    class RefreshRotate {

        @Test
        @DisplayName("성공: 새 토큰 쌍이 발급되고 이전 토큰은 즉시 폐기된다")
        void success_Rotate() {
            // given
            String oldToken = issueRefreshToken();
            jwtService.addRefresh(USERNAME, oldToken);

            // when
            TokenResponse response = jwtService.refreshRotate(refreshRequest(oldToken));

            // then
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(jwtService.existsRefresh(oldToken)).isFalse();
            assertThat(jwtService.existsRefresh(response.refreshToken())).isTrue();
        }

        @Test
        @DisplayName("실패: 화이트리스트에 없는 토큰은 서명이 유효해도 거부된다")
        void fail_NotWhitelisted() {
            // given - 서명은 정상이지만 저장소에 등록되지 않은 토큰
            String refreshToken = issueRefreshToken();

            // when & then
            assertThatThrownBy(() -> jwtService.refreshRotate(refreshRequest(refreshToken)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않은 refreshToken입니다.");
        }

        @Test
        @DisplayName("실패: 이미 회전된 토큰으로 재요청하면 거부된다 (재사용 차단)")
        void fail_ReuseRotatedToken() {
            // given
            String oldToken = issueRefreshToken();
            jwtService.addRefresh(USERNAME, oldToken);
            jwtService.refreshRotate(refreshRequest(oldToken));

            // when & then - 탈취된 이전 토큰으로 재발급 시도
            assertThatThrownBy(() -> jwtService.refreshRotate(refreshRequest(oldToken)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않은 refreshToken입니다.");
        }

        @Test
        @DisplayName("실패: 위조된 토큰은 거부된다")
        void fail_InvalidSignature() {
            // when & then
            assertThatThrownBy(() -> jwtService.refreshRotate(refreshRequest("tampered.token.value")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않은 refreshToken입니다.");
        }

        @Test
        @DisplayName("실패: access 토큰으로는 재발급할 수 없다")
        void fail_AccessTokenRejected() {
            // given
            String accessToken = JwtUtil.createJWT(USER_ID, USERNAME, ROLE, true);

            // when & then
            assertThatThrownBy(() -> jwtService.refreshRotate(refreshRequest(accessToken)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효하지 않은 refreshToken입니다.");
        }
    }

    @Nested
    @DisplayName("로그아웃 / 탈퇴")
    class Remove {

        @Test
        @DisplayName("성공: 로그아웃한 토큰은 즉시 무효화된다")
        void success_LogoutInvalidatesImmediately() {
            // given
            String refreshToken = issueRefreshToken();
            jwtService.addRefresh(USERNAME, refreshToken);

            // when
            jwtService.removeRefresh(refreshToken);

            // then
            assertThat(jwtService.existsRefresh(refreshToken)).isFalse();
        }

        @Test
        @DisplayName("성공: 파싱할 수 없는 토큰으로 로그아웃해도 예외가 전파되지 않는다")
        void success_LogoutWithBrokenToken() {
            // when & then
            jwtService.removeRefresh("not-a-jwt");
        }

        @Test
        @DisplayName("성공: 탈퇴 시 해당 유저의 모든 세션이 폐기된다")
        void success_RemoveAllSessionsOnWithdrawal() {
            // given - 두 기기에서 로그인한 상태
            String phoneToken = issueRefreshToken();
            String desktopToken = "desktop-refresh-token";
            jwtService.addRefresh(USERNAME, phoneToken);
            refreshTokenStore.save(USERNAME, desktopToken, Duration.ofDays(28));

            // when
            jwtService.removeRefreshUser(USERNAME);

            // then
            assertThat(jwtService.existsRefresh(phoneToken)).isFalse();
            assertThat(refreshTokenStore.exists(desktopToken)).isFalse();
            assertThat(jwtService.countActiveSessions(USERNAME)).isZero();
        }
    }

    /**
     * Redis 대신 메모리에서 동작하는 RefreshTokenStore 대역
     * - 토큰별 TTL과 유저별 세션 인덱스를 실제 구현과 동일한 규칙으로 관리한다
     */
    private static class FakeRefreshTokenStore extends RefreshTokenStore {

        private final Map<String, Duration> ttlByToken = new HashMap<>();
        private final Map<String, Set<String>> tokensByUsername = new HashMap<>();

        FakeRefreshTokenStore() {
            super(null);
        }

        @Override
        public void save(String username, String refreshToken, Duration ttl) {
            if (ttl.isZero() || ttl.isNegative()) {
                return;
            }
            ttlByToken.put(refreshToken, ttl);
            tokensByUsername.computeIfAbsent(username, key -> new HashSet<>()).add(refreshToken);
        }

        @Override
        public boolean exists(String refreshToken) {
            return ttlByToken.containsKey(refreshToken);
        }

        @Override
        public void remove(String username, String refreshToken) {
            ttlByToken.remove(refreshToken);
            Set<String> tokens = tokensByUsername.get(username);
            if (tokens != null) {
                tokens.remove(refreshToken);
            }
        }

        @Override
        public long removeAllByUsername(String username) {
            Set<String> tokens = tokensByUsername.remove(username);
            if (tokens == null) {
                return 0L;
            }
            tokens.forEach(ttlByToken::remove);
            return tokens.size();
        }

        @Override
        public long countActiveSessions(String username) {
            Set<String> tokens = tokensByUsername.get(username);
            if (tokens == null) {
                return 0L;
            }
            return tokens.stream().filter(ttlByToken::containsKey).count();
        }

        Duration ttlOf(String refreshToken) {
            return ttlByToken.get(refreshToken);
        }
    }
}
