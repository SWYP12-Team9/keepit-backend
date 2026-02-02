package swyp12.team9.server.global.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import swyp12.team9.server.global.common.dto.ErrorResponse;
import swyp12.team9.server.global.exception.ErrorCode;

import java.io.IOException;

/**
 * 인가 실패 시 403 Forbidden 에러를 ErrorCode 기반으로 처리하는 핸들러
 * - Spring Security의 AccessDeniedHandler 구현
 * - 인증은 되었지만 권한이 없을 때 호출됨
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 접근 거부 시 호출되는 메서드
     *
     * @param request               HTTP 요청
     * @param response              HTTP 응답
     * @param accessDeniedException 접근 거부 예외
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.ACCESS_DENIED);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}