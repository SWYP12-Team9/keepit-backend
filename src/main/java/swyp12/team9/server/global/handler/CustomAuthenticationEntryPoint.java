package swyp12.team9.server.global.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import swyp12.team9.server.global.common.dto.ErrorResponse;
import swyp12.team9.server.global.exception.ErrorCode;

import java.io.IOException;

/**
 * 인증 실패 시 401 Unauthorized 에러를 ErrorCode 기반으로 처리하는 핸들러
 * - Spring Security의 AuthenticationEntryPoint 구현
 * - JWT 토큰이 없거나 유효하지 않을 때 호출됨
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 인증 실패 시 호출되는 메서드
     *
     * @param request       HTTP 요청
     * @param response      HTTP 응답
     * @param authException 인증 예외
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}