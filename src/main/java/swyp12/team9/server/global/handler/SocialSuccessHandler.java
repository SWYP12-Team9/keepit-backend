package swyp12.team9.server.global.handler;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.jwt.service.JwtService;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.global.util.JwtUtil;

import java.io.IOException;

@Component
@Qualifier("SocialSuccessHandler")
public class SocialSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public SocialSuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // username, role
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // userId 조회
        User user = userRepository.findByUsernameAndIsSocial(username, true)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Long userId = user.getId();

        // JWT(Refresh) 발급 - userId 포함
        String refreshToken = JwtUtil.createJWT(userId, username, "ROLE_" + role, false);

        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        jwtService.addRefresh(username, refreshToken);

        // 응답
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10); // 10초 (프론트에서 발급 후 바로 헤더 전환 로직 진행 예정)

        response.addCookie(refreshCookie);
        response.sendRedirect("http://localhost:5173/cookie");
    }

}