package swyp12.team9.server.global.handler;

import jakarta.servlet.ServletException;
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

// 자체 로그인 성공 핸들러
@Component
@Qualifier("LoginSuccessHandler")
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public LoginSuccessHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // username, role
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // userId 조회
        User user = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Long userId = user.getId();

        // JWT(Access/Refresh) 발급 - userId 포함
        String accessToken = JwtUtil.createJWT(userId, username, role, true);
        String refreshToken = JwtUtil.createJWT(userId, username, role, false);

        // 발급한 Refresh DB 테이블 저장 (Refresh whitelist)
        jwtService.addRefresh(username, refreshToken);

        // 응답
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format("{\"accessToken\":\"%s\", \"refreshToken\":\"%s\"}", accessToken, refreshToken);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

}
