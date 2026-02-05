package swyp12.team9.server.global.handler;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.jwt.service.JwtService;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.global.security.CustomOAuth2User;
import swyp12.team9.server.global.util.JwtUtil;

// 소셜 로그인 성공 핸들러
@Slf4j
@Component
@Qualifier("SocialSuccessHandler")
public class SocialSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String frontendUrl;

    // 환경변수/설정으로 관리
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite; // "Lax" or "None" or "Strict"
    private final int refreshCookieMaxAgeSeconds;

    public SocialSuccessHandler(JwtService jwtService, UserRepository userRepository,
                                @Value("${frontend.url}") String frontendUrl,
                                @Value("${spring.security.cookie.refresh.secure}") boolean refreshCookieSecure,
                                @Value("${spring.security.cookie.refresh.same-site}") String refreshCookieSameSite,
                                @Value("${spring.security.cookie.refresh.max-age-seconds}") int refreshCookieMaxAgeSeconds) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshCookieMaxAgeSeconds = refreshCookieMaxAgeSeconds;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // CustomOAuth2User에서 직접 정보 추출 (DB 조회 불필요)
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = oAuth2User.getUserId();
        String username = oAuth2User.getUsername();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // DB에서 User 조회하여 상태 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // JWT(Refresh) 발급 - userId 포함
        // role은 이미 "ROLE_USER" 형태이므로 prefix 추가하지 않음
        String refreshToken = JwtUtil.createJWT(userId, username, role, false);

        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        jwtService.addRefresh(username, refreshToken);

        // 응답
        // SameSite + Secure + HttpOnly
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure) // 프로덕션에서는 true
                .path("/").maxAge(refreshCookieMaxAgeSeconds) // 60초 (프론트에서 발급 후 바로 헤더 전환 로직 진행 예정)
                .sameSite(refreshCookieSameSite) // "Lax", "Strict", "None"
                // .domain("example.com") // 필요할 때만 (서브도메인 공유 등)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // UserStatus에 따라 리다이렉트 URL 분기
        // 모두 /cookie 페이지로 이동, 신규 사용자는 isNewUser 파라미터로 구분
        String redirectUrl;
        if (user.getStatus() == UserStatus.PENDING) {
            // 프로필 미완성 → 쿠키 전환 + 신규 사용자 표시
            redirectUrl = frontendUrl + "/cookie?isNewUser=true";
            log.info("소셜 로그인을 통한 회원가입 진행 중 - 프로필 입력 필요 (userId: {}, status: PENDING)", userId);
        } else {
            // 프로필 완성 → 쿠키 전환 페이지로 (기존 로직)
            redirectUrl = frontendUrl + "/cookie";
            log.info("소셜 로그인 성공 - 프로필 완성됨 (userId: {}, status: {})", userId, user.getStatus());
        }

        response.sendRedirect(redirectUrl);
    }

}