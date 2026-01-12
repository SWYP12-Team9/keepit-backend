package swyp12.team9.server.domain.jwt.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.jwt.dto.JwtResponse;
import swyp12.team9.server.domain.jwt.dto.RefreshRequest;
import swyp12.team9.server.domain.jwt.model.JwtRefresh;
import swyp12.team9.server.domain.jwt.repository.RefreshRepository;
import swyp12.team9.server.global.util.JwtUtil;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final RefreshRepository refreshRepository;

    // 소셜 로그인 성공 후 쿠키(Refresh) -> 헤더 방식으로 응답
    @Transactional
    public JwtResponse cookie2Header(
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

        // 정보 추출
        String username = JwtUtil.getUsername(refreshToken);
        String role = JwtUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccessToken = JwtUtil.createJWT(username, role, true);
        String newRefreshToken = JwtUtil.createJWT(username, role, false);

        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        JwtRefresh newRefresh = JwtRefresh.builder()
                .username(username)
                .refresh(newRefreshToken)
                .build();

        removeRefresh(refreshToken);
        refreshRepository.flush(); // 같은 트랜잭션 내부라 : 삭제 -> 생성 문제 해결
        refreshRepository.save(newRefresh);

        // 기존 쿠키 제거
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10);
        response.addCookie(refreshCookie);

        return new JwtResponse(newAccessToken, newRefreshToken);
    }

    // Refresh 토큰으로 Access 토큰 재발급 로직 (Rotate 포함)
    @Transactional
    public JwtResponse refreshRotate(RefreshRequest request) {

        String refreshToken = request.getRefreshToken();

        // Refresh 토큰 검증
        Boolean isValid = JwtUtil.isValid(refreshToken, false);
        if (!isValid) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        // JwtRefresh 엔티티 존재 확인 (화이트리스트)
        if (!existsRefresh(refreshToken)) {
            throw new RuntimeException("유효하지 않은 refreshToken입니다.");
        }

        // 정보 추출
        String username = JwtUtil.getUsername(refreshToken);
        String role = JwtUtil.getRole(refreshToken);

        // 토큰 생성
        String newAccessToken = JwtUtil.createJWT(username, role, true);
        String newRefreshToken = JwtUtil.createJWT(username, role, false);

        // 기존 Refresh 토큰 DB 삭제 후 신규 추가
        JwtRefresh newRefresh = JwtRefresh.builder()
                .username(username)
                .refresh(newRefreshToken)
                .build();

        removeRefresh(refreshToken);
        refreshRepository.save(newRefresh);

        return new JwtResponse(newAccessToken, newRefreshToken);
    }

    // JWT Refresh 토큰 발급 후 저장
    @Transactional
    public void addRefresh(String username, String refreshToken) {
        JwtRefresh newRefresh = JwtRefresh.builder()
                .username(username)
                .refresh(refreshToken)
                .build();

        refreshRepository.save(newRefresh);
    }

    // JWT Refresh 존재 확인
    @Transactional(readOnly = true)
    public Boolean existsRefresh(String refreshToken) {
        return refreshRepository.existsByRefresh(refreshToken);
    }

    // JWT Refresh 토큰 삭제
    @Transactional
    public void removeRefresh(String refreshToken) {
        refreshRepository.deleteByRefresh(refreshToken);
    }

    // 특정 유저 Refresh 토큰 모두 삭제 (탈퇴)
    @Transactional
    public void removeRefreshUser(String username) {
        refreshRepository.deleteByUsername(username);
    }

}