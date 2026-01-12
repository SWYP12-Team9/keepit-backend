package swyp12.team9.server.global.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;
import swyp12.team9.server.domain.jwt.service.JwtService;
import swyp12.team9.server.global.util.JwtUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// 로그아웃 핸들러
public class RefreshTokenLogoutHandler implements LogoutHandler {

    private final JwtService jwtService;

    public RefreshTokenLogoutHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
                    .lines().reduce("", String::concat);

            if (!StringUtils.hasText(body)) return;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(body);
            String refreshToken = jsonNode.has("refreshToken") ? jsonNode.get("refreshToken").asText() : null;

            // 유효성 검증
            if (refreshToken == null) {
                return;
            }
            Boolean isValid = JwtUtil.isValid(refreshToken, false);
            if (!isValid) {
                return;
            }

            // Refresh 토큰 삭제
            jwtService.removeRefresh(refreshToken);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read refresh token", e);
        }
    }

}

//로그아웃 설정 : 핸들러 추가
//로그아웃시, 발급한 Refresh 토큰을 받아 백엔드에서는 무효화 처리를 진행해야 한다.