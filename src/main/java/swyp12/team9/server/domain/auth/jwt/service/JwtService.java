package swyp12.team9.server.domain.auth.jwt.service;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.auth.dto.TokenRefreshRequest;
import swyp12.team9.server.domain.auth.dto.TokenResponse;
import swyp12.team9.server.domain.auth.jwt.infrastructure.RefreshTokenStore;
import swyp12.team9.server.global.util.JwtUtil;

/**
 * Refresh 토큰 발급/회전/폐기 서비스
 *
 * <p>refresh 토큰은 Redis 화이트리스트({@link RefreshTokenStore})로 관리한다.
 * 서명이 유효하더라도 화이트리스트에 없으면 거부되므로, 로그아웃 시 즉시 무효화가 가능하다.
 * 만료 처리는 Redis TTL이 담당해 별도의 정리 배치가 필요 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final RefreshTokenStore refreshTokenStore;

    // 소셜 로그인 성공 후 쿠키(Refresh) -> 헤더 방식으로 응답
    public TokenResponse cookie2Header(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        // 쿠키 리스트
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new RuntimeException("쿠키가 존재하지 않습니다.");
        }

        // Refresh 토큰 획득
        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            throw new RuntimeException("refreshToken 쿠키가 없습니다.");
        }

        // Refresh 토큰 검증
        Boolean isValid = JwtUtil.isValid(refreshToken, false);
        if (!isValid) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        TokenResponse tokenResponse = rotate(refreshToken);

        // 기존 쿠키 제거
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10);
        response.addCookie(refreshCookie);

        return tokenResponse;
    }

    // Refresh 토큰으로 Access 토큰 재발급 로직 (Rotate 포함)
    public TokenResponse refreshRotate(TokenRefreshRequest request) {

        String refreshToken = request.getRefreshToken();

        // Refresh 토큰 검증
        Boolean isValid = JwtUtil.isValid(refreshToken, false);
        if (!isValid) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        // 화이트리스트 확인: 로그아웃되었거나 이미 회전된 토큰은 서명이 유효해도 거부
        if (!existsRefresh(refreshToken)) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        return rotate(refreshToken);
    }

    // JWT Refresh 토큰 발급 후 저장 (기존 토큰 삭제 없이 새 토큰 추가)
    // 로그인할 때마다 세션이 하나씩 추가되어 기기별 다중 세션을 유지한다.
    public void addRefresh(String username, String refreshToken) {
        refreshTokenStore.save(username, refreshToken, JwtUtil.getRemainingValidity(refreshToken));
    }

    // JWT Refresh 존재 확인
    public Boolean existsRefresh(String refreshToken) {
        return refreshTokenStore.exists(refreshToken);
    }

    // JWT Refresh 토큰 삭제 (로그아웃 시 즉시 무효화)
    public void removeRefresh(String refreshToken) {
        try {
            refreshTokenStore.remove(JwtUtil.getUsername(refreshToken), refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            // 서명이 깨졌거나 이미 만료된 토큰은 화이트리스트에도 남아있지 않으므로 무시한다
            log.debug("[refresh 세션] 폐기 대상 토큰을 파싱할 수 없어 건너뜀: {}", e.getMessage());
        }
    }

    // 특정 유저 Refresh 토큰 모두 삭제 (탈퇴)
    public void removeRefreshUser(String username) {
        long revoked = refreshTokenStore.removeAllByUsername(username);
        log.info("[refresh 세션] 유저 전체 세션 폐기 - username: {}, 폐기 세션 수: {}", username, revoked);
    }

    // 활성 세션 수 조회 (세션 단위 제어/모니터링용)
    public long countActiveSessions(String username) {
        return refreshTokenStore.countActiveSessions(username);
    }

    // 이전 세션을 폐기하고 새 토큰 쌍을 발급 (Refresh Token Rotation)
    private TokenResponse rotate(String refreshToken) {
        Long userId = JwtUtil.getUserId(refreshToken);
        String username = JwtUtil.getUsername(refreshToken);
        String role = JwtUtil.getRole(refreshToken);

        String newAccessToken = JwtUtil.createJWT(userId, username, role, true);
        String newRefreshToken = JwtUtil.createJWT(userId, username, role, false);

        // 재사용을 막기 위해 새 세션을 등록하기 전에 기존 세션을 먼저 폐기
        refreshTokenStore.remove(username, refreshToken);
        refreshTokenStore.save(username, newRefreshToken, JwtUtil.getRemainingValidity(newRefreshToken));

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

}
